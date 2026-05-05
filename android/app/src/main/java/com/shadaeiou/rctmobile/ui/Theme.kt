package com.shadaeiou.rctmobile.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Theme colors are park-themed (greens, sunny yellows, ride pinks). We
 * intentionally do *not* use dynamic Material You here because the
 * tile colors in [com.shadaeiou.rctmobile.game.TileCatalog] are tuned
 * against this fixed palette — letting Material You override the
 * surface colors would make rides hard to read on some devices.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5D6A7),
    onPrimaryContainer = Color(0xFF002106),
    secondary = Color(0xFFFF8F00),
    onSecondary = Color.White,
    background = Color(0xFFF5F5DC),
    surface = Color(0xFFFFF8E1),
    onBackground = Color(0xFF1B1B1B),
    onSurface = Color(0xFF1B1B1B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF003908),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFFFFB74D),
    onSecondary = Color(0xFF3E2723),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
)

@Composable
fun RctTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
