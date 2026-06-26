package com.johnchourp.learnbyzantinemusic.lessons.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrownSoft
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmMeasureBar
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmOutline
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurface
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import kotlinx.coroutines.delay

private const val BEAT_INTERVAL_MS = 620L
private val CELL_SIZE = 48.dp
private val MEASURE_BAR_HEIGHT = 60.dp

/**
 * An animated metronome for one [BeatGrouping]. The χρόνοι (beats) sit as note-cells between
 * two crimson measure bars — exactly like the red boundary bars in the neume diagrams — and a
 * marker pulses 1 → N and repeats, so the learner can *see* that a δίσημος/τρίσημος/τετράσημος
 * group keeps cycling every 2/3/4 beats. Play/pause; each cell is tappable to jump to a beat.
 */
@Composable
fun BeatPulse(grouping: BeatGrouping, modifier: Modifier = Modifier) {
    var playing by remember { mutableStateOf(false) }
    // The highlighted beat resets to the downbeat whenever the grouping changes.
    var activeBeat by remember(grouping) { mutableIntStateOf(1) }

    LaunchedEffect(playing, grouping) {
        while (playing) {
            delay(BEAT_INTERVAL_MS)
            activeBeat = grouping.nextBeat(activeBeat)
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
            )
        }
        Spacer(Modifier.height(18.dp))
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
