package com.johnchourp.learnbyzantinemusic.recordings.analysis.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.johnchourp.learnbyzantinemusic.recordings.analysis.AlignmentResult
import com.johnchourp.learnbyzantinemusic.recordings.analysis.AlignmentStep
import com.johnchourp.learnbyzantinemusic.recordings.analysis.AnalysisStatus
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
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                        notes = uiState.notes,
                        alignment = uiState.alignment,
                        onAdd = onAddExpected,
                        onRemoveLast = onRemoveLastExpected,
                        onClear = onClearExpected,
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
    notes: List<SungNote>,
    alignment: AlignmentResult?,
    onAdd: (PhthongName) -> Unit,
    onRemoveLast: () -> Unit,
    onClear: () -> Unit,
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
        Spacer(Modifier.height(10.dp))
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
        if (alignment != null) {
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
                        is AlignmentStep.Match -> NoteChip("✓ ${expected[step.expected].displayName}", AccentGreenContainer, AccentGreenContent)
                        is AlignmentStep.Substitution -> NoteChip(
                            stringResource(R.string.analysis_step_wrong, expected[step.expected].displayName, phthongLabel(notes[step.sung].degree)),
                            AccentOrangeContainer,
                            AccentOrangeContent,
                        )
                        is AlignmentStep.Missing -> NoteChip(
                            stringResource(R.string.analysis_step_missing, expected[step.expected].displayName),
                            LbmSurfaceVariant,
                            AccentCrimsonContent,
                        )
                        is AlignmentStep.Extra -> NoteChip(
                            stringResource(R.string.analysis_step_extra, phthongLabel(notes[step.sung].degree)),
                            LbmSurfaceVariant,
                            LbmTextSecondary,
                        )
                    }
                }
            }
        }
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
