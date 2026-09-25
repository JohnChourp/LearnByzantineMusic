package com.johnchourp.learnbyzantinemusic.recordings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.johnchourp.learnbyzantinemusic.recordings.index.RecordingsRepository
import com.johnchourp.learnbyzantinemusic.recordings.session.PendingState
import com.johnchourp.learnbyzantinemusic.recordings.session.RecordingSessionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecordingsViewModel(
    private val repository: RecordingsRepository,
    private val prefs: RecordingsPrefs
) : ViewModel() {

    private val rootUriFlow = MutableStateFlow<Uri?>(null)

    private val _uiState = MutableStateFlow(
        RecordingsUiState(
            selectedFormat = prefs.getSelectedFormat()
        )
    )
    val uiState: StateFlow<RecordingsUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val recentItemsFlow: Flow<PagingData<RecordingListItem>> = rootUriFlow.flatMapLatest { rootUri ->
        val safeRootUri = rootUri ?: return@flatMapLatest flowOf(PagingData.empty())
        repository.observeOwnRecent(rootUri = safeRootUri, limit = RECENT_LIMIT)
    }.cachedIn(viewModelScope)

    fun setRootFolder(rootUri: Uri, folderName: String) {
        rootUriFlow.value = rootUri
        _uiState.update { state ->
            state.copy(
                folderName = folderName,
                statusMessage = "",
                isIndexing = false
            )
        }
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

    fun setRecordingTarget(label: String?) {
        _uiState.update { state -> state.copy(targetLabel = label) }
    }

    /** Shows the process-wide recording session; a null [statusMessage] leaves the status line as it is. */
    fun applySession(session: RecordingSessionState, statusMessage: String?, targetLabel: String?) {
        _uiState.update { state ->
            state.copy(
                recordingState = session.phase,
                statusMessage = statusMessage ?: state.statusMessage,
                targetLabel = targetLabel,
                recordingElapsedBeforeMs = session.elapsedBeforeMs,
                recordingSince = session.recordingSince,
            )
        }
    }

    fun setPending(pending: PendingState) {
        _uiState.update { state ->
            state.copy(pendingRecordings = pending.recordings, pendingBusyIds = pending.busyIds)
        }
    }

    fun renameItem(item: RecordingListItem, targetName: String, onCompleted: (RenameOutcome) -> Unit) {
        viewModelScope.launch {
            val result = repository.renameEntry(item, targetName)
            onCompleted(result)
        }
    }

    fun deleteItem(item: RecordingListItem, onCompleted: (DeleteOutcome) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteEntry(item)
            onCompleted(result)
        }
    }

    companion object {
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
