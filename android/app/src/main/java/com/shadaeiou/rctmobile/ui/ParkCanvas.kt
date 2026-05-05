package com.shadaeiou.rctmobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.shadaeiou.rctmobile.data.ParkState
import com.shadaeiou.rctmobile.game.TileCatalog
import com.shadaeiou.rctmobile.game.TileType

@Composable
fun ParkCanvas(
    state: ParkState,
    selectedBuild: TileType?,
    onTap: (Int, Int) -> Unit,
    onLongPress: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val w = state.gridWidth
        val h = state.gridHeight
        val cellSize = remember(maxWidth, maxHeight, w, h) {
            val byWidth = maxWidth / w
            val byHeight = maxHeight / h
            if (byWidth < byHeight) byWidth else byHeight
        }
        val gridW = cellSize * w
        val gridH = cellSize * h
        val density = LocalDensity.current
        val measurer = rememberTextMeasurer()

        val cellPx = with(density) { cellSize.toPx() }
        val gridLineColor = Color(0x33000000)
        val highlightColor = Color(0x66FFFFFF)

        Canvas(
            modifier = Modifier
                .size(gridW, gridH)
                .pointerInput(w, h, cellPx) {
                    detectTapGestures(
                        onTap = { offset ->
                            val tx = (offset.x / cellPx).toInt()
                            val ty = (offset.y / cellPx).toInt()
                            if (tx in 0 until w && ty in 0 until h) onTap(tx, ty)
                        },
                        onLongPress = { offset ->
                            val tx = (offset.x / cellPx).toInt()
                            val ty = (offset.y / cellPx).toInt()
                            if (tx in 0 until w && ty in 0 until h) onLongPress(tx, ty)
                        },
                    )
                },
        ) {
            val pad = cellPx * 0.04f
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val type = state.tileAt(x, y)
                    val def = TileCatalog.def(type)
                    val left = x * cellPx
                    val top = y * cellPx
                    drawRect(
                        color = def.color,
                        topLeft = Offset(left, top),
                        size = Size(cellPx, cellPx),
                    )
                    drawRect(
                        color = gridLineColor,
                        topLeft = Offset(left, top),
                        size = Size(cellPx, cellPx),
                        style = Stroke(width = 1f),
                    )
                    if (def.glyph.isNotEmpty()) {
                        val style = TextStyle(
                            color = Color.White,
                            fontSize = (cellPx / density.density * 0.45f).sp,
                            textAlign = TextAlign.Center,
                        )
                        val layout = measurer.measure(def.glyph, style)
                        val tx = left + (cellPx - layout.size.width) / 2f
                        val ty = top + (cellPx - layout.size.height) / 2f
                        drawText(layout, topLeft = Offset(tx, ty))
                    }
                    // ghost preview of selected build on EMPTY cells
                    if (selectedBuild != null && type == TileType.EMPTY) {
                        val bdef = TileCatalog.def(selectedBuild)
                        drawRect(
                            color = bdef.color.copy(alpha = 0.18f),
                            topLeft = Offset(left + pad, top + pad),
                            size = Size(cellPx - 2 * pad, cellPx - 2 * pad),
                        )
                    }
                }
            }
            // entrance highlight: pulse-style ring
            for (y in 0 until h) for (x in 0 until w) {
                if (state.tileAt(x, y) == TileType.ENTRANCE) {
                    drawRect(
                        color = highlightColor,
                        topLeft = Offset(x * cellPx + pad, y * cellPx + pad),
                        size = Size(cellPx - 2 * pad, cellPx - 2 * pad),
                        style = Stroke(width = 3f),
                    )
                }
            }
        }
    }
}
