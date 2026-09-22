package com.johnchourp.learnbyzantinemusic.recordings.analysis.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.johnchourp.learnbyzantinemusic.recordings.analysis.PhthongSegmenter
import com.johnchourp.learnbyzantinemusic.recordings.analysis.PitchTrack
import com.johnchourp.learnbyzantinemusic.recordings.analysis.SungNote
import com.johnchourp.learnbyzantinemusic.trainer.TrainerPhthong
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGreenContent
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentOrangeContent
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmOutline
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary
import kotlin.math.abs
import kotlin.math.max

/** Moria within which a note counts as in tune (same ±4 moria as the Melody Trainer's voice check). */
const val IN_TUNE_MORIA = 4.0

private val DIAGRAM_HEIGHT = 240.dp
private val LABEL_WIDTH = 44.dp
private val DP_PER_SECOND = 56.dp

/**
 * Pitch over time on the lines of the mode's scale: the phthong names in a fixed column, then a
 * horizontally scrolling plot of the voiced frames (dots) and the recognised notes (bars, green
 * when within [IN_TUNE_MORIA], orange otherwise).
 */
@Composable
fun PitchDiagram(
    track: PitchTrack,
    notes: List<SungNote>,
    niHz: Double,
    positions: IntArray,
    description: String,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val moria = remember(track, niHz) { PhthongSegmenter.smoothedMoria(track.frames, niHz) }
    val lowDegree = (notes.minOfOrNull { it.degree } ?: 0) - 1
    val highDegree = (notes.maxOfOrNull { it.degree } ?: 7) + 1
    fun degreeMoria(degree: Int): Double =
        Math.floorDiv(degree, 7) * 72.0 + positions[Math.floorMod(degree, 7)]
    val bottomMoria = degreeMoria(lowDegree) - 3.0
    val topMoria = degreeMoria(highDegree) + 3.0
    val labelStyle = TextStyle(fontSize = 11.sp, color = LbmBrown)
    val timeStyle = TextStyle(fontSize = 10.sp, color = LbmTextSecondary)

    // Read the palette HERE, in composable scope. These are @Composable @ReadOnlyComposable getters
    // over LocalLbmPalette (dark mode, PR #110), and a Canvas DrawScope is not a composable scope —
    // reading them inside the lambdas stopped compiling under Kotlin 2.4. Hoisting is also what makes
    // the diagram follow a theme change: the colours are captured on recomposition, not once.
    val gridColor = LbmOutline
    val traceColor = LbmTextSecondary.copy(alpha = 0.45f)
    val inTuneColor = AccentGreenContent
    val offTuneColor = AccentOrangeContent

    Row(modifier = modifier.fillMaxWidth().semantics { contentDescription = description }) {
        Canvas(modifier = Modifier.width(LABEL_WIDTH).height(DIAGRAM_HEIGHT)) {
            for (degree in lowDegree..highDegree) {
                val y = yOf(degreeMoria(degree), bottomMoria, topMoria, size.height)
                val label = textMeasurer.measure(phthongLabel(degree), labelStyle)
                drawText(label, topLeft = Offset(0f, y - label.size.height / 2f))
            }
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val seconds = track.durationMs / 1000f
            val contentWidth: Dp = max(maxWidth.value, seconds * DP_PER_SECOND.value).dp
            Canvas(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .width(contentWidth)
                    .height(DIAGRAM_HEIGHT),
            ) {
                val pxPerMs = size.width / max(1L, track.durationMs).toFloat()
                // Scale lines; Νη lines a little stronger to mark the octaves.
                for (degree in lowDegree..highDegree) {
                    val y = yOf(degreeMoria(degree), bottomMoria, topMoria, size.height)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = if (Math.floorMod(degree, 7) == 0) 2.dp.toPx() else 1.dp.toPx(),
                    )
                }
                // One tick per second along the bottom.
                var second = 0
                while (second * 1000L <= track.durationMs) {
                    val x = second * 1000L * pxPerMs
                    val label = textMeasurer.measure("${second}s", timeStyle)
                    drawText(label, topLeft = Offset(x + 2.dp.toPx(), size.height - label.size.height))
                    second += if (seconds > 60) 5 else 1
                }
                // Voiced frames.
                val points = track.frames.indices.mapNotNull { index ->
                    val value = moria[index]
                    if (value.isNaN() || value < bottomMoria || value > topMoria) {
                        null
                    } else {
                        Offset(track.frames[index].timeMs * pxPerMs, yOf(value, bottomMoria, topMoria, size.height))
                    }
                }
                drawPoints(points, PointMode.Points, traceColor, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                // Notes.
                val barHeight = 7.dp.toPx()
                for (note in notes) {
                    val y = yOf(note.moria, bottomMoria, topMoria, size.height)
                    drawRoundRect(
                        color = if (abs(note.deviationMoria) <= IN_TUNE_MORIA) inTuneColor else offTuneColor,
                        topLeft = Offset(note.startMs * pxPerMs, y - barHeight / 2f),
                        size = Size(max(barHeight, (note.endMs - note.startMs) * pxPerMs), barHeight),
                        cornerRadius = CornerRadius(barHeight / 2f),
                    )
                }
            }
        }
    }
}

private fun yOf(moria: Double, bottom: Double, top: Double, height: Float): Float =
    (height * (1.0 - (moria - bottom) / (top - bottom))).toFloat()

/** Phthong name with an octave mark: «΄» above the starting octave, «͵» below. */
fun phthongLabel(degree: Int): String {
    val octave = Math.floorDiv(degree, 7)
    val name = TrainerPhthong.ascending[Math.floorMod(degree, 7)].displayName
    return when {
        octave > 0 -> name + "΄".repeat(octave)
        octave < 0 -> name + "͵".repeat(-octave)
        else -> name
    }
}
