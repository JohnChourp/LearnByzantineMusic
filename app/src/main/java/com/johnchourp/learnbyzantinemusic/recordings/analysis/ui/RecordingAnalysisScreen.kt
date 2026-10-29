package com.johnchourp.learnbyzantinemusic.recordings.analysis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.modes.ModeResources
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.IntonationProfile
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.recordings.analysis.AlignmentStep
import com.johnchourp.learnbyzantinemusic.recordings.analysis.AnalysisStatus
import com.johnchourp.learnbyzantinemusic.recordings.analysis.ExerciseChoice
import com.johnchourp.learnbyzantinemusic.recordings.analysis.ExercisePicker
import com.johnchourp.learnbyzantinemusic.recordings.analysis.MelodyComparison
import com.johnchourp.learnbyzantinemusic.recordings.analysis.RecordingAnalysisUiState
import com.johnchourp.learnbyzantinemusic.recordings.analysis.SungNote
import com.johnchourp.learnbyzantinemusic.ui.components.LessonCard
import com.johnchourp.learnbyzantinemusic.ui.components.LessonChip
import com.johnchourp.learnbyzantinemusic.ui.components.LessonHero
import com.johnchourp.learnbyzantinemusic.ui.components.StaggeredAppear
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentCrimsonContent
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGreenContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGreenContent
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentOrangeContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentOrangeContent
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPageBg
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurfaceVariant
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary
import kotlin.math.roundToInt

/** Renders [RecordingAnalysisUiState]; all work happens in the view model. */
@Composable
fun RecordingAnalysisScreen(
    uiState: RecordingAnalysisUiState,
    onBack: () -> Unit,
    onSelectMode: (Mode) -> Unit,
    onSelectStart: (PhthongName) -> Unit,
    onAddExpected: (PhthongName) -> Unit,
    onRemoveLastExpected: () -> Unit,
    onClearExpected: () -> Unit,
    onOpenExercises: () -> Unit,
    onUseExercise: (ExerciseChoice) -> Unit,
    onCloseExercises: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Outside the list: a dialog inside a LazyColumn item would close when the item scrolls away.
    uiState.exercisePicker?.let { picker ->
        ExercisePickerDialog(
            picker = picker,
            hasMelody = uiState.expected.isNotEmpty(),
            onUse = onUseExercise,
            onDismiss = onCloseExercises,
        )
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LbmPageBg),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "hero") {
            LessonHero(
                title = stringResource(R.string.analysis_title),
                subtitle = uiState.recordingName,
                onBack = onBack,
                icon = Icons.Filled.GraphicEq,
            )
        }

        item(key = "scale") {
            StaggeredAppear(delayMillis = 60, modifier = Modifier.padding(horizontal = 16.dp)) {
                ScaleCard(uiState = uiState, onSelectMode = onSelectMode, onSelectStart = onSelectStart)
            }
        }

        item(key = "status") {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                StatusBlock(uiState = uiState, onRetry = onRetry)
            }
        }

        if (uiState.status == AnalysisStatus.READY) {
            item(key = "sung") {
                StaggeredAppear(delayMillis = 60, modifier = Modifier.padding(horizontal = 16.dp)) {
                    SungCard(uiState.notes, uiState.track?.durationMs ?: 0L)
                }
            }
            val track = uiState.track
            val niHz = uiState.niHz
            if (track != null && niHz != null) {
                item(key = "diagram") {
                    StaggeredAppear(delayMillis = 120, modifier = Modifier.padding(horizontal = 16.dp)) {
                        LessonCard(title = stringResource(R.string.analysis_diagram_title)) {
                            PitchDiagram(
                                track = track,
                                notes = uiState.notes,
                                niHz = niHz,
                                positions = uiState.positions,
                                description = stringResource(R.string.analysis_diagram_cd, uiState.notes.size),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.analysis_diagram_legend),
                                style = MaterialTheme.typography.bodySmall,
                                color = LbmTextSecondary,
                            )
                        }
                    }
                }
            }
            item(key = "compare") {
                StaggeredAppear(delayMillis = 180, modifier = Modifier.padding(horizontal = 16.dp)) {
                    CompareCard(
                        expected = uiState.expected,
                        comparison = uiState.comparison,
                        onAdd = onAddExpected,
                        onRemoveLast = onRemoveLastExpected,
                        onClear = onClearExpected,
                        onOpenExercises = onOpenExercises,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScaleCard(
    uiState: RecordingAnalysisUiState,
    onSelectMode: (Mode) -> Unit,
    onSelectStart: (PhthongName) -> Unit,
) {
    LessonCard(title = stringResource(R.string.analysis_scale_title)) {
        Text(stringResource(R.string.analysis_mode_label), style = MaterialTheme.typography.labelLarge, color = LbmBrown)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Mode.entries.forEach { mode ->
                LessonChip(
                    label = stringResource(ModeResources.nameRes(mode)),
                    selected = mode == uiState.mode,
                    onClick = { onSelectMode(mode) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.analysis_start_label), style = MaterialTheme.typography.labelLarge, color = LbmBrown)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PhthongName.entries.forEach { phthong ->
                LessonChip(
                    label = phthong.displayName,
                    selected = phthong == uiState.startPhthong,
                    onClick = { onSelectStart(phthong) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.analysis_scale_hint),
            style = MaterialTheme.typography.bodySmall,
            color = LbmTextSecondary,
        )
    }
}

@Composable
private fun StatusBlock(uiState: RecordingAnalysisUiState, onRetry: () -> Unit) {
    when (uiState.status) {
        AnalysisStatus.DECODING, AnalysisStatus.ANALYZING -> {
            Text(
                text = stringResource(
                    if (uiState.status == AnalysisStatus.DECODING) R.string.analysis_status_decoding else R.string.analysis_status_analyzing,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
            )
            Spacer(Modifier.height(6.dp))
            if (uiState.status == AnalysisStatus.ANALYZING) {
                LinearProgressIndicator(progress = { uiState.progress }, modifier = Modifier.fillMaxWidth(), color = LbmBrown)
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = LbmBrown)
            }
        }

        AnalysisStatus.FAILED -> {
            Text(stringResource(R.string.analysis_status_failed), style = MaterialTheme.typography.bodyMedium, color = AccentCrimsonContent)
            TextButton(onClick = onRetry) { Text(stringResource(R.string.analysis_retry)) }
        }

        AnalysisStatus.NO_PITCH -> Text(
            text = stringResource(R.string.analysis_status_no_pitch),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )

        AnalysisStatus.READY -> Unit
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SungCard(notes: List<SungNote>, durationMs: Long) {
    LessonCard(title = stringResource(R.string.analysis_sung_title)) {
        val inTune = notes.count { it.isInTune }
        Text(
            // The tolerance is printed from the profile it is judged by, so the text cannot drift from it.
            text = stringResource(
                R.string.analysis_sung_summary,
                notes.size,
                formatDuration(durationMs),
                inTune,
                IntonationProfile.IN_TUNE_MORIA.roundToInt(),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            notes.forEach { note ->
                val good = note.isInTune
                NoteChip(
                    text = phthongLabel(note.degree),
                    container = if (good) AccentGreenContainer else AccentOrangeContainer,
                    content = if (good) AccentGreenContent else AccentOrangeContent,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompareCard(
    expected: List<PhthongName>,
    comparison: MelodyComparison?,
    onAdd: (PhthongName) -> Unit,
    onRemoveLast: () -> Unit,
    onClear: () -> Unit,
    onOpenExercises: () -> Unit,
) {
    LessonCard(title = stringResource(R.string.analysis_compare_title)) {
        Text(
            text = stringResource(R.string.analysis_compare_hint),
            style = MaterialTheme.typography.bodySmall,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PhthongName.entries.forEach { phthong ->
                OutlinedButton(onClick = { onAdd(phthong) }, contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Text(phthong.displayName, color = LbmTextPrimary)
                }
            }
            IconButton(onClick = onRemoveLast, enabled = expected.isNotEmpty()) {
                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = stringResource(R.string.analysis_compare_backspace))
            }
        }
        TextButton(onClick = onOpenExercises) { Text(stringResource(R.string.analysis_compare_from_exercises)) }
        Spacer(Modifier.height(4.dp))
        if (expected.isEmpty()) {
            Text(stringResource(R.string.analysis_compare_empty), style = MaterialTheme.typography.bodyMedium, color = LbmTextSecondary)
        } else {
            Text(
                text = stringResource(R.string.analysis_compare_expected, expected.size),
                style = MaterialTheme.typography.labelLarge,
                color = LbmBrown,
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                expected.forEach { NoteChip(it.displayName, LbmSurfaceVariant, LbmTextPrimary) }
            }
            TextButton(onClick = onClear) { Text(stringResource(R.string.analysis_compare_clear)) }
        }
        if (comparison != null) {
            // Steps are read only through the comparison's own lists (MelodyComparison).
            val alignment = comparison.alignment
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(
                    R.string.analysis_compare_score,
                    alignment.matches,
                    alignment.expectedCount,
                    (alignment.accuracy * 100).roundToInt(),
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (alignment.accuracy >= 0.8) AccentGreenContent else AccentOrangeContent,
            )
            if (alignment.extras > 0) {
                Text(
                    text = stringResource(R.string.analysis_compare_extras, alignment.extras),
                    style = MaterialTheme.typography.bodySmall,
                    color = LbmTextSecondary,
                )
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                alignment.steps.forEach { step ->
                    when (step) {
                        is AlignmentStep.Match -> NoteChip("✓ ${comparison.expected[step.expected].displayName}", AccentGreenContainer, AccentGreenContent)
                        is AlignmentStep.Substitution -> NoteChip(
                            stringResource(
                                R.string.analysis_step_wrong,
                                comparison.expected[step.expected].displayName,
                                phthongLabel(comparison.notes[step.sung].degree),
                            ),
                            AccentOrangeContainer,
                            AccentOrangeContent,
                        )
                        is AlignmentStep.Missing -> NoteChip(
                            stringResource(R.string.analysis_step_missing, comparison.expected[step.expected].displayName),
                            LbmSurfaceVariant,
                            AccentCrimsonContent,
                        )
                        is AlignmentStep.Extra -> NoteChip(
                            stringResource(R.string.analysis_step_extra, phthongLabel(comparison.notes[step.sung].degree)),
                            LbmSurfaceVariant,
                            LbmTextSecondary,
                        )
                    }
                }
            }
        }
    }
}

/**
 * «Από τις ασκήσεις μου». A melody already entered is replaced only after a yes, as «Άνοιγμα» asks
 * first in the Melody Trainer.
 */
@Composable
private fun ExercisePickerDialog(
    picker: ExercisePicker,
    hasMelody: Boolean,
    onUse: (ExerciseChoice) -> Unit,
    onDismiss: () -> Unit,
) {
    var pending by remember(picker) { mutableStateOf<ExerciseChoice?>(null) }
    val asking = pending
    if (asking != null) {
        AlertDialog(
            onDismissRequest = { pending = null },
            text = { Text(stringResource(R.string.analysis_exercise_replace, asking.phthongs.size, asking.name)) },
            confirmButton = {
                TextButton(onClick = { onUse(asking) }) { Text(stringResource(R.string.analysis_exercise_replace_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text(stringResource(R.string.melody_trainer_exercise_cancel)) }
            },
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.melody_trainer_exercises_title)) },
        text = {
            when {
                picker.newerFormat -> Text(stringResource(R.string.melody_trainer_exercises_newer))
                picker.choices.isEmpty() -> Text(stringResource(R.string.analysis_exercises_empty))
                else -> LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(picker.choices, key = { it.name }) { choice ->
                        ExerciseChoiceRow(choice, onClick = { if (hasMelody) pending = choice else onUse(choice) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.melody_trainer_exercise_cancel)) }
        },
    )
}

@Composable
private fun ExerciseChoiceRow(choice: ExerciseChoice, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Text(choice.name, style = MaterialTheme.typography.bodyLarge, color = LbmTextPrimary, fontWeight = FontWeight.SemiBold)
        Text(
            text = stringResource(R.string.analysis_exercise_count, choice.phthongs.size),
            style = MaterialTheme.typography.bodySmall,
            color = LbmTextSecondary,
        )
    }
}

@Composable
private fun NoteChip(text: String, container: Color, content: Color) {
    Surface(shape = RoundedCornerShape(10.dp), color = container) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
