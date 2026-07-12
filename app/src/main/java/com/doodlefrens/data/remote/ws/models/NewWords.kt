package com.doodlefrens.data.remote.ws.models

import com.doodlefrens.util.Constants.TYPE_NEW_WORDS
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName(TYPE_NEW_WORDS)
data class NewWords(
    val newWords: List<String>
) : BaseModel()
