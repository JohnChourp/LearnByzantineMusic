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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrownSoft
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmOutline
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurface
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary
import kotlinx.coroutines.delay

private const val STEP_INTERVAL_MS = 720L
private val TREAD_HEIGHT = 48.dp
private val TREAD_INDENT = 20.dp

/**
 * An interactive staircase of the eight ascending phthongs (Νη → Νη). A marker climbs the
 * stairs step by step and then comes back down, looping while playing, so the learner can
 * *see* the voice rise and fall. Each tread is also tappable to jump straight to a phthong.
 */
@Composable
fun PhthongStaircase(modifier: Modifier = Modifier) {
    // Treads 0..size: position 0 is Νη at the bottom, position [size] is Νη one octave up.
    val maxStep = PhthongScale.size
    var step by remember { mutableIntStateOf(0) }
    var ascending by remember { mutableStateOf(true) }
    var playing by remember { mutableStateOf(false) }

    LaunchedEffect(playing) {
        while (playing) {
            delay(STEP_INTERVAL_MS)
            if (ascending) {
                if (step >= maxStep) {
                    ascending = false
                    step = maxStep - 1
                } else {
                    step++
                }
            } else {
                if (step <= 0) {
                    ascending = true
                    step = 1
                } else {
                    step--
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        StaircaseControls(
            playing = playing,
            ascending = ascending,
            onTogglePlay = { playing = !playing },
        )
        Spacer(Modifier.height(14.dp))
        // Render top-to-bottom: highest phthong first.
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (position in maxStep downTo 0) {
                StaircaseTread(
                    position = position,
                    active = position == step,
                    onTap = {
                        playing = false
                        step = position
                    },
                )
            }
        }
    }
}

@Composable
private fun StaircaseControls(
    playing: Boolean,
    ascending: Boolean,
    onTogglePlay: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val playLabel = stringResource(
            if (playing) R.string.phthongs_staircase_pause else R.string.phthongs_staircase_play,
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(LbmBrown)
                .clickable(role = Role.Button, onClick = onTogglePlay),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = playLabel,
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Icon(
            imageVector = if (ascending) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
            contentDescription = null,
            tint = LbmBrownSoft,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(
                if (ascending) R.string.phthongs_ascending else R.string.phthongs_descending,
            ),
            style = MaterialTheme.typography.titleMedium,
            color = LbmTextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StaircaseTread(
    position: Int,
    active: Boolean,
    onTap: () -> Unit,
) {
    val phthong = PhthongScale.phthongAt(position)
    val container by animateColorAsState(
        targetValue = if (active) LbmBrown else LbmSurface,
        label = "treadBg",
    )
    val textColor by animateColorAsState(
        targetValue = if (active) Color.White else LbmTextPrimary,
        label = "treadText",
    )
    val lift by animateDpAsState(if (active) 4.dp else 0.dp, label = "treadLift")
    val scale by animateFloatAsState(if (active) 1.03f else 1f, label = "treadScale")

    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.width(TREAD_INDENT * position))
        Row(
            modifier = Modifier
                .weight(1f)
                .height(TREAD_HEIGHT)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationY = -lift.toPx()
                }
                .clip(RoundedCornerShape(12.dp))
                .background(container)
                .border(1.dp, if (active) LbmBrown else LbmOutline, RoundedCornerShape(12.dp))
                .selectable(selected = active, role = Role.Button, onClick = onTap)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = (position + 1).toString(),
                style = MaterialTheme.typography.labelLarge,
                color = if (active) Color.White else LbmTextSecondary,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(phthong.nameRes()),
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
            )
        }
    }
}
