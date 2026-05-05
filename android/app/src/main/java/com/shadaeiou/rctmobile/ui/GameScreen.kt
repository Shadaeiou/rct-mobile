package com.shadaeiou.rctmobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shadaeiou.rctmobile.game.GameViewModel
import com.shadaeiou.rctmobile.game.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    onOpenSettings: () -> Unit,
    vm: GameViewModel = viewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val selected by vm.selectedBuild.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val name = (ui as? UiState.Playing)?.state?.name ?: "RCT Mobile"
                    Text(name)
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            when (val s = ui) {
                UiState.Loading -> LoadingState()
                is UiState.Corrupted -> CorruptedState(s.reason, onStartFresh = vm::discardCorruptedAndStartFresh)
                is UiState.Playing -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        HudBar(state = s.state, onTogglePause = vm::togglePause)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            ParkCanvas(
                                state = s.state,
                                selectedBuild = selected,
                                onTap = vm::tap,
                                onLongPress = vm::longPress,
                                modifier = Modifier.fillMaxSize(),
                            )
                            if (s.state.paused) {
                                PauseOverlay()
                            }
                        }
                        BuildPalette(
                            selected = selected,
                            moneyCents = s.state.moneyCents,
                            onSelect = vm::selectBuild,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text("Opening the gates...", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CorruptedState(reason: String, onStartFresh: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* user must choose */ },
        title = { Text("Save couldn't be loaded") },
        text = {
            Column {
                Text("Your park save is on disk but couldn't be parsed.")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Reinstall the previous version to keep playing the same park, " +
                        "or start a new park (this will replace the old save on disk).",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onStartFresh) { Text("Start a new park") }
        },
    )
}

@Composable
private fun PauseOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0x66000000)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "PAUSED",
            style = MaterialTheme.typography.headlineLarge,
            color = androidx.compose.ui.graphics.Color.White,
            textAlign = TextAlign.Center,
        )
    }
}
