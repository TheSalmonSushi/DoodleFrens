package com.doodlefrens.data.remote.ws.models

import com.doodlefrens.util.Constants.TYPE_PING
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName(TYPE_PING)
class Ping : BaseModel()
