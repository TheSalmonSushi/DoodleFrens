package com.doodlefrens.data.remote.ws

import com.doodlefrens.data.remote.ws.models.BaseModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

class KtorDrawingApi @Inject constructor(
    private val client: HttpClient,
    private val json: Json,
    private val baseUrl: String
) : DrawingApi {

    private var session: WebSocketSession? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _events = MutableSharedFlow<DrawingApi.WebSocketEvent>()
    override val events = _events.asSharedFlow()

    private val _messages = MutableSharedFlow<BaseModel>()

    override suspend fun connect() {
        try {
            session = client.webSocketSession {
                url("$baseUrl/ws/draw")
            }
            _events.emit(DrawingApi.WebSocketEvent.OnConnectionOpened)
            observeIncoming()
        } catch (e: Exception) {
            Timber.e(e, "WebSocket connection failed")
            _events.emit(DrawingApi.WebSocketEvent.OnConnectionError(e))
        }
    }

    private fun observeIncoming() {
        scope.launch {
            session?.incoming?.consumeAsFlow()
                ?.filterIsInstance<Frame.Text>()
                ?.mapNotNull { frame ->
                    try {
                        json.decodeFromString<BaseModel>(frame.readText())
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to decode message: ${frame.readText()}")
                        null
                    }
                }
                ?.onCompletion {
                    _events.emit(DrawingApi.WebSocketEvent.OnConnectionClosed())
                }
                ?.collect { message ->
                    _messages.emit(message)
                }
        }
    }

    override suspend fun sendBaseModel(baseModel: BaseModel) {
        try {
            val jsonString = json.encodeToString(baseModel)
            session?.send(Frame.Text(jsonString))
        } catch (e: Exception) {
            Timber.e(e, "Failed to send message")
        }
    }

    override fun observeMessages(): Flow<BaseModel> = _messages.asSharedFlow()

    override fun disconnect() {
        scope.launch {
            session?.close()
            session = null
        }
    }
}
