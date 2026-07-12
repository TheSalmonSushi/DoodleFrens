package com.doodlefrens.data.remote.ws.models

import com.doodlefrens.util.Constants.TYPE_CUR_ROUND_DRAW_INFO
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName(TYPE_CUR_ROUND_DRAW_INFO)
data class RoundDrawInfo(
    val data: List<String>
) : BaseModel()
