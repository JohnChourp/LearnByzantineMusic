package com.johnchourp.learnbyzantinemusic.notes

/**
 * Keeps the notes editor and the database in step without either overwriting the other. Pure — no
 * Android, no coroutines — so every rule here runs in plain JVM tests: [NotesViewModel] only feeds it
 * events (a list from the database, a keystroke, a save starting or finishing) and runs the saves it
 * asks for.
 *
 * **Why it exists.** The screen used to reload the editor from the database on every list Room sent
 * back, and saves trimmed the text. So the echo of each autosave replaced what was on screen: a
 * trailing space or new line vanished ~1.2 s after it was typed, and letters typed while a save was
 * running were replaced by the older text being saved. «Νέα σημείωση» left the old note open, because
 * the new note's id was never known and the open note was kept for as long as it existed.
 *
 * **Rules.**
 * - A save stores exactly what was typed, title and body — no trimming ([noteToStore]). Where
 *   emptiness matters (the list's «untitled» label) the screen checks `isBlank()`.
 * - The editor is loaded from the database only when (a) another note is opened, (b) the open note
 *   leaves the list, or (c) a replace-all import resets it ([onImportFinished]). Any other list,
 *   including the echo of the editor's own save, refreshes the list and leaves the editor alone.
 * - A created note opens as soon as the list containing it has arrived ([onNoteCreated]).
 * - Every save also writes a backup file, so a save is asked for only when the editor differs from
 *   what the database holds or is about to hold ([saveRequest]) — never twice for the same text.
 * - Before the editor can lose the open note, its unsaved text is saved: a new search may hide it
 *   ([onSearchChanged]). A note being deleted is the exception — nothing saves it again
 *   ([onDeleteRequested]).
 */
object NotesEditorSync {

    /** A list from the database — already filtered by the search — reached the screen. */
    fun onNotesChanged(state: NotesUiState, notes: List<NoteEntity>): NotesUiState {
        val listed = state.copy(notes = notes)
        val created = notes.firstOrNull { it.id == state.noteToOpen }
        if (created != null) {
            return listed.load(created).copy(noteToOpen = null, editorFollowsDatabase = false)
        }
        val open = notes.firstOrNull { it.id == state.selectedNoteId }
        return when {
            // (b) The open note left the list (deleted, or hidden by the search), or none was open yet.
            open == null -> listed.load(notes.firstOrNull())
            // (c) Until the user types after an import, the editor shows what the database holds: the
            // list that reflects the import can arrive after the import was reported finished.
            state.editorFollowsDatabase -> listed.load(open)
            // Same note, so the editor keeps its text. This list may be the echo of the editor's own
            // save, older than what has been typed since — reloading it is what erased the typing.
            else -> listed
        }
    }

    /** (a) The user opened a note from the list. Tapping the note already open keeps what is typed. */
    fun open(state: NotesUiState, noteId: String): NotesUiState {
        val chosen = state.copy(noteToOpen = null, editorFollowsDatabase = false)
        if (noteId == state.selectedNoteId) {
            return chosen
        }
        val note = state.notes.firstOrNull { it.id == noteId } ?: return state
        return chosen.load(note)
    }

    /**
     * «Νέα σημείωση» created [noteId]. It opens now if the list containing it has already arrived,
     * otherwise with that list ([onNotesChanged]) — never earlier, so the open note always has a row.
     */
    fun onNoteCreated(state: NotesUiState, noteId: String): NotesUiState =
        if (state.notes.any { it.id == noteId }) open(state, noteId) else state.copy(noteToOpen = noteId)

    /**
     * (c) A replace-all import finished: reload the editor from the database — the open note if it is
     * still there, else the first — and keep following the database until the user types. Kept as it
     * was, the editor would show the text from before the import, and the next autosave would write
     * that back over the import.
     */
    fun onImportFinished(state: NotesUiState): NotesUiState {
        val note = state.notes.firstOrNull { it.id == state.selectedNoteId } ?: state.notes.firstOrNull()
        return state.load(note).copy(noteToOpen = null, editorFollowsDatabase = true)
    }

    /** A state change, and the save it asks the ViewModel to run, if any. */
    data class Step(val state: NotesUiState, val save: NoteSaveRequest? = null)

    /**
     * The open note's unsaved text, handed to a save now and recorded as running — or no save, when
     * the database holds that text already or a running save is writing it ([saveRequest]).
     */
    fun saveOpenNote(state: NotesUiState): Step {
        val request = saveRequest(state) ?: return Step(state)
        return Step(onSaveStarted(state, request), request)
    }

    /**
     * A new search. It can hide the open note, which then leaves the list (b) and the editor with it,
     * and its pending autosave, running later, would find another note open. So the open note's
     * unsaved text is saved first: the save that autosave would have made, only sooner.
     */
    fun onSearchChanged(state: NotesUiState, query: String): Step =
        saveOpenNote(state.copy(searchQuery = query))

    /**
     * The user confirmed deleting the open note. From here on nothing saves it — not its pending
     * autosave, not leaving it, not the exit flush — because a save now would queue behind the delete
     * and write the note back. The guard lifts once the editor holds another note ([load]).
     */
    fun onDeleteRequested(state: NotesUiState): NotesUiState =
        state.copy(noteBeingDeleted = state.selectedNoteId)

    fun onTitleEdited(state: NotesUiState, title: String): NotesUiState =
        state.copy(editorTitle = title, editorFollowsDatabase = false)

    fun onBodyEdited(state: NotesUiState, body: String): NotesUiState =
        state.copy(editorBody = body, editorFollowsDatabase = false)

    /**
     * The save that autosave, «Αποθήκευση» or leaving the note should run now — or null when the
     * database already holds the editor's text, or a running save is writing it.
     */
    fun saveRequest(state: NotesUiState): NoteSaveRequest? {
        val request = state.editorRequest() ?: return null
        // What the database will hold once the running save of this note, if any, has landed.
        val expected = state.saveInFlight?.takeIf { it.noteId == request.noteId }
            ?: NoteSaveRequest(request.noteId, state.storedTitle, state.storedBody)
        return request.takeIf { it != expected }
    }

    /**
     * The save to run when the screen is left: anything not known to be stored. A save still running
     * does not count — the screen's scope is cancelled on the way out, and that save with it.
     */
    fun exitSaveRequest(state: NotesUiState): NoteSaveRequest? {
        val request = state.editorRequest() ?: return null
        val stored = request.title == state.storedTitle && request.body == state.storedBody
        val saving = state.saveInFlight?.noteId == request.noteId
        return request.takeIf { !stored || saving }
    }

    fun onSaveStarted(state: NotesUiState, request: NoteSaveRequest): NotesUiState =
        state.copy(saveInFlight = request)

    /** [request] is now what the database holds for its note. */
    fun onSaveFinished(state: NotesUiState, request: NoteSaveRequest): NotesUiState {
        // A later save may already be running; that one stays the save to wait for.
        val stillRunning = state.saveInFlight?.takeIf { it != request }
        if (request.noteId != state.selectedNoteId) {
            return state.copy(saveInFlight = stillRunning)
        }
        return state.copy(storedTitle = request.title, storedBody = request.body, saveInFlight = stillRunning)
    }

    /** The row a save writes: the title and body exactly as typed — no trimming. */
    fun noteToStore(request: NoteSaveRequest, existing: NoteEntity?, nowEpochMs: Long): NoteEntity =
        NoteEntity(
            id = request.noteId,
            title = request.title,
            body = request.body,
            createdAtEpochMs = existing?.createdAtEpochMs ?: nowEpochMs,
            updatedAtEpochMs = nowEpochMs
        )

    private fun NotesUiState.editorRequest(): NoteSaveRequest? {
        val noteId = selectedNoteId ?: return null
        if (!canInteractWithNotes || noteId == noteBeingDeleted) {
            return null
        }
        return NoteSaveRequest(noteId, editorTitle, editorBody)
    }

    /** Puts [note] in the editor (or empties it, for null) and records it as what is stored. */
    private fun NotesUiState.load(note: NoteEntity?): NotesUiState {
        // A save of this note that is still running holds newer text than the list: show that.
        val saving = saveInFlight?.takeIf { it.noteId == note?.id }
        return copy(
            selectedNoteId = note?.id,
            editorTitle = saving?.title ?: note?.title.orEmpty(),
            editorBody = saving?.body ?: note?.body.orEmpty(),
            storedTitle = note?.title.orEmpty(),
            storedBody = note?.body.orEmpty(),
            // A delete's guard ends with the note it guarded: an import can bring the same id back.
            noteBeingDeleted = noteBeingDeleted?.takeIf { it == note?.id }
        )
    }
}
