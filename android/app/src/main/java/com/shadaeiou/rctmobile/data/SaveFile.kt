package com.shadaeiou.rctmobile.data

import com.shadaeiou.rctmobile.game.TileType
import kotlinx.serialization.Serializable

/**
 * Bumped any time the on-disk shape of [SaveFile] or anything reachable
 * from [ParkState] changes in a non-additive way (renamed field, changed
 * meaning, narrowed type, removed enum value, etc.).
 *
 * When you bump this, register a migration in [Migrations.migrations]
 * for the previous version. Migrations operate on the raw JSON, not the
 * Kotlin model.
 */
const val CURRENT_SCHEMA_VERSION = 1

const val DEFAULT_GRID_WIDTH = 10
const val DEFAULT_GRID_HEIGHT = 10

/** $20,000.00 in cents. */
const val STARTING_CASH_CENTS = 2_000_000L

@Serializable
data class SaveFile(
    val schemaVersion: Int,
    val park: ParkState,
)

@Serializable
data class ParkState(
    val name: String = "Sunny Acres",
    val moneyCents: Long = STARTING_CASH_CENTS,
    val day: Int = 1,
    val tickOfDay: Int = 8,
    val rating: Int = 250,
    val totalGuests: Long = 0,
    val lifetimeRevenueCents: Long = 0,
    val gridWidth: Int = DEFAULT_GRID_WIDTH,
    val gridHeight: Int = DEFAULT_GRID_HEIGHT,
    val tiles: List<TileType> = defaultTiles(DEFAULT_GRID_WIDTH, DEFAULT_GRID_HEIGHT),
    val paused: Boolean = false,
) {
    fun tileAt(x: Int, y: Int): TileType {
        if (x !in 0 until gridWidth || y !in 0 until gridHeight) return TileType.EMPTY
        return tiles[y * gridWidth + x]
    }

    fun withTile(x: Int, y: Int, t: TileType): ParkState {
        if (x !in 0 until gridWidth || y !in 0 until gridHeight) return this
        val idx = y * gridWidth + x
        if (tiles[idx] == t) return this
        val next = tiles.toMutableList().also { it[idx] = t }
        return copy(tiles = next)
    }
}

private fun defaultTiles(w: Int, h: Int): List<TileType> {
    val list = MutableList(w * h) { TileType.EMPTY }
    list[(h - 1) * w + w / 2] = TileType.ENTRANCE
    return list
}
