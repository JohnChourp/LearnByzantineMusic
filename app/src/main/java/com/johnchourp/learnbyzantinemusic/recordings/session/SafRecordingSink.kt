package com.johnchourp.learnbyzantinemusic.recordings.session

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.johnchourp.learnbyzantinemusic.recordings.RecordingDocumentOps
import com.johnchourp.learnbyzantinemusic.recordings.RecordingListItem
import com.johnchourp.learnbyzantinemusic.recordings.RecordingsPrefs
import com.johnchourp.learnbyzantinemusic.recordings.index.RecordingsRepository
import java.io.OutputStream

/**
 * The folder picked on «Ηχογραφήσεις» — or the hymn sub-folder of it a recording was made for — as a
 * [RecordingSink]. Resolved at save time from `recordings_folder_tree_uri`, so a recording made
 * before the folder was changed, or saved later from the pending list, goes to the folder of *now*.
 *
 * Sub-folders are found or created exactly as the recording screen always did it (the last one also
 * matched by prefix, so a renamed hymn folder keeps receiving its recordings); stored folder names,
 * `΄` included, are used as they are.
 */
class SafRecordingSink private constructor(
    private val context: Context,
    private val repository: RecordingsRepository,
    private val rootUri: Uri,
    private val folder: DocumentFile,
    private val folderPath: List<String>,
) : RecordingSink {

    override fun createFile(displayName: String, mimeType: String): SinkFile? =
        folder.createFile(mimeType, displayName)?.let { SafSinkFile(it, displayName, mimeType) }

    override suspend fun register(file: SinkFile) {
        val saved = file as? SafSinkFile ?: return
        repository.registerOwnedRecording(rootUri, saved.toListItem())
    }

    private inner class SafSinkFile(
        private val document: DocumentFile,
        private val requestedName: String,
        private val mimeType: String,
    ) : SinkFile {
        override val name: String get() = document.name ?: requestedName

        override fun openOutputStream(): OutputStream? =
            context.contentResolver.openOutputStream(document.uri, "w")

        /** Null only when the provider leaves the size column empty; no answer at all is a failure. */
        override fun reportedSize(): Long? {
            val cursor = context.contentResolver.query(
                document.uri,
                arrayOf(DocumentsContract.Document.COLUMN_SIZE),
                null,
                null,
                null,
            ) ?: error("size_query_failed")
            return cursor.use {
                check(it.moveToFirst()) { "size_query_empty" }
                if (it.isNull(0)) null else it.getLong(0)
            }
        }

        override fun delete(): Boolean = document.delete()

        fun toListItem(): RecordingListItem {
            val resolvedName = name
            val updatedTimestamp = runCatching { document.lastModified() }
                .getOrDefault(System.currentTimeMillis())
                .takeIf { it > 0L }
                ?: System.currentTimeMillis()
            val createdTimestamp = RecordingDocumentOps.resolveCreationLikeTimestamp(resolvedName) ?: updatedTimestamp
            val path = folderPath.joinToString("/")
            return RecordingListItem(
                name = resolvedName,
                uri = document.uri,
                mimeType = document.type ?: mimeType,
                relativePath = if (path.isEmpty()) resolvedName else "$path/$resolvedName",
                parentRelativePath = if (path.isEmpty()) "/" else "/$path",
                parentUri = folder.uri,
                createdTimestamp = createdTimestamp,
                updatedTimestamp = updatedTimestamp,
            )
        }
    }

    companion object {
        /** The folder a recording for [target] goes into; null when the picked folder is not usable now. */
        fun open(context: Context, repository: RecordingsRepository, target: RecordingTarget): SafRecordingSink? {
            val rootUri = RecordingsPrefs(context).getFolderUri() ?: return null
            val root = runCatching { DocumentFile.fromTreeUri(context, rootUri) }.getOrNull()
                ?.takeIf { it.exists() && it.isDirectory && it.canRead() && it.canWrite() }
                ?: return null
            val (folder, path) = resolveTargetFolder(root, target) ?: return null
            return SafRecordingSink(context, repository, rootUri, folder, path)
        }

        /**
         * The folder to save into and its path below the picked root: the root itself, or the
         * target's sub-folders, created on first use. The last segment also matches an existing
         * folder that starts with the target's match prefix, so a renamed hymn folder keeps
         * receiving its recordings.
         */
        private fun resolveTargetFolder(root: DocumentFile, target: RecordingTarget): Pair<DocumentFile, List<String>>? {
            val segments = target.folderSegments
            var current = root
            val actualNames = mutableListOf<String>()
            segments.forEachIndexed { index, segment ->
                val prefix = target.folderMatchPrefix?.takeIf { index == segments.lastIndex }
                val children = current.listFiles().filter { it.isDirectory }
                val existing = children.firstOrNull { it.name == segment }
                    ?: prefix?.let { p -> children.firstOrNull { it.name?.startsWith(p) == true } }
                current = existing ?: current.createDirectory(segment) ?: return null
                actualNames += current.name ?: segment
            }
            return current to actualNames
        }
    }
}
