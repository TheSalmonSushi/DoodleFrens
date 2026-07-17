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
                            var newRemoteStrokes = state.remoteStrokes
                            var newCurrentRemotePath = state.currentRemotePath

                            when (data.motionEvent) {
                                MotionEvent.ACTION_DOWN -> {
                                    val path = Path().apply {
                                        // We need the canvas size to normalize, but the VM doesn't know it yet.
                                        // This is a problem. The normalization should happen in the UI
                                        // OR the VM should receive normalized coordinates and the UI applies them.
                                        // The DrawData ALREADY has normalized coordinates (0..1).
                                        // So we just need to keep them as 0..1 and scale in the UI.
                                        moveTo(data.fromX, data.fromY)
                                    }
                                    newCurrentRemotePath = RemotePathData(path, Color(data.color), data.thickness)
                                }
                                MotionEvent.ACTION_MOVE -> {
                                    newCurrentRemotePath?.path?.lineTo(data.toX, data.toY)
                                }
                                MotionEvent.ACTION_UP -> {
                                    newCurrentRemotePath?.let {
                                        newRemoteStrokes = newRemoteStrokes + it
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
                        when (data.action) {
                            ACTION_UNDO -> {
                                _uiState.update { it.copy(
                                    remoteStrokes = if (it.remoteStrokes.isNotEmpty()) it.remoteStrokes.dropLast(1) else it.remoteStrokes
                                ) }
                            }
                            DrawAction.ACTION_CLEAR -> {
                                _uiState.update { it.copy(remoteStrokes = emptyList(), strokes = emptyList()) }
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
                        _uiState.update { it.copy(
                            phase = data.phase,
                            time = data.time,
                            drawingPlayer = data.drawingPlayer,
                            isUserDrawing = data.drawingPlayer == username
                            )
                        }
                    }
                    is NewWords -> {
                        _uiState.update { it.copy(newWords = data.newWords) }
                        socketEventChannel.send(SocketEvent.NewWordsEvent(data))
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
        sendBaseModel(DrawAction(ACTION_UNDO))
    }

    fun clear() {
        _uiState.update { it.copy(strokes = emptyList()) }
        sendBaseModel(DrawAction(DrawAction.ACTION_CLEAR))
    }

    override fun onCleared() {
        drawingApi.disconnect()
    }
}
