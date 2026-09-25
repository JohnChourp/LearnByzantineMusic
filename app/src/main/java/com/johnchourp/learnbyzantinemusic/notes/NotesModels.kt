package com.johnchourp.learnbyzantinemusic.notes

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

/** A new, empty note and the outcome of its backup; [noteId] is what the editor opens next. */
data class CreatedNote(
    val noteId: String,
    val result: NotesMutationResult
)

/** One save of the editor: exactly the text typed into [noteId]. */
data class NoteSaveRequest(
    val noteId: String,
    val title: String,
    val body: String
)

data class NotesUiState(
    val notes: List<NoteEntity> = emptyList(),
    val selectedNoteId: String? = null,
    val editorTitle: String = "",
    val editorBody: String = "",
    // Not rendered — NotesEditorSync's bookkeeping: what the database holds for the open note, the
    // save still running, a created note waiting for its list, an import's reset in force, and a
    // note being deleted (which nothing may save again).
    val storedTitle: String = "",
    val storedBody: String = "",
    val saveInFlight: NoteSaveRequest? = null,
    val noteToOpen: String? = null,
    val editorFollowsDatabase: Boolean = false,
    val noteBeingDeleted: String? = null,
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
