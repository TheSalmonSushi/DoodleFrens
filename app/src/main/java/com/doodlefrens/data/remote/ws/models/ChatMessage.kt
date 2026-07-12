package com.doodlefrens.data.remote.ws.models

import com.doodlefrens.util.Constants.TYPE_CHAT_MESSAGE
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName(TYPE_CHAT_MESSAGE)
data class ChatMessage(
    val from: String,
    val roomName: String,
    val message: String,
    val timestamp: Long
) : BaseModel()
