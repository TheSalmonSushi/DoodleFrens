package com.doodlefrens.data.remote.ws.models

import com.doodlefrens.util.Constants.TYPE_PLAYERS_LIST
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName(TYPE_PLAYERS_LIST)
data class PlayersList(
    val players: List<PlayerData>
) : BaseModel()
