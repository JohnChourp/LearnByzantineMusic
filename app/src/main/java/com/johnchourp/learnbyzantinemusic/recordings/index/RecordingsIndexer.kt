package com.johnchourp.learnbyzantinemusic.recordings.index

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.johnchourp.learnbyzantinemusic.recordings.RecordingDocumentOps
import com.johnchourp.learnbyzantinemusic.recordings.RecordingFormatOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.ArrayDeque
import java.util.Locale
import kotlin.coroutines.coroutineContext

data class IndexingResult(
    val indexedItems: Int,
    val indexedFolders: Int,
    val indexingErrors: Int,
    val completed: Boolean
)

class RecordingsIndexer(
    private val context: Context,
    private val database: RecordingsDatabase
) {
    suspend fun reindex(rootUri: Uri): IndexingResult {
        return withContext(Dispatchers.IO) {
            val rootFolder = DocumentFile.fromTreeUri(context, rootUri)
            if (rootFolder == null || !rootFolder.exists() || !rootFolder.isDirectory || !rootFolder.canRead()) {
                return@withContext IndexingResult(
                    indexedItems = 0,
                    indexedFolders = 0,
                    indexingErrors = 1,
                    completed = false
                )
            }

            val rootUriString = rootUri.toString()
            val queue = ArrayDeque<Pair<DocumentFile, String>>()
            val entries = mutableListOf<RecordingIndexEntity>()
            var folderCount = 0
            var errorCount = 0

            queue.add(rootFolder to "")

            while (queue.isNotEmpty()) {
                coroutineContext.ensureActive()
                val (folder, folderRelativePath) = queue.removeFirst()
                val children = runCatching { folder.listFiles().toList() }.getOrElse {
                    errorCount += 1
                    emptyList()
                }

                for (child in children) {
                    coroutineContext.ensureActive()
                    val name = child.name ?: continue
                    val isDirectory = child.isDirectory
                    val isAudio = child.isFile && RecordingFormatOption.supportsFileName(name)
                    if (!isDirectory && !isAudio) {
                        continue
                    }

                    val relativePath = if (folderRelativePath.isBlank()) {
                        name
                    } else {
                        "$folderRelativePath/$name"
                    }

                    val updatedTs = runCatching { child.lastModified() }.getOrDefault(0L)
                    val createdTs = if (isDirectory) {
                        updatedTs
                    } else {
                        RecordingDocumentOps.resolveCreationLikeTimestamp(name) ?: updatedTs
                    }

                    entries.add(
                        RecordingIndexEntity(
                            docUri = child.uri.toString(),
                            rootUri = rootUriString,
                            parentUri = folder.uri.toString(),
                            relativePath = relativePath,
                            name = name,
                            nameNormalized = normalizeForSearch(name),
                            isDirectory = isDirectory,
                            isAudio = isAudio,
                            mimeType = child.type,
                            createdTs = createdTs,
                            updatedTs = updatedTs,
                            fileExtension = if (isDirectory) null else resolveExtension(name)
                        )
                    )

                    if (isDirectory) {
                        folderCount += 1
                        queue.add(child to relativePath)
                    }
                }
            }

            database.withTransaction {
                val dao = database.recordingIndexDao()
                dao.deleteByRootUri(rootUriString)
                if (entries.isNotEmpty()) {
                    entries.chunked(200).forEach { chunk ->
                        dao.insertAll(chunk)
                    }
                }
            }

            IndexingResult(
                indexedItems = entries.size,
                indexedFolders = folderCount,
                indexingErrors = errorCount,
                completed = true
            )
        }
    }

    private fun resolveExtension(name: String): String {
        return name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    }

    private fun normalizeForSearch(raw: String): String {
        return raw.trim().lowercase(Locale.ROOT)
    }
}
