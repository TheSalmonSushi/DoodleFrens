package com.doodlefrens.ui.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.doodlefrens.R
import com.doodlefrens.data.remote.ws.Room
import com.doodlefrens.util.Constants.SEARCH_DELAY
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SelectRoomScreen(
    username: String,
    onNewRoomClick: () -> Unit,
    onRoomSelected: (roomName: String) -> Unit,
    viewModel: SelectRoomViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val roomsState by viewModel.rooms.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            delay(SEARCH_DELAY.milliseconds)
        }
        viewModel.getRooms(searchQuery)
    }

    LaunchedEffect(key1 = true) {
        viewModel.setupEvent.collect { event ->
            when (event) {
                is SelectRoomViewModel.SetupEvent.JoinRoomEvent -> {
                    onRoomSelected(event.roomName)
                }
                is SelectRoomViewModel.SetupEvent.JoinRoomErrorEvent -> {
                    snackbarHostState.showSnackbar(event.error)
                }
                is SelectRoomViewModel.SetupEvent.GetRoomErrorEvent -> {
                    snackbarHostState.showSnackbar(event.error)
                }
                else -> Unit
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .imePadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                    },
                    label = { Text(stringResource(R.string.search_for_rooms)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = { viewModel.getRooms(searchQuery) }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reload")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (val state = roomsState) {
                    is SelectRoomViewModel.SetupEvent.GetRoomLoadingEvent -> {
                        CircularProgressIndicator(modifier = Modifier.size(150.dp))
                    }
                    is SelectRoomViewModel.SetupEvent.GetRoomEmptyEvent -> {
                        NoRoomsFound()
                    }
                    is SelectRoomViewModel.SetupEvent.GetRoomEvent -> {
                        if (state.rooms.isEmpty()) {
                            NoRoomsFound()
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                // Add some padding for the list
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(state.rooms, key = { it.name }) { room ->
                                    RoomItem(room = room) {
                                        viewModel.joinRoom(username, room.name)
                                    }
                                }
                            }
                        }
                    }
                    else -> Unit
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = stringResource(R.string.or))
                TextButton(onClick = onNewRoomClick) {
                    Text(text = stringResource(R.string.create_room))
                }
            }
        }
    }
}

@Composable
private fun RoomItem(room: Room, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = room.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${room.playerCount}/ ${room.maxPlayers}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NoRoomsFound() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            imageVector = Icons.Default.SentimentVeryDissatisfied,
            contentDescription = null,
            modifier = Modifier.size(150.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.outline)
        )
        Spacer(modifier = Modifier.height(50.dp))
        Text(
            text = stringResource(R.string.no_rooms_found),
            fontSize = 45.sp,
            textAlign = TextAlign.Center,
            lineHeight = 50.sp
        )
    }
}
