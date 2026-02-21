package com.johnchourp.learnbyzantinemusic.recordings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.johnchourp.learnbyzantinemusic.recordings.index.RecordingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecordingsManagerViewModel(
    private val repository: RecordingsRepository
) : ViewModel() {
    private val rootUriFlow = MutableStateFlow<Uri?>(null)
    private val currentFolderUriFlow = MutableStateFlow<Uri?>(null)
    private val currentRelativePathFlow = MutableStateFlow("")
    private val searchQueryFlow = MutableStateFlow("")
    private val sortOptionFlow = MutableStateFlow(RecordingSortOption.default)
    private val filterFlow = MutableStateFlow(RecordingTypeFilter.default)
    private val indexVersionFlow = MutableStateFlow(0)

    private val _uiState = MutableStateFlow(RecordingsManagerUiState())
    val uiState: StateFlow<RecordingsManagerUiState> = _uiState.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val managerQueryFlow = combine(
        rootUriFlow,
        currentFolderUriFlow,
        searchQueryFlow.debounce(250),
        sortOptionFlow,
        filterFlow
    ) { rootUri, parentUri, searchQuery, sortOption, typeFilter ->
        ManagerQuery(
            rootUri = rootUri,
            parentUri = parentUri,
            searchQuery = searchQuery,
            sortOption = sortOption,
            typeFilter = typeFilter
        )
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val entriesFlow: Flow<PagingData<ManagerListItem>> = combine(
        managerQueryFlow,
        indexVersionFlow
    ) { query, _ ->
        query
    }.flatMapLatest { query ->
        val rootUri = query.rootUri ?: return@flatMapLatest flowOf(PagingData.empty())
        val parentUri = query.parentUri ?: return@flatMapLatest flowOf(PagingData.empty())
        repository.observeFolderEntries(
            rootUri = rootUri,
            parentUri = parentUri,
            query = query.searchQuery,
            sortOption = query.sortOption,
            filter = query.typeFilter
        )
    }.cachedIn(viewModelScope)

    private var isReindexing = false
    private var lastReindexTriggerAt: Long = 0L

    fun setRootFolder(rootUri: Uri, rootName: String) {
        rootUriFlow.value = rootUri
        currentFolderUriFlow.value = repository.normalizeDirectoryUri(rootUri)
        currentRelativePathFlow.value = ""
        _uiState.update { state ->
            state.copy(
                rootFolderName = rootName,
                currentRelativePath = "",
                statusMessage = ""
            )
        }
        refreshMoveTargets()
        requestReindex(force = true)
    }

    fun setSearchQuery(value: String) {
        searchQueryFlow.value = value
        _uiState.update { state -> state.copy(searchQuery = value) }
    }

    fun setSortOption(value: RecordingSortOption) {
        sortOptionFlow.value = value
        _uiState.update { state -> state.copy(selectedSortOption = value) }
    }

    fun setFilter(value: RecordingTypeFilter) {
        filterFlow.value = value
        _uiState.update { state -> state.copy(selectedFilter = value) }
    }

    fun enterFolder(item: ManagerListItem) {
        if (item.type != ManagerEntryType.FOLDER) {
            return
        }
        currentFolderUriFlow.value = item.uri
        currentRelativePathFlow.value = item.relativePath
        _uiState.update { state ->
            state.copy(
                currentRelativePath = item.relativePath,
                statusMessage = ""
            )
        }
    }

    fun navigateUp(onResolved: (Boolean) -> Unit) {
        val currentPath = currentRelativePathFlow.value
        if (currentPath.isBlank()) {
            onResolved(false)
            return
        }

        val rootUri = rootUriFlow.value ?: run {
            onResolved(false)
            return
        }

        val parentPath = currentPath.substringBeforeLast('/', "")
        viewModelScope.launch {
            val targetUri = if (parentPath.isBlank()) {
                repository.normalizeDirectoryUri(rootUri)
            } else {
                repository.resolveDirectoryUriByRelativePath(rootUri, parentPath)
            }

            if (targetUri == null) {
                onResolved(false)
                return@launch
            }

            currentFolderUriFlow.value = targetUri
            currentRelativePathFlow.value = parentPath
            _uiState.update { state ->
                state.copy(
                    currentRelativePath = parentPath,
                    statusMessage = ""
                )
            }
            onResolved(true)
        }
    }

    fun createFolder(rawName: String, onCompleted: (Boolean) -> Unit) {
        val parentUri = currentFolderUriFlow.value ?: run {
            onCompleted(false)
            return
        }

        viewModelScope.launch {
            val created = repository.createFolder(parentUri, rawName)
            onCompleted(created)
            if (created) {
                requestReindex(force = true)
            }
        }
    }

    fun renameItem(item: ManagerListItem, targetName: String, onCompleted: (RenameOutcome) -> Unit) {
        viewModelScope.launch {
            val result = repository.renameEntry(item, targetName)
            onCompleted(result)
            if (result == RenameOutcome.SUCCESS || result == RenameOutcome.REMOVED) {
                requestReindex(force = true)
            }
        }
    }

    fun deleteItem(item: ManagerListItem, onCompleted: (DeleteOutcome) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteEntry(item)
            onCompleted(result)
            if (result == DeleteOutcome.SUCCESS || result == DeleteOutcome.REMOVED) {
                requestReindex(force = true)
            }
        }
    }

    fun moveItem(item: ManagerListItem, target: MoveTargetFolder, onCompleted: (MoveOutcome) -> Unit) {
        viewModelScope.launch {
            val moveResult = repository.moveEntry(item, target)
            onCompleted(moveResult)
            if (moveResult == MoveOutcome.SUCCESS) {
                requestReindex(force = true)
            }
        }
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
            refreshMoveTargets()
            _uiState.update { state -> state.copy(isIndexing = false) }
            if (!result.completed) {
                _uiState.update { state -> state.copy(statusMessage = "") }
            }
            isReindexing = false
        }
    }

    private fun refreshMoveTargets() {
        val rootUri = rootUriFlow.value ?: return
        viewModelScope.launch {
            val targets = repository.observeMoveTargets(rootUri, query = "").first()
            _uiState.update { state -> state.copy(moveTargets = targets) }
        }
    }

    private data class ManagerQuery(
        val rootUri: Uri?,
        val parentUri: Uri?,
        val searchQuery: String,
        val sortOption: RecordingSortOption,
        val typeFilter: RecordingTypeFilter
    )

    companion object {
        private const val REINDEX_THROTTLE_MS = 4_000L
    }
}

class RecordingsManagerViewModelFactory(
    private val repository: RecordingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecordingsManagerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecordingsManagerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
