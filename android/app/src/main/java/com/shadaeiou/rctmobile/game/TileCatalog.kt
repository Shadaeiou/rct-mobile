package com.shadaeiou.rctmobile.game

import androidx.compose.ui.graphics.Color

/**
 * The serialized name of each enum entry is what ends up on disk in
 * `park.json`. **Do not rename an existing entry without writing a save
 * migration that rewrites the old name in the JSON.** Adding a new entry
 * is fine — saves predating it simply won't reference it.
 */
enum class TileType {
    EMPTY,
    ENTRANCE,
    PATH,
    CAROUSEL, BUMPER_CARS, FERRIS_WHEEL, DROP_TOWER, COASTER,
    FOOD_STALL, DRINK_STALL, SOUVENIR_SHOP,
    RESTROOM,
    TREE, FOUNTAIN, BENCH,
}

enum class TileKind {
    Special, Path, Ride, Shop, Facility, Scenery,
}

data class TileDef(
    val type: TileType,
    val label: String,
    val glyph: String,
    val emoji: String,
    val kind: TileKind,
    val color: Color,
    val costCents: Long,
    val excitement: Int = 0,
    val hourlyFeeCents: Long = 0,
    val maintenanceCents: Long = 0,
    val sceneryBonus: Int = 0,
)

object TileCatalog {

    val all: List<TileDef> = listOf(
        TileDef(TileType.EMPTY, "Empty", "", "·", TileKind.Special, Color(0xFFB8E27A), 0L),
        TileDef(TileType.ENTRANCE, "Entrance", "E", "🚪", TileKind.Special, Color(0xFFFFC107), 0L),
        TileDef(TileType.PATH, "Path", "·", "▦", TileKind.Path, Color(0xFFD7CCC8), 5_00L),

        TileDef(
            TileType.CAROUSEL, "Carousel", "C", "🎠", TileKind.Ride, Color(0xFFE91E63),
            costCents = 1_500_00L, excitement = 4, hourlyFeeCents = 250L, maintenanceCents = 60L,
        ),
        TileDef(
            TileType.BUMPER_CARS, "Bumper Cars", "B", "🚗", TileKind.Ride, Color(0xFF3F51B5),
            costCents = 2_500_00L, excitement = 5, hourlyFeeCents = 350L, maintenanceCents = 100L,
        ),
        TileDef(
            TileType.FERRIS_WHEEL, "Ferris Wheel", "F", "🎡", TileKind.Ride, Color(0xFF673AB7),
            costCents = 4_000_00L, excitement = 6, hourlyFeeCents = 500L, maintenanceCents = 160L,
        ),
        TileDef(
            TileType.DROP_TOWER, "Drop Tower", "D", "🏗", TileKind.Ride, Color(0xFFFF5722),
            costCents = 6_000_00L, excitement = 8, hourlyFeeCents = 700L, maintenanceCents = 250L,
        ),
        TileDef(
            TileType.COASTER, "Coaster", "X", "🎢", TileKind.Ride, Color(0xFFD32F2F),
            costCents = 10_000_00L, excitement = 10, hourlyFeeCents = 1_000L, maintenanceCents = 400L,
        ),

        TileDef(
            TileType.FOOD_STALL, "Food Stall", "f", "🍔", TileKind.Shop, Color(0xFFFF9800),
            costCents = 600_00L, hourlyFeeCents = 200L, maintenanceCents = 30L,
        ),
        TileDef(
            TileType.DRINK_STALL, "Drink Stall", "d", "🥤", TileKind.Shop, Color(0xFF03A9F4),
            costCents = 400_00L, hourlyFeeCents = 150L, maintenanceCents = 20L,
        ),
        TileDef(
            TileType.SOUVENIR_SHOP, "Souvenirs", "s", "🎁", TileKind.Shop, Color(0xFF8BC34A),
            costCents = 800_00L, hourlyFeeCents = 250L, maintenanceCents = 40L,
        ),

        TileDef(
            TileType.RESTROOM, "Restroom", "R", "🚻", TileKind.Facility, Color(0xFF9E9E9E),
            costCents = 300_00L, maintenanceCents = 15L,
        ),

        TileDef(TileType.TREE, "Tree", "T", "🌳", TileKind.Scenery, Color(0xFF388E3C),
            costCents = 50_00L, sceneryBonus = 3),
        TileDef(TileType.FOUNTAIN, "Fountain", "O", "⛲", TileKind.Scenery, Color(0xFF4FC3F7),
            costCents = 200_00L, sceneryBonus = 8),
        TileDef(TileType.BENCH, "Bench", "b", "🪑", TileKind.Scenery, Color(0xFF795548),
            costCents = 30_00L, sceneryBonus = 2),
    )

    private val byType: Map<TileType, TileDef> = all.associateBy { it.type }

    fun def(t: TileType): TileDef = byType.getValue(t)

    /** Tiles the player can place from the build palette. */
    val placeable: List<TileDef> = all.filter {
        it.type != TileType.EMPTY && it.type != TileType.ENTRANCE
    }
}
