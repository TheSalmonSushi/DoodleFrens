package com.doodlefrens.data.remote.ws.models

import com.doodlefrens.util.Constants.TYPE_PHASE_CHANGE
import com.doodlefrens.data.remote.ws.Room
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName(TYPE_PHASE_CHANGE)
data class PhaseChange(
    var phase: Room.Phase,
    var time: Long,
    val drawingPlayer: String
) : BaseModel()
