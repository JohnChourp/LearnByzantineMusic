package com.johnchourp.learnbyzantinemusic.recordings.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.recordings.RecordingFormatOption
import com.johnchourp.learnbyzantinemusic.recordings.RecordingListItem
import com.johnchourp.learnbyzantinemusic.recordings.RecordingStateUi
import com.johnchourp.learnbyzantinemusic.recordings.RecordingsUiState
import com.johnchourp.learnbyzantinemusic.recordings.ui.components.RecordingListItemRow
import com.johnchourp.learnbyzantinemusic.ui.components.LessonCard
import com.johnchourp.learnbyzantinemusic.ui.components.LessonChip
import com.johnchourp.learnbyzantinemusic.ui.components.LessonHero
import com.johnchourp.learnbyzantinemusic.ui.components.StaggeredAppear
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentCrimsonContent
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGoldContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGoldContent
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGreenContent
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmMeasureBar
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPageBg
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurfaceVariant
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary
import java.util.Locale

/**
 * Redesigned «Ηχογραφήσεις» main screen. A pure renderer of [RecordingsUiState] + callbacks built on
 * the shared design system (hero, [StaggeredAppear] + [LessonCard] sections, [LessonChip] format
 * picker) with state-aware recording feedback (pulsing dot + MM:SS timer). All recording, folder
 * (SAF), transcoding and indexing logic stays in the host Activity — this only changes rendering.
 */
@Composable
fun RecordingsScreen(
    uiState: RecordingsUiState,
    recentItems: LazyPagingItems<RecordingListItem>,
    onBack: () -> Unit,
    onChangeFolder: () -> Unit,
    onOpenFolder: () -> Unit,
    onOpenManager: () -> Unit,
    onStartRecording: () -> Unit,
    onPauseResume: () -> Unit,
    onStopRecording: () -> Unit,
    onFormatChanged: (RecordingFormatOption) -> Unit,
    onOpenRecording: (RecordingListItem) -> Unit,
    onRenameRecording: (RecordingListItem) -> Unit,
    onDeleteRecording: (RecordingListItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasSelectedFolder = !uiState.folderName.isNullOrBlank()
    val refreshState = recentItems.loadState.refresh
    val appendState = recentItems.loadState.append
    val showListLoading = hasSelectedFolder &&
        recentItems.itemCount > 0 &&
        (refreshState is LoadState.Loading || appendState is LoadState.Loading)
    val showEmptyState = hasSelectedFolder &&
        recentItems.itemCount == 0 &&
        refreshState !is LoadState.Error

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LbmPageBg),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "hero") {
            Column {
                LessonHero(
                    title = stringResource(R.string.recordings_page_title),
                    subtitle = stringResource(R.string.recordings_subtitle),
                    onBack = onBack,
                    icon = Icons.Filled.Mic,
                )
                if (uiState.isIndexing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = LbmBrown,
                        trackColor = LbmSurfaceVariant,
                    )
                }
            }
        }

        item(key = "folder") {
            StaggeredAppear(delayMillis = 60, modifier = Modifier.padding(horizontal = 16.dp)) {
                FolderSection(
                    folderName = uiState.folderName,
                    onChangeFolder = onChangeFolder,
                    onOpenFolder = onOpenFolder,
                    onOpenManager = onOpenManager,
                )
            }
        }

        item(key = "record") {
            StaggeredAppear(delayMillis = 120, modifier = Modifier.padding(horizontal = 16.dp)) {
                RecordSection(
                    recordingState = uiState.recordingState,
                    statusMessage = uiState.statusMessage,
                    onStartRecording = onStartRecording,
                    onPauseResume = onPauseResume,
                    onStopRecording = onStopRecording,
                )
            }
        }

        item(key = "format") {
            StaggeredAppear(delayMillis = 180, modifier = Modifier.padding(horizontal = 16.dp)) {
                FormatSection(
                    selectedFormat = uiState.selectedFormat,
                    onFormatChanged = onFormatChanged,
                )
            }
        }

        item(key = "recent-header") {
            StaggeredAppear(delayMillis = 240, modifier = Modifier.padding(horizontal = 16.dp)) {
                SectionHeader(
                    icon = Icons.Filled.GraphicEq,
                    title = stringResource(R.string.recordings_recent_title),
                )
            }
        }

        items(
            count = recentItems.itemCount,
            key = { index -> recentItems[index]?.uri?.toString() ?: "placeholder_$index" },
        ) { index ->
            val item = recentItems[index] ?: return@items
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                RecordingListItemRow(
                    item = item,
                    onOpen = onOpenRecording,
                    onRename = onRenameRecording,
                    onDelete = onDeleteRecording,
                )
            }
        }

        if (showListLoading) {
            item(key = "loading") {
                RecordingsLoadingRow(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        if (showEmptyState) {
            item(key = "empty") {
                RecordingsEmptyState(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        if (!hasSelectedFolder) {
            item(key = "no-folder-hint") {
                Text(
                    text = stringResource(R.string.recordings_select_folder_first),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LbmTextSecondary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun FolderSection(
    folderName: String?,
    onChangeFolder: () -> Unit,
    onOpenFolder: () -> Unit,
    onOpenManager: () -> Unit,
) {
    LessonCard(title = stringResource(R.string.recordings_section_folder)) {
        val hasFolder = !folderName.isNullOrBlank()
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = LbmSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = LbmBrown,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = folderName?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.recordings_folder_status_missing),
                    style = MaterialTheme.typography.titleMedium,
                    color = LbmTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                AccessDot(ready = hasFolder)
            }
        }

        Spacer(Modifier.height(12.dp))

        IconLabelButton(
            icon = Icons.Filled.Folder,
            label = stringResource(R.string.recordings_change_folder),
            onClick = onChangeFolder,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = LbmSurfaceVariant,
                contentColor = LbmTextPrimary,
            ),
            tonal = true,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onOpenFolder,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = LbmBrown),
        ) {
            ButtonContent(Icons.Filled.FolderOpen, stringResource(R.string.recordings_open_folder))
        }
        Spacer(Modifier.height(8.dp))
        IconLabelButton(
            icon = Icons.Filled.Inventory2,
            label = stringResource(R.string.recordings_manage_open),
            onClick = onOpenManager,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = AccentGoldContainer,
                contentColor = AccentGoldContent,
            ),
            tonal = true,
        )
    }
}

@Composable
private fun RecordSection(
    recordingState: RecordingStateUi,
    statusMessage: String,
    onStartRecording: () -> Unit,
    onPauseResume: () -> Unit,
    onStopRecording: () -> Unit,
) {
    val elapsedMs = rememberRecordingElapsed(recordingState)
    val isActive = recordingState == RecordingStateUi.RECORDING ||
        recordingState == RecordingStateUi.PAUSED ||
        recordingState == RecordingStateUi.SAVING
    val isError = recordingState == RecordingStateUi.ERROR
    val showTimer = recordingState == RecordingStateUi.RECORDING || recordingState == RecordingStateUi.PAUSED
    val elapsedCd = stringResource(R.string.recordings_recording_elapsed_content_description)

    LessonCard(title = stringResource(R.string.recordings_section_record)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RecordStateIndicator(recordingState)
                if (showTimer) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = formatElapsed(elapsedMs),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (recordingState == RecordingStateUi.RECORDING) LbmMeasureBar else LbmTextSecondary,
                        modifier = Modifier.semantics {
                            contentDescription = elapsedCd
                        },
                    )
                }
            }

            val caption = statusMessage.takeIf { it.isNotBlank() } ?: defaultCaption(recordingState)
            if (caption.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isError) AccentCrimsonContent else LbmTextSecondary,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(16.dp))

            AnimatedContent(
                targetState = isActive,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                label = "recordControls",
            ) { active ->
                if (!active) {
                    Button(
                        onClick = onStartRecording,
                        enabled = recordingState == RecordingStateUi.IDLE || recordingState == RecordingStateUi.ERROR,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LbmBrown,
                            contentColor = Color.White,
                        ),
                    ) {
                        ButtonContent(Icons.Filled.Mic, stringResource(R.string.recordings_start))
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton(
                            onClick = onPauseResume,
                            enabled = recordingState != RecordingStateUi.SAVING,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = LbmSurfaceVariant,
                                contentColor = LbmTextPrimary,
                            ),
                        ) {
                            val paused = recordingState == RecordingStateUi.PAUSED
                            ButtonContent(
                                icon = if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                label = stringResource(
                                    if (paused) R.string.recordings_resume else R.string.recordings_pause,
                                ),
                            )
                        }
                        Button(
                            onClick = onStopRecording,
                            enabled = recordingState != RecordingStateUi.SAVING,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LbmMeasureBar,
                                contentColor = Color.White,
                            ),
                        ) {
                            ButtonContent(Icons.Filled.Stop, stringResource(R.string.recordings_stop))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordStateIndicator(recordingState: RecordingStateUi) {
    when (recordingState) {
        RecordingStateUi.RECORDING -> PulsingRecordDot()
        RecordingStateUi.SAVING -> CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.5.dp,
            color = LbmBrown,
        )
        RecordingStateUi.PAUSED -> StaticDot(LbmTextSecondary)
        RecordingStateUi.ERROR -> Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = AccentCrimsonContent,
            modifier = Modifier.size(26.dp),
        )
        RecordingStateUi.IDLE -> Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = null,
            tint = LbmBrown,
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
private fun PulsingRecordDot() {
    val infinite = rememberInfiniteTransition(label = "recDot")
    val scale by infinite.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "recDotScale",
    )
    val alpha by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "recDotAlpha",
    )
    val indicatorCd = stringResource(R.string.recordings_recording_indicator_content_description)
    Box(
        modifier = Modifier
            .size(16.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(CircleShape)
            .background(LbmMeasureBar)
            .semantics { contentDescription = indicatorCd },
    )
}

@Composable
private fun StaticDot(color: Color) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun FormatSection(
    selectedFormat: RecordingFormatOption,
    onFormatChanged: (RecordingFormatOption) -> Unit,
) {
    LessonCard(title = stringResource(R.string.recordings_format_label)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RecordingFormatOption.entries.forEach { option ->
                LessonChip(
                    label = stringResource(option.labelResId),
                    selected = option == selectedFormat,
                    onClick = { onFormatChanged(option) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = AccentGoldContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(selectedFormat.descriptionResId),
                style = MaterialTheme.typography.bodyMedium,
                color = AccentGoldContent,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = LbmBrown)
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = LbmTextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RecordingsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(AccentGoldContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = null,
                tint = AccentGoldContent,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.recordings_list_empty),
            style = MaterialTheme.typography.titleMedium,
            color = LbmTextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.recordings_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RecordingsLoadingRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp, color = LbmBrown)
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.recordings_status_loading_list),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
    }
}

@Composable
private fun AccessDot(ready: Boolean) {
    val dotCd = stringResource(
        if (ready) R.string.recordings_folder_status_ready else R.string.recordings_folder_status_missing,
    )
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(if (ready) AccentGreenContent else AccentCrimsonContent)
            .semantics { contentDescription = dotCd },
    )
}

@Composable
private fun IconLabelButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    colors: androidx.compose.material3.ButtonColors,
    tonal: Boolean,
) {
    if (tonal) {
        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = colors,
        ) {
            ButtonContent(icon, label)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = colors,
        ) {
            ButtonContent(icon, label)
        }
    }
}

@Composable
private fun ButtonContent(icon: ImageVector, label: String) {
    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    Text(text = label, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/**
 * Pure Compose-side recording stopwatch: counts up only while [RecordingStateUi.RECORDING], holds
 * while PAUSED/SAVING, resets on IDLE/ERROR. Frame-clock based so it stays accurate without touching
 * the audio capture engine.
 */
@Composable
private fun rememberRecordingElapsed(recordingState: RecordingStateUi): Long {
    var elapsedMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(recordingState) {
        when (recordingState) {
            RecordingStateUi.IDLE, RecordingStateUi.ERROR -> elapsedMs = 0L
            RecordingStateUi.RECORDING -> {
                var last = 0L
                var primed = false
                while (true) {
                    withFrameMillis { frame ->
                        if (primed) elapsedMs += frame - last
                        last = frame
                        primed = true
                    }
                }
            }
            else -> Unit // PAUSED, SAVING — hold the current value
        }
    }
    return elapsedMs
}

private fun formatElapsed(elapsedMs: Long): String {
    val totalSeconds = elapsedMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

@Composable
private fun defaultCaption(recordingState: RecordingStateUi): String = when (recordingState) {
    RecordingStateUi.IDLE -> stringResource(R.string.recordings_ready)
    RecordingStateUi.RECORDING -> stringResource(R.string.recordings_status_recording)
    RecordingStateUi.PAUSED -> stringResource(R.string.recordings_status_paused)
    RecordingStateUi.SAVING -> stringResource(R.string.recordings_status_saving)
    RecordingStateUi.ERROR -> stringResource(R.string.recordings_error_start)
}
