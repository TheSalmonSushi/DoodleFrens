package com.doodlefrens.ui.drawing

import android.view.MotionEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.ink.strokes.Stroke
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doodlefrens.data.remote.ws.DrawingApi
import com.doodlefrens.data.remote.ws.models.*
import com.doodlefrens.data.remote.ws.models.DrawAction.Companion.ACTION_UNDO
import com.doodlefrens.ui.state.DrawingUiState
import com.doodlefrens.ui.state.RemotePathData
import com.doodlefrens.util.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class DrawingViewModel @Inject constructor(
    private val drawingApi: DrawingApi,
    private val dispatchers: DispatcherProvider,
    @param:Named("clientId") private val clientId: String,
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
        drawingApi.events
            .onEach { event ->
                when (event) {
                    is DrawingApi.WebSocketEvent.OnConnectionOpened -> {
                        _uiState.update { it.copy(isConnected = true, connectionError = null, isConnecting = false) }
                        sendBaseModel(JoinRoomHandshake(username, roomName, clientId))
                    }
                    is DrawingApi.WebSocketEvent.OnConnectionError -> {
                        _uiState.update { it.copy(isConnected = false, connectionError = event.error.message, isConnecting = false) }
                    }
                    is DrawingApi.WebSocketEvent.OnConnectionClosed -> {
                        _uiState.update { it.copy(isConnected = false, isConnecting = false) }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeBaseModels() {
        drawingApi.observeMessages()
            .onEach { data ->
                when (data) {
                    is DrawData -> {
                        _uiState.update { state ->
                            // Echo suppression logic:
                            // We only skip drawing incoming data if WE are the one who sent it.
                            // We determine if we sent it based on the 'isUserDrawing' flag.
                            // BUT, we only do this if a specific player is assigned to draw, 
                            // or if we are using the 'test' override.
                            if (state.isUserDrawing && state.drawingPlayer == username) {
                                return@update state
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
                                    newRemoteStrokes = emptyList()
                                }
                                MotionEvent.ACTION_DOWN -> {
                                    val path = Path().apply {
                                        moveTo(data.fromX, data.fromY)
                                    }
                                    newCurrentRemotePath = RemotePathData(path, Color(data.color), data.thickness)
                                }
                                MotionEvent.ACTION_MOVE -> {
                                    if (newCurrentRemotePath == null) {
                                        val path = Path().apply {
                                            moveTo(data.fromX, data.fromY)
                                        }
                                        newCurrentRemotePath = RemotePathData(path, Color(data.color), data.thickness)
                                    }
                                    
                                    // Create a NEW path instance to force Compose to detect the change
                                    val newPath = Path()
                                    newPath.addPath(newCurrentRemotePath.path)
                                    newPath.quadraticTo(
                                        data.fromX, 
                                        data.fromY, 
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
                            state.copy(
                                remoteStrokes = newRemoteStrokes,
                                currentRemotePath = newCurrentRemotePath
                            )
                        }
                    }
                    is DrawAction -> {
                        // Echo suppression: if we're the drawer, we already
                        // handled undo/clear locally — ignore our own echoes.
                        val currentState = _uiState.value
                        if (currentState.isUserDrawing && currentState.drawingPlayer == username) {
                            return@onEach
                        }
                        when (data.action) {
                            ACTION_UNDO -> {
                                _uiState.update { it.copy(
                                    remoteStrokes = if (it.remoteStrokes.isNotEmpty()) it.remoteStrokes.dropLast(1) else it.remoteStrokes
                                ) }
                            }
                            DrawAction.ACTION_CLEAR -> {
                                _uiState.update { it.copy(remoteStrokes = emptyList()) }
                            }
                        }
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
                        _uiState.update { it.copy(
                            drawingPlayer = data.drawingPlayer,
                            word = data.word,
                            isUserDrawing = data.drawingPlayer == username
                            )
                        }
                        socketEventChannel.send(SocketEvent.GameStateEvent(data))
                    }
                    is PlayersList -> {
                        _uiState.update { it.copy(players = data.players) }
                    }
                    is PhaseChange -> {
                        _uiState.update { state ->
                            val newDrawingPlayer = data.drawingPlayer
                            val newState = state.copy(
                                phase = data.phase,
                                time = data.time,
                                drawingPlayer = if (newDrawingPlayer.isBlank()) state.drawingPlayer else newDrawingPlayer,
                                isUserDrawing = newDrawingPlayer == username
                            )
                            newState
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
            }
            .launchIn(viewModelScope)
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
        _uiState.update { it.copy(strokes = emptyList()) }
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
}
