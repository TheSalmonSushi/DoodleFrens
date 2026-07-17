package com.doodlefrens.ui.state

import androidx.compose.ui.graphics.Color
import androidx.ink.strokes.Stroke
import com.doodlefrens.data.remote.ws.Room
import com.doodlefrens.data.remote.ws.models.BaseModel
import com.doodlefrens.data.remote.ws.models.PlayerData
import com.doodlefrens.designsystem.components.Black

data class DrawingUiState(
    val selectedColor: Color = Black,
    val isEraser: Boolean = false,
    val strokes: List<Stroke> = emptyList(),
    val messages: List<BaseModel> = emptyList(),
    val players: List<PlayerData> = emptyList(),
    val phase: Room.Phase? = null,
    val time: Long = 0L,
    val drawingPlayer: String? = null,
    val word: String? = null,
    val newWords: List<String> = emptyList(),
    val connectionError: String? = null,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = true,
    val isUserDrawing: Boolean = false,
    val remoteStrokes: List<RemotePathData> = emptyList(),
    val currentRemotePath: RemotePathData? = null
)
