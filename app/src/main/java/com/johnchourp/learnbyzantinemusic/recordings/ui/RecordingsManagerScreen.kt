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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.johnchourp.learnbyzantinemusic.recordings.ManagerEntryType
import com.johnchourp.learnbyzantinemusic.recordings.ManagerListItem
import com.johnchourp.learnbyzantinemusic.recordings.MoveTargetFolder
import com.johnchourp.learnbyzantinemusic.recordings.RecordingsManagerUiState
import com.johnchourp.learnbyzantinemusic.recordings.RecordingSortOption
import com.johnchourp.learnbyzantinemusic.recordings.RecordingTypeFilter
import com.johnchourp.learnbyzantinemusic.recordings.ui.components.ManagerListItemRow

@Composable
fun RecordingsManagerScreen(
    uiState: RecordingsManagerUiState,
    entries: LazyPagingItems<ManagerListItem>,
    onNavigateUp: () -> Unit,
    onCreateFolder: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onSortChanged: (RecordingSortOption) -> Unit,
    onFilterChanged: (RecordingTypeFilter) -> Unit,
    onOpenEntry: (ManagerListItem) -> Unit,
    onRenameEntry: (ManagerListItem) -> Unit,
    onDeleteEntry: (ManagerListItem) -> Unit,
    onMoveEntry: (ManagerListItem, MoveTargetFolder) -> Unit
) {
    var sortExpanded by remember { mutableStateOf(false) }
    var filterExpanded by remember { mutableStateOf(false) }
    var pendingMoveItem by remember { mutableStateOf<ManagerListItem?>(null) }
    var moveSearchQuery by remember { mutableStateOf("") }

    val moveCandidates = remember(uiState.moveTargets, moveSearchQuery, pendingMoveItem) {
        uiState.moveTargets.filter { target ->
            if (pendingMoveItem != null && target.uri == pendingMoveItem?.parentUri) {
                return@filter false
            }
            if (moveSearchQuery.isBlank()) {
                true
            } else {
                target.displayPath.contains(moveSearchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(
            text = stringResource(R.string.recordings_manage_open),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isIndexing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = stringResource(R.string.recordings_manage_breadcrumb_template, uiState.rootFolderName, uiState.breadcrumbPathDisplay),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (uiState.statusMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = uiState.statusMessage,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onNavigateUp,
                enabled = uiState.currentRelativePath.isNotBlank()
            ) {
                Text(text = stringResource(R.string.recordings_manage_back))
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = onCreateFolder
            ) {
                Text(text = stringResource(R.string.recordings_manage_create_folder))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(text = stringResource(R.string.recordings_search_label)) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = { filterExpanded = true }) {
                Text(text = stringResource(uiState.selectedFilter.labelResId))
            }
            DropdownMenu(
                expanded = filterExpanded,
                onDismissRequest = { filterExpanded = false }
            ) {
                RecordingTypeFilter.entries.forEach { filter ->
                    DropdownMenuItem(
                        text = { Text(text = stringResource(filter.labelResId)) },
                        onClick = {
                            filterExpanded = false
                            onFilterChanged(filter)
                        }
                    )
                }
            }

            TextButton(onClick = { sortExpanded = true }) {
                Text(text = stringResource(uiState.selectedSortOption.labelResId))
            }
            DropdownMenu(
                expanded = sortExpanded,
                onDismissRequest = { sortExpanded = false }
            ) {
                RecordingSortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = stringResource(option.labelResId)) },
                        onClick = {
                            sortExpanded = false
                            onSortChanged(option)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(
                count = entries.itemCount,
                key = { index -> entries[index]?.uri?.toString() ?: "placeholder_$index" }
            ) { index ->
                val item = entries[index] ?: return@items
                ManagerListItemRow(
                    item = item,
                    onOpen = onOpenEntry,
                    onRename = onRenameEntry,
                    onMove = { pendingMoveItem = it },
                    onDelete = onDeleteEntry
                )
            }

            if (entries.loadState.append is LoadState.Loading || entries.loadState.refresh is LoadState.Loading) {
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

            if (entries.itemCount == 0 && entries.loadState.refresh is LoadState.NotLoading) {
                item(key = "empty") {
                    Text(
                        modifier = Modifier.padding(vertical = 16.dp),
                        text = stringResource(R.string.recordings_manage_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    val moveItem = pendingMoveItem
    if (moveItem != null) {
        AlertDialog(
            onDismissRequest = {
                pendingMoveItem = null
                moveSearchQuery = ""
            },
            title = {
                Text(text = stringResource(R.string.recordings_manage_move_confirm_title))
            },
            text = {
                Column {
                    Text(text = moveItem.name)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = moveSearchQuery,
                        onValueChange = { moveSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.recordings_manage_move_target_search_label)) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.height(220.dp)) {
                        items(moveCandidates) { target ->
                            TextButton(
                                onClick = {
                                    pendingMoveItem = null
                                    moveSearchQuery = ""
                                    onMoveEntry(moveItem, target)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = target.displayPath,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingMoveItem = null
                    moveSearchQuery = ""
                }) {
                    Text(text = stringResource(R.string.recordings_delete_cancel_button))
                }
            }
        )
    }
}
