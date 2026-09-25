package com.johnchourp.learnbyzantinemusic.recordings.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.recordings.player.PlaybackTuning
import com.johnchourp.learnbyzantinemusic.recordings.player.PlayerPhase
import com.johnchourp.learnbyzantinemusic.recordings.player.PlayerState
import com.johnchourp.learnbyzantinemusic.ui.components.LessonCard
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentCrimsonContent
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurfaceVariant
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/** The card's callbacks, bundled so each of the three screens hands over one value. */
class RecordingPlayerActions(
    val onTogglePlay: () -> Unit,
    val onSeek: (Int) -> Unit,
    val onSpeedChange: (Float) -> Unit,
    val onShiftChange: (Int) -> Unit,
    val onResetTuning: () -> Unit,
    val onMarkLoopStart: () -> Unit,
    val onMarkLoopEnd: () -> Unit,
    val onClearLoop: () -> Unit,
    val onOpenExternally: () -> Unit,
    val onClose: () -> Unit,
)

/**
 * The in-app player (ClickUp `869f5x268`): play/pause, the position, an A-B loop, speed ½×–1× that
 * keeps the pitch, and a shift in μόρια — with «Άνοιγμα σε άλλη εφαρμογή» kept as a secondary way
 * out, and the only one when the device cannot play the file. A pure renderer of [PlayerState]:
 * `PlayerController` decides everything.
 *
 * Sliders apply when released, so the player is not retuned at every step of a drag.
 */
@Composable
fun RecordingPlayerCard(state: PlayerState, actions: RecordingPlayerActions, modifier: Modifier = Modifier) {
    LessonCard(title = stringResource(R.string.recordings_player_title), modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Filled.GraphicEq, contentDescription = null, tint = LbmBrown)
            Spacer(Modifier.width(8.dp))
            Text(
                text = state.title.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = LbmTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = actions.onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.recordings_player_close),
                    tint = LbmTextSecondary,
                )
            }
        }
        if (state.phase == PlayerPhase.FAILED) {
            Text(
                text = stringResource(R.string.recordings_player_unsupported),
                style = MaterialTheme.typography.bodyMedium,
                color = AccentCrimsonContent,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = actions.onOpenExternally,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LbmBrown),
            ) {
                OpenExternallyLabel()
            }
        } else {
            PlayerControls(state, actions)
        }
    }
}

/** Where every screen puts the player card: right under its title, so a long list cannot hide it. */
const val PLAYER_CARD_INDEX = 1

/**
 * Brings the player card into view each time a recording is opened in it. Not when the screen is
 * re-created: the list keeps the place the user left it at.
 */
@Composable
fun ScrollToPlayerOnOpen(openCount: Int, listState: LazyListState) {
    var handled by rememberSaveable { mutableIntStateOf(openCount) }
    LaunchedEffect(openCount) {
        if (openCount != handled) {
            handled = openCount
            listState.animateScrollToItem(PLAYER_CARD_INDEX)
        }
    }
}

@Composable
private fun PlayerControls(state: PlayerState, actions: RecordingPlayerActions) {
    val enabled = state.controlsEnabled
    val playing = state.phase == PlayerPhase.PLAYING
    if (state.blocked) {
        Text(
            text = stringResource(R.string.recordings_player_blocked),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(8.dp))
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledIconButton(
            onClick = actions.onTogglePlay,
            enabled = enabled,
            modifier = Modifier.size(52.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = LbmBrown, contentColor = Color.White),
        ) {
            if (state.phase == PlayerPhase.LOADING) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp, color = Color.White)
            } else {
                Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(if (playing) R.string.recordings_pause else R.string.recordings_player_play),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "${clock(state.positionMs)} / ${clock(state.durationMs)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = LbmTextPrimary,
        )
    }

    var dragPosition by remember { mutableStateOf<Float?>(null) }
    val end = state.durationMs.coerceAtLeast(1).toFloat()
    Slider(
        value = (dragPosition ?: state.positionMs.toFloat()).coerceIn(0f, end),
        onValueChange = { dragPosition = it },
        onValueChangeFinished = {
            dragPosition?.let { actions.onSeek(it.roundToInt()) }
            dragPosition = null
        },
        valueRange = 0f..end,
        enabled = enabled && state.durationMs > 0,
        colors = sliderColors(),
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = actions.onMarkLoopStart, enabled = enabled, colors = ButtonDefaults.outlinedButtonColors(contentColor = LbmBrown)) {
            Text(stringResource(R.string.recordings_player_loop_a))
        }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(onClick = actions.onMarkLoopEnd, enabled = enabled, colors = ButtonDefaults.outlinedButtonColors(contentColor = LbmBrown)) {
            Text(stringResource(R.string.recordings_player_loop_b))
        }
        Spacer(Modifier.width(8.dp))
        val loop = state.loop
        Text(
            text = when {
                loop != null -> stringResource(R.string.recordings_player_loop_template, clock(loop.startMs), clock(loop.endMs))
                state.loopStartMs != null -> stringResource(R.string.recordings_player_loop_start_template, clock(state.loopStartMs))
                else -> stringResource(R.string.recordings_player_loop_hint)
            },
            style = MaterialTheme.typography.bodySmall,
            color = LbmTextSecondary,
            modifier = Modifier.weight(1f),
        )
        if (state.loopStartMs != null) {
            IconButton(onClick = actions.onClearLoop) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.recordings_player_loop_clear),
                    tint = LbmTextSecondary,
                )
            }
        }
    }

    var dragSpeed by remember { mutableStateOf<Float?>(null) }
    val speed = dragSpeed ?: state.tuning.speed
    LabelledValue(stringResource(R.string.recordings_player_speed), speedLabel(speed))
    Slider(
        value = speed,
        onValueChange = { dragSpeed = snapSpeed(it) },
        onValueChangeFinished = {
            dragSpeed?.let(actions.onSpeedChange)
            dragSpeed = null
        },
        valueRange = PlaybackTuning.MIN_SPEED..PlaybackTuning.MAX_SPEED,
        steps = SPEED_STEPS,
        enabled = enabled,
        colors = sliderColors(),
    )

    var dragShift by remember { mutableStateOf<Int?>(null) }
    val shift = dragShift ?: state.tuning.shiftMoria
    LabelledValue(stringResource(R.string.recordings_player_shift), if (shift > 0) "+$shift" else "$shift")
    Slider(
        value = shift.toFloat(),
        onValueChange = { dragShift = it.roundToInt() },
        onValueChangeFinished = {
            dragShift?.let(actions.onShiftChange)
            dragShift = null
        },
        valueRange = BaseShift.MIN_MORIA.toFloat()..BaseShift.MAX_MORIA.toFloat(),
        steps = BaseShift.MAX_MORIA - BaseShift.MIN_MORIA - 1,
        enabled = enabled,
        colors = sliderColors(),
    )

    if (!state.tuningAvailable) {
        Text(
            text = stringResource(R.string.recordings_player_tuning_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = AccentCrimsonContent,
        )
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (!state.tuning.isDefault) {
            TextButton(onClick = actions.onResetTuning, enabled = enabled) {
                Text(stringResource(R.string.recordings_player_reset), color = LbmBrown)
            }
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = actions.onOpenExternally) {
            OpenExternallyLabel()
        }
    }
}

@Composable
private fun LabelledValue(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = LbmTextPrimary, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = LbmBrown)
    }
}

@Composable
private fun OpenExternallyLabel() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp), tint = LbmBrown)
        Spacer(Modifier.width(6.dp))
        Text(text = stringResource(R.string.recordings_player_open_external), color = LbmBrown)
    }
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = LbmBrown,
    activeTrackColor = LbmBrown,
    inactiveTrackColor = LbmSurfaceVariant,
)

/** 0.05× steps between ½× and 1×. */
private const val SPEED_STEPS = 9

private fun snapSpeed(value: Float): Float = (value * 20f).roundToInt() / 20f

private fun speedLabel(speed: Float): String {
    val format = NumberFormat.getNumberInstance(Locale.getDefault()).apply { maximumFractionDigits = 2 }
    return format.format(speed.toDouble()) + "×"
}

/** m:ss, or h:mm:ss past an hour. */
private fun clock(ms: Int): String {
    val totalSeconds = ms.coerceAtLeast(0) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
