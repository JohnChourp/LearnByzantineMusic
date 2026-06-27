package com.johnchourp.learnbyzantinemusic.modes.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.modes.ModeScaleGenus
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmOutline
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurface
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import kotlin.math.abs

private val PILL_COLUMN_WIDTH = 96.dp
private val PILL_HEIGHT = 40.dp
private const val DP_PER_MORIA = 3.6f
// Floor ≥ pill height + a gap so adjacent pills never overlap and every φθόγγος owns its own band.
private const val MIN_SEGMENT_DP = 44f

/**
 * The interactive 8 Ήχοι scale "ladder": a column of genus-tinted cells whose heights honestly
 * encode the μόρια of each διάστημα, with the φθόγγος names as press-and-hold pills on the right.
 * Pressing/holding/dragging on the pill column reports the touched φθόγγος via [onActiveIndexChange]
 * (the host plays the matching tone); releasing — or dragging off the top/bottom — reports -1.
 * TalkBack users activate a pill to pulse its tone via [onAccessibilityPlay]. Everything is keyed
 * top → bottom so it lines up with the labels.
 */
@Composable
fun ModeScaleDiagram(
    phthongsTopToBottom: List<String>,
    intervalsTopToBottom: List<Int>,
    genus: ModeScaleGenus,
    tonicIndices: Set<Int>,
    activeIndex: Int,
    onActiveIndexChange: (Int) -> Unit,
    onAccessibilityPlay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (intervalsTopToBottom.isEmpty() ||
        phthongsTopToBottom.size != intervalsTopToBottom.size + 1
    ) {
        return
    }

    val accent = ModeGenusPalette.accent(genus)
    val cellFill = accent.container
    val cellInk = accent.content
    val pillHalf = PILL_HEIGHT.value / 2f

    // Segment heights (dp) encode μόρια directly, with a floor so tiny intervals stay legible.
    val segmentHeights = remember(intervalsTopToBottom) {
        intervalsTopToBottom.map { maxOf(MIN_SEGMENT_DP, it * DP_PER_MORIA) }
    }
    // Boundary centre (dp) for each φθόγγος, top → bottom. Inset by pillHalf so the first/last pills
    // sit fully inside the (single) gesture surface.
    val boundaries = remember(segmentHeights, pillHalf) {
        val out = ArrayList<Float>(segmentHeights.size + 1)
        var y = pillHalf
        out.add(y)
        for (h in segmentHeights) {
            y += h
            out.add(y)
        }
        out
    }
    val totalHeight = boundaries.last() + pillHalf

    // Always invoke the freshest callback from the long-lived gesture coroutine, so a base-shift
    // change (which updates frequencies but not the gesture keys) is reflected on the next press.
    val currentOnActive by rememberUpdatedState(onActiveIndexChange)

    val numberStyle = MaterialTheme.typography.titleMedium.copy(
        color = cellInk,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    val textMeasurer = rememberTextMeasurer()

    Box(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().height(totalHeight.dp)) {
            // ---- Left: the genus-tinted μόρια cells ----
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 10.dp),
            ) {
                val chartW = size.width
                val octaveColor = cellInk.copy(alpha = 0.45f)
                val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                for (i in segmentHeights.indices) {
                    val top = boundaries[i].dp.toPx()
                    val bottom = boundaries[i + 1].dp.toPx()
                    val inset = 1.6.dp.toPx()
                    val cellTop = top + inset
                    val cellBottom = bottom - inset
                    if (cellBottom <= cellTop) continue
                    drawRoundRect(
                        color = cellFill,
                        topLeft = Offset(0f, cellTop),
                        size = Size(chartW, cellBottom - cellTop),
                        cornerRadius = CornerRadius(10.dp.toPx()),
                    )
                    drawRoundRect(
                        color = cellInk.copy(alpha = 0.20f),
                        topLeft = Offset(0f, cellTop),
                        size = Size(chartW, cellBottom - cellTop),
                        cornerRadius = CornerRadius(10.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                    val label = intervalsTopToBottom[i].toString()
                    val measured = textMeasurer.measure(label, numberStyle)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            (chartW - measured.size.width) / 2f,
                            (cellTop + cellBottom) / 2f - measured.size.height / 2f,
                        ),
                    )
                }
                // Octave/base separator lines.
                for (i in boundaries.indices) {
                    if (i in tonicIndices) {
                        val y = boundaries[i].dp.toPx()
                        drawLine(
                            color = octaveColor,
                            start = Offset(0f, y),
                            end = Offset(chartW, y),
                            strokeWidth = 1.4.dp.toPx(),
                            pathEffect = dash,
                        )
                    }
                }
                // Active φθόγγος accent line, linking the cell to the lit pill.
                if (activeIndex in boundaries.indices) {
                    val y = boundaries[activeIndex].dp.toPx()
                    drawLine(
                        color = LbmBrown,
                        start = Offset(0f, y),
                        end = Offset(chartW, y),
                        strokeWidth = 2.4.dp.toPx(),
                    )
                }
            }

            // ---- Right: the press-and-hold φθόγγος pills ----
            Box(
                modifier = Modifier
                    .width(PILL_COLUMN_WIDTH)
                    .fillMaxHeight()
                    .pointerInput(boundaries) {
                        val heightPx = size.height.toFloat()
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var last = indexForY(down.position.y, boundaries, this, heightPx)
                                currentOnActive(last)
                                down.consume()
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: break
                                    if (!change.pressed) break
                                    val idx = indexForY(change.position.y, boundaries, this, heightPx)
                                    if (idx != last) {
                                        last = idx
                                        currentOnActive(idx)
                                    }
                                    change.consume()
                                }
                                currentOnActive(-1)
                            }
                        }
                    },
            ) {
                for (i in phthongsTopToBottom.indices) {
                    NotePill(
                        label = phthongsTopToBottom[i],
                        isTonic = i in tonicIndices,
                        isActive = i == activeIndex,
                        accentInk = cellInk,
                        contentDescription = stringResource(
                            if (i in tonicIndices) {
                                R.string.cd_eight_modes_base_note_pill
                            } else {
                                R.string.cd_eight_modes_note_pill
                            },
                            phthongsTopToBottom[i],
                        ),
                        onAccessibilityPlay = { onAccessibilityPlay(i) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (boundaries[i] - pillHalf).dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NotePill(
    label: String,
    isTonic: Boolean,
    isActive: Boolean,
    accentInk: Color,
    contentDescription: String,
    onAccessibilityPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(targetValue = if (isActive) 1.12f else 1f, label = "pillScale")
    val container by animateColorAsState(
        targetValue = when {
            isActive -> LbmBrown
            isTonic -> accentInk.copy(alpha = 0.16f)
            else -> LbmSurface
        },
        label = "pillBg",
    )
    val border by animateColorAsState(
        targetValue = when {
            isActive -> LbmBrown
            isTonic -> accentInk
            else -> LbmOutline
        },
        label = "pillBorder",
    )
    val textColor = when {
        isActive -> Color.White
        isTonic -> accentInk
        else -> LbmTextPrimary
    }
    Box(
        modifier = modifier
            .height(PILL_HEIGHT)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = if (isActive) 10f else 0f
                shape = RoundedCornerShape(50)
                clip = false
            }
            .clip(RoundedCornerShape(50))
            .background(container)
            .border(if (isActive || isTonic) 1.5.dp else 1.dp, border, RoundedCornerShape(50))
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
                onClick { onAccessibilityPlay(); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            fontWeight = if (isTonic || isActive) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/**
 * Maps a touch y (px) to the nearest φθόγγος boundary index, or -1 when the finger is past the
 * top/bottom of the ladder (so a drag off the ends stops the tone, matching the legacy view).
 * [boundaries] are in dp.
 */
private fun indexForY(
    y: Float,
    boundaries: List<Float>,
    density: Density,
    heightPx: Float,
): Int {
    if (y < 0f || y > heightPx) return -1
    var best = 0
    var bestDist = Float.MAX_VALUE
    with(density) {
        for (i in boundaries.indices) {
            val d = abs(y - boundaries[i].dp.toPx())
            if (d < bestDist) {
                bestDist = d
                best = i
            }
        }
    }
    return best
}
