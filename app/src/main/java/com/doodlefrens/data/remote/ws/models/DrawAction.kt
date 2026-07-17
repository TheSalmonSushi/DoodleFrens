package com.doodlefrens.data.remote.ws.models

import com.doodlefrens.util.Constants.TYPE_DRAW_ACTION
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName(TYPE_DRAW_ACTION)
data class DrawAction(
    val action: String
) : BaseModel() {
    companion object {
        const val ACTION_UNDO = "ACTION_UNDO"
        const val ACTION_CLEAR = "ACTION_CLEAR"
    }
}
