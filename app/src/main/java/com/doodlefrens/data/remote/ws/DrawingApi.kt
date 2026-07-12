package com.doodlefrens.data.remote.ws

import com.doodlefrens.data.remote.ws.models.BaseModel
import kotlinx.coroutines.flow.Flow

interface DrawingApi {

    val events: Flow<WebSocketEvent>

    suspend fun connect()

    suspend fun sendBaseModel(baseModel: BaseModel)

    fun observeMessages(): Flow<BaseModel>

    fun disconnect()

    sealed class WebSocketEvent {
        data object OnConnectionOpened : WebSocketEvent()
        data class OnConnectionError(val error: Throwable) : WebSocketEvent()
        data class OnConnectionClosed(val reason: String? = null) : WebSocketEvent()
    }
}
