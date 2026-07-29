package com.doodlefrens.data.remote.ws.models

import com.doodlefrens.util.Constants.TYPE_DRAW_DATA
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName(TYPE_DRAW_DATA)
data class DrawData(
    val roomName: String,
    val color: Int,
    val thickness: Float,
    val fromX: Float,
    val fromY: Float,
    val toX: Float,
    val toY: Float,
    val motionEvent: Int
) : BaseModel() {
    companion object {
        const val MOTION_EVENT_UNDO = -1
        const val MOTION_EVENT_CLEAR = -2
    }
}
