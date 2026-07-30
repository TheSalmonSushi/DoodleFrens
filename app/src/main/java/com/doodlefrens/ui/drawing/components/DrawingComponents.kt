package com.doodlefrens.ui.drawing.components

import androidx.compose.foundation.background
import com.doodlefrens.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doodlefrens.designsystem.components.Black
import com.doodlefrens.designsystem.components.ColorRadioButton
import com.doodlefrens.designsystem.components.Green
import com.doodlefrens.designsystem.components.IconRadioButton
import com.doodlefrens.designsystem.components.LightGrey
import com.doodlefrens.designsystem.components.Orange
import com.doodlefrens.designsystem.components.Red
import com.doodlefrens.designsystem.components.White
import com.doodlefrens.designsystem.components.Yellow
import com.doodlefrens.data.remote.ws.models.Announcement
import com.doodlefrens.data.remote.ws.models.BaseModel
import com.doodlefrens.data.remote.ws.models.ChatMessage
import com.doodlefrens.data.remote.ws.models.PlayerData

import java.text.SimpleDateFormat
import androidx.compose.ui.platform.LocalLocale

@Composable
fun AnnouncementItem(
    message: String,
    time: String,
    backgroundColor: Color = Yellow,
    textColor: Color = Black
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            color = textColor,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = time,
            color = textColor,
            modifier = Modifier.padding(end = 16.dp),
            fontSize = 20.sp,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun ChatMessageItem(
    username: String,
    message: String,
    time: String,
    isOutgoing: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (isOutgoing) {
            Text(
                text = time,
                modifier = Modifier.padding(end = 16.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }

        val bubbleColor = if (isOutgoing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        val textColor = if (isOutgoing) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isOutgoing) 16.dp else 0.dp,
                        bottomEnd = if (isOutgoing) 0.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .padding(16.dp)
        ) {
            Text(
                text = username,
                color = textColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (!isOutgoing) {
            Text(
                text = time,
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun PlayerItem(
    rank: Int,
    username: String,
    score: Int,
    isDrawing: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank. ",
            fontSize = 40.sp,
            modifier = Modifier.width(60.dp),
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = username,
            fontSize = 30.sp,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        if (isDrawing) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Text(
            text = score.toString(),
            fontSize = 40.sp,
            style = MaterialTheme.typography.headlineLarge
        )
    }
}

@Composable
fun PlayersSection(
    players: List<PlayerData>,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Players",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        HorizontalDivider()
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(players.size) { index ->
                val player = players[index]
                PlayerItem(
                    rank = player.rank,
                    username = player.username,
                    score = player.score,
                    isDrawing = player.isDrawing
                )
            }
        }
    }
}

@Composable
fun ChatSection(
    username: String,
    roomName: String,
    messages: List<BaseModel>,
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        // Header with Close Button
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chat",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        HorizontalDivider()

        // Current Word
        Text(
            text = "Room: $roomName",
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        val listState = rememberLazyListState()
        val isAtBottom by remember {
            derivedStateOf {
                val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                lastVisibleItem == null || lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 2
            }
        }

        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty() && isAtBottom) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }

        // Chat List
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(messages.size) { index ->
                val message = messages[index]
                if (message is Announcement) {
                    val dateFormat = SimpleDateFormat("kk:mm:ss", LocalLocale.current.platformLocale)
                    val time = dateFormat.format(message.timestamp)
                    
                    val backgroundColor = when (message.announcementType) {
                        Announcement.TYPE_EVERYBODY_GUESSED_IT -> LightGrey
                        Announcement.TYPE_PLAYER_GUESSED_WORD -> Yellow
                        Announcement.TYPE_PLAYER_JOINED -> Green
                        Announcement.TYPE_PLAYER_LEFT -> Red
                        else -> LightGrey
                    }
                    val textColor = when (message.announcementType) {
                        Announcement.TYPE_PLAYER_LEFT -> White
                        else -> Black
                    }

                    AnnouncementItem(
                        message = message.message,
                        time = time,
                        backgroundColor = backgroundColor,
                        textColor = textColor
                    )
                } else if (message is ChatMessage) {
                    val dateFormat = SimpleDateFormat("kk:mm:ss", LocalLocale.current.platformLocale)
                    val time = dateFormat.format(message.timestamp)
                    val isOutgoing = message.from == username

                    ChatMessageItem(
                        username = message.from,
                        message = message.message,
                        time = time,
                        isOutgoing = isOutgoing
                    )
                }
            }
        }

        // Message Input
        MessageInput(
            messageText = messageText,
            onMessageChange = onMessageChange,
            onSendMessage = onSendMessage,
            onClearText = { onMessageChange("") }
        )
        // Add padding for bottom sheet drag handle / system bars
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ColorPicker(
    selectedColor: Color,
    isEraser: Boolean,
    onColorSelected: (Color) -> Unit,
    onEraserSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(Red, Green, Color.Blue, Orange, Yellow, Black)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEach { color ->
            ColorRadioButton(
                color = color,
                isSelected = !isEraser && selectedColor == color,
                onClick = { onColorSelected(color) }
            )
        }

        VerticalDivider(
            modifier = Modifier
                .height(24.dp)
                .padding(horizontal = 4.dp),
            color = Color.LightGray
        )

        IconRadioButton(
            icon = Icons.Default.CleaningServices,
            isSelected = isEraser,
            onClick = onEraserSelected,
            unselectedTint = Black
        )
    }
}

@Composable
fun MessageInput(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onClearText: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = messageText,
            onValueChange = onMessageChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Message") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (messageText.isNotBlank()) {
                        onSendMessage()
                        keyboardController?.hide()
                    }
                }
            ),
            trailingIcon = {
                if (messageText.isNotEmpty()) {
                    IconButton(onClick = onClearText) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = { 
            if (messageText.isNotBlank()) {
                onSendMessage()
                keyboardController?.hide()
            } 
        }) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
        }
    }
}

@Composable
fun ChooseWordOverlay(
    words: List<String>,
    timerSeconds: Int,
    onWordClicked: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.SentimentSatisfiedAlt,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = Yellow
            )
            Text(
                text = stringResource(R.string.choose_your_word),
                style = MaterialTheme.typography.headlineLarge,
                color = White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            words.forEach { word ->
                Button(
                    onClick = { onWordClicked(word) },
                    modifier = Modifier.width(200.dp)
                ) {
                    Text(word)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = timerSeconds.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = White
            )
        }
    }
}
