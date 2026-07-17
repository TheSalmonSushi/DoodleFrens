package com.doodlefrens.ui.drawing

import android.content.res.Configuration
import android.graphics.Matrix
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.ink.authoring.compose.InProgressStrokes
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doodlefrens.data.remote.ws.models.DrawData
import com.doodlefrens.designsystem.components.White
import com.doodlefrens.ui.drawing.components.ChatSection
import com.doodlefrens.ui.drawing.components.ChooseWordOverlay
import com.doodlefrens.ui.drawing.components.ColorPicker
import com.doodlefrens.ui.drawing.components.PlayersSection
import com.doodlefrens.ui.state.RemotePathData
import kotlinx.coroutines.launch

enum class DrawingDrawerContent {
    NONE, PLAYERS, CHAT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(
    username: String,
    roomName: String,
    onNavigateBack: () -> Unit,
    viewModel: DrawingViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(Unit) {
        viewModel.socketEvent.collect { event ->
            when (event) {
                is DrawingViewModel.SocketEvent.GameErrorEvent -> {
                    when (event.data.errorType) {
                        com.doodlefrens.data.remote.ws.models.GameError.ERROR_ROOM_NOT_FOUND -> {
                            onNavigateBack()
                        }
                        else -> {
                            snackbarHostState.showSnackbar("Error: ${event.data.errorType}")
                        }
                    }
                }
                else -> Unit
            }
        }
    }

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
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
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
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(uiState.isUserDrawing, uiState.selectedColor, uiState.isEraser) {
                            if (!uiState.isUserDrawing) return@pointerInput
                            detectDragGestures(
                                onDragStart = { offset ->
                                    viewModel.sendDrawData(
                                        DrawData(
                                            roomName = roomName,
                                            color = (if (uiState.isEraser) Color.White else uiState.selectedColor).toArgb(),
                                            thickness = (if (uiState.isEraser) 50f else 10f) / canvasSize.width,
                                            fromX = offset.x / canvasSize.width,
                                            fromY = offset.y / canvasSize.height,
                                            toX = offset.x / canvasSize.width,
                                            toY = offset.y / canvasSize.height,
                                            motionEvent = MotionEvent.ACTION_DOWN
                                        )
                                    )
                                },
                                onDrag = { change, _ ->
                                    viewModel.sendDrawData(
                                        DrawData(
                                            roomName = roomName,
                                            color = (if (uiState.isEraser) Color.White else uiState.selectedColor).toArgb(),
                                            thickness = (if (uiState.isEraser) 50f else 10f) / canvasSize.width,
                                            fromX = change.previousPosition.x / canvasSize.width,
                                            fromY = change.previousPosition.y / canvasSize.height,
                                            toX = change.position.x / canvasSize.width,
                                            toY = change.position.y / canvasSize.height,
                                            motionEvent = MotionEvent.ACTION_MOVE
                                        )
                                    )
                                },
                                onDragEnd = {
                                    viewModel.sendDrawData(
                                        DrawData(
                                            roomName = roomName,
                                            color = (if (uiState.isEraser) Color.White else uiState.selectedColor).toArgb(),
                                            thickness = (if (uiState.isEraser) 50f else 10f) / canvasSize.width,
                                            fromX = 0f,
                                            fromY = 0f,
                                            toX = 0f,
                                            toY = 0f,
                                            motionEvent = MotionEvent.ACTION_UP
                                        )
                                    )
                                }
                            )
                        }
                ) {
                    val renderer = remember { CanvasStrokeRenderer.create() }
                    val identityMatrix = remember { Matrix() }

                    // Render finished strokes (both local and theoretically remote if they were converted)
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithCache {
                                onDrawWithContent {
                                    drawIntoCanvas { canvas ->
                                        uiState.strokes.forEach { stroke ->
                                            renderer.draw(canvas.nativeCanvas, stroke, identityMatrix)
                                        }
                                    }
                                }
                            }
                    ) {
                        // Content is drawn via drawWithCache
                    }

                    // Render remote drawing segments
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (canvasSize.width > 0 && canvasSize.height > 0) {
                            scale(canvasSize.width.toFloat(), canvasSize.height.toFloat(), pivot = Offset.Zero) {
                                uiState.remoteStrokes.forEach { stroke ->
                                    drawPath(
                                        path = stroke.path,
                                        color = stroke.color,
                                        style = DrawStroke(
                                            width = stroke.thickness,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                                uiState.currentRemotePath?.let { stroke ->
                                    drawPath(
                                        path = stroke.path,
                                        color = stroke.color,
                                        style = DrawStroke(
                                            width = stroke.thickness,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Handle in-progress strokes for the local user
                    if (uiState.isUserDrawing) {
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

                if (uiState.newWords.isNotEmpty()) {
                    ChooseWordOverlay(
                        words = uiState.newWords,
                        onWordClicked = viewModel::chooseWord
                    )
                }

                if (uiState.isConnecting) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
        }
    }
}
