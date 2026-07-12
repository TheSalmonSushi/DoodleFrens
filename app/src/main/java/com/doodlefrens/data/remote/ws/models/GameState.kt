package com.doodlefrens.data.remote.ws.models

import com.doodlefrens.util.Constants.TYPE_GAME_STATE
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName(TYPE_GAME_STATE)
data class GameState(
    val drawingPlayer: String,
    val word: String
) : BaseModel()
