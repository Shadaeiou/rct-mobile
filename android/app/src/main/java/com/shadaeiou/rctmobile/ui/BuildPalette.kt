package com.shadaeiou.rctmobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shadaeiou.rctmobile.game.TileCatalog
import com.shadaeiou.rctmobile.game.TileDef
import com.shadaeiou.rctmobile.game.TileKind
import com.shadaeiou.rctmobile.game.TileType

@Composable
fun BuildPalette(
    selected: TileType?,
    moneyCents: Long,
    onSelect: (TileType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var category by rememberSaveable { mutableStateOf(TileKind.Path) }

    val items = remember(category) {
        TileCatalog.placeable.filter { it.kind == category }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CategoryTab("Path", category == TileKind.Path) { category = TileKind.Path }
            CategoryTab("Rides", category == TileKind.Ride) { category = TileKind.Ride }
            CategoryTab("Shops", category == TileKind.Shop) { category = TileKind.Shop }
            CategoryTab("WC", category == TileKind.Facility) { category = TileKind.Facility }
            CategoryTab("Decor", category == TileKind.Scenery) { category = TileKind.Scenery }
        }
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items, key = { it.type }) { def ->
                BuildChip(
                    def = def,
                    selected = selected == def.type,
                    affordable = moneyCents >= def.costCents,
                    onClick = { onSelect(if (selected == def.type) null else def.type) },
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val hint = when {
                selected == null -> "Pick a tile, then tap a cell. Long-press a placed tile to demolish for 50% refund."
                else -> "Tap an empty cell to place ${TileCatalog.def(selected).label}."
            }
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CategoryTab(label: String, active: Boolean, onClick: () -> Unit) {
    val bg = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

@Composable
private fun BuildChip(
    def: TileDef,
    selected: Boolean,
    affordable: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = when {
        selected -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(def.color),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = def.emoji,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
        }
        Text(
            text = def.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = formatMoney(def.costCents),
            style = MaterialTheme.typography.labelSmall,
            color = if (affordable) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFD32F2F),
        )
    }
}
