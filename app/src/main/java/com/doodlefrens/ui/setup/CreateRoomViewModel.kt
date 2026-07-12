package com.doodlefrens.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doodlefrens.data.remote.ws.Room
import com.doodlefrens.repository.SetupRepository
import com.doodlefren.util.Constants.MAX_ROOM_NAME_LENGTH
import com.doodlefren.util.Constants.MIN_ROOM_NAME_LENGTH
import com.doodlefrens.util.DispatcherProvider
import com.doodlefrens.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateRoomViewModel @Inject constructor(
    private val repository: SetupRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    sealed class SetupEvent {
        object InputEmptyError : SetupEvent()
        object InputTooShortError : SetupEvent()
        object InputTooLongError : SetupEvent()

        data class CreateRoomEvent(val room: Room) : SetupEvent()
        data class CreateRoomErrorEvent(val error: String) : SetupEvent()

        data class JoinRoomEvent(val roomName: String) : SetupEvent()
        data class JoinRoomErrorEvent(val error: String) : SetupEvent()
    }

    private val _setupEvent = MutableSharedFlow<SetupEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val setupEvent: SharedFlow<SetupEvent> = _setupEvent

    fun createRoom(room: Room) {
        viewModelScope.launch(dispatchers.main) {
            val trimmedRoomName = room.name.trim()
            when {
                trimmedRoomName.isEmpty() -> {
                    _setupEvent.emit(SetupEvent.InputEmptyError)
                }
                trimmedRoomName.length < MIN_ROOM_NAME_LENGTH -> {
                    _setupEvent.emit(SetupEvent.InputTooShortError)
                }
                trimmedRoomName.length > MAX_ROOM_NAME_LENGTH -> {
                    _setupEvent.emit(SetupEvent.InputTooLongError)
                }
                else -> {
                    val result = repository.createRoom(room)
                    if (result is Resource.Success) {
                        _setupEvent.emit(SetupEvent.CreateRoomEvent(room))
                    } else {
                        _setupEvent.emit(SetupEvent.CreateRoomErrorEvent(
                            result.message ?: return@launch
                        ))
                    }
                }
            }
        }
    }

    fun joinRoom(username: String, roomName: String) {
        viewModelScope.launch(dispatchers.main) {
            val result = repository.joinRoom(username, roomName)
            if (result is Resource.Success) {
                _setupEvent.emit(SetupEvent.JoinRoomEvent(roomName))
            } else {
                _setupEvent.emit(SetupEvent.JoinRoomErrorEvent(result.message ?: return@launch))
            }
        }
    }
}
