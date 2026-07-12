package com.doodlefrens.data.remote.ws.models

import com.doodlefrens.util.Constants.TYPE_GAME_ERROR
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName(TYPE_GAME_ERROR)
data class GameError(
    val errorType: Int
) : BaseModel() {
    companion object {
        const val ERROR_ROOM_NOT_FOUND = 0
    }
}
