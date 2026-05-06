package com.shadaeiou.rctmobile

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shadaeiou.rctmobile.data.DownloadResult
import com.shadaeiou.rctmobile.data.Updater
import com.shadaeiou.rctmobile.game.GameViewModel
import com.shadaeiou.rctmobile.ui.GameScreen
import com.shadaeiou.rctmobile.ui.RctTheme
import com.shadaeiou.rctmobile.ui.SettingsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val gameVm: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleAutoUpdateIntent(intent)
        setContent {
            val themePref by gameVm.themePreference.collectAsStateWithLifecycle()
            RctTheme(preference = themePref) {
                RequestNotificationPermissionIfNeeded()
                Surface(modifier = Modifier.fillMaxSize()) {
                    val nav = rememberNavController()
                    NavHost(navController = nav, startDestination = "game") {
                        composable("game") {
                            GameScreen(
                                onOpenSettings = { nav.navigate("settings") },
                                vm = gameVm,
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onBack = { nav.popBackStack() },
                                vm = gameVm,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAutoUpdateIntent(intent)
    }

    private fun handleAutoUpdateIntent(intent: Intent?) {
        if (intent == null) return
        if (!intent.getBooleanExtra(EXTRA_AUTO_UPDATE, false)) return
        intent.removeExtra(EXTRA_AUTO_UPDATE)
        startAutoUpdateFlow()
    }

    private fun startAutoUpdateFlow() {
        val appContext = applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                Toast.makeText(appContext, "Checking for update...", Toast.LENGTH_SHORT).show()
            }
            val updater = Updater(appContext)
            val info = runCatching {
                updater.checkForUpdate(BuildConfig.VERSION_CODE)
            }.getOrNull()

            if (info == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        appContext,
                        "You're already on the latest build.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    appContext,
                    "Downloading v${info.versionName}...",
                    Toast.LENGTH_SHORT,
                ).show()
            }

            val id = updater.startDownload(info)
            when (val result = updater.awaitDownload(id)) {
                DownloadResult.Success -> updater.launchInstall(id)
                is DownloadResult.Failure -> withContext(Dispatchers.Main) {
                    Toast.makeText(
                        appContext,
                        "Update failed: ${result.reason}",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    companion object {
        const val EXTRA_AUTO_UPDATE = "auto_update"
    }
}

/**
 * On Android 13+, POST_NOTIFICATIONS is a runtime permission. Declaring
 * it in the manifest only makes the OS willing to ask the user — it
 * doesn't ask on its own. Without this prompt, FCM update pushes
 * silently no-op on the device.
 *
 * The OS handles "user denied twice" itself: the launcher then becomes
 * a no-op, so we can call this on every launch without spamming.
 */
@Composable
private fun RequestNotificationPermissionIfNeeded() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* user choice; nothing to do here */ }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
