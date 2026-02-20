package com.johnchourp.learnbyzantinemusic.recordings

import android.net.Uri

data class RecentRecordingItem(
    val name: String,
    val uri: Uri,
    val mimeType: String?,
    val relativePath: String,
    val parentRelativePath: String,
    val parentUri: Uri,
    val sortTimestamp: Long
)

enum class ManagerEntryType {
    FOLDER,
    AUDIO_FILE
}

data class ManagerEntry(
    val type: ManagerEntryType,
    val name: String,
    val uri: Uri,
    val parentUri: Uri,
    val mimeType: String?,
    val relativePath: String,
    val lastModified: Long
)

data class MoveRequestPayload(
    val sourceUri: Uri,
    val sourceParentUri: Uri,
    val sourceType: ManagerEntryType,
    val sourceRelativePath: String,
    val sourceName: String
)

enum class MoveOutcome {
    SUCCESS,
    SAME_PARENT,
    BLOCKED_SELF_OR_DESCENDANT,
    FAILED
}

enum class RenameOutcome {
    SUCCESS,
    NAME_EXISTS,
    FAILED
}
