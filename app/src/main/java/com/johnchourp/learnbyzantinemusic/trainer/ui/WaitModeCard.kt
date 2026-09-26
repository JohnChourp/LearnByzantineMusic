package com.johnchourp.learnbyzantinemusic.trainer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.trainer.VoiceTrace
import com.johnchourp.learnbyzantinemusic.ui.components.LessonCard
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGreenContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGreenContent
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmOutline
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary

/**
 * «Παραλλαγή με αναμονή» (ClickUp `869f5x2cd`, J1): the line waits on each φθόγγος until it is sung
 * and held; meanwhile the card says how many μόρια away the voice is and which way to go, over a
 * scrolling trace of the voice on the ήχος's ladder, and fills a bar while the note is held.
 *
 * A pure renderer of [WaitModeUi]. It never moves on by itself: «Παράλειψη» is the only other way,
 * and only a tap. The ison is off unless the learner switches it on, because the microphone may hear
 * it from the loudspeaker.
 */
@Composable
internal fun WaitModeCard(
    wait: WaitModeUi,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSkip: () -> Unit,
    onIsonChange: (Boolean) -> Unit,
) {
    LessonCard(title = stringResource(R.string.melody_trainer_wait_title)) {
        Text(
            text = stringResource(R.string.melody_trainer_wait_hint),
            style = MaterialTheme.typography.bodySmall,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = if (wait.running) onStop else onStart,
                enabled = wait.running || wait.startEnabled,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LbmBrown, contentColor = Color.White),
            ) {
                Icon(
                    imageVector = if (wait.running) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(if (wait.running) R.string.melody_trainer_wait_stop else R.string.melody_trainer_wait_start))
            }
            if (wait.running) {
                OutlinedButton(onClick = onSkip, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(imageVector = Icons.Filled.SkipNext, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.melody_trainer_wait_skip))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(text = wait.toleranceLabel, style = MaterialTheme.typography.labelLarge, color = LbmTextSecondary)
        wait.status?.let { status ->
            Spacer(Modifier.height(6.dp))
            Text(text = status, style = MaterialTheme.typography.bodyMedium, color = LbmBrown)
        }
        val target = wait.targetLabel
        if (wait.running && target != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.melody_trainer_wait_target, target),
                style = MaterialTheme.typography.headlineSmall,
                color = LbmTextPrimary,
                fontWeight = FontWeight.Bold,
            )
            wait.guidance?.let { guidance ->
                Text(
                    text = guidance,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (wait.onTarget) AccentGreenContent else LbmBrown,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { wait.holdProgress },
                modifier = Modifier.fillMaxWidth(),
                color = AccentGreenContent,
                trackColor = AccentGreenContainer,
            )
            Spacer(Modifier.height(8.dp))
            VoiceTraceView(
                points = wait.trace,
                rungOffsets = wait.rungOffsets,
                toleranceMoria = wait.toleranceMoria,
                description = stringResource(R.string.melody_trainer_wait_trace_cd, target),
            )
        }
        wait.result?.let { result ->
            Spacer(Modifier.height(10.dp))
            val starsText = "★".repeat(result.stars) + "☆".repeat(STAR_COUNT - result.stars)
            val starsDescription = stringResource(R.string.melody_trainer_wait_stars_cd, result.stars)
            Text(
                text = starsText,
                style = MaterialTheme.typography.headlineMedium,
                color = LbmBrown,
                modifier = Modifier.semantics {
                    contentDescription = starsDescription
                    liveRegion = LiveRegionMode.Polite
                },
            )
            Text(text = result.summary, style = MaterialTheme.typography.bodyMedium, color = LbmTextPrimary)
            Text(text = result.nextLevel, style = MaterialTheme.typography.bodySmall, color = LbmTextSecondary)
        }
        Spacer(Modifier.height(10.dp))
        // Merged, so TalkBack reads the switch with its label.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        ) {
            Text(
                text = stringResource(R.string.melody_trainer_wait_ison),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextPrimary,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = wait.isonOn, onCheckedChange = onIsonChange)
        }
        Text(
            text = stringResource(R.string.melody_trainer_wait_ison_hint),
            style = MaterialTheme.typography.bodySmall,
            color = LbmTextSecondary,
        )
    }
}

/**
 * The voice's last seconds against the rungs around the target: the target as a green line inside
 * its tolerance band, the neighbouring rungs as thin lines, the voice as a line that breaks in
 * silence. Offsets beyond [TRACE_RANGE_MORIA] are drawn at the edge.
 */
@Composable
private fun VoiceTraceView(
    points: List<VoiceTrace.Point>,
    rungOffsets: List<Double>,
    toleranceMoria: Double,
    description: String,
) {
    val targetColor = AccentGreenContent
    val bandColor = AccentGreenContainer
    val rungColor = LbmOutline
    val voiceColor = LbmBrown
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .semantics { contentDescription = description },
    ) {
        val middle = size.height / 2f
        fun y(offsetMoria: Double): Float =
            middle - (offsetMoria.coerceIn(-TRACE_RANGE_MORIA, TRACE_RANGE_MORIA) / TRACE_RANGE_MORIA).toFloat() * middle
        drawRect(
            color = bandColor,
            topLeft = Offset(0f, y(toleranceMoria)),
            size = Size(size.width, y(-toleranceMoria) - y(toleranceMoria)),
        )
        rungOffsets.forEach { offset ->
            val isTarget = offset == 0.0
            drawLine(
                color = if (isTarget) targetColor else rungColor,
                start = Offset(0f, y(offset)),
                end = Offset(size.width, y(offset)),
                strokeWidth = if (isTarget) 4f else 2f,
            )
        }
        points.zipWithNext().forEach { (from, to) ->
            val a = from.offsetMoria ?: return@forEach
            val b = to.offsetMoria ?: return@forEach
            drawLine(
                color = voiceColor,
                start = Offset(from.x * size.width, y(a)),
                end = Offset(to.x * size.width, y(b)),
                strokeWidth = 6f,
                cap = StrokeCap.Round,
            )
        }
        points.lastOrNull()?.offsetMoria?.let { last ->
            drawCircle(color = voiceColor, radius = 9f, center = Offset(size.width - 9f, y(last)))
        }
    }
}

/** How far above and below the target the trace reaches: past a diatonic neighbour, 12 μόρια at most away. */
internal const val TRACE_RANGE_MORIA = 14.0

private const val STAR_COUNT = 3
