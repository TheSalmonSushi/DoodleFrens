package com.doodlefrens.repository

import com.doodlefrens.data.remote.ws.Room
import com.doodlefrens.util.Resource

interface SetupRepository {

    suspend fun createRoom(room: Room): Resource<Unit>

    suspend fun getRooms(searchQuery: String) : Resource<List<Room>>

    suspend fun joinRoom(username: String, roomName: String): Resource<Unit>

}