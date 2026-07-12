package com.doodlefrens.ui.drawing

import androidx.compose.ui.graphics.Color
import androidx.ink.strokes.Stroke
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doodlefrens.data.remote.ws.DrawingApi
import com.doodlefrens.data.remote.ws.models.*
import com.doodlefrens.ui.state.DrawingUiState
import com.doodlefrens.util.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class DrawingViewModel @Inject constructor(
    private val drawingApi: DrawingApi,
    private val dispatchers: DispatcherProvider,
    @Named("clientId") private val clientId: String,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(DrawingUiState())
    val uiState: StateFlow<DrawingUiState> = _uiState.asStateFlow()

    private val username = savedStateHandle.get<String>("username") ?: ""
    private val roomName = savedStateHandle.get<String>("roomName") ?: ""

    init {
        connectToRoom()
        observeEvents()
        observeMessages()
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

    private fun observeMessages() {
        drawingApi.observeMessages()
            .onEach { message ->
                when (message) {
                    is DrawData -> {
                        // In a real app, you'd translate coordinates and draw
                        // For now, we'll just handle the model
                    }
                    is ChatMessage -> {
                        _uiState.update { it.copy(messages = it.messages + message) }
                    }
                    is Announcement -> {
                        _uiState.update { it.copy(messages = it.messages + message) }
                    }
                    is GameState -> {
                        _uiState.update { it.copy(drawingPlayer = message.drawingPlayer, word = message.word) }
                    }
                    is PlayersList -> {
                        _uiState.update { it.copy(players = message.players) }
                    }
                    is PhaseChange -> {
                        _uiState.update { it.copy(phase = message.phase, time = message.time, drawingPlayer = message.drawingPlayer) }
                    }
                    is NewWords -> {
                        _uiState.update { it.copy(newWords = message.newWords) }
                    }
                    is GameError -> {
                        _uiState.update { it.copy(connectionError = "Error: ${message.errorType}") }
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
        // Here you would also send DrawData to the server
    }

    fun undo() {
        _uiState.update {
            if (it.strokes.isNotEmpty()) {
                it.copy(strokes = it.strokes.dropLast(1))
            } else it
        }
        sendBaseModel(DrawAction(DrawAction.ACTION_UNDO))
    }

    fun clear() {
        _uiState.update { it.copy(strokes = emptyList()) }
    }

    override fun onCleared() {
        super.onCleared()
        drawingApi.disconnect()
    }
}
