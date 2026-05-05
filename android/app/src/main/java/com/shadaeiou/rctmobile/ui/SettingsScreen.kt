package com.shadaeiou.rctmobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shadaeiou.rctmobile.BuildConfig
import com.shadaeiou.rctmobile.data.CHANGELOG
import com.shadaeiou.rctmobile.data.DownloadResult
import com.shadaeiou.rctmobile.data.ReleaseNote
import com.shadaeiou.rctmobile.data.UpdateInfo
import com.shadaeiou.rctmobile.data.Updater
import com.shadaeiou.rctmobile.game.GameViewModel
import com.shadaeiou.rctmobile.game.UiState
import kotlinx.coroutines.launch

private sealed class UpdateUi {
    data object Idle : UpdateUi()
    data object Checking : UpdateUi()
    data class Available(val info: UpdateInfo) : UpdateUi()
    data object Downloading : UpdateUi()
    data object UpToDate : UpdateUi()
    data class Error(val message: String) : UpdateUi()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: GameViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updater = remember { Updater(context.applicationContext) }
    var update by remember { mutableStateOf<UpdateUi>(UpdateUi.Idle) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    val gameUi by vm.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            (gameUi as? UiState.Playing)?.let { ParkStatsBlock(it) }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("Updates", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            val canCheck = update is UpdateUi.Idle ||
                update is UpdateUi.UpToDate ||
                update is UpdateUi.Error
            Button(
                enabled = canCheck,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    update = UpdateUi.Checking
                    scope.launch {
                        val info = runCatching {
                            updater.checkForUpdate(BuildConfig.VERSION_CODE)
                        }.getOrElse {
                            update = UpdateUi.Error(it.message ?: "Update check failed")
                            return@launch
                        }
                        update = if (info != null) UpdateUi.Available(info) else UpdateUi.UpToDate
                    }
                },
            ) {
                Text(
                    when (update) {
                        is UpdateUi.Checking -> "Checking..."
                        is UpdateUi.Downloading -> "Downloading..."
                        else -> "Check for updates"
                    },
                )
            }

            when (val u = update) {
                is UpdateUi.UpToDate -> {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "You're on the latest build (${BuildConfig.VERSION_NAME}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is UpdateUi.Error -> {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        u.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                else -> Unit
            }

            (update as? UpdateUi.Available)?.let { available ->
                AlertDialog(
                    onDismissRequest = { update = UpdateUi.Idle },
                    title = { Text("Update available") },
                    text = {
                        Column {
                            Text("Version ${available.info.versionName} is ready to install.")
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Your park save survives the update.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            available.info.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            update = UpdateUi.Downloading
                            scope.launch {
                                val downloadId = updater.startDownload(available.info)
                                when (val result = updater.awaitDownload(downloadId)) {
                                    DownloadResult.Success -> {
                                        updater.launchInstall(downloadId)
                                        update = UpdateUi.Idle
                                    }
                                    is DownloadResult.Failure -> {
                                        update = UpdateUi.Error("Update failed: ${result.reason}")
                                    }
                                }
                            }
                        }) { Text("Update") }
                    },
                    dismissButton = {
                        TextButton(onClick = { update = UpdateUi.Idle }) { Text("Later") }
                    },
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Version ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))
            ChangelogSection()

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("Danger zone", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Wipe your park save and start over. This deletes everything you've " +
                    "built. Updates do not delete your save — only this button does.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { confirmReset = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Reset park")
            }

            if (confirmReset) {
                AlertDialog(
                    onDismissRequest = { confirmReset = false },
                    title = { Text("Reset park?") },
                    text = {
                        Text(
                            "This permanently deletes your save. There's no undo. " +
                                "Are you sure?",
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                confirmReset = false
                                vm.resetPark()
                                onBack()
                            },
                        ) {
                            Text("Yes, wipe it")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
                    },
                )
            }
        }
    }
}

@Composable
private fun ParkStatsBlock(playing: UiState.Playing) {
    val s = playing.state
    Text("Park stats", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    StatRow("Park name", s.name)
    StatRow("Day", "${s.day}")
    StatRow("Cash on hand", formatMoney(s.moneyCents))
    StatRow("Park rating", "${s.rating} / 1000")
    StatRow("Lifetime guests", s.totalGuests.toString())
    StatRow("Lifetime revenue", formatMoney(s.lifetimeRevenueCents))
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ChangelogSection() {
    if (CHANGELOG.isEmpty()) return
    Column {
        Text("What's new", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        ReleaseNoteEntry(CHANGELOG.first())

        if (CHANGELOG.size > 1) {
            var expanded by rememberSaveable { mutableStateOf(false) }
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Hide older updates" else "Show all updates")
            }
            if (expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CHANGELOG.drop(1).forEach { ReleaseNoteEntry(it) }
                }
            }
        }
    }
}

@Composable
private fun ReleaseNoteEntry(entry: ReleaseNote) {
    Column {
        Text(
            text = "${entry.version} . ${entry.date}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        entry.bullets.forEach { line ->
            Text(
                text = "• $line",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
