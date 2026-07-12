package com.doodlefrens.data.remote.ws.models

import com.doodlefrens.util.Constants.TYPE_JOIN_ROOM_HANDSHAKE
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName(TYPE_JOIN_ROOM_HANDSHAKE)
data class JoinRoomHandshake(
    val username: String,
    val roomName: String,
    val clientId: String
) : BaseModel()
