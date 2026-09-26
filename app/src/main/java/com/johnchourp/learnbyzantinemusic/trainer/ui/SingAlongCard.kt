package com.johnchourp.learnbyzantinemusic.trainer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.trainer.TrainerNote
import com.johnchourp.learnbyzantinemusic.ui.components.LessonCard
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmOutline
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary
import kotlin.math.roundToInt

/**
 * «Ψάλλε μαζί» (ClickUp `869f5x2cv`, J2): start and stop the loop, see which round it is and how
 * loud the guide still is, choose what the line shows — φθόγγοι or syllables — and set the three
 * volumes: melody, ison, metronome.
 *
 * A pure renderer of [SingAlongUi]. The loop runs until «Στάση»; the volumes and the line's labels
 * can change while it runs, everything else waits for it to stop. The syllables themselves are typed
 * per note, in [SyllableDialog], from the line.
 */
@Composable
internal fun SingAlongCard(
    singAlong: SingAlongUi,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onShowSyllables: (Boolean) -> Unit,
    onVolumeChange: (SingAlongSound, Int) -> Unit,
) {
    LessonCard(title = stringResource(R.string.melody_trainer_sing_along_title)) {
        Text(
            text = stringResource(R.string.melody_trainer_sing_along_hint),
            style = MaterialTheme.typography.bodySmall,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = if (singAlong.running) onStop else onStart,
                enabled = singAlong.running || singAlong.startEnabled,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LbmBrown, contentColor = Color.White),
            ) {
                Icon(
                    imageVector = if (singAlong.running) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(
                        if (singAlong.running) R.string.melody_trainer_sing_along_stop else R.string.melody_trainer_sing_along_start
                    )
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.melody_trainer_sing_along_ison, singAlong.isonLabel),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
            )
        }
        val round = singAlong.round
        val guide = singAlong.guidePercent
        val now = singAlong.nowLabel
        if (singAlong.running && now != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                // The lit note, big: the line itself may be scrolled out of sight while you chant.
                text = now,
                style = MaterialTheme.typography.displaySmall,
                color = LbmTextPrimary,
                fontWeight = FontWeight.Bold,
            )
        }
        if (singAlong.running && round != null && guide != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (guide > 0) {
                    stringResource(R.string.melody_trainer_sing_along_round, round, guide)
                } else {
                    stringResource(R.string.melody_trainer_sing_along_round_silent, round)
                },
                style = MaterialTheme.typography.titleSmall,
                color = LbmBrown,
                fontWeight = FontWeight.SemiBold,
                // A new round is announced, so the fade can be followed without looking.
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.melody_trainer_sing_along_line),
            style = MaterialTheme.typography.labelLarge,
            color = LbmTextPrimary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !singAlong.showSyllables,
                onClick = { onShowSyllables(false) },
                label = { Text(stringResource(R.string.melody_trainer_sing_along_line_phthongs)) },
            )
            FilterChip(
                selected = singAlong.showSyllables,
                onClick = { onShowSyllables(true) },
                label = { Text(stringResource(R.string.melody_trainer_sing_along_line_syllables)) },
            )
        }
        if (singAlong.showSyllables && !singAlong.hasSyllables) {
            Text(
                text = stringResource(R.string.melody_trainer_sing_along_no_syllables),
                style = MaterialTheme.typography.bodySmall,
                color = LbmTextSecondary,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.melody_trainer_sing_along_volumes),
            style = MaterialTheme.typography.labelLarge,
            color = LbmTextPrimary,
        )
        VolumeRow(
            icon = Icons.Filled.MusicNote,
            label = stringResource(R.string.melody_trainer_sing_along_volume_melody),
            percent = singAlong.melodyPercent,
            onChange = { onVolumeChange(SingAlongSound.MELODY, it) },
        )
        VolumeRow(
            icon = Icons.Filled.GraphicEq,
            label = stringResource(R.string.melody_trainer_sing_along_volume_ison),
            percent = singAlong.isonPercent,
            onChange = { onVolumeChange(SingAlongSound.ISON, it) },
        )
        VolumeRow(
            icon = Icons.Filled.Timer,
            label = stringResource(R.string.melody_trainer_sing_along_volume_metronome),
            percent = singAlong.metronomePercent,
            onChange = { onVolumeChange(SingAlongSound.METRONOME, it) },
        )
    }
}

@Composable
private fun VolumeRow(icon: ImageVector, label: String, percent: Int, onChange: (Int) -> Unit) {
    val description = stringResource(R.string.melody_trainer_sing_along_volume_a11y, label, percent)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = LbmBrown, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextPrimary,
            modifier = Modifier.widthIn(min = 88.dp),
        )
        Slider(
            value = percent.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 0f..100f,
            modifier = Modifier
                .weight(1f)
                .semantics { stateDescription = description },
            colors = SliderDefaults.colors(
                thumbColor = LbmBrown,
                activeTrackColor = LbmBrown,
                inactiveTrackColor = LbmOutline,
            ),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.melody_trainer_sing_along_volume_value, percent),
            style = MaterialTheme.typography.labelLarge,
            color = LbmTextSecondary,
            modifier = Modifier.widthIn(min = 44.dp),
        )
    }
}

/**
 * Types the syllable of one note. Cut to [TrainerNote.MAX_SYLLABLE_LENGTH] as it is typed; the note
 * cleans it again when it stores it, so what is shown and what is saved agree. «Αφαίρεση» takes the
 * syllable off, and the line shows that note's φθόγγος again.
 */
@Composable
internal fun SyllableDialog(
    dialog: SyllableDialogUi,
    onSave: (Int, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember(dialog.index) { mutableStateOf(dialog.syllable) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.melody_trainer_syllable_title, dialog.phthongLabel)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(TrainerNote.MAX_SYLLABLE_LENGTH) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.melody_trainer_syllable_label)) },
                    supportingText = {
                        Text(stringResource(R.string.melody_trainer_syllable_hint, TrainerNote.MAX_SYLLABLE_LENGTH))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(dialog.index, text) }) {
                Text(stringResource(R.string.melody_trainer_syllable_save))
            }
        },
        dismissButton = {
            Row {
                if (dialog.syllable.isNotEmpty()) {
                    TextButton(onClick = { onSave(dialog.index, "") }) {
                        Text(stringResource(R.string.melody_trainer_syllable_remove))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.melody_trainer_syllable_cancel))
                }
            }
        },
    )
}
