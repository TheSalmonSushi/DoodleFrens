package com.doodlefrens.data.remote.ws.models

import com.doodlefrens.util.Constants.TYPE_DISCONNECT_REQUEST
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName(TYPE_DISCONNECT_REQUEST)
class DisconnectRequest : BaseModel()
