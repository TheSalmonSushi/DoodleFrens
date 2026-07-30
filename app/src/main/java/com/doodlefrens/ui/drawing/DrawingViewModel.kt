package com.doodlefrens.ui.drawing

import android.content.Context
import android.view.MotionEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.ink.strokes.Stroke
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doodlefrens.R
import com.doodlefrens.data.remote.ws.DrawingApi
import com.doodlefrens.data.remote.ws.Room
import com.doodlefrens.data.remote.ws.models.*
import com.doodlefrens.data.remote.ws.models.DrawAction.Companion.ACTION_UNDO
import com.doodlefrens.ui.state.DrawingUiState
import com.doodlefrens.ui.state.RemotePathData
import com.doodlefrens.util.CoroutineTimer
import com.doodlefrens.util.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class DrawingViewModel @Inject constructor(
    private val drawingApi: DrawingApi,
    private val dispatchers: DispatcherProvider,
    private val json: Json,
    @param:Named("clientId") private val clientId: String,
    @param:ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    sealed class SocketEvent {
        data class ChatMessageEvent(val data: ChatMessage) : SocketEvent()
        data class AnnouncementEvent(val data: Announcement) : SocketEvent()
        data class GameStateEvent(val data: GameState) : SocketEvent()
        data class NewWordsEvent(val data: NewWords) : SocketEvent()
        data class ChosenWordEvent(val data: ChosenWord) : SocketEvent()
        data class GameErrorEvent(val data: GameError) : SocketEvent()
    }

    private val _uiState = MutableStateFlow(DrawingUiState())
    val uiState: StateFlow<DrawingUiState> = _uiState.asStateFlow()

    private val socketEventChannel = Channel<SocketEvent>()
    val socketEvent = socketEventChannel.receiveAsFlow().flowOn(dispatchers.io)

    private val username = savedStateHandle.get<String>("username") ?: ""
    private val roomName = savedStateHandle.get<String>("roomName") ?: ""

    // --- Timer ---
    private val timer = CoroutineTimer()
    private var timerJob: Job? = null

    private fun startTimer(duration: Long) {
        timerJob?.cancel()
        timerJob = timer.timeAndEmit(
            duration = duration, coroutineScope = viewModelScope
        ) { remaining ->
            _uiState.update { it.copy(phaseTime = remaining) }
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    init {
        connectToRoom()
        observeEvents()
        observeBaseModels()
    }

    private fun connectToRoom() {
        viewModelScope.launch(dispatchers.io) {
            drawingApi.connect()
        }
    }

    private fun observeEvents() {
        drawingApi.events.onEach { event ->
                when (event) {
                    is DrawingApi.WebSocketEvent.OnConnectionOpened -> {
                        _uiState.update {
                            it.copy(
                                isConnected = true, connectionError = null, isConnecting = false
                            )
                        }
                        sendBaseModel(JoinRoomHandshake(username, roomName, clientId))
                    }

                    is DrawingApi.WebSocketEvent.OnConnectionError -> {
                        Timber.e("DrawingVM: WebSocket ERROR: ${event.error.message}")
                        _uiState.update {
                            it.copy(
                                isConnected = false,
                                connectionError = event.error.message,
                                isConnecting = false
                            )
                        }
                    }

                    is DrawingApi.WebSocketEvent.OnConnectionClosed -> {
                        Timber.w("DrawingVM: WebSocket CLOSED")
                        _uiState.update { it.copy(isConnected = false, isConnecting = false) }
                    }
                }
            }.launchIn(viewModelScope)
    }

    private fun observeBaseModels() {
        drawingApi.observeMessages().onEach { data ->
                when (data) {
                    is RoundDrawInfo -> {
                        val drawActions = data.data.mapNotNull { jsonString ->
                            try {
                                json.decodeFromString<BaseModel>(jsonString)
                            } catch (e: Exception) {
                                Timber.e(e, "Failed to decode RoundDrawInfo element: $jsonString")
                                null
                            }
                        }
                        
                        _uiState.update { initialState ->
                            var currentState = initialState
                            drawActions.forEach { model ->
                                when (model) {
                                    is DrawData -> currentState = processDrawData(currentState, model, isRestoring = true)
                                    is DrawAction -> currentState = processDrawAction(currentState, model, isRestoring = true)
                                    else -> Unit
                                }
                            }
                            currentState
                        }
                    }

                    is DrawData -> {
                        _uiState.update { state -> processDrawData(state, data) }
                    }

                    is DrawAction -> {
                        _uiState.update { state -> processDrawAction(state, data) }
                    }

                    is ChatMessage -> {
                        _uiState.update { it.copy(messages = it.messages + data) }
                        socketEventChannel.send(SocketEvent.ChatMessageEvent(data))
                    }

                    is Announcement -> {
                        _uiState.update { it.copy(messages = it.messages + data) }
                        socketEventChannel.send(SocketEvent.AnnouncementEvent(data))
                    }

                    is GameState -> {
                        _uiState.update {
                            it.copy(
                                drawingPlayer = data.drawingPlayer,
                                word = data.word,
                                isUserDrawing = data.drawingPlayer == username,
                                // Clear the canvas for every new game state (new round start)
                                strokes = emptyList(),
                                remoteStrokes = emptyList(),
                                currentRemotePath = null
                            )
                        }
                        socketEventChannel.send(SocketEvent.GameStateEvent(data))
                    }

                    is PlayersList -> {
                        _uiState.update { it.copy(players = data.players) }
                    }

                    is PhaseChange -> {
                        val incomingPhase = data.phase

                        if (incomingPhase == null) {
                            // Timer-only tick — just update the countdown
                            _uiState.update { it.copy(time = data.time) }
                        } else {
                            // Full phase change
                            if (incomingPhase == Room.Phase.WAITING_FOR_PLAYERS) {
                                cancelTimer()
                            } else {
                                startTimer(data.time)
                            }

                            _uiState.update { state ->
                                val newDrawingPlayer = data.drawingPlayer
                                val resolvedDrawingPlayer =
                                    newDrawingPlayer ?: state.drawingPlayer

                                val isUserDrawer = resolvedDrawingPlayer == username

                                // Build a human-readable status label for the top bar
                                val statusText = when (incomingPhase) {
                                    Room.Phase.WAITING_FOR_PLAYERS ->
                                        context.getString(R.string.waiting_for_players)
                                    Room.Phase.WAITING_FOR_START ->
                                        context.getString(R.string.waiting_for_start)
                                    Room.Phase.NEW_ROUND ->
                                        if (resolvedDrawingPlayer != null)
                                            context.getString(R.string.player_is_drawing, resolvedDrawingPlayer)
                                        else
                                            context.getString(R.string.waiting_for_start)
                                    Room.Phase.GAME_RUNNING ->
                                        state.word ?: context.getString(R.string.guess_the_word)
                                    Room.Phase.SHOW_WORD ->
                                        context.getString(R.string.round_over)
                                }

                                // For SHOW_WORD: clear any dangling mid-stroke from a remote player.
                                // This is the Compose equivalent of the tutorial's finishOffDrawing() —
                                // since InProgressStrokes (local) self-cancels when it leaves composition,
                                // we only need to manually clean up the remote currentRemotePath here.
                                val clearRemotePath = incomingPhase == Room.Phase.SHOW_WORD

                                state.copy(
                                    phase = incomingPhase,
                                    time = data.time,
                                    phaseTimerMax = data.time,
                                    drawingPlayer = resolvedDrawingPlayer,
                                    // Only the assigned drawing player can draw during NEW_ROUND/GAME_RUNNING
                                    isUserDrawing = isUserDrawer && (incomingPhase == Room.Phase.NEW_ROUND || incomingPhase == Room.Phase.GAME_RUNNING),
                                    statusText = statusText,
                                    currentRemotePath = if (clearRemotePath) null else state.currentRemotePath
                                )
                            }
                        }
                    }

                    is NewWords -> {
                        _uiState.update { it.copy(newWords = data.newWords) }
                        socketEventChannel.send(SocketEvent.NewWordsEvent(data))
                    }

                    is ChosenWord -> {
                        _uiState.update { it.copy(word = data.chosenWord) }
                        socketEventChannel.send(SocketEvent.ChosenWordEvent(data))
                    }

                    is Ping -> sendBaseModel(Ping())
                    is GameError -> {
                        _uiState.update { it.copy(connectionError = "Error: ${data.errorType}") }
                        socketEventChannel.send(SocketEvent.GameErrorEvent(data))
                    }

                    else -> Unit
                }
            }.launchIn(viewModelScope)
    }

    fun sendBaseModel(baseModel: BaseModel) {
        viewModelScope.launch(dispatchers.io) {
            drawingApi.sendBaseModel(baseModel)
        }
    }

    fun sendDrawData(drawData: DrawData) {
        viewModelScope.launch(dispatchers.io) {
            drawingApi.sendBaseModel(drawData)
        }
    }

    fun selectColor(color: Color) {
        _uiState.update { it.copy(selectedColor = color, isEraser = false) }
    }

    fun selectEraser() {
        _uiState.update { it.copy(isEraser = true) }
    }

    fun chooseWord(word: String) {
        sendBaseModel(ChosenWord(word, roomName))
        _uiState.update { it.copy(newWords = emptyList()) }
    }

    fun addStrokes(newStrokes: List<Stroke>) {
        _uiState.update { it.copy(strokes = it.strokes + newStrokes) }
    }

    fun undo() {
        _uiState.update {
            if (it.strokes.isNotEmpty()) {
                it.copy(strokes = it.strokes.dropLast(1))
            } else if (it.remoteStrokes.isNotEmpty()) {
                it.copy(remoteStrokes = it.remoteStrokes.dropLast(1))
            } else it
        }
        val drawData = DrawData(
            roomName = roomName,
            color = 0,
            thickness = 0f,
            fromX = 0f,
            fromY = 0f,
            toX = 0f,
            toY = 0f,
            motionEvent = DrawData.MOTION_EVENT_UNDO
        )
        sendBaseModel(drawData)
    }

    fun clear() {
        _uiState.update { it.copy(strokes = emptyList(), remoteStrokes = emptyList(), currentRemotePath = null) }
        val drawData = DrawData(
            roomName = roomName,
            color = 0,
            thickness = 0f,
            fromX = 0f,
            fromY = 0f,
            toX = 0f,
            toY = 0f,
            motionEvent = DrawData.MOTION_EVENT_CLEAR
        )
        sendBaseModel(drawData)
    }

    fun sendMessage(message: String) {
        val chatMessage = ChatMessage(
            from = username,
            roomName = roomName,
            message = message,
            timestamp = System.currentTimeMillis()
        )
        sendBaseModel(chatMessage)
    }

    override fun onCleared() {
        drawingApi.disconnect()
    }

    private fun processDrawData(state: DrawingUiState, data: DrawData, isRestoring: Boolean = false): DrawingUiState {
        // Echo suppression logic:
        if (!isRestoring && state.isUserDrawing && state.drawingPlayer == username) {
            return state
        }

        var newRemoteStrokes = state.remoteStrokes
        var newCurrentRemotePath = state.currentRemotePath

        when (data.motionEvent) {
            DrawData.MOTION_EVENT_UNDO -> {
                // Hack: -1 means UNDO
                newRemoteStrokes = if (newRemoteStrokes.isNotEmpty()) newRemoteStrokes.dropLast(1) else newRemoteStrokes
            }
            DrawData.MOTION_EVENT_CLEAR -> {
                // Hack: -2 means CLEAR
                // Wipe both finished and any in-progress remote stroke
                newRemoteStrokes = emptyList()
                newCurrentRemotePath = null
            }
            MotionEvent.ACTION_DOWN -> {
                val path = Path().apply { moveTo(data.fromX, data.fromY) }
                newCurrentRemotePath = RemotePathData(path, Color(data.color), data.thickness)
            }
            MotionEvent.ACTION_MOVE -> {
                if (newCurrentRemotePath == null) {
                    val path = Path().apply { moveTo(data.fromX, data.fromY) }
                    newCurrentRemotePath = RemotePathData(path, Color(data.color), data.thickness)
                }
                // Create a NEW path instance to force Compose to detect the change
                val newPath = Path()
                newPath.addPath(newCurrentRemotePath.path)
                newPath.quadraticTo(
                    data.fromX, data.fromY,
                    (data.fromX + data.toX) / 2f,
                    (data.fromY + data.toY) / 2f
                )
                newCurrentRemotePath = newCurrentRemotePath.copy(path = newPath)
            }
            MotionEvent.ACTION_UP -> {
                newCurrentRemotePath?.let { remotePath ->
                    // Finalize the path
                    val finalPath = Path()
                    finalPath.addPath(remotePath.path)
                    finalPath.lineTo(data.fromX, data.fromY)
                    newRemoteStrokes = newRemoteStrokes + remotePath.copy(path = finalPath)
                }
                newCurrentRemotePath = null
            }
        }
        return state.copy(remoteStrokes = newRemoteStrokes, currentRemotePath = newCurrentRemotePath)
    }

    private fun processDrawAction(state: DrawingUiState, data: DrawAction, isRestoring: Boolean = false): DrawingUiState {
        if (!isRestoring && state.isUserDrawing && state.drawingPlayer == username) {
            return state
        }
        return when (data.action) {
            ACTION_UNDO -> {
                state.copy(
                    remoteStrokes = if (state.remoteStrokes.isNotEmpty()) state.remoteStrokes.dropLast(1) else state.remoteStrokes
                )
            }
            DrawAction.ACTION_CLEAR -> {
                state.copy(
                    remoteStrokes = emptyList(),
                    currentRemotePath = null
                )
            }
            else -> state
        }
    }
}
