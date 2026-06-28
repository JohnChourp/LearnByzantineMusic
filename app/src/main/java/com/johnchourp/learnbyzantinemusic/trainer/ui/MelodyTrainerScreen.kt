package com.johnchourp.learnbyzantinemusic.trainer.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.trainer.MelodyTempo
import com.johnchourp.learnbyzantinemusic.ui.components.LessonCard
import com.johnchourp.learnbyzantinemusic.ui.components.LessonHero
import com.johnchourp.learnbyzantinemusic.ui.components.StaggeredAppear
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentBlueContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentBlueContent
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGreenContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGreenContent
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentPurpleContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentPurpleContent
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmOutline
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPageBg
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPrimaryContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurface
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary

/* Active-note amber glow (mirrors the legacy HIGHLIGHT_COLOR semantics). */
private val ActiveGlow = Color(0xFFFFF1C9)
private val ActiveGlowBorder = Color(0xFFE0A100)

/**
 * Redesigned «Γυμναστής Μελωδίας» screen. A pure renderer of [MelodyTrainerUiState] + callbacks:
 * a hero, a visual timing-rules card, the note picker + octave stepper, the editable sequence
 * with animated match/active feedback, a tempo slider, the transport row, and the three
 * voice-practice mode cards. All audio / mic / timing logic stays in the host Activity.
 */
@Composable
fun MelodyTrainerScreen(
    state: MelodyTrainerUiState,
    phthongLabels: List<String>,
    onBack: () -> Unit,
    onAddPhthong: (Int) -> Unit,
    onOctaveDown: () -> Unit,
    onOctaveUp: () -> Unit,
    onDecrementDuration: (Int) -> Unit,
    onIncrementDuration: (Int) -> Unit,
    onToggleGorgo: (Int) -> Unit,
    onRemoveNote: (Int) -> Unit,
    onTempoChange: (Int) -> Unit,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit,
    onToggleVoice: (Boolean) -> Unit,
    onToggleRhythm: (Boolean) -> Unit,
    onToggleCombo: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LbmPageBg)
            .verticalScroll(scroll),
    ) {
        LessonHero(
            title = stringResource(R.string.title_activity_melody_trainer),
            subtitle = stringResource(R.string.melody_trainer_subtitle),
            onBack = onBack,
            icon = Icons.Filled.GraphicEq,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            StaggeredAppear(delayMillis = 60) { IntroCard() }
            StaggeredAppear(delayMillis = 120) { RulesCard() }
            StaggeredAppear(delayMillis = 180) {
                AddPhthongCard(
                    phthongLabels = phthongLabels,
                    addEnabled = state.addEnabled,
                    onAddPhthong = onAddPhthong,
                    octaveLabel = state.octaveLabel,
                    octaveDownEnabled = state.octaveDownEnabled,
                    octaveUpEnabled = state.octaveUpEnabled,
                    onOctaveDown = onOctaveDown,
                    onOctaveUp = onOctaveUp,
                )
            }
            StaggeredAppear(delayMillis = 240) {
                SequenceCard(
                    notes = state.notes,
                    totalBeatsLabel = state.totalBeatsLabel,
                    nowPlayingLabel = state.nowPlayingLabel,
                    onDecrementDuration = onDecrementDuration,
                    onIncrementDuration = onIncrementDuration,
                    onToggleGorgo = onToggleGorgo,
                    onRemoveNote = onRemoveNote,
                )
            }
            StaggeredAppear(delayMillis = 300) {
                TempoCard(
                    bpm = state.bpm,
                    enabled = state.tempoEnabled,
                    onTempoChange = onTempoChange,
                )
            }
            StaggeredAppear(delayMillis = 360) {
                TransportRow(
                    playEnabled = state.playEnabled,
                    stopEnabled = state.stopEnabled,
                    clearEnabled = state.clearEnabled,
                    onPlay = onPlay,
                    onStop = onStop,
                    onClear = onClear,
                )
            }
            StaggeredAppear(delayMillis = 420) {
                PracticeSection(
                    voice = state.voice,
                    rhythm = state.rhythm,
                    combo = state.combo,
                    onToggleVoice = onToggleVoice,
                    onToggleRhythm = onToggleRhythm,
                    onToggleCombo = onToggleCombo,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/* ----------------------------- Intro ----------------------------- */

@Composable
private fun IntroCard() {
    LessonCard(title = stringResource(R.string.melody_trainer_title)) {
        Text(
            text = stringResource(R.string.melody_trainer_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
    }
}

/* ----------------------------- Timing rules ----------------------------- */

private data class RuleEntry(
    val icon: ImageVector,
    val container: Color,
    val content: Color,
    val titleRes: Int,
    val bodyRes: Int,
)

@Composable
private fun RulesCard() {
    val rules = listOf(
        RuleEntry(
            Icons.Filled.MusicNote, LbmPrimaryContainer, LbmBrown,
            R.string.melody_trainer_rule_default_title, R.string.melody_trainer_rule_default_body,
        ),
        RuleEntry(
            Icons.Filled.Bolt, AccentPurpleContainer, AccentPurpleContent,
            R.string.melody_trainer_rule_gorgo_title, R.string.melody_trainer_rule_gorgo_body,
        ),
        RuleEntry(
            Icons.Filled.Add, AccentGreenContainer, AccentGreenContent,
            R.string.melody_trainer_rule_klasma_title, R.string.melody_trainer_rule_klasma_body,
        ),
        RuleEntry(
            Icons.Filled.Speed, AccentBlueContainer, AccentBlueContent,
            R.string.melody_trainer_rule_speed_title, R.string.melody_trainer_rule_speed_body,
        ),
    )
    LessonCard(title = stringResource(R.string.melody_trainer_rules_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rules.forEach { rule -> RuleRow(rule) }
        }
    }
}

@Composable
private fun RuleRow(rule: RuleEntry) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(rule.container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = rule.icon,
                contentDescription = null,
                tint = rule.content,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(rule.titleRes),
                style = MaterialTheme.typography.titleMedium,
                color = LbmTextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(rule.bodyRes),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
            )
        }
    }
}

/* ----------------------------- Add phthong + octave ----------------------------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddPhthongCard(
    phthongLabels: List<String>,
    addEnabled: Boolean,
    onAddPhthong: (Int) -> Unit,
    octaveLabel: String,
    octaveDownEnabled: Boolean,
    octaveUpEnabled: Boolean,
    onOctaveDown: () -> Unit,
    onOctaveUp: () -> Unit,
) {
    LessonCard(title = stringResource(R.string.melody_trainer_add_note_title)) {
        Text(
            text = stringResource(R.string.melody_trainer_add_note_hint),
            style = MaterialTheme.typography.bodySmall,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            phthongLabels.forEachIndexed { index, label ->
                NotePillButton(
                    label = label,
                    enabled = addEnabled,
                    onClick = { onAddPhthong(index) },
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        OctaveStepper(
            octaveLabel = octaveLabel,
            downEnabled = octaveDownEnabled,
            upEnabled = octaveUpEnabled,
            onDown = onOctaveDown,
            onUp = onOctaveUp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.melody_trainer_octave_hint),
            style = MaterialTheme.typography.bodySmall,
            color = LbmTextSecondary,
        )
    }
}

@Composable
private fun NotePillButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "notePillScale",
    )
    val alpha = if (enabled) 1f else 0.4f
    Box(
        modifier = Modifier
            .scale(scale)
            .heightIn(min = 48.dp)
            .widthIn(min = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(LbmPrimaryContainer.copy(alpha = alpha))
            .border(1.dp, LbmBrown.copy(alpha = 0.45f * alpha), RoundedCornerShape(14.dp))
            .selectable(
                selected = false,
                enabled = enabled,
                role = Role.Button,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = LbmBrown.copy(alpha = alpha),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun OctaveStepper(
    octaveLabel: String,
    downEnabled: Boolean,
    upEnabled: Boolean,
    onDown: () -> Unit,
    onUp: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LbmSurface)
            .border(1.dp, LbmOutline, RoundedCornerShape(14.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(
            icon = Icons.Filled.KeyboardArrowDown,
            contentDescription = stringResource(R.string.melody_trainer_octave_lower),
            enabled = downEnabled,
            onClick = onDown,
        )
        Text(
            text = stringResource(R.string.melody_trainer_octave_label, octaveLabel),
            style = MaterialTheme.typography.titleMedium,
            color = LbmTextPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        StepperButton(
            icon = Icons.Filled.KeyboardArrowUp,
            contentDescription = stringResource(R.string.melody_trainer_octave_higher),
            enabled = upEnabled,
            onClick = onUp,
        )
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(44.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) LbmBrown else LbmTextSecondary.copy(alpha = 0.4f),
        )
    }
}

/* ----------------------------- Your sequence ----------------------------- */

@Composable
private fun SequenceCard(
    notes: List<TrainerNoteUi>,
    totalBeatsLabel: String,
    nowPlayingLabel: String?,
    onDecrementDuration: (Int) -> Unit,
    onIncrementDuration: (Int) -> Unit,
    onToggleGorgo: (Int) -> Unit,
    onRemoveNote: (Int) -> Unit,
) {
    LessonCard(title = stringResource(R.string.melody_trainer_sequence_title)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ValueChip(
                text = stringResource(R.string.melody_trainer_total_beats, totalBeatsLabel),
                container = LbmPrimaryContainer,
                content = LbmBrown,
            )
            Crossfade(targetState = nowPlayingLabel, label = "nowPlaying") { playing ->
                if (playing != null) {
                    ValueChip(
                        text = stringResource(R.string.melody_trainer_now_playing, playing),
                        container = ActiveGlow,
                        content = ActiveGlowBorder,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(modifier = Modifier.animateContentSize()) {
            if (notes.isEmpty()) {
                EmptySequence()
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    notes.forEach { note ->
                        NoteTile(
                            note = note,
                            onDecrement = { onDecrementDuration(note.index) },
                            onIncrement = { onIncrementDuration(note.index) },
                            onToggleGorgo = { onToggleGorgo(note.index) },
                            onRemove = { onRemoveNote(note.index) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySequence() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LbmSurface)
            .border(1.dp, LbmOutline, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = LbmTextSecondary.copy(alpha = 0.6f),
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.melody_trainer_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun NoteTile(
    note: TrainerNoteUi,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onToggleGorgo: () -> Unit,
    onRemove: () -> Unit,
) {
    val container by animateColorAsState(
        targetValue = when {
            note.matched -> AccentGreenContainer
            note.active -> ActiveGlow
            else -> LbmSurface
        },
        animationSpec = tween(260),
        label = "noteContainer",
    )
    val borderColor = when {
        note.matched -> AccentGreenContent
        note.active -> ActiveGlowBorder
        else -> LbmOutline
    }
    val rowDescription = stringResource(
        R.string.melody_trainer_note_a11y,
        note.index + 1,
        note.phthongLabel,
        note.beatsLabel,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Order index badge.
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(LbmBrown),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = (note.index + 1).toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f).semantics { contentDescription = rowDescription }) {
                Text(
                    text = note.phthongLabel,
                    style = MaterialTheme.typography.titleLarge,
                    color = LbmTextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.melody_trainer_note_duration, note.beatsLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = LbmTextSecondary,
                )
            }
            IconButton(
                onClick = onRemove,
                enabled = note.editable,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.melody_trainer_note_remove),
                    tint = if (note.editable) LbmTextSecondary else LbmTextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Duration stepper.
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, LbmOutline, RoundedCornerShape(10.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepperButton(
                    icon = Icons.Filled.Remove,
                    contentDescription = stringResource(R.string.melody_trainer_duration_decrease),
                    enabled = note.durationEditable,
                    onClick = onDecrement,
                )
                Text(
                    text = note.beatsLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = LbmTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 36.dp),
                )
                StepperButton(
                    icon = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.melody_trainer_duration_increase),
                    enabled = note.durationEditable,
                    onClick = onIncrement,
                )
            }
            Spacer(Modifier.weight(1f))
            // γοργόν toggle chip.
            GorgoChip(
                selected = note.hasGorgo,
                enabled = note.gorgoEnabled,
                onClick = onToggleGorgo,
            )
        }
    }
}

@Composable
private fun GorgoChip(selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        targetValue = if (selected) AccentPurpleContent else LbmSurface,
        label = "gorgoBg",
    )
    val contentColor = when {
        selected -> Color.White
        enabled -> AccentPurpleContent
        else -> LbmTextSecondary.copy(alpha = 0.4f)
    }
    val borderColor = when {
        selected -> AccentPurpleContent
        enabled -> AccentPurpleContent.copy(alpha = 0.5f)
        else -> LbmOutline
    }
    Row(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(container)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .selectable(selected = selected, enabled = enabled, role = Role.Switch, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Bolt,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.melody_trainer_gorgo),
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/* ----------------------------- Tempo ----------------------------- */

@Composable
private fun TempoCard(bpm: Int, enabled: Boolean, onTempoChange: (Int) -> Unit) {
    val valueText = stringResource(R.string.melody_trainer_tempo_value, bpm)
    LessonCard(title = stringResource(R.string.melody_trainer_tempo_title)) {
        ValueChip(text = valueText, container = LbmPrimaryContainer, content = LbmBrown)
        Slider(
            value = bpm.toFloat(),
            onValueChange = { onTempoChange(it.roundToInt()) },
            enabled = enabled,
            valueRange = MelodyTempo.MIN_BPM.toFloat()..MelodyTempo.MAX_BPM.toFloat(),
            modifier = Modifier.semantics { stateDescription = valueText },
            colors = SliderDefaults.colors(
                thumbColor = LbmBrown,
                activeTrackColor = LbmBrown,
                inactiveTrackColor = LbmOutline,
            ),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.melody_trainer_tempo_slow),
                style = MaterialTheme.typography.bodySmall,
                color = LbmTextSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.melody_trainer_tempo_fast),
                style = MaterialTheme.typography.bodySmall,
                color = LbmTextSecondary,
            )
        }
    }
}

/* ----------------------------- Transport ----------------------------- */

@Composable
private fun TransportRow(
    playEnabled: Boolean,
    stopEnabled: Boolean,
    clearEnabled: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = onPlay,
            enabled = playEnabled,
            modifier = Modifier.weight(1f).heightIn(min = 50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LbmBrown,
                contentColor = Color.White,
            ),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.melody_trainer_play), maxLines = 1)
        }
        OutlinedButton(
            onClick = onStop,
            enabled = stopEnabled,
            modifier = Modifier.weight(1f).heightIn(min = 50.dp),
            border = BorderStroke(1.dp, if (stopEnabled) LbmBrown else LbmOutline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = LbmBrown),
        ) {
            Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.melody_trainer_stop), maxLines = 1)
        }
        OutlinedButton(
            onClick = onClear,
            enabled = clearEnabled,
            modifier = Modifier.weight(1f).heightIn(min = 50.dp),
            border = BorderStroke(1.dp, if (clearEnabled) LbmBrown else LbmOutline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = LbmBrown),
        ) {
            Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.melody_trainer_clear), maxLines = 1)
        }
    }
}

/* ----------------------------- Practice modes ----------------------------- */

@Composable
private fun PracticeSection(
    voice: PracticeModeUi,
    rhythm: PracticeModeUi,
    combo: PracticeModeUi,
    onToggleVoice: (Boolean) -> Unit,
    onToggleRhythm: (Boolean) -> Unit,
    onToggleCombo: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text(
                text = stringResource(R.string.melody_trainer_practice_title),
                style = MaterialTheme.typography.titleLarge,
                color = LbmBrown,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.melody_trainer_practice_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
            )
        }
        ModeCard(
            icon = Icons.Filled.RecordVoiceOver,
            titleRes = R.string.melody_trainer_voice_check,
            mode = voice,
            onToggle = onToggleVoice,
        )
        ModeCard(
            icon = Icons.Filled.Timer,
            titleRes = R.string.melody_trainer_rhythm_check,
            mode = rhythm,
            onToggle = onToggleRhythm,
        )
        ModeCard(
            icon = Icons.Filled.Spellcheck,
            titleRes = R.string.melody_trainer_combo_check,
            mode = combo,
            onToggle = onToggleCombo,
        )
    }
}

@Composable
private fun ModeCard(
    icon: ImageVector,
    titleRes: Int,
    mode: PracticeModeUi,
    onToggle: (Boolean) -> Unit,
) {
    val container by animateColorAsState(
        targetValue = if (mode.checked) LbmPrimaryContainer else LbmSurface,
        animationSpec = tween(260),
        label = "modeContainer",
    )
    val borderColor = if (mode.checked) LbmBrown else LbmOutline
    val title = stringResource(titleRes)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        // Merge so TalkBack announces the mode name together with the switch on/off state.
        Row(
            modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (mode.checked) LbmBrown else LbmPrimaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (mode.checked) Color.White else LbmBrown,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = LbmTextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = mode.checked,
                onCheckedChange = onToggle,
                enabled = mode.enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = LbmBrown,
                    uncheckedTrackColor = LbmSurface,
                    uncheckedBorderColor = LbmOutline,
                ),
            )
        }
        Spacer(Modifier.height(10.dp))
        Crossfade(targetState = mode.status, label = "modeStatus") { status ->
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
            )
        }
    }
}

/* ----------------------------- Shared bits ----------------------------- */

@Composable
private fun ValueChip(text: String, container: Color, content: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            fontWeight = FontWeight.Bold,
        )
    }
}
