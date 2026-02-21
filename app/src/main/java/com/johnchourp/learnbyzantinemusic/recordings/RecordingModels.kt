package com.johnchourp.learnbyzantinemusic.recordings

import android.net.Uri
import com.johnchourp.learnbyzantinemusic.R

data class RecordingListItem(
    val name: String,
    val uri: Uri,
    val mimeType: String?,
    val relativePath: String,
    val parentRelativePath: String,
    val parentUri: Uri,
    val createdTimestamp: Long,
    val updatedTimestamp: Long
)

enum class RecordingSortOption(val labelResId: Int) {
    NAME(R.string.recordings_sort_option_name),
    CREATED(R.string.recordings_sort_option_created),
    UPDATED(R.string.recordings_sort_option_updated);

    companion object {
        val default: RecordingSortOption = NAME
    }
}

enum class RecordingTypeFilter(val labelResId: Int) {
    ALL(R.string.recordings_filter_all),
    FOLDERS(R.string.recordings_filter_folders),
    AUDIO(R.string.recordings_filter_audio);

    companion object {
        val default: RecordingTypeFilter = ALL
    }
}

enum class ManagerEntryType {
    FOLDER,
    AUDIO_FILE
}

data class ManagerListItem(
    val type: ManagerEntryType,
    val name: String,
    val uri: Uri,
    val parentUri: Uri,
    val mimeType: String?,
    val relativePath: String,
    val createdTimestamp: Long,
    val updatedTimestamp: Long
)

data class MoveTargetFolder(
    val uri: Uri,
    val relativePath: String,
    val displayPath: String
)

enum class RecordingStateUi {
    IDLE,
    RECORDING,
    PAUSED,
    SAVING,
    ERROR
}

data class RecordingsUiState(
    val folderName: String? = null,
    val statusMessage: String = "",
    val selectedFormat: RecordingFormatOption = RecordingFormatOption.FLAC,
    val selectedSortOption: RecordingSortOption = RecordingSortOption.default,
    val selectedFilter: RecordingTypeFilter = RecordingTypeFilter.default,
    val searchQuery: String = "",
    val isIndexing: Boolean = false,
    val recordingState: RecordingStateUi = RecordingStateUi.IDLE
)

data class RecordingsManagerUiState(
    val rootFolderName: String = "",
    val currentRelativePath: String = "",
    val statusMessage: String = "",
    val selectedSortOption: RecordingSortOption = RecordingSortOption.default,
    val selectedFilter: RecordingTypeFilter = RecordingTypeFilter.default,
    val searchQuery: String = "",
    val isIndexing: Boolean = false,
    val moveTargets: List<MoveTargetFolder> = emptyList()
) {
    val breadcrumbPathDisplay: String
        get() = if (currentRelativePath.isBlank()) "/" else "/$currentRelativePath"
}

sealed interface RecordingsAction {
    data object StartRecording : RecordingsAction
    data object PauseOrResumeRecording : RecordingsAction
    data object StopRecording : RecordingsAction
    data object ChangeFolder : RecordingsAction
    data object OpenFolder : RecordingsAction
    data object OpenManager : RecordingsAction
    data class SearchChanged(val value: String) : RecordingsAction
    data class SortChanged(val value: RecordingSortOption) : RecordingsAction
    data class FilterChanged(val value: RecordingTypeFilter) : RecordingsAction
    data class FormatChanged(val value: RecordingFormatOption) : RecordingsAction
}

sealed interface ManagerAction {
    data object NavigateBack : ManagerAction
    data object CreateFolder : ManagerAction
    data class EnterFolder(val item: ManagerListItem) : ManagerAction
    data class SearchChanged(val value: String) : ManagerAction
    data class SortChanged(val value: RecordingSortOption) : ManagerAction
    data class FilterChanged(val value: RecordingTypeFilter) : ManagerAction
    data class RenameRequested(val item: ManagerListItem) : ManagerAction
    data class DeleteRequested(val item: ManagerListItem) : ManagerAction
    data class MoveRequested(
        val item: ManagerListItem,
        val target: MoveTargetFolder
    ) : ManagerAction
}

enum class MoveOutcome {
    SUCCESS,
    SAME_PARENT,
    BLOCKED_SELF_OR_DESCENDANT,
    FAILED
}

enum class RenameOutcome {
    SUCCESS,
    NAME_EXISTS,
    REMOVED,
    FAILED
}

enum class DeleteOutcome {
    SUCCESS,
    REMOVED,
    FAILED
}
