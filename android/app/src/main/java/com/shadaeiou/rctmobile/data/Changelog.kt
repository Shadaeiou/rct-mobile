package com.shadaeiou.rctmobile.data

data class ReleaseNote(
    val version: String,
    val date: String,
    val bullets: List<String>,
)

val CHANGELOG: List<ReleaseNote> = listOf(
    ReleaseNote(
        version = "0.1.1",
        date = "2026-05-06",
        bullets = listOf(
            "Dark mode: full dark color scheme for HUD, palette, dialogs, and settings",
            "Theme picker in Settings → Appearance: System / Light / Dark",
            "Theme choice persists across updates (lives in SharedPreferences, separate from your park save)",
        ),
    ),
    ReleaseNote(
        version = "0.1.0",
        date = "2026-05-05",
        bullets = listOf(
            "Initial park sandbox: 10x10 grid, $20,000 starting cash",
            "Five rides, three shops, restrooms, and three scenery types",
            "Per-tick simulation: park rating, guest demand, ride revenue, maintenance",
            "Path connectivity: rides only earn when reachable from the entrance",
            "Save format v1 with forward-only migration chain",
            "Auto-update from GitHub Releases on every push to main",
        ),
    ),
)
