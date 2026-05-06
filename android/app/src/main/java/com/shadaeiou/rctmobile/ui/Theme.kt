package com.shadaeiou.rctmobile.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.shadaeiou.rctmobile.data.ThemePreference

/**
 * Theme colors are park-themed (greens, sunny yellows, ride pinks). We
 * intentionally do *not* use dynamic Material You here because the
 * tile colors in [com.shadaeiou.rctmobile.game.TileCatalog] are tuned
 * against this fixed palette — letting Material You override the
 * surface colors would make rides hard to read on some devices.
 *
 * The park canvas itself stays in its sunny color scheme regardless of
 * dark/light mode (parks are green grass and bright rides in the
 * player's mental model). Dark mode only affects the surrounding chrome:
 * HUD, build palette, dialogs, settings.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5D6A7),
    onPrimaryContainer = Color(0xFF002106),
    secondary = Color(0xFFFF8F00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0B2),
    onSecondaryContainer = Color(0xFF2A1800),
    background = Color(0xFFF5F5DC),
    onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFFFF8E1),
    onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(0xFFE6E0CC),
    onSurfaceVariant = Color(0xFF49463E),
    outline = Color(0xFF7A776F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF003908),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFFFFB74D),
    onSecondary = Color(0xFF3E2723),
    secondaryContainer = Color(0xFF5D4037),
    onSecondaryContainer = Color(0xFFFFE0B2),
    background = Color(0xFF101410),
    onBackground = Color(0xFFE6E2DA),
    surface = Color(0xFF1A1F1A),
    onSurface = Color(0xFFE6E2DA),
    surfaceVariant = Color(0xFF2A2F2A),
    onSurfaceVariant = Color(0xFFB8B5AD),
    outline = Color(0xFF8A8A82),
)

@Composable
fun RctTheme(
    preference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit,
) {
    val isDark = when (preference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (isDark) DarkColors else LightColors,
        content = content,
    )
}
