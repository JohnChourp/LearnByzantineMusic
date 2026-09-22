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
import androidx.compose.material3.minimumInteractiveComponentSize

private val PILL_COLUMN_WIDTH = 96.dp
private val PILL_HEIGHT = 26.dp
// Cell heights stay proportional to μόρια so the ladder visually teaches interval sizes; the floor is
// only big enough to keep the number legible (a too-tall floor would draw a 4-μόρια step as tall as a
// 12-μόρια one). Touch stays forgiving via the full-width, no-dead-zone nearest-boundary gesture, so
// the small floor does not hurt usability even though the densest chromatic pills slightly overlap.
private const val DP_PER_MORIA = 4.0f
private const val MIN_SEGMENT_DP = 22f

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
    // Hoisted: a Canvas DrawScope is not a composable scope, so theme colours must be read here.
    val activeLineColor = LbmBrown

    // Distance of each φθόγγος from the mode's base, top → bottom, for the spoken description.
    // Derived from the very intervals the cells are drawn from, so what TalkBack says and what the
    // diagram shows cannot disagree.
    val moriaFromBase = remember(intervalsTopToBottom, tonicIndices) {
        moriaFromNearestBaseBelow(intervalsTopToBottom, tonicIndices)
    }
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
                        color = activeLineColor,
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
                        val widthPx = size.width.toFloat()
                        // -1 when the finger is off the pill column (sideways) or past the top/bottom,
                        // so the tone stops the moment the finger leaves the note controls.
                        fun resolve(pos: Offset): Int =
                            if (pos.x < 0f || pos.x > widthPx) -1
                            else indexForY(pos.y, boundaries, this, heightPx)
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var last = resolve(down.position)
                                currentOnActive(last)
                                down.consume()
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: break
                                    if (!change.pressed) break
                                    val idx = resolve(change.position)
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
                        // TalkBack must say the φθόγγος AND how far it sits from the base, because
                        // the μόρια are the information the diagram exists to convey — a sighted
                        // user reads them off the cell heights, and a screen-reader user otherwise
                        // gets only a list of names (ClickUp 869f4tpju).
                        contentDescription = phthongDescription(
                            label = phthongsTopToBottom[i],
                            moriaFromBase = moriaFromBase.getOrNull(i),
                            isTonic = i in tonicIndices,
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
            // The pill is 26dp because the ladder packs 22 of them into one screen; growing it
            // would break the diagram. The TOUCH target is widened instead — Material's minimum
            // interactive size gives the accessibility services a 48dp box around a smaller visual,
            // which is exactly the case it exists for.
            .minimumInteractiveComponentSize()
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
 * Spoken description of one φθόγγος: its name, whether it is the base, and how many μόρια above the
 * base it sits (ClickUp `869f4tpju`).
 *
 * Null [moriaFromBase] means the distance could not be derived — the base itself, or a ladder with
 * no marked tonic. The description then falls back to the name alone rather than announcing a
 * number that might be wrong.
 */
@Composable
private fun phthongDescription(label: String, moriaFromBase: Int?, isTonic: Boolean): String = when {
    isTonic -> stringResource(R.string.cd_eight_modes_base_note_pill, label)
    moriaFromBase != null -> stringResource(R.string.cd_eight_modes_note_pill_moria, label, moriaFromBase)
    else -> stringResource(R.string.cd_eight_modes_note_pill, label)
}

/**
 * For each φθόγγος (top → bottom), its distance in μόρια above the nearest base **below** it, or
 * null when there is none below.
 *
 * Measuring from the base below rather than from the bottom of the ladder is what makes the number
 * useful: «Δι, 30 μόρια πάνω από τη βάση» locates the φθόγγος inside its own octave, which is how
 * the interval is taught. A distance from the very bottom would grow past 72 and mean little.
 *
 * [intervalsTopToBottom] has one fewer entry than the φθόγγοι, and index `i` is the gap between
 * φθόγγος `i` and φθόγγος `i + 1` — i.e. the μόρια *below* φθόγγος `i`.
 */
internal fun moriaFromNearestBaseBelow(
    intervalsTopToBottom: List<Int>,
    tonicIndices: Set<Int>,
): List<Int?> {
    val count = intervalsTopToBottom.size + 1
    val result = arrayOfNulls<Int>(count)
    // Walk upward (from the bottom index to the top) accumulating the gaps since the last base.
    var sinceBase: Int? = null
    for (i in count - 1 downTo 0) {
        if (i in tonicIndices) {
            result[i] = 0
            sinceBase = 0
        } else if (sinceBase != null) {
            sinceBase += intervalsTopToBottom[i]
            result[i] = sinceBase
        }
    }
    return result.toList()
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
