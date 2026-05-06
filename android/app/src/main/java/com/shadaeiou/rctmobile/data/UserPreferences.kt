package com.shadaeiou.rctmobile.data

import android.content.Context

/**
 * Persisted UI preference for the app theme.
 *
 * Stored separately from [ParkState] (the park save) because it's a UI
 * choice, not game data. Lives in SharedPreferences so it survives
 * uninstall-free updates the same way the park save does. Don't add a
 * code path that clears these prefs on update — see CLAUDE.md.
 */
enum class ThemePreference { SYSTEM, LIGHT, DARK }

class UserPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    var theme: ThemePreference
        get() = prefs.getString(KEY_THEME, null)
            ?.let { name -> runCatching { ThemePreference.valueOf(name) }.getOrNull() }
            ?: ThemePreference.SYSTEM
        set(value) { prefs.edit().putString(KEY_THEME, value.name).apply() }

    companion object {
        private const val KEY_THEME = "theme_preference"
    }
}
