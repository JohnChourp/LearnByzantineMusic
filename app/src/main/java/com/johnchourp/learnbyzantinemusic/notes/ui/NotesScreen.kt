package com.johnchourp.learnbyzantinemusic.notes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.notes.NoteEntity
import com.johnchourp.learnbyzantinemusic.notes.NotesUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesScreen(
    uiState: NotesUiState,
    statusText: String,
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF4F7FF),
                        Color(0xFFFDF7EE)
                    )
                )
            )
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HeroHeader(
                folderName = uiState.folderName,
                pendingSyncCount = uiState.pendingSyncCount,
                lastSyncEpochMs = uiState.lastSyncEpochMs,
                lastSyncError = uiState.lastSyncError
            )

            Spacer(modifier = Modifier.height(10.dp))

            StatsStrip(
                notesCount = uiState.notesCount,
                pendingSyncCount = uiState.pendingSyncCount,
                isSaving = uiState.isSaving
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (!statusText.isBlank()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            ActionBar(
                enabled = uiState.canInteractWithNotes,
                onCreateNote = onCreateNote,
                onDeleteSelected = { showDeleteDialog = true },
                onSaveNow = onSaveNow,
                onExportNow = onExportNow,
                onImport = onImport,
                onSelectFolder = onSelectFolder,
                onResyncPending = onResyncPending
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(id = R.string.notes_search_label)) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NotesList(
                    notes = uiState.notes,
                    selectedNoteId = uiState.selectedNoteId,
                    onSelectNote = onSelectNote,
                    modifier = Modifier.weight(1f)
                )

                NotesEditor(
                    enabled = uiState.canInteractWithNotes,
                    title = uiState.editorTitle,
                    body = uiState.editorBody,
                    onTitleChanged = onEditorTitleChanged,
                    onBodyChanged = onEditorBodyChanged,
                    modifier = Modifier.weight(1.2f)
                )
            }
        }

        if (uiState.isSaving) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.BottomEnd))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = stringResource(id = R.string.notes_delete_confirm_title)) },
            text = { Text(text = stringResource(id = R.string.notes_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteSelected()
                    }
                ) {
                    Text(text = stringResource(id = R.string.notes_delete_confirm_action))
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
private fun HeroHeader(
    folderName: String?,
    pendingSyncCount: Int,
    lastSyncEpochMs: Long?,
    lastSyncError: String?
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F4B6E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.notes_page_title),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.notes_page_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE8F0F6)
            )

            Spacer(modifier = Modifier.height(12.dp))

            val folderLabel = folderName?.takeIf { it.isNotBlank() }
                ?: stringResource(id = R.string.notes_folder_not_selected)
            Text(
                text = stringResource(id = R.string.notes_folder_selected_template, folderLabel),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )

            val syncLabel = when {
                !lastSyncError.isNullOrBlank() -> stringResource(id = R.string.notes_last_sync_error_label)
                lastSyncEpochMs != null -> stringResource(
                    id = R.string.notes_last_sync_success_template,
                    formatEpoch(lastSyncEpochMs)
                )
                else -> stringResource(id = R.string.notes_last_sync_never)
            }
            Text(
                text = syncLabel,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFD8E8F2)
            )

            if (pendingSyncCount > 0) {
                Text(
                    text = stringResource(id = R.string.notes_pending_sync_template, pendingSyncCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFE7B5)
                )
            }
        }
    }
}

@Composable
private fun StatsStrip(notesCount: Int, pendingSyncCount: Int, isSaving: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            title = stringResource(id = R.string.notes_stats_total_title),
            value = notesCount.toString(),
            accent = Color(0xFF385A7E)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = stringResource(id = R.string.notes_stats_pending_title),
            value = pendingSyncCount.toString(),
            accent = Color(0xFF8D5D2C)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = stringResource(id = R.string.notes_stats_save_mode_title),
            value = if (isSaving) {
                stringResource(id = R.string.notes_stats_save_mode_saving)
            } else {
                stringResource(id = R.string.notes_stats_save_mode_idle)
            },
            accent = Color(0xFF2D7D58)
        )
    }
}

@Composable
private fun StatCard(modifier: Modifier, title: String, value: String, accent: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = accent
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ActionBar(
    enabled: Boolean,
    onCreateNote: () -> Unit,
    onDeleteSelected: () -> Unit,
    onSaveNow: () -> Unit,
    onExportNow: () -> Unit,
    onImport: () -> Unit,
    onSelectFolder: () -> Unit,
    onResyncPending: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onCreateNote, enabled = enabled, modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(id = R.string.notes_action_new_note))
                }
                Button(onClick = onDeleteSelected, enabled = enabled, modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(id = R.string.notes_action_delete_note))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onSaveNow, enabled = enabled, modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(id = R.string.notes_action_save_now))
                }
                Button(onClick = onExportNow, enabled = enabled, modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(id = R.string.notes_action_export_now))
                }
                Button(onClick = onImport, enabled = enabled, modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(id = R.string.notes_action_import_backup))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onSelectFolder, modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(id = R.string.notes_action_select_folder))
                }
                Button(onClick = onResyncPending, enabled = enabled, modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(id = R.string.notes_action_resync_pending))
                }
            }
        }
    }
}

@Composable
private fun NotesList(
    notes: List<NoteEntity>,
    selectedNoteId: String?,
    onSelectNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = stringResource(id = R.string.notes_list_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (notes.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.notes_list_empty),
                    style = MaterialTheme.typography.bodyMedium
                )
                return@Column
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(notes, key = { it.id }) { note ->
                    val isSelected = selectedNoteId == note.id
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFE8F2FF) else Color(0xFFF8F9FD)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelectNote(note.id) }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = note.title.ifBlank { stringResource(id = R.string.notes_untitled_note) },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = note.body.ifBlank { stringResource(id = R.string.notes_body_placeholder_short) },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(
                                    id = R.string.notes_updated_at_template,
                                    formatEpoch(note.updatedAtEpochMs)
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4A5D73)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesEditor(
    enabled: Boolean,
    title: String,
    body: String,
    onTitleChanged: (String) -> Unit,
    onBodyChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = stringResource(id = R.string.notes_editor_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChanged,
                enabled = enabled,
                label = { Text(text = stringResource(id = R.string.notes_editor_title_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = body,
                onValueChange = onBodyChanged,
                enabled = enabled,
                label = { Text(text = stringResource(id = R.string.notes_editor_body_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                minLines = 10,
                maxLines = 20
            )

            if (!enabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(id = R.string.notes_folder_required_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8A5A24)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}

private fun formatEpoch(epochMs: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(epochMs))
}
