package com.doodlefrens.ui.state

import androidx.compose.ui.graphics.Color
import androidx.ink.strokes.Stroke
import com.doodlefrens.designsystem.components.Black

data class DrawingUiState(
    val selectedColor: Color = Black,
    val isEraser: Boolean = false,
    val strokes: List<Stroke> = emptyList()
)
