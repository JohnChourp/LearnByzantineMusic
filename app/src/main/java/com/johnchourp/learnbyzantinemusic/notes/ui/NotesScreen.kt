package com.johnchourp.learnbyzantinemusic.notes.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.notes.NoteEntity
import com.johnchourp.learnbyzantinemusic.notes.NotesUiState
import com.johnchourp.learnbyzantinemusic.ui.components.LessonCard
import com.johnchourp.learnbyzantinemusic.ui.components.LessonHero
import com.johnchourp.learnbyzantinemusic.ui.components.StaggeredAppear
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentCrimsonContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentCrimsonContent
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGoldContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGoldContent
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGreenContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGreenContent
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmOutline
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPageBg
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPrimaryContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurface
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurfaceVariant
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Redesigned «Οι Σημειώσεις Μου» screen. A pure renderer of [NotesUiState] + callbacks built on the
 * shared design system (hero, [StaggeredAppear] + [LessonCard] sections, branded palette) with a
 * phone-first single-column layout, animated status/selection feedback and clear, icon-led actions.
 * All persistence, backup and SAF logic stays in [com.johnchourp.learnbyzantinemusic.notes.NotesViewModel] /
 * the host Activity — this only changes rendering.
 */

// The editor card's fixed position in the LazyColumn (hero=0, backup=1, stats+status=2, editor=3),
// so tapping a note can smoothly scroll to it. Stays constant because no item above is conditional.
private const val EDITOR_ITEM_INDEX = 3

@Composable
fun NotesScreen(
    uiState: NotesUiState,
    statusText: String,
    onBack: () -> Unit,
    onCreateNote: () -> Unit,
    onDeleteSelected: () -> Unit,
    onSaveNow: () -> Unit,
    onExportNow: () -> Unit,
    onImport: () -> Unit,
    onSelectFolder: () -> Unit,
    onResyncPending: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSelectNote: (String) -> Unit,
    onEditorTitleChanged: (String) -> Unit,
    onEditorBodyChanged: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var scrollToEditorSignal by remember { mutableIntStateOf(0) }

    LaunchedEffect(scrollToEditorSignal) {
        if (scrollToEditorSignal > 0) {
            listState.animateScrollToItem(EDITOR_ITEM_INDEX)
        }
    }

    val enabled = uiState.canInteractWithNotes

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(LbmPageBg),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "hero") {
            Column {
                LessonHero(
                    title = stringResource(R.string.notes_page_title),
                    subtitle = stringResource(R.string.notes_page_subtitle),
                    onBack = onBack,
                    icon = Icons.Filled.Description,
                )
                AnimatedVisibility(visible = uiState.isSaving) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = LbmBrown,
                        trackColor = LbmSurfaceVariant,
                    )
                }
            }
        }

        item(key = "backup") {
            StaggeredAppear(delayMillis = 60, modifier = Modifier.padding(horizontal = 16.dp)) {
                BackupSyncCard(
                    folderName = uiState.folderName,
                    hasConfiguredFolder = uiState.hasConfiguredFolder,
                    pendingSyncCount = uiState.pendingSyncCount,
                    lastSyncEpochMs = uiState.lastSyncEpochMs,
                    lastSyncError = uiState.lastSyncError,
                    enabled = enabled,
                    onSelectFolder = onSelectFolder,
                    onResyncPending = onResyncPending,
                    onExportNow = onExportNow,
                    onImport = onImport,
                )
            }
        }

        // Stats + status share one item so the editor stays at a fixed index (EDITOR_ITEM_INDEX)
        // for tap-to-scroll, and the status banner adds no phantom inter-item gap when idle.
        item(key = "stats") {
            StaggeredAppear(delayMillis = 120, modifier = Modifier.padding(horizontal = 16.dp)) {
                Column {
                    StatsStrip(
                        notesCount = uiState.notesCount,
                        pendingSyncCount = uiState.pendingSyncCount,
                        isSaving = uiState.isSaving,
                    )
                    StatusBanner(statusKey = uiState.statusMessage, statusText = statusText)
                }
            }
        }

        item(key = "editor") {
            StaggeredAppear(delayMillis = 180, modifier = Modifier.padding(horizontal = 16.dp)) {
                EditorCard(
                    enabled = enabled,
                    hasSelectedNote = uiState.selectedNoteId != null,
                    title = uiState.editorTitle,
                    body = uiState.editorBody,
                    onTitleChanged = onEditorTitleChanged,
                    onBodyChanged = onEditorBodyChanged,
                    onCreateNote = {
                        onCreateNote()
                        scrollToEditorSignal++
                    },
                    onSaveNow = onSaveNow,
                    onDeleteSelected = { showDeleteDialog = true },
                )
            }
        }

        item(key = "search") {
            StaggeredAppear(delayMillis = 220, modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(id = R.string.notes_search_label)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = LbmBrown) },
                    singleLine = true,
                )
            }
        }

        item(key = "list-header") {
            StaggeredAppear(delayMillis = 260, modifier = Modifier.padding(horizontal = 16.dp)) {
                SectionHeader(
                    icon = Icons.Filled.Description,
                    title = stringResource(id = R.string.notes_list_title),
                    count = uiState.notesCount,
                )
            }
        }

        if (uiState.notes.isEmpty()) {
            item(key = "list-empty") {
                NotesEmptyState(
                    isSearching = uiState.searchQuery.isNotBlank(),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        } else {
            // Prefix note keys so a note id from an imported backup can never collide with the
            // static section keys ("hero", "editor", …) sharing this LazyColumn.
            items(uiState.notes, key = { "note:${it.id}" }) { note ->
                NoteRow(
                    note = note,
                    selected = uiState.selectedNoteId == note.id,
                    onClick = {
                        onSelectNote(note.id)
                        scrollToEditorSignal++
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = AccentCrimsonContent) },
            title = { Text(text = stringResource(id = R.string.notes_delete_confirm_title)) },
            text = { Text(text = stringResource(id = R.string.notes_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteSelected()
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.notes_delete_confirm_action),
                        color = AccentCrimsonContent,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = stringResource(id = R.string.notes_delete_cancel_action))
                }
            }
        )
    }
}

@Composable
private fun BackupSyncCard(
    folderName: String?,
    hasConfiguredFolder: Boolean,
    pendingSyncCount: Int,
    lastSyncEpochMs: Long?,
    lastSyncError: String?,
    enabled: Boolean,
    onSelectFolder: () -> Unit,
    onResyncPending: () -> Unit,
    onExportNow: () -> Unit,
    onImport: () -> Unit,
) {
    LessonCard(title = stringResource(R.string.notes_section_backup_title)) {
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
                    text = folderName?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.notes_folder_not_selected),
                    style = MaterialTheme.typography.titleMedium,
                    color = LbmTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                AccessDot(ready = hasConfiguredFolder)
            }
        }

        Spacer(Modifier.height(10.dp))

        val syncLabel = when {
            !lastSyncError.isNullOrBlank() -> stringResource(R.string.notes_last_sync_error_label)
            lastSyncEpochMs != null -> stringResource(
                R.string.notes_last_sync_success_template,
                formatEpoch(lastSyncEpochMs),
            )
            else -> stringResource(R.string.notes_last_sync_never)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (!lastSyncError.isNullOrBlank()) Icons.Filled.ErrorOutline else Icons.Filled.Sync,
                contentDescription = null,
                tint = if (!lastSyncError.isNullOrBlank()) AccentCrimsonContent else LbmTextSecondary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = syncLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = if (!lastSyncError.isNullOrBlank()) AccentCrimsonContent else LbmTextSecondary,
            )
        }

        AnimatedVisibility(visible = pendingSyncCount > 0) {
            Box(modifier = Modifier.padding(top = 8.dp)) {
                Surface(shape = RoundedCornerShape(10.dp), color = AccentGoldContainer) {
                    Text(
                        text = stringResource(R.string.notes_pending_sync_template, pendingSyncCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentGoldContent,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        IconActionButton(
            icon = Icons.Filled.Folder,
            label = stringResource(R.string.notes_action_select_folder),
            onClick = onSelectFolder,
            variant = ButtonVariant.PRIMARY,
        )
        Spacer(Modifier.height(8.dp))
        IconActionButton(
            icon = Icons.Filled.Sync,
            label = stringResource(R.string.notes_action_resync_pending),
            onClick = onResyncPending,
            enabled = enabled,
            variant = ButtonVariant.TONAL,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.notes_section_backup_actions_title),
            style = MaterialTheme.typography.labelLarge,
            color = LbmTextSecondary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconActionButton(
                icon = Icons.Filled.Upload,
                label = stringResource(R.string.notes_action_export_now),
                onClick = onExportNow,
                enabled = enabled,
                variant = ButtonVariant.OUTLINED,
                modifier = Modifier.weight(1f),
            )
            IconActionButton(
                icon = Icons.Filled.Download,
                label = stringResource(R.string.notes_action_import_backup),
                onClick = onImport,
                enabled = enabled,
                variant = ButtonVariant.OUTLINED,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatsStrip(notesCount: Int, pendingSyncCount: Int, isSaving: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.notes_stats_total_title),
            container = LbmPrimaryContainer,
            accent = LbmBrown,
        ) {
            StatValueText(notesCount.toString())
        }
        StatCard(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.notes_stats_pending_title),
            container = AccentGoldContainer,
            accent = AccentGoldContent,
        ) {
            StatValueText(pendingSyncCount.toString())
        }
        StatCard(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.notes_stats_save_mode_title),
            container = AccentGreenContainer,
            accent = AccentGreenContent,
        ) {
            AnimatedContent(
                targetState = isSaving,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label = "saveState",
            ) { saving ->
                Text(
                    text = stringResource(
                        if (saving) R.string.notes_stats_save_mode_saving else R.string.notes_stats_save_mode_idle,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = LbmTextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    container: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    value: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = container,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            value()
        }
    }
}

@Composable
private fun StatValueText(value: String) {
    Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        color = LbmTextPrimary,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun StatusBanner(statusKey: String, statusText: String) {
    AnimatedVisibility(
        visible = statusText.isNotBlank(),
        enter = fadeIn(tween(220)) + expandVertically(tween(220)),
        exit = fadeOut(tween(160)) + shrinkVertically(tween(160)),
    ) {
        val visual = statusVisual(statusKey)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = visual.container,
            // Leading top space lives inside the animated content so it collapses with the banner
            // when idle (no phantom gap below the stats strip).
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = visual.icon, contentDescription = null, tint = visual.content)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = visual.content,
                )
            }
        }
    }
}

private data class StatusVisual(val container: Color, val content: Color, val icon: ImageVector)

private fun statusVisual(statusKey: String): StatusVisual = when (statusKey) {
    "notes_status_resync_failed",
    "notes_status_sync_failed_local_saved",
    "notes_status_import_invalid_json",
    "notes_status_import_read_failed",
    "notes_status_folder_permission_error",
    -> StatusVisual(AccentCrimsonContainer, AccentCrimsonContent, Icons.Filled.ErrorOutline)

    "notes_status_resync_nothing",
    "notes_status_sync_partial_pending",
    -> StatusVisual(AccentGoldContainer, AccentGoldContent, Icons.Filled.Info)

    else -> StatusVisual(AccentGreenContainer, AccentGreenContent, Icons.Filled.CheckCircle)
}

@Composable
private fun EditorCard(
    enabled: Boolean,
    hasSelectedNote: Boolean,
    title: String,
    body: String,
    onTitleChanged: (String) -> Unit,
    onBodyChanged: (String) -> Unit,
    onCreateNote: () -> Unit,
    onSaveNow: () -> Unit,
    onDeleteSelected: () -> Unit,
) {
    LessonCard(title = stringResource(R.string.notes_editor_title)) {
        Button(
            onClick = onCreateNote,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LbmBrown, contentColor = Color.White),
        ) {
            ButtonContent(Icons.Filled.Add, stringResource(R.string.notes_action_new_note))
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = title,
            onValueChange = onTitleChanged,
            enabled = enabled,
            label = { Text(text = stringResource(id = R.string.notes_editor_title_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = body,
            onValueChange = onBodyChanged,
            enabled = enabled,
            label = { Text(text = stringResource(id = R.string.notes_editor_body_label)) },
            modifier = Modifier.fillMaxWidth(),
            // Bounded so a long note scrolls inside the field instead of expanding the whole
            // LazyColumn (and pushing the search/list far below the fold).
            minLines = 8,
            maxLines = 14,
        )

        AnimatedVisibility(visible = enabled && !hasSelectedNote) {
            EditorHint(
                text = stringResource(R.string.notes_editor_no_note_hint),
                color = LbmTextSecondary,
            )
        }
        AnimatedVisibility(visible = !enabled) {
            EditorHint(
                text = stringResource(R.string.notes_folder_required_hint),
                color = AccentCrimsonContent,
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconActionButton(
                icon = Icons.Filled.Save,
                label = stringResource(R.string.notes_action_save_now),
                onClick = onSaveNow,
                enabled = enabled && hasSelectedNote,
                variant = ButtonVariant.TONAL,
                modifier = Modifier.weight(1f),
            )
            IconActionButton(
                icon = Icons.Filled.DeleteOutline,
                label = stringResource(R.string.notes_action_delete_note),
                onClick = onDeleteSelected,
                enabled = enabled && hasSelectedNote,
                variant = ButtonVariant.DANGER,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EditorHint(text: String, color: Color) {
    Row(
        modifier = Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = LbmBrown)
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = LbmTextPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Surface(shape = CircleShape, color = LbmPrimaryContainer) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = LbmBrown,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun NoteRow(
    note: NoteEntity,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container by animateColorAsState(
        targetValue = if (selected) LbmPrimaryContainer else LbmSurface,
        label = "noteRowBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) LbmBrown else LbmOutline,
        label = "noteRowBorder",
    )
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = container,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (selected) 56.dp else 0.dp)
                    .background(borderColor),
            )
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = note.title.ifBlank { stringResource(id = R.string.notes_untitled_note) },
                    style = MaterialTheme.typography.titleSmall,
                    color = LbmTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = note.body.ifBlank { stringResource(id = R.string.notes_body_placeholder_short) },
                    style = MaterialTheme.typography.bodySmall,
                    color = LbmTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        id = R.string.notes_updated_at_template,
                        formatEpoch(note.updatedAtEpochMs),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = LbmBrown,
                )
            }
        }
    }
}

@Composable
private fun NotesEmptyState(isSearching: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(LbmPrimaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isSearching) Icons.Filled.Search else Icons.Filled.Add,
                contentDescription = null,
                tint = LbmBrown,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(
                if (isSearching) R.string.notes_list_no_results else R.string.notes_list_empty_title,
            ),
            style = MaterialTheme.typography.titleSmall,
            color = LbmTextPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        if (!isSearching) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.notes_list_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AccessDot(ready: Boolean) {
    val dotCd = stringResource(
        if (ready) R.string.notes_folder_status_ready else R.string.notes_folder_not_selected,
    )
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(if (ready) AccentGreenContent else AccentCrimsonContent)
            .semantics { contentDescription = dotCd },
    )
}

private enum class ButtonVariant { PRIMARY, TONAL, OUTLINED, DANGER }

@Composable
private fun IconActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    variant: ButtonVariant,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val buttonModifier = modifier
        .fillMaxWidth()
        .height(50.dp)
    when (variant) {
        ButtonVariant.PRIMARY -> Button(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier,
            colors = ButtonDefaults.buttonColors(containerColor = LbmBrown, contentColor = Color.White),
        ) { ButtonContent(icon, label) }

        ButtonVariant.TONAL -> FilledTonalButton(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier,
            colors = filledTonalColors(),
        ) { ButtonContent(icon, label) }

        ButtonVariant.OUTLINED -> OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = LbmBrown),
        ) { ButtonContent(icon, label) }

        ButtonVariant.DANGER -> OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCrimsonContent),
        ) { ButtonContent(icon, label) }
    }
}

@Composable
private fun filledTonalColors(): ButtonColors = ButtonDefaults.filledTonalButtonColors(
    containerColor = LbmSurfaceVariant,
    contentColor = LbmTextPrimary,
)

@Composable
private fun ButtonContent(icon: ImageVector, label: String) {
    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    Text(text = label, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

private fun formatEpoch(epochMs: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(epochMs))
}
