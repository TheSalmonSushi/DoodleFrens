package com.doodlefrens.ui.drawing

import androidx.compose.ui.graphics.Color
import androidx.ink.strokes.Stroke
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.doodlefrens.ui.state.DrawingUiState
import com.doodlefrens.util.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class DrawingViewModel @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val gson: Gson
) : ViewModel() {

    private val _uiState = MutableStateFlow(DrawingUiState())
    val uiState: StateFlow<DrawingUiState> = _uiState.asStateFlow()

    fun selectColor(color: Color) {
        _uiState.update { it.copy(selectedColor = color, isEraser = false) }
    }

    fun selectEraser() {
        _uiState.update { it.copy(isEraser = true) }
    }

    fun addStrokes(newStrokes: List<Stroke>) {
        _uiState.update { it.copy(strokes = it.strokes + newStrokes) }
    }

    fun undo() {
        _uiState.update {
            if (it.strokes.isNotEmpty()) {
                it.copy(strokes = it.strokes.dropLast(1))
            } else it
        }
    }

    fun clear() {
        _uiState.update { it.copy(strokes = emptyList()) }
    }
}
