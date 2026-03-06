package com.johnchourp.learnbyzantinemusic.notes

import java.util.Locale

data class NotesSyncState(
    val folderName: String?,
    val folderUri: String?,
    val pendingSyncCount: Int,
    val lastSyncEpochMs: Long?,
    val lastSyncError: String?
)

data class NotesMutationResult(
    val syncState: NotesSyncState,
    val message: String
)

data class NotesUiState(
    val notes: List<NoteEntity> = emptyList(),
    val selectedNoteId: String? = null,
    val editorTitle: String = "",
    val editorBody: String = "",
    val searchQuery: String = "",
    val statusMessage: String = "",
    val isSaving: Boolean = false,
    val hasConfiguredFolder: Boolean = false,
    val folderName: String? = null,
    val pendingSyncCount: Int = 0,
    val lastSyncEpochMs: Long? = null,
    val lastSyncError: String? = null
) {
    val canInteractWithNotes: Boolean
        get() = hasConfiguredFolder

    val notesCount: Int
        get() = notes.size

    val selectedNote: NoteEntity?
        get() = notes.firstOrNull { it.id == selectedNoteId }
}

enum class SaveTrigger {
    AUTO,
    MANUAL
}

fun String.toNotesSearchPattern(): String {
    val normalized = trim().lowercase(Locale.getDefault())
    if (normalized.isBlank()) {
        return "%"
    }
    val escaped = normalized
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
    return "%$escaped%"
}
