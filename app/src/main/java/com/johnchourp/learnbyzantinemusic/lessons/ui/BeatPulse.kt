package com.johnchourp.learnbyzantinemusic.lessons.ui

import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrownSoft
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmMeasureBar
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmOutline
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurface
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.material3.Slider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.stateDescription

private val CELL_SIZE = 48.dp
private val MEASURE_BAR_HEIGHT = 60.dp
private val HAND_SIZE = 56.dp

/**
 * An animated metronome for one [BeatGrouping]. The χρόνοι (beats) sit as note-cells between
 * two crimson measure bars — exactly like the red boundary bars in the neume diagrams — and a
 * marker pulses 1 → N and repeats, so the learner can *see* that a δίσημος/τρίσημος/τετράσημος
 * group keeps cycling every 2/3/4 beats. Play/pause; each cell is tappable to jump to a beat.
 *
 * It can also be *felt* (ClickUp `869f5x2d7`): a hand circles once per grouping and reaches the
 * bottom on the θέση; the phone vibrates strongly on the θέση and lightly on the άρσεις; «Σιωπηλά»
 * drops the click, for use inside the church; «Πόδι» marks only the θέσεις. The click, the
 * vibration, the hand and the highlight all follow one [MetronomeRun] — see [MetronomeBeats].
 *
 * The screen stays on while it plays, and it stops when the page leaves the screen: it used to
 * keep ticking in the background, and with vibration it would buzz in a pocket.
 */
@Composable
fun BeatPulse(grouping: BeatGrouping, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val view = LocalView.current
    var playing by remember { mutableStateOf(false) }
    var bpm by remember { mutableIntStateOf(MetronomePrefs.savedBpm(context)) }
    // The highlighted beat resets to the downbeat whenever the grouping changes.
    var activeBeat by remember(grouping) { mutableIntStateOf(1) }
    var options by remember { mutableStateOf(MetronomePrefs.savedOptions(context)) }
    var run by remember { mutableStateOf<MetronomeRun?>(null) }

    val clicker = remember { MetronomeClicker() }
    DisposableEffect(Unit) { onDispose { clicker.release() } }
    val vibrator = remember(view) { DeviceBeatVibrator(context, view) }
    val effective = options.effectiveOn(vibrator.available)
    // Read on every beat, so a switch takes effect on the next one instead of restarting the count.
    val currentOptions by rememberUpdatedState(effective)

    // Leaving the page — Home, another app, the power button — stops it: nothing ticks or buzzes
    // in the background. No foreground service, by decision.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { playing = false }
    // On a stand nobody touches the phone, so the screen must not sleep while it plays.
    DisposableEffect(playing) {
        view.keepScreenOn = playing
        onDispose { view.keepScreenOn = false }
    }

    // Every beat is scheduled at an ABSOLUTE offset from the moment playback started, on a monotonic
    // clock, so rounding error and scheduler jitter cannot accumulate — and the click, the vibration,
    // the hand and the highlight all read the same MetronomeRun, so they cannot drift apart. See
    // MetronomeSchedule for the measurement behind that choice.
    LaunchedEffect(playing, grouping, bpm) {
        if (!playing) {
            run = null
            return@LaunchedEffect
        }
        val current = MetronomeRun(SystemClock.elapsedRealtime(), bpm, grouping.beats)
        run = current
        while (isActive) {
            val wait = current.waitMillis(SystemClock.elapsedRealtime())
            if (wait > 0) delay(wait)
            val event = current.next(currentOptions)
            activeBeat = event.beatInGrouping
            MetronomeBeats.perform(event, clicker::click, vibrator)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlayPauseButton(playing = playing, onToggle = { playing = !playing })
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.duotrio_beat_counter, activeBeat, grouping.beats),
                style = MaterialTheme.typography.titleMedium,
                color = LbmTextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            ChironomyHand(run = run, beats = grouping.beats)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.duotrio_tempo_label, bpm),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
        Slider(
            value = bpm.toFloat(),
            onValueChange = { bpm = it.toInt() },
            onValueChangeFinished = { MetronomePrefs.saveBpm(context, bpm) },
            valueRange = MetronomeSchedule.MIN_BPM.toFloat()..MetronomeSchedule.MAX_BPM.toFloat(),
            modifier = Modifier.semantics {
                stateDescription = "$bpm"
            },
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MeasureBar()
            grouping.beatNumbers.forEach { beat ->
                BeatCell(
                    number = beat,
                    active = beat == activeBeat,
                    onTap = {
                        playing = false
                        activeBeat = beat
                    },
                )
            }
            MeasureBar()
        }
        Spacer(Modifier.height(12.dp))
        val choose: (MetronomeOptions) -> Unit = {
            options = it
            MetronomePrefs.saveOptions(context, it)
        }
        // Without a vibrator there is nothing to feel, and «Σιωπηλά» would leave nothing at all.
        if (MetronomeBeats.showsVibrationOptions(vibrator)) {
            MetronomeOptionRow(
                label = stringResource(R.string.duotrio_vibration_label),
                hint = stringResource(R.string.duotrio_vibration_hint),
                checked = options.vibrate,
                onToggle = { choose(options.copy(vibrate = it)) },
            )
            MetronomeOptionRow(
                label = stringResource(R.string.duotrio_silent_label),
                hint = stringResource(R.string.duotrio_silent_hint),
                checked = effective.silent,
                enabled = effective.vibrate,
                onToggle = { choose(options.copy(silent = it)) },
            )
        }
        MetronomeOptionRow(
            label = stringResource(R.string.duotrio_foot_label),
            hint = stringResource(R.string.duotrio_foot_hint),
            checked = options.foot,
            onToggle = { choose(options.copy(foot = it)) },
        )
    }
}

/**
 * The moving hand (χειρονομία): one circle per grouping, at its lowest on the θέση. The small marks
 * are where each χρόνος falls; the crimson one at the bottom is the θέση. It reads the same
 * [MetronomeRun] as the click, every frame, and waits on the θέση while stopped.
 */
@Composable
private fun ChironomyHand(run: MetronomeRun?, beats: Int, modifier: Modifier = Modifier) {
    val phase = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(run) {
        phase.floatValue = 0f
        val current = run ?: return@LaunchedEffect
        while (isActive) {
            withFrameMillis { }
            phase.floatValue = current.handPhase(SystemClock.elapsedRealtime()).toFloat()
        }
    }
    // Palette reads stay out of the DrawScope, which is not a composable scope (see PitchDiagram).
    val track = LbmOutline
    val beatMark = LbmBrownSoft
    val thesisMark = LbmMeasureBar
    val hand = LbmBrown
    val description = stringResource(R.string.duotrio_hand_cd)
    Canvas(modifier = modifier.size(HAND_SIZE).semantics { contentDescription = description }) {
        val radius = size.minDimension / 2 - 8.dp.toPx()
        fun at(p: Double): Offset {
            val (x, y) = ChironomyPhase.position(p)
            return Offset(center.x + radius * x.toFloat(), center.y + radius * y.toFloat())
        }
        drawCircle(color = track, radius = radius, center = center, style = Stroke(width = 2.dp.toPx()))
        for (k in 0 until beats) {
            drawCircle(
                color = if (k == 0) thesisMark else beatMark,
                radius = if (k == 0) 4.dp.toPx() else 2.5.dp.toPx(),
                center = at(k.toDouble() / beats),
            )
        }
        drawCircle(color = hand, radius = 6.dp.toPx(), center = at(phase.floatValue.toDouble()))
    }
}

/** One switch of the metronome. The whole row is the switch, so TalkBack reads its name with it. */
@Composable
private fun MetronomeOptionRow(
    label: String,
    hint: String,
    checked: Boolean,
    enabled: Boolean = true,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onToggle)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) LbmTextPrimary else LbmTextSecondary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = hint, style = MaterialTheme.typography.bodySmall, color = LbmTextSecondary)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun PlayPauseButton(playing: Boolean, onToggle: () -> Unit) {
    val label = stringResource(
        if (playing) R.string.duotrio_pause else R.string.duotrio_play,
    )
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(LbmBrown)
            .clickable(role = Role.Button, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(26.dp),
        )
    }
}

/** A vertical crimson measure bar — the χρόνος-group boundary, mirroring the neume diagrams. */
@Composable
private fun MeasureBar() {
    Box(
        modifier = Modifier
            .clearAndSetSemantics {}
            .width(3.dp)
            .height(MEASURE_BAR_HEIGHT)
            .clip(RoundedCornerShape(2.dp))
            .background(LbmMeasureBar),
    )
}

@Composable
private fun BeatCell(number: Int, active: Boolean, onTap: () -> Unit) {
    val container by animateColorAsState(
        targetValue = if (active) LbmBrown else LbmSurface,
        label = "cellBg",
    )
    val textColor by animateColorAsState(
        targetValue = if (active) Color.White else LbmTextPrimary,
        label = "cellText",
    )
    val scale by animateFloatAsState(if (active) 1.12f else 1f, label = "cellScale")
    val lift by animateDpAsState(if (active) 6.dp else 0.dp, label = "cellLift")
    val cellLabel = stringResource(R.string.duotrio_beat_cell_cd, number)

    Column(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = -lift.toPx()
            }
            .size(CELL_SIZE)
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .border(1.5.dp, if (active) LbmBrown else LbmOutline, RoundedCornerShape(14.dp))
            .selectable(selected = active, role = Role.Button, onClick = onTap)
            .semantics { contentDescription = cellLabel }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = if (active) Color.White else LbmBrownSoft,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}
