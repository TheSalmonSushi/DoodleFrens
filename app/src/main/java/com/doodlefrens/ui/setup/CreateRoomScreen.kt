package com.doodlefrens.ui.setup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.doodlefrens.R
import com.doodlefrens.data.remote.ws.Room
import com.doodlefrens.util.Constants.MAX_ROOM_NAME_LENGTH
import com.doodlefrens.util.Constants.MIN_ROOM_NAME_LENGTH

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoomScreen(
    username: String,
    onRoomCreated: (roomName: String) -> Unit,
    viewModel: CreateRoomViewModel = hiltViewModel()
) {
    var roomName by remember { mutableStateOf("") }
    var maxPlayers by remember { mutableIntStateOf(4) }
    var isExpanded by remember { mutableStateOf(false) }
    val roomSizeOptions = stringArrayResource(R.array.room_size_array)
    
    val snackbarHostState = remember { SnackbarHostState() }
    var isLoading by remember { mutableStateOf(false) }

    val errorEmpty = stringResource(R.string.error_field_empty)
    val errorTooShort = stringResource(R.string.error_room_name_too_short, MIN_ROOM_NAME_LENGTH)
    val errorTooLong = stringResource(R.string.error_room_name_too_long, MAX_ROOM_NAME_LENGTH)

    LaunchedEffect(key1 = true) {
        viewModel.setupEvent.collect { event ->
            when (event) {
                is CreateRoomViewModel.SetupEvent.CreateRoomEvent -> {
                    viewModel.joinRoom(username, event.room.name)
                }
                is CreateRoomViewModel.SetupEvent.CreateRoomErrorEvent -> {
                    isLoading = false
                    snackbarHostState.showSnackbar(event.error)
                }
                is CreateRoomViewModel.SetupEvent.JoinRoomEvent -> {
                    isLoading = false
                    onRoomCreated(event.roomName)
                }
                is CreateRoomViewModel.SetupEvent.JoinRoomErrorEvent -> {
                    isLoading = false
                    snackbarHostState.showSnackbar(event.error)
                }
                is CreateRoomViewModel.SetupEvent.InputEmptyError -> {
                    isLoading = false
                    snackbarHostState.showSnackbar(errorEmpty)
                }
                is CreateRoomViewModel.SetupEvent.InputTooShortError -> {
                    isLoading = false
                    snackbarHostState.showSnackbar(errorTooShort)
                }
                is CreateRoomViewModel.SetupEvent.InputTooLongError -> {
                    isLoading = false
                    snackbarHostState.showSnackbar(errorTooLong)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 50.dp)
            ) {
                // ivDoodleWorld - Placeholder icon
                Image(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )

                Text(
                    text = stringResource(R.string.create_a_new_room),
                    fontSize = 50.sp,
                    lineHeight = 55.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = roomName,
                    onValueChange = {
                        if (it.length <= MAX_ROOM_NAME_LENGTH) roomName = it
                    },
                    label = { Text(stringResource(R.string.room_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = isExpanded,
                        onExpandedChange = { isExpanded = !isExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = maxPlayers.toString(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.room_size)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                        )
                        ExposedDropdownMenu(
                            expanded = isExpanded,
                            onDismissRequest = { isExpanded = false }
                        ) {
                            roomSizeOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(text = option) },
                                    onClick = {
                                        maxPlayers = option.toInt()
                                        isExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = {
                            isLoading = true
                            viewModel.createRoom(Room(roomName, maxPlayers))
                        },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(text = stringResource(R.string.create_room))
                    }
                }

                if (isLoading) {
                    Spacer(modifier = Modifier.height(32.dp))
                    // Placeholder for Lottie Animation
                    CircularProgressIndicator(modifier = Modifier.size(150.dp))
                }
            }
        }
    }
}
