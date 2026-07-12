package com.doodlefrens.ui.drawing

import android.content.res.Configuration
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.ink.authoring.compose.InProgressStrokes
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doodlefrens.designsystem.components.White
import com.doodlefrens.ui.drawing.components.ChatSection
import com.doodlefrens.ui.drawing.components.ChooseWordOverlay
import com.doodlefrens.ui.drawing.components.ColorPicker
import com.doodlefrens.ui.drawing.components.PlayersSection
import kotlinx.coroutines.launch

enum class DrawingDrawerContent {
    NONE, PLAYERS, CHAT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(
    username: String,
    roomName: String,
    viewModel: DrawingViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var messageText by remember { mutableStateOf("") }
    var currentDrawerContent by remember { mutableStateOf(DrawingDrawerContent.NONE) }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Sync state when drawer closes
    LaunchedEffect(drawerState.isClosed) {
        if (drawerState.isClosed) {
            currentDrawerContent = DrawingDrawerContent.NONE
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentDrawerContent != DrawingDrawerContent.NONE,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(if (isLandscape) 400.dp else 300.dp)
            ) {
                when (currentDrawerContent) {
                    DrawingDrawerContent.PLAYERS -> PlayersSection(onClose = {
                        scope.launch { drawerState.close() }
                    })
                    DrawingDrawerContent.CHAT -> ChatSection(
                        roomName = roomName,
                        messageText = messageText,
                        onMessageChange = { messageText = it },
                        onClose = { scope.launch { drawerState.close() } }
                    )
                    else -> Unit
                }
            }
        }
    ) {
        Scaffold { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Layer 0: Full Screen Drawing Area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(White)
                ) {
                    val renderer = remember { CanvasStrokeRenderer.create() }
                    val identityMatrix = remember { Matrix() }

                    // Render finished strokes
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawIntoCanvas { canvas ->
                            uiState.strokes.forEach { stroke ->
                                renderer.draw(canvas.nativeCanvas, stroke, identityMatrix)
                            }
                        }
                    }

                    // Handle in-progress strokes
                    // We move the brush calculation to a stable state so it can be reliably read by the nextBrush lambda
                    val latestBrush by rememberUpdatedState(
                        Brush.createWithColorIntArgb(
                            family = if (uiState.isEraser) StockBrushes.marker() else StockBrushes.pressurePen(),
                            colorIntArgb = (if (uiState.isEraser) Color.White else uiState.selectedColor).toArgb(),
                            size = if (uiState.isEraser) 50f else 10f,
                            epsilon = 0.1f
                        )
                    )

                    InProgressStrokes(
                        defaultBrush = latestBrush,
                        nextBrush = { latestBrush },
                        onStrokesFinished = viewModel::addStrokes
                    )
                }

                // Layer 1: Floating Tools & Sidebar
                Column(modifier = Modifier.fillMaxSize()) {
                    // Timer Progress Bar (Full Width, Top)
                    LinearProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )

                    // Top Bar: Color Picker & Undo
                    Surface(
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ColorPicker(
                                selectedColor = uiState.selectedColor,
                                isEraser = uiState.isEraser,
                                onColorSelected = viewModel::selectColor,
                                onEraserSelected = viewModel::selectEraser,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = viewModel::undo) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                            }
                            IconButton(onClick = viewModel::clear) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear")
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        // Left Sidebar: Actions
                        Surface(
                            color = Color.Transparent,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .width(56.dp)
                                .fillMaxHeight()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 16.dp)
                            ) {
                                IconButton(onClick = { 
                                    currentDrawerContent = DrawingDrawerContent.PLAYERS
                                    scope.launch { drawerState.open() }
                                }) {
                                    Icon(
                                        if (currentDrawerContent == DrawingDrawerContent.PLAYERS) Icons.Default.Person else Icons.Default.PersonOutline,
                                        contentDescription = "Players"
                                    )
                                }
                                IconButton(onClick = { /* Toggle Mic */ }) {
                                    Icon(Icons.Default.MicOff, contentDescription = "Mic")
                                }
                                IconButton(onClick = { 
                                    currentDrawerContent = DrawingDrawerContent.CHAT
                                    scope.launch { drawerState.open() }
                                }) {
                                    Icon(
                                        if (currentDrawerContent == DrawingDrawerContent.CHAT) Icons.Default.ChatBubble else Icons.Default.ChatBubbleOutline,
                                        contentDescription = "Chat"
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.selectedColor == Color.Transparent) { // Placeholder check
                    ChooseWordOverlay()
                }
            }
        }
    }
}
