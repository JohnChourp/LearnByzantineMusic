package com.johnchourp.learnbyzantinemusic.recordings.ui

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.recordings.ManagerListItem
import com.johnchourp.learnbyzantinemusic.recordings.MoveTargetFolder
import com.johnchourp.learnbyzantinemusic.recordings.RecordingsManagerUiState
import com.johnchourp.learnbyzantinemusic.recordings.RecordingSortOption
import com.johnchourp.learnbyzantinemusic.recordings.RecordingTypeFilter
import com.johnchourp.learnbyzantinemusic.recordings.ui.components.ManagerListItemRow
import com.johnchourp.learnbyzantinemusic.ui.components.LessonCard
import com.johnchourp.learnbyzantinemusic.ui.components.LessonChip
import com.johnchourp.learnbyzantinemusic.ui.components.LessonHero
import com.johnchourp.learnbyzantinemusic.ui.components.StaggeredAppear
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGoldContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGoldContent
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPageBg
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurface
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurfaceVariant
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary

/**
 * Redesigned «Διαχείριση ηχογραφήσεων» screen. A pure renderer of [RecordingsManagerUiState] +
 * callbacks built on the shared design system (hero, toolbar/search [LessonCard]s, redesigned rows,
 * a branded move-target picker). All folder navigation, search debounce, paging, move-candidate
 * filtering and dialog outcomes stay in the host Activity/ViewModel — this only changes rendering.
 */
@Composable
fun RecordingsManagerScreen(
    uiState: RecordingsManagerUiState,
    entries: LazyPagingItems<ManagerListItem>,
    onBack: () -> Unit,
    onNavigateUp: () -> Unit,
    onCreateFolder: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onSortChanged: (RecordingSortOption) -> Unit,
    onFilterChanged: (RecordingTypeFilter) -> Unit,
    onOpenEntry: (ManagerListItem) -> Unit,
    onRenameEntry: (ManagerListItem) -> Unit,
    onDeleteEntry: (ManagerListItem) -> Unit,
    onMoveEntry: (ManagerListItem, MoveTargetFolder) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sortExpanded by remember { mutableStateOf(false) }
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

    val showEmptyState = entries.itemCount == 0 && entries.loadState.refresh is LoadState.NotLoading
    val showLoading = entries.loadState.append is LoadState.Loading || entries.loadState.refresh is LoadState.Loading

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LbmPageBg),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "hero") {
            Column {
                LessonHero(
                    title = stringResource(R.string.recordings_manage_open),
                    subtitle = stringResource(R.string.recordings_manage_subtitle),
                    onBack = onBack,
                    icon = Icons.Filled.FolderOpen,
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

        item(key = "toolbar") {
            StaggeredAppear(delayMillis = 60, modifier = Modifier.padding(horizontal = 16.dp)) {
                ToolbarCard(
                    rootFolderName = uiState.rootFolderName,
                    breadcrumbPathDisplay = uiState.breadcrumbPathDisplay,
                    statusMessage = uiState.statusMessage,
                    canNavigateUp = uiState.currentRelativePath.isNotBlank(),
                    onNavigateUp = onNavigateUp,
                    onCreateFolder = onCreateFolder,
                )
            }
        }

        item(key = "filters") {
            StaggeredAppear(delayMillis = 120, modifier = Modifier.padding(horizontal = 16.dp)) {
                SearchFilterCard(
                    searchQuery = uiState.searchQuery,
                    selectedFilter = uiState.selectedFilter,
                    selectedSortOption = uiState.selectedSortOption,
                    sortExpanded = sortExpanded,
                    onSortExpandedChange = { sortExpanded = it },
                    onSearchChanged = onSearchChanged,
                    onSortChanged = onSortChanged,
                    onFilterChanged = onFilterChanged,
                )
            }
        }

        items(
            count = entries.itemCount,
            key = { index -> entries[index]?.uri?.toString() ?: "placeholder_$index" },
        ) { index ->
            val item = entries[index] ?: return@items
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                ManagerListItemRow(
                    item = item,
                    onOpen = onOpenEntry,
                    onRename = onRenameEntry,
                    onMove = { pendingMoveItem = it },
                    onDelete = onDeleteEntry,
                )
            }
        }

        if (showLoading) {
            item(key = "loading") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp, color = LbmBrown)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.recordings_manage_status_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LbmTextSecondary,
                    )
                }
            }
        }

        if (showEmptyState) {
            item(key = "empty") {
                ManagerEmptyState(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }

    val moveItem = pendingMoveItem
    if (moveItem != null) {
        MoveTargetDialog(
            sourceName = moveItem.name,
            moveSearchQuery = moveSearchQuery,
            candidates = moveCandidates,
            onSearchChange = { moveSearchQuery = it },
            onPickTarget = { target ->
                pendingMoveItem = null
                moveSearchQuery = ""
                onMoveEntry(moveItem, target)
            },
            onDismiss = {
                pendingMoveItem = null
                moveSearchQuery = ""
            },
        )
    }
}

@Composable
private fun ToolbarCard(
    rootFolderName: String,
    breadcrumbPathDisplay: String,
    statusMessage: String,
    canNavigateUp: Boolean,
    onNavigateUp: () -> Unit,
    onCreateFolder: () -> Unit,
) {
    LessonCard(title = stringResource(R.string.recordings_section_folder)) {
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
                Icon(imageVector = Icons.Filled.Folder, contentDescription = null, tint = LbmBrown)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(
                        R.string.recordings_manage_breadcrumb_template,
                        rootFolderName,
                        breadcrumbPathDisplay,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = LbmTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (statusMessage.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = LbmTextSecondary,
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onNavigateUp,
                enabled = canNavigateUp,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LbmBrown),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.recordings_manage_back), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            FilledTonalButton(
                onClick = onCreateFolder,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = AccentGoldContainer,
                    contentColor = AccentGoldContent,
                ),
            ) {
                Icon(Icons.Filled.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.recordings_manage_create_folder), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun SearchFilterCard(
    searchQuery: String,
    selectedFilter: RecordingTypeFilter,
    selectedSortOption: RecordingSortOption,
    sortExpanded: Boolean,
    onSortExpandedChange: (Boolean) -> Unit,
    onSearchChanged: (String) -> Unit,
    onSortChanged: (RecordingSortOption) -> Unit,
    onFilterChanged: (RecordingTypeFilter) -> Unit,
) {
    LessonCard(title = stringResource(R.string.recordings_search_label)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.recordings_search_label)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChanged("") }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.recordings_search_clear_content_description),
                        )
                    }
                }
            },
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RecordingTypeFilter.entries.forEach { filter ->
                LessonChip(
                    label = stringResource(filter.labelResId),
                    selected = filter == selectedFilter,
                    onClick = { onFilterChanged(filter) },
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Box {
            TextButton(onClick = { onSortExpandedChange(true) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = LbmBrown, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.recordings_sort_label) + ": " + stringResource(selectedSortOption.labelResId),
                    color = LbmTextPrimary,
                )
            }
            DropdownMenu(expanded = sortExpanded, onDismissRequest = { onSortExpandedChange(false) }) {
                RecordingSortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option.labelResId)) },
                        onClick = {
                            onSortExpandedChange(false)
                            onSortChanged(option)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ManagerEmptyState(modifier: Modifier = Modifier) {
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
                imageVector = Icons.Filled.FolderOpen,
                contentDescription = null,
                tint = AccentGoldContent,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.recordings_manage_empty),
            style = MaterialTheme.typography.titleMedium,
            color = LbmTextPrimary,
        )
    }
}

@Composable
private fun MoveTargetDialog(
    sourceName: String,
    moveSearchQuery: String,
    candidates: List<MoveTargetFolder>,
    onSearchChange: (String) -> Unit,
    onPickTarget: (MoveTargetFolder) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LbmSurface,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = stringResource(R.string.recordings_manage_move_confirm_title),
                color = LbmTextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                Surface(shape = RoundedCornerShape(12.dp), color = LbmSurfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = sourceName,
                        style = MaterialTheme.typography.titleMedium,
                        color = LbmTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = moveSearchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.recordings_manage_move_target_search_label)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.height(220.dp)) {
                    items(candidates, key = { it.uri.toString() }) { target ->
                        Surface(
                            color = LbmSurface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            onClick = { onPickTarget(target) },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Folder, contentDescription = null, tint = LbmBrown, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = target.displayPath,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LbmTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.recordings_delete_cancel_button), color = LbmBrown)
            }
        },
    )
}
