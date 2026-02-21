package com.johnchourp.learnbyzantinemusic.recordings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.recordings.RecordingFormatOption
import com.johnchourp.learnbyzantinemusic.recordings.RecordingListItem
import com.johnchourp.learnbyzantinemusic.recordings.RecordingStateUi
import com.johnchourp.learnbyzantinemusic.recordings.RecordingsUiState
import com.johnchourp.learnbyzantinemusic.recordings.ui.components.RecordingListItemRow

@Composable
fun RecordingsScreen(
    uiState: RecordingsUiState,
    recentItems: LazyPagingItems<RecordingListItem>,
    onChangeFolder: () -> Unit,
    onOpenFolder: () -> Unit,
    onOpenManager: () -> Unit,
    onStartRecording: () -> Unit,
    onPauseResume: () -> Unit,
    onStopRecording: () -> Unit,
    onFormatChanged: (RecordingFormatOption) -> Unit,
    onOpenRecording: (RecordingListItem) -> Unit,
    onRenameRecording: (RecordingListItem) -> Unit,
    onDeleteRecording: (RecordingListItem) -> Unit
) {
    var formatExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.recordings_page_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isIndexing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (uiState.statusMessage.isNotBlank()) {
            Text(
                text = uiState.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        val folderLabel = uiState.folderName?.takeIf { it.isNotBlank() }?.let {
            stringResource(R.string.recordings_folder_selected_template, it)
        } ?: stringResource(R.string.recordings_folder_not_selected)

        Text(
            text = folderLabel,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onChangeFolder
            ) {
                Text(text = stringResource(R.string.recordings_change_folder))
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = onOpenFolder
            ) {
                Text(text = stringResource(R.string.recordings_open_folder))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenManager
        ) {
            Text(text = stringResource(R.string.recordings_manage_open))
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onStartRecording,
            enabled = uiState.recordingState == RecordingStateUi.IDLE || uiState.recordingState == RecordingStateUi.ERROR
        ) {
            Text(text = stringResource(R.string.recordings_start))
        }

        if (uiState.recordingState == RecordingStateUi.RECORDING || uiState.recordingState == RecordingStateUi.PAUSED || uiState.recordingState == RecordingStateUi.SAVING) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onPauseResume,
                    enabled = uiState.recordingState != RecordingStateUi.SAVING
                ) {
                    val pauseLabel = if (uiState.recordingState == RecordingStateUi.PAUSED) {
                        stringResource(R.string.recordings_resume)
                    } else {
                        stringResource(R.string.recordings_pause)
                    }
                    Text(text = pauseLabel)
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onStopRecording,
                    enabled = uiState.recordingState != RecordingStateUi.SAVING
                ) {
                    Text(text = stringResource(R.string.recordings_stop))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(id = R.string.recordings_format_label),
            style = MaterialTheme.typography.titleSmall
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { formatExpanded = true }) {
                Text(text = stringResource(id = uiState.selectedFormat.labelResId))
            }
            androidx.compose.material3.DropdownMenu(
                expanded = formatExpanded,
                onDismissRequest = { formatExpanded = false }
            ) {
                RecordingFormatOption.entries.forEach { option ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(text = stringResource(id = option.labelResId)) },
                        onClick = {
                            formatExpanded = false
                            onFormatChanged(option)
                        }
                    )
                }
            }
        }

        Text(
            text = stringResource(id = uiState.selectedFormat.descriptionResId),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.recordings_recent_title),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(
                count = recentItems.itemCount,
                key = { index -> recentItems[index]?.uri?.toString() ?: "placeholder_$index" }
            ) { index ->
                val item = recentItems[index] ?: return@items
                RecordingListItemRow(
                    item = item,
                    onOpen = onOpenRecording,
                    onRename = onRenameRecording,
                    onDelete = onDeleteRecording
                )
            }

            if (recentItems.loadState.append is LoadState.Loading || recentItems.loadState.refresh is LoadState.Loading) {
                item(key = "loading") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (recentItems.itemCount == 0 && recentItems.loadState.refresh is LoadState.NotLoading) {
                item(key = "empty") {
                    Text(
                        modifier = Modifier.padding(vertical = 16.dp),
                        text = stringResource(R.string.recordings_list_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
