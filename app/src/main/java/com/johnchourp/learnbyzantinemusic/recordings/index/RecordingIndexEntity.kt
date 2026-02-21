package com.johnchourp.learnbyzantinemusic.recordings.index

import android.net.Uri
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.johnchourp.learnbyzantinemusic.recordings.ManagerEntryType
import com.johnchourp.learnbyzantinemusic.recordings.ManagerListItem
import com.johnchourp.learnbyzantinemusic.recordings.RecordingListItem

@Entity(
    tableName = "recordings_index",
    indices = [
        Index(value = ["rootUri"]),
        Index(value = ["rootUri", "parentUri"]),
        Index(value = ["rootUri", "relativePath"], unique = true),
        Index(value = ["rootUri", "isDirectory"]),
        Index(value = ["rootUri", "isAudio"]),
        Index(value = ["rootUri", "nameNormalized"]),
        Index(value = ["rootUri", "createdTs"]),
        Index(value = ["rootUri", "updatedTs"])
    ]
)
data class RecordingIndexEntity(
    @PrimaryKey
    val docUri: String,
    val rootUri: String,
    val parentUri: String,
    val relativePath: String,
    val name: String,
    val nameNormalized: String,
    val isDirectory: Boolean,
    val isAudio: Boolean,
    val mimeType: String?,
    val createdTs: Long,
    val updatedTs: Long,
    val fileExtension: String?
)

fun RecordingIndexEntity.toRecordingListItem(): RecordingListItem {
    val parentRelativePath = relativePath.substringBeforeLast('/', "")
    val parentPathDisplay = if (parentRelativePath.isBlank()) "/" else "/$parentRelativePath"
    return RecordingListItem(
        name = name,
        uri = Uri.parse(docUri),
        mimeType = mimeType,
        relativePath = relativePath,
        parentRelativePath = parentPathDisplay,
        parentUri = Uri.parse(parentUri),
        createdTimestamp = createdTs,
        updatedTimestamp = updatedTs
    )
}

fun RecordingIndexEntity.toManagerListItem(): ManagerListItem {
    return ManagerListItem(
        type = if (isDirectory) ManagerEntryType.FOLDER else ManagerEntryType.AUDIO_FILE,
        name = name,
        uri = Uri.parse(docUri),
        parentUri = Uri.parse(parentUri),
        mimeType = mimeType,
        relativePath = relativePath,
        createdTimestamp = createdTs,
        updatedTimestamp = updatedTs
    )
}
