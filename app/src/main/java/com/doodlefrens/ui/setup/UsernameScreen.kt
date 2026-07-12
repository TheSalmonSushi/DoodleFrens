package com.doodlefrens.ui.setup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.doodlefrens.R
import com.doodlefrens.util.Constants.MAX_USERNAME_LENGTH
import com.doodlefrens.util.Constants.MIN_USERNAME_LENGTH

@Composable
fun UsernameScreen(
    onNavigateToSelectRoom: (username: String) -> Unit,
    viewModel: UsernameViewModel = hiltViewModel()
) {
    var username by rememberSaveable { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val errorFieldEmpty = stringResource(R.string.error_field_empty)
    val errorTooShort = stringResource(R.string.error_username_too_short, MIN_USERNAME_LENGTH)
    val errorTooLong = stringResource(R.string.error_username_too_long, MAX_USERNAME_LENGTH)

    // Compose equivalent of lifecycleScope.launchWhenStarted { setupEvent.collect { ... } }
    LaunchedEffect(key1 = true) {
        viewModel.setupEvent.collect { event ->
            when (event) {
                is UsernameViewModel.SetupEvent.NavigateToSelectRoomEvent -> {
                    onNavigateToSelectRoom(event.username)
                }
                is UsernameViewModel.SetupEvent.InputEmptyError -> {
                    snackbarHostState.showSnackbar(errorFieldEmpty)
                }
                is UsernameViewModel.SetupEvent.InputTooShortError -> {
                    snackbarHostState.showSnackbar(errorTooShort)
                }
                is UsernameViewModel.SetupEvent.InputTooLongError -> {
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
                // Missing Resource: ic_appicon_v2. Using Icons.Default.Face as placeholder.
                Image(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )

                Text(
                    text = stringResource(R.string.doodlefrens),
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(32.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.choose_a_username),
                        fontSize = 28.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            if (it.length <= MAX_USERNAME_LENGTH) username = it
                        },
                        label = { Text(stringResource(R.string.username)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.validateUsernameAndNavigateToSelectRoom(username)
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(text = stringResource(R.string.next))
                    }
                }
            }
        }
    }
}
