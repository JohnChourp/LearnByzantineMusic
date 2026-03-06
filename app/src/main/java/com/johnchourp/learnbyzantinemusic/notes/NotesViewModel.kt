package com.johnchourp.learnbyzantinemusic.notes

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
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
    private var lastPersistedTitle: String = ""
    private var lastPersistedBody: String = ""

    init {
        viewModelScope.launch {
            searchQueryFlow
                .flatMapLatest { search -> repository.observeNotes(search) }
                .collectLatest { notes ->
                    _uiState.update { current ->
                        val selectedId = when {
                            current.selectedNoteId != null && notes.any { it.id == current.selectedNoteId } -> current.selectedNoteId
                            notes.isNotEmpty() -> notes.first().id
                            else -> null
                        }
                        val selectedNote = notes.firstOrNull { it.id == selectedId }
                        lastPersistedTitle = selectedNote?.title.orEmpty()
                        lastPersistedBody = selectedNote?.body.orEmpty()
                        current.copy(
                            notes = notes,
                            selectedNoteId = selectedId,
                            editorTitle = selectedNote?.title.orEmpty(),
                            editorBody = selectedNote?.body.orEmpty()
                        )
                    }
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
        _uiState.update { it.copy(searchQuery = value) }
        searchQueryFlow.value = value
    }

    fun onSelectNote(noteId: String) {
        autoSaveNow(SaveTrigger.AUTO)
        val selected = _uiState.value.notes.firstOrNull { it.id == noteId } ?: return
        lastPersistedTitle = selected.title
        lastPersistedBody = selected.body
        _uiState.update {
            it.copy(
                selectedNoteId = selected.id,
                editorTitle = selected.title,
                editorBody = selected.body
            )
        }
    }

    fun onEditorTitleChanged(value: String) {
        _uiState.update { it.copy(editorTitle = value) }
        scheduleAutoSave()
    }

    fun onEditorBodyChanged(value: String) {
        _uiState.update { it.copy(editorBody = value) }
        scheduleAutoSave()
    }

    fun createNewNote() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = repository.createNote()
            applyMutationResult(result)
            _uiState.update { it.copy(isSaving = false, statusMessage = resolveMessageKey(result.message)) }
        }
    }

    fun deleteSelectedNote() {
        val selectedId = _uiState.value.selectedNoteId ?: return
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

    fun exportNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = repository.exportNow()
            applyMutationResult(result)
            _uiState.update { it.copy(isSaving = false, statusMessage = resolveMessageKey(result.message)) }
        }
    }

    fun importReplace(snapshotUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = repository.importReplace(snapshotUri)
            applyMutationResult(result)
            _uiState.update { it.copy(isSaving = false, statusMessage = resolveMessageKey(result.message)) }
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

    private fun autoSaveNow(trigger: SaveTrigger) {
        val currentState = _uiState.value
        val selectedId = currentState.selectedNoteId ?: return
        if (!currentState.canInteractWithNotes) {
            return
        }
        if (currentState.editorTitle == lastPersistedTitle && currentState.editorBody == lastPersistedBody) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = trigger == SaveTrigger.MANUAL) }
            val result = repository.saveNote(
                noteId = selectedId,
                title = currentState.editorTitle,
                body = currentState.editorBody
            )
            applyMutationResult(result)
            lastPersistedTitle = currentState.editorTitle.trim()
            lastPersistedBody = currentState.editorBody.trimEnd()
            _uiState.update {
                it.copy(
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
            "import_read_failed" -> "notes_status_import_read_failed"
            else -> "notes_status_saved"
        }
    }

    companion object {
        private const val AUTO_SAVE_DELAY_MS = 1200L
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
