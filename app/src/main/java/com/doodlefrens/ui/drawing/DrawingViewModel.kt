package com.doodlefrens.ui.drawing

import androidx.compose.ui.graphics.Color
import androidx.ink.strokes.Stroke
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doodlefrens.data.remote.ws.DrawingApi
import com.doodlefrens.data.remote.ws.models.*
import com.doodlefrens.data.remote.ws.models.DrawAction.Companion.ACTION_UNDO
import com.doodlefrens.ui.state.DrawingUiState
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
        data class DrawDataEvent(val data: DrawData) : SocketEvent()
        data class NewWordsEvent(val data: NewWords) : SocketEvent()
        data class GameErrorEvent(val data: GameError) : SocketEvent()
        data object UndoEvent : SocketEvent()
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
                        _uiState.update { it.copy(isConnected = true, connectionError = null) }
                        sendBaseModel(JoinRoomHandshake(username, roomName, clientId))
                    }
                    is DrawingApi.WebSocketEvent.OnConnectionError -> {
                        _uiState.update { it.copy(isConnected = false, connectionError = event.error.message) }
                    }
                    is DrawingApi.WebSocketEvent.OnConnectionClosed -> {
                        _uiState.update { it.copy(isConnected = false) }
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
                        socketEventChannel.send(SocketEvent.DrawDataEvent(data))
                    }
                    is DrawAction -> {
                        when (data.action) {
                            ACTION_UNDO -> socketEventChannel.send(SocketEvent.UndoEvent)
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
                        _uiState.update { it.copy(drawingPlayer = data.drawingPlayer, word = data.word) }
                        socketEventChannel.send(SocketEvent.GameStateEvent(data))
                    }
                    is PlayersList -> {
                        _uiState.update { it.copy(players = data.players) }
                    }
                    is PhaseChange -> {
                        _uiState.update { it.copy(phase = data.phase, time = data.time, drawingPlayer = data.drawingPlayer) }
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

    fun selectColor(color: Color) {
        _uiState.update { it.copy(selectedColor = color, isEraser = false) }
    }

    fun selectEraser() {
        _uiState.update { it.copy(isEraser = true) }
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
    }

    override fun onCleared() {
        drawingApi.disconnect()
    }
}
