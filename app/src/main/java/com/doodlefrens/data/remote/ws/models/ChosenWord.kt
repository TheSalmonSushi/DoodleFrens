package com.doodlefrens.data.remote.ws.models

import com.doodlefrens.util.Constants.TYPE_CHOSEN_WORD
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName(TYPE_CHOSEN_WORD)
data class ChosenWord(
    val chosenWord: String,
    val roomName: String
) : BaseModel()
