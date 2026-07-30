package com.doodlefrens.ui.drawing

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Matrix
import android.view.MotionEvent
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val context = LocalContext.current

    // Adaptive UI: Tablet vs Phone Detection
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val thicknessMultiplier = if (isTablet) 1.25f else 1.0f

    // Lock orientation to landscape for drawing
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

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
                        username = username,
                        roomName = roomName,
                        messages = uiState.messages,
                        messageText = messageText,
                        onMessageChange = { messageText = it },
                        onSendMessage = {
                            viewModel.sendMessage(messageText)
                            messageText = ""
                        },
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
                        .pointerInput(uiState.isUserDrawing, uiState.drawingPlayer, canvasSize) {
                            if (!uiState.isUserDrawing || canvasSize.width <= 0 || canvasSize.height <= 0) return@pointerInput
                            
                            awaitPointerEventScope {
                                var activePointerId: PointerId? = null
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)

                                    // 1. Assign a primary pointer if we don't have one
                                    event.changes.forEach { change ->
                                        if (activePointerId == null && change.changedToDown()) {
                                            activePointerId = change.id
                                        }
                                    }
                                    
                                    // Store the ID active for this specific frame
                                    val frameActiveId = activePointerId

                                    event.changes.forEach { change ->
                                        if (change.id == frameActiveId) {
                                            val position = change.position
                                            val prevPosition = change.previousPosition
                                            
                                            val drawData = DrawData(
                                                roomName = roomName,
                                                color = (if (uiState.isEraser) Color.White else uiState.selectedColor).toArgb(),
                                                thickness = if (uiState.isEraser) 40f else 8f,
                                                fromX = prevPosition.x / canvasSize.width,
                                                fromY = prevPosition.y / canvasSize.height,
                                                toX = position.x / canvasSize.width,
                                                toY = position.y / canvasSize.height,
                                                motionEvent = when {
                                                    change.changedToDown() -> MotionEvent.ACTION_DOWN
                                                    change.changedToUp() -> MotionEvent.ACTION_UP
                                                    else -> MotionEvent.ACTION_MOVE
                                                }
                                            )
                                            viewModel.sendDrawData(drawData)
                                            
                                            if (change.changedToUp()) {
                                                activePointerId = null // Reset for next frame
                                            }
                                        }
                                    }
                                    
                                    // SINGLE-FINGER ENFORCEMENT: Consume events from all
                                    // non-primary pointers so InProgressStrokes (Main pass)
                                    // only sees the one active finger (frameActiveId).
                                    event.changes.forEach { change ->
                                        if (change.id != frameActiveId) {
                                            change.consume()
                                        }
                                    }
                                    
                                    // DEAD MAN'S SWITCH: Fallback safety check for missed UP events.
                                    // If our active pointer is still in the event but no longer pressed,
                                    // and forEach didn't already reset it (e.g., changedToUp wasn't triggered),
                                    // force-reset to prevent the drawing from locking up.
                                    if (activePointerId != null) {
                                        val activeChange = event.changes.find { it.id == activePointerId }
                                        if (activeChange != null && !activeChange.pressed) {
                                            activePointerId = null
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    val renderer = remember { CanvasStrokeRenderer.create() }
                    val identityMatrix = remember { Matrix() }

                    // Render finished strokes (local)
                    // We key the Canvas with the list size to force a recomposition on Undo/Add
                    key(uiState.strokes.size) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawIntoCanvas { canvas ->
                                uiState.strokes.forEach { stroke ->
                                    renderer.draw(canvas.nativeCanvas, stroke, identityMatrix)
                                }
                            }
                        }
                    }

                    // Render remote drawing segments
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (size.width > 0 && size.height > 0) {
                            scale(size.width, size.height, pivot = Offset.Zero) {
                                // Draw finished remote strokes
                                uiState.remoteStrokes.forEach { stroke ->
                                    drawPath(
                                        path = stroke.path,
                                        color = stroke.color,
                                        style = DrawStroke(
                                            // Ensure width is scaled correctly to be visible
                                            width = (stroke.thickness * thicknessMultiplier) / size.width,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                                
                                // Draw the active/wet remote stroke dynamically
                                uiState.currentRemotePath?.let { stroke ->
                                    drawPath(
                                        path = stroke.path,
                                        color = stroke.color,
                                        style = DrawStroke(
                                            width = (stroke.thickness * thicknessMultiplier) / size.width,
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
                        val baseSize = if (uiState.isEraser) 40f else 8f
                        val adaptiveSize = baseSize * thicknessMultiplier
                        
                        val latestBrush by rememberUpdatedState(
                            Brush.createWithColorIntArgb(
                                family = if (uiState.isEraser) StockBrushes.marker() else StockBrushes.pressurePen(),
                                colorIntArgb = (if (uiState.isEraser) Color.White else uiState.selectedColor).toArgb(),
                                size = adaptiveSize,
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
                    val timerProgress = if (uiState.phaseTimerMax > 0L) {
                        uiState.phaseTime.toFloat() / uiState.phaseTimerMax.toFloat()
                    } else 1f

                    LinearProgressIndicator(
                        progress = { timerProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )

                    // Show statusText (phase label) or the current word, whichever is available
                    val displayText = uiState.word?.takeIf { it.isNotBlank() }
                        ?: uiState.statusText
                    if (!displayText.isNullOrEmpty()) {
                        Text(
                            text = displayText,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 8.dp)
                                .background(Color.White.copy(alpha = 0.8f), shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

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
                            IconButton(
                                onClick = viewModel::undo,
                                enabled = uiState.isUserDrawing
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = if (uiState.isUserDrawing) Color.Black else Color.Gray)
                            }
                            IconButton(
                                onClick = viewModel::clear,
                                enabled = uiState.isUserDrawing
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear", tint = if (uiState.isUserDrawing) Color.Black else Color.Gray)
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
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
                                        contentDescription = "Players",
                                        tint = Color.Black
                                    )
                                }
                                IconButton(onClick = { /* Toggle Mic */ }) {
                                    Icon(Icons.Default.MicOff, contentDescription = "Mic", tint = Color.Black)
                                }
                                IconButton(onClick = { 
                                    currentDrawerContent = DrawingDrawerContent.CHAT
                                    scope.launch { drawerState.open() }
                                }) {
                                    Icon(
                                        if (currentDrawerContent == DrawingDrawerContent.CHAT) Icons.Default.ChatBubble else Icons.Default.ChatBubbleOutline,
                                        contentDescription = "Chat",
                                        tint = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.newWords.isNotEmpty()) {
                    ChooseWordOverlay(
                        words = uiState.newWords,
                        timerSeconds = (uiState.phaseTime / 1000L).toInt(),
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
