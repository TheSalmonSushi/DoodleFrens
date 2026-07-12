package com.doodlefrens.data.remote.ws

import kotlinx.serialization.Serializable

@Serializable
data class Room(
    val name: String,
    val maxPlayers: Int,
    val playerCount: Int = 1,
) {
    @Serializable
    enum class Phase  {
        WAITING_FOR_PLAYERS,
        WAITING_FOR_START,
        NEW_ROUND,
        GAME_RUNNING,
        SHOW_WORD
    }
}
