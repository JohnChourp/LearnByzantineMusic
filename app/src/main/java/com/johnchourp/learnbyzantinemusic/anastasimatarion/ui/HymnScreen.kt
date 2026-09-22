package com.johnchourp.learnbyzantinemusic.anastasimatarion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.anastasimatarion.AnastasimatarionLabels
import com.johnchourp.learnbyzantinemusic.anastasimatarion.HymnFolders
import com.johnchourp.learnbyzantinemusic.anastasimatarion.HymnRecording
import com.johnchourp.learnbyzantinemusic.anastasimatarion.HymnUiState
import com.johnchourp.learnbyzantinemusic.ui.components.LessonCard
import com.johnchourp.learnbyzantinemusic.ui.components.LessonHero
import com.johnchourp.learnbyzantinemusic.ui.components.StaggeredAppear
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmOutline
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPageBg
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary
import java.text.DateFormat
import java.util.Date

/**
 * Renders [HymnUiState]: the hymn (incipit, mode · service · group), a record button that saves
 * into the hymn's folder, and the hymn's recordings with an «Άνοιγμα» action each.
 */
@Composable
fun HymnScreen(
    uiState: HymnUiState,
    onBack: () -> Unit,
    onRecord: () -> Unit,
    onOpenRecording: (HymnRecording) -> Unit,
    onAnalyzeRecording: (HymnRecording) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ref = uiState.ref
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LbmPageBg),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "hero") {
            LessonHero(
                title = ref?.hymn?.incipit ?: stringResource(R.string.anastasimatarion_title),
                subtitle = if (ref == null) {
                    ""
                } else {
                    listOfNotNull(
                        stringResource(AnastasimatarionLabels.modeName(ref.modeKey)),
                        stringResource(AnastasimatarionLabels.serviceName(ref.serviceKey)),
                        stringResource(AnastasimatarionLabels.groupName(ref.groupKey)),
                        ref.hymn.note?.let { noteText(it) },
                    ).joinToString(" · ")
                },
                onBack = onBack,
                icon = Icons.Filled.LibraryMusic,
            )
        }

        if (ref == null) {
            if (!uiState.loading) {
                item(key = "missing") {
                    Text(
                        text = stringResource(R.string.anastasimatarion_hymn_missing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LbmTextSecondary,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }
            return@LazyColumn
        }

        item(key = "record") {
            StaggeredAppear(delayMillis = 60, modifier = Modifier.padding(horizontal = 16.dp)) {
                LessonCard(title = stringResource(R.string.anastasimatarion_record_title)) {
                    Text(
                        text = stringResource(
                            R.string.anastasimatarion_record_folder_template,
                            HymnFolders.pathSegments(ref.modeKey, ref.hymn).joinToString(" › "),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LbmTextSecondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onRecord,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LbmBrown, contentColor = Color.White),
                    ) {
                        Icon(imageVector = Icons.Filled.Mic, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.anastasimatarion_record_button))
                    }
                }
            }
        }

        item(key = "recordings") {
            StaggeredAppear(delayMillis = 120, modifier = Modifier.padding(horizontal = 16.dp)) {
                LessonCard(title = stringResource(R.string.anastasimatarion_my_recordings)) {
                    when {
                        !uiState.hasRecordingsFolder && !uiState.loading -> HintLine(stringResource(R.string.anastasimatarion_no_folder_hint))
                        uiState.recordings.isEmpty() && !uiState.loading -> HintLine(stringResource(R.string.anastasimatarion_no_recordings))
                        else -> Column(modifier = Modifier.fillMaxWidth()) {
                            uiState.recordings.forEachIndexed { index, recording ->
                                if (index > 0) HorizontalDivider(color = LbmOutline)
                                RecordingRow(
                                    recording = recording,
                                    onOpen = { onOpenRecording(recording) },
                                    onAnalyze = { onAnalyzeRecording(recording) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingRow(recording: HymnRecording, onOpen: () -> Unit, onAnalyze: () -> Unit) {
    val formatter = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = Icons.Filled.GraphicEq, contentDescription = null, tint = LbmBrown)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = recording.name,
                style = MaterialTheme.typography.bodyLarge,
                color = LbmTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (recording.lastModified > 0L) {
                Text(
                    text = formatter.format(Date(recording.lastModified)),
                    style = MaterialTheme.typography.bodySmall,
                    color = LbmTextSecondary,
                )
            }
        }
        TextButton(onClick = onAnalyze) {
            Text(stringResource(R.string.anastasimatarion_analyze_recording))
        }
        TextButton(onClick = onOpen) {
            Text(stringResource(R.string.anastasimatarion_open_recording))
        }
    }
}

@Composable
private fun HintLine(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = LbmTextSecondary)
}
