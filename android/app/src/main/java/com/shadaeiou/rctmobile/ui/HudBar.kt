package com.shadaeiou.rctmobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shadaeiou.rctmobile.data.ParkState
import com.shadaeiou.rctmobile.game.HOURS_PER_DAY

@Composable
fun HudBar(
    state: ParkState,
    onTogglePause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dayFraction = state.tickOfDay.toFloat() / HOURS_PER_DAY
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Stat(
                label = "CASH",
                value = formatMoney(state.moneyCents),
                emphasis = state.moneyCents < 0,
                modifier = Modifier.weight(1f),
            )
            Stat(
                label = "DAY",
                value = "${state.day} . ${formatHour(state.tickOfDay)}",
                modifier = Modifier.weight(1f),
            )
            Stat(
                label = "RATING",
                value = "${state.rating}",
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onTogglePause) {
                Icon(
                    imageVector = if (state.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = if (state.paused) "Resume" else "Pause",
                )
            }
        }
        LinearProgressIndicator(
            progress = { dayFraction },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
    }
}

@Composable
private fun Stat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasis: Boolean = false,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (emphasis) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

internal fun formatMoney(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val abs = kotlin.math.abs(cents)
    val dollars = abs / 100
    return if (dollars >= 1_000_000) "$sign\$${dollars / 1_000}k"
    else "$sign\$${dollars.toString().reversed().chunked(3).joinToString(",").reversed()}"
}

private fun formatHour(hour: Int): String {
    val h12 = ((hour + 11) % 12) + 1
    val ampm = if (hour < 12) "am" else "pm"
    return "${h12}${ampm}"
}
