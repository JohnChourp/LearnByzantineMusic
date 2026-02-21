package com.johnchourp.learnbyzantinemusic.recordings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.johnchourp.learnbyzantinemusic.recordings.index.RecordingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecordingsViewModel(
    private val repository: RecordingsRepository,
    private val prefs: RecordingsPrefs
) : ViewModel() {

    private val rootUriFlow = MutableStateFlow<Uri?>(null)
    private val indexVersionFlow = MutableStateFlow(0)

    private val _uiState = MutableStateFlow(
        RecordingsUiState(
            selectedFormat = prefs.getSelectedFormat()
        )
    )
    val uiState: StateFlow<RecordingsUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val recentItemsFlow: Flow<PagingData<RecordingListItem>> = combine(
        rootUriFlow,
        indexVersionFlow
    ) { rootUri, _ ->
        rootUri
    }.flatMapLatest { rootUri ->
        val safeRootUri = rootUri ?: return@flatMapLatest flowOf(PagingData.empty())
        repository.observeOwnRecent(rootUri = safeRootUri, limit = RECENT_LIMIT)
    }.cachedIn(viewModelScope)

    private var isReindexing = false
    private var lastReindexTriggerAt: Long = 0L

    fun setRootFolder(rootUri: Uri, folderName: String) {
        rootUriFlow.value = rootUri
        _uiState.update { state ->
            state.copy(
                folderName = folderName,
                statusMessage = "",
                isIndexing = false
            )
        }
        requestReindex(force = true)
    }

    fun clearRootFolder(statusMessage: String) {
        rootUriFlow.value = null
        _uiState.update { state ->
            state.copy(
                folderName = null,
                statusMessage = statusMessage,
                isIndexing = false
            )
        }
    }

    fun setStatusMessage(message: String) {
        _uiState.update { state -> state.copy(statusMessage = message) }
    }

    fun setSelectedFormat(value: RecordingFormatOption) {
        prefs.setSelectedFormat(value)
        _uiState.update { state -> state.copy(selectedFormat = value) }
    }

    fun setRecordingState(value: RecordingStateUi) {
        _uiState.update { state -> state.copy(recordingState = value) }
    }

    fun requestReindex(force: Boolean = false) {
        val rootUri = rootUriFlow.value ?: return
        val now = System.currentTimeMillis()
        if (!force && (now - lastReindexTriggerAt) < REINDEX_THROTTLE_MS) {
            return
        }
        if (isReindexing) {
            return
        }

        lastReindexTriggerAt = now
        repository.enqueueReindex(rootUri)

        viewModelScope.launch {
            isReindexing = true
            _uiState.update { state ->
                state.copy(
                    isIndexing = true,
                    statusMessage = ""
                )
            }
            val result = repository.reindex(rootUri)
            indexVersionFlow.update { it + 1 }
            _uiState.update { state -> state.copy(isIndexing = false) }
            if (!result.completed) {
                _uiState.update { state -> state.copy(recordingState = RecordingStateUi.ERROR) }
            }
            isReindexing = false
        }
    }

    fun renameItem(item: RecordingListItem, targetName: String, onCompleted: (RenameOutcome) -> Unit) {
        viewModelScope.launch {
            val result = repository.renameEntry(item, targetName)
            onCompleted(result)
            if (result == RenameOutcome.SUCCESS || result == RenameOutcome.REMOVED) {
                requestReindex(force = true)
            }
        }
    }

    fun deleteItem(item: RecordingListItem, onCompleted: (DeleteOutcome) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteEntry(item)
            onCompleted(result)
            if (result == DeleteOutcome.SUCCESS || result == DeleteOutcome.REMOVED) {
                requestReindex(force = true)
            }
        }
    }

    companion object {
        private const val REINDEX_THROTTLE_MS = 4_000L
        private const val RECENT_LIMIT = 10
    }
}

class RecordingsViewModelFactory(
    private val repository: RecordingsRepository,
    private val prefs: RecordingsPrefs
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecordingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecordingsViewModel(repository, prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
