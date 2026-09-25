package com.johnchourp.learnbyzantinemusic.notes

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotesViewModel(
    private val repository: NotesRepository,
    private val prefs: NotesPrefs
) : ViewModel() {
    private val searchQueryFlow = MutableStateFlow("")
    private val _uiState = MutableStateFlow(
        NotesUiState(
            hasConfiguredFolder = false,
            folderName = repository.getSyncState().folderName,
            pendingSyncCount = repository.getSyncState().pendingSyncCount,
            lastSyncEpochMs = repository.getSyncState().lastSyncEpochMs,
            lastSyncError = repository.getSyncState().lastSyncError
        )
    )
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null

    init {
        viewModelScope.launch {
            // Leaving the previous notes screen may have queued a save of its last edits behind a slow
            // backup write (flushPendingEdits). Open the notes only once it has landed: the editor is not
            // reloaded for a note that stays open, so it would keep the older text and save it back.
            lastExitFlush?.takeIf { it.isActive }?.let { exitFlush ->
                _uiState.update { it.copy(isSaving = true) }
                exitFlush.join()
                _uiState.update { it.copy(isSaving = false) }
            }
            // Each list from the database is normalised once; a keystroke in the search box then only
            // filters it. Both off the main thread: a long note is not cheap to normalise.
            combine(
                repository.observeNotes().map { notes -> NotesSearch.index(notes) },
                searchQueryFlow
            ) { indexed, search -> NotesSearch.filter(indexed, search) }
                .flowOn(Dispatchers.Default)
                .collectLatest { notes ->
                    _uiState.update { NotesEditorSync.onNotesChanged(it, notes) }
                }
        }
    }

    fun refreshSyncState() {
        val syncState = repository.getSyncState()
        _uiState.update { current ->
            current.copy(
                hasConfiguredFolder = syncState.folderUri != null,
                folderName = syncState.folderName,
                pendingSyncCount = syncState.pendingSyncCount,
                lastSyncEpochMs = syncState.lastSyncEpochMs,
                lastSyncError = syncState.lastSyncError
            )
        }
    }

    fun onFolderConfigured(uri: Uri) {
        prefs.setFolderUri(uri)
        refreshSyncState()
        _uiState.update { it.copy(statusMessage = "notes_status_folder_selected") }
    }

    fun onSearchQueryChanged(value: String) {
        // The search may hide the open note: its unsaved text is saved now, not by the pending autosave.
        autoSaveJob?.cancel()
        runStep(NotesEditorSync.onSearchChanged(_uiState.value, value), SaveTrigger.AUTO)
        searchQueryFlow.value = value
    }

    fun onSelectNote(noteId: String) {
        leaveOpenNote()
        _uiState.update { NotesEditorSync.open(it, noteId) }
    }

    fun onEditorTitleChanged(value: String) {
        _uiState.update { NotesEditorSync.onTitleEdited(it, value) }
        scheduleAutoSave()
    }

    fun onEditorBodyChanged(value: String) {
        _uiState.update { NotesEditorSync.onBodyEdited(it, value) }
        scheduleAutoSave()
    }

    fun createNewNote() {
        leaveOpenNote()
        // The new note is empty, so an active search would keep it out of the list, and so out of
        // the editor too.
        if (_uiState.value.searchQuery.isNotEmpty()) {
            onSearchQueryChanged("")
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val created = repository.createNote()
            applyMutationResult(created.result)
            // Anything typed into the old note while this one was being created is saved to it.
            leaveOpenNote()
            _uiState.update {
                NotesEditorSync.onNoteCreated(it, created.noteId)
                    .copy(isSaving = false, statusMessage = resolveMessageKey(created.result.message))
            }
        }
    }

    fun deleteSelectedNote() {
        val selectedId = _uiState.value.selectedNoteId ?: return
        // No save of this note from here on: one queued behind the delete would write it back.
        autoSaveJob?.cancel()
        _uiState.update { NotesEditorSync.onDeleteRequested(it) }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = repository.deleteNote(selectedId)
            applyMutationResult(result)
            _uiState.update { it.copy(isSaving = false, statusMessage = resolveMessageKey(result.message)) }
        }
    }

    fun saveNow() {
        autoSaveNow(SaveTrigger.MANUAL)
    }

    /**
     * Persist any unsaved editor edits on a process-lifetime scope so they survive the host
     * Activity/ViewModel being torn down — e.g. the user taps Back within the auto-save debounce
     * window, which would otherwise cancel [viewModelScope] before the pending save reaches the
     * repository. Invoked only from the real back-exit path (never while a SAF picker is open, so it
     * cannot race an import). Idempotent and a no-op when nothing is dirty.
     */
    fun flushPendingEdits() {
        val request = NotesEditorSync.exitSaveRequest(_uiState.value) ?: return
        autoSaveJob?.cancel()
        // Counted as stored at once: flushScope is never cancelled, and a second call must not
        // queue the same save again.
        _uiState.update { NotesEditorSync.onSaveFinished(it, request) }
        lastExitFlush = flushScope.launch {
            repository.saveNote(request)
        }
    }

    fun exportNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = repository.exportNow()
            applyMutationResult(result)
            _uiState.update { it.copy(isSaving = false, statusMessage = resolveMessageKey(result.message)) }
        }
    }

    fun importReplace(snapshotUri: Uri) {
        // Saved before the import — a pending autosave running after it would write this text over
        // the imported notes.
        leaveOpenNote()
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = repository.importReplace(snapshotUri)
            applyMutationResult(result)
            _uiState.update {
                NotesEditorSync.onImportFinished(it)
                    .copy(isSaving = false, statusMessage = resolveMessageKey(result.message))
            }
        }
    }

    fun syncPendingNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = repository.syncPendingBackups()
            applyMutationResult(result)
            _uiState.update { it.copy(isSaving = false, statusMessage = resolveMessageKey(result.message)) }
        }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(AUTO_SAVE_DELAY_MS)
            autoSaveNow(SaveTrigger.AUTO)
        }
    }

    /**
     * Before the editor moves away from the open note: its unsaved text is saved now, not by a pending
     * autosave that would find another note open by the time it runs.
     */
    private fun leaveOpenNote() {
        autoSaveJob?.cancel()
        autoSaveNow(SaveTrigger.AUTO)
    }

    private fun autoSaveNow(trigger: SaveTrigger) {
        runStep(NotesEditorSync.saveOpenNote(_uiState.value), trigger)
    }

    /**
     * Applies [step] and runs the save it asks for, if any. The save is already recorded as running
     * in [step]: a second trigger meanwhile does not repeat it, and typing that continues is compared
     * against it and saved next.
     */
    private fun runStep(step: NotesEditorSync.Step, trigger: SaveTrigger) {
        _uiState.value = step.state
        val request = step.save ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = trigger == SaveTrigger.MANUAL) }
            val result = repository.saveNote(request)
            applyMutationResult(result)
            _uiState.update {
                NotesEditorSync.onSaveFinished(it, request).copy(
                    isSaving = false,
                    statusMessage = if (trigger == SaveTrigger.AUTO) {
                        resolveMessageKey("auto_saved")
                    } else {
                        resolveMessageKey(result.message)
                    }
                )
            }
        }
    }

    private fun applyMutationResult(result: NotesMutationResult) {
        _uiState.update {
            it.copy(
                hasConfiguredFolder = result.syncState.folderUri != null,
                folderName = result.syncState.folderName,
                pendingSyncCount = result.syncState.pendingSyncCount,
                lastSyncEpochMs = result.syncState.lastSyncEpochMs,
                lastSyncError = result.syncState.lastSyncError
            )
        }
    }

    private fun resolveMessageKey(message: String): String {
        return when (message) {
            "created" -> "notes_status_created"
            "saved" -> "notes_status_saved"
            "auto_saved" -> "notes_status_auto_saved"
            "deleted" -> "notes_status_deleted"
            "exported" -> "notes_status_exported"
            "imported" -> "notes_status_imported"
            "resync_success" -> "notes_status_resync_success"
            "resync_nothing_pending" -> "notes_status_resync_nothing"
            "resync_failed" -> "notes_status_resync_failed"
            "sync_failed_local_saved" -> "notes_status_sync_failed_local_saved"
            "sync_partial_pending" -> "notes_status_sync_partial_pending"
            "import_invalid_json" -> "notes_status_import_invalid_json"
            "import_newer_version" -> "notes_status_import_newer_version"
            "import_read_failed" -> "notes_status_import_read_failed"
            else -> "notes_status_saved"
        }
    }

    companion object {
        private const val AUTO_SAVE_DELAY_MS = 1200L

        // Process-lifetime scope for exit-time flushes; outlives any single Activity/ViewModel so a
        // save triggered on the way out (Back/background) completes instead of being cancelled.
        private val flushScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        // The latest of those flushes; the next notes screen waits for it before opening a note (init).
        @Volatile
        private var lastExitFlush: Job? = null
    }
}

class NotesViewModelFactory(
    private val repository: NotesRepository,
    private val prefs: NotesPrefs
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotesViewModel(repository, prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
