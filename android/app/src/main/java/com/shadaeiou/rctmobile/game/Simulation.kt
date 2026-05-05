package com.shadaeiou.rctmobile.game

import com.shadaeiou.rctmobile.data.ParkState

const val HOURS_PER_DAY = 24
const val OPEN_HOUR = 9
const val CLOSE_HOUR = 21

/**
 * Pure tick function: given a [ParkState], produce the next one. Always
 * returns a new instance when anything changes; returns the same instance
 * (referentially) when paused so Compose can skip recomposition.
 *
 * No I/O, no side effects. The view model handles autosave and the loop
 * timer; this file is the entire game's economic model.
 */
object Simulation {

    fun tick(state: ParkState): ParkState {
        if (state.paused) return state

        val served = computeServedTiles(state)
        val isOpen = state.tickOfDay in OPEN_HOUR until CLOSE_HOUR

        var rideCount = 0
        var rideExcitement = 0
        val rideTypes = mutableSetOf<TileType>()
        var shopCount = 0
        var restroomCount = 0
        var sceneryBonus = 0
        var maintenance = 0L

        state.tiles.forEach { t ->
            val def = TileCatalog.def(t)
            when (def.kind) {
                TileKind.Ride -> {
                    rideCount += 1
                    rideExcitement += def.excitement
                    rideTypes += t
                    maintenance += def.maintenanceCents
                }
                TileKind.Shop -> {
                    shopCount += 1
                    maintenance += def.maintenanceCents
                }
                TileKind.Facility -> {
                    if (t == TileType.RESTROOM) restroomCount += 1
                    maintenance += def.maintenanceCents
                }
                TileKind.Scenery -> sceneryBonus += def.sceneryBonus
                else -> Unit
            }
        }

        // Park rating: ride excitement + variety + scenery + balanced shops/restrooms.
        val varietyBonus = rideTypes.size * 20
        val rideContrib = rideExcitement * 8
        val expectedRestrooms = (rideCount / 4).coerceAtLeast(if (rideCount > 0) 1 else 0)
        val restroomDeficit = (expectedRestrooms - restroomCount).coerceAtLeast(0)
        val restroomPenalty = if (rideCount > 0 && restroomCount == 0) 80
            else restroomDeficit * 25
        val expectedShops = (rideCount / 3).coerceAtLeast(0)
        val shopBonus = shopCount.coerceAtMost(expectedShops) * 15
        val rawRating = 200 + rideContrib + varietyBonus + sceneryBonus + shopBonus - restroomPenalty
        val newRating = rawRating.coerceIn(0, 1000)

        var revenue = 0L
        var guestsThisTick = 0L
        if (isOpen) {
            val ratingFactor = newRating / 1000.0
            for (idx in served) {
                val type = state.tiles[idx]
                val def = TileCatalog.def(type)
                when (def.kind) {
                    TileKind.Ride -> {
                        val capacity = (def.excitement * 6).coerceAtLeast(1)
                        val visitors = (capacity * ratingFactor).toLong().coerceAtLeast(0)
                        revenue += visitors * def.hourlyFeeCents
                        guestsThisTick += visitors
                    }
                    TileKind.Shop -> {
                        val visitors = (4 * ratingFactor).toLong().coerceAtLeast(0)
                        revenue += visitors * def.hourlyFeeCents
                    }
                    else -> Unit
                }
            }
        }

        val net = revenue - maintenance
        val newTickOfDay = (state.tickOfDay + 1) % HOURS_PER_DAY
        val newDay = if (newTickOfDay == 0) state.day + 1 else state.day

        return state.copy(
            moneyCents = state.moneyCents + net,
            day = newDay,
            tickOfDay = newTickOfDay,
            rating = newRating,
            totalGuests = state.totalGuests + guestsThisTick,
            lifetimeRevenueCents = state.lifetimeRevenueCents + revenue.coerceAtLeast(0),
        )
    }

    /**
     * Indices of ride/shop/facility tiles that have at least one
     * orthogonal neighbor on a path which is itself reachable from the
     * entrance via path tiles.
     *
     * Implementation: 4-neighbor BFS over PATH and ENTRANCE tiles seeded
     * from every ENTRANCE; then a single pass to mark non-path tiles
     * adjacent to the visited set.
     */
    private fun computeServedTiles(state: ParkState): Set<Int> {
        val w = state.gridWidth
        val h = state.gridHeight
        val total = w * h
        val pathReachable = BooleanArray(total)
        val queue = ArrayDeque<Int>()
        state.tiles.forEachIndexed { idx, t ->
            if (t == TileType.ENTRANCE) {
                pathReachable[idx] = true
                queue.add(idx)
            }
        }
        while (queue.isNotEmpty()) {
            val idx = queue.removeFirst()
            for (n in neighbors(idx, w, h)) {
                if (pathReachable[n]) continue
                val nt = state.tiles[n]
                if (nt == TileType.PATH || nt == TileType.ENTRANCE) {
                    pathReachable[n] = true
                    queue.add(n)
                }
            }
        }

        val served = mutableSetOf<Int>()
        for (idx in 0 until total) {
            val t = state.tiles[idx]
            val kind = TileCatalog.def(t).kind
            if (kind != TileKind.Ride && kind != TileKind.Shop && kind != TileKind.Facility) continue
            if (neighbors(idx, w, h).any { pathReachable[it] }) {
                served += idx
            }
        }
        return served
    }

    private fun neighbors(idx: Int, w: Int, h: Int): List<Int> {
        val x = idx % w
        val y = idx / w
        val out = ArrayList<Int>(4)
        if (x > 0) out += idx - 1
        if (x < w - 1) out += idx + 1
        if (y > 0) out += idx - w
        if (y < h - 1) out += idx + w
        return out
    }
}
