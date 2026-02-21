package com.johnchourp.learnbyzantinemusic.recordings.index

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.johnchourp.learnbyzantinemusic.recordings.DeleteOutcome
import com.johnchourp.learnbyzantinemusic.recordings.ManagerEntryType
import com.johnchourp.learnbyzantinemusic.recordings.ManagerListItem
import com.johnchourp.learnbyzantinemusic.recordings.MoveOutcome
import com.johnchourp.learnbyzantinemusic.recordings.MoveTargetFolder
import com.johnchourp.learnbyzantinemusic.recordings.OwnedRecordingEntry
import com.johnchourp.learnbyzantinemusic.recordings.OwnedRecordingsStore
import com.johnchourp.learnbyzantinemusic.recordings.RecordingDocumentOps
import com.johnchourp.learnbyzantinemusic.recordings.RecordingListItem
import com.johnchourp.learnbyzantinemusic.recordings.RecordingSortOption
import com.johnchourp.learnbyzantinemusic.recordings.RecordingTypeFilter
import com.johnchourp.learnbyzantinemusic.recordings.RenameOutcome
import com.johnchourp.learnbyzantinemusic.recordings.toRecordingListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Locale

class RecordingsRepository private constructor(
    private val context: Context,
    private val database: RecordingsDatabase,
    private val indexer: RecordingsIndexer
) {
    private val dao: RecordingIndexDao = database.recordingIndexDao()
    private val ownedRecordingsStore = OwnedRecordingsStore(context)

    fun observeOwnRecent(
        rootUri: Uri,
        limit: Int = 10
    ): Flow<PagingData<RecordingListItem>> {
        return ownedRecordingsStore.observeRecent(rootUri, limit).map { entries ->
            PagingData.from(entries.map { entry -> entry.toRecordingListItem() })
        }
    }

    suspend fun registerOwnedRecording(rootUri: Uri, item: RecordingListItem) {
        return withContext(Dispatchers.IO) {
            ownedRecordingsStore.register(item.toOwnedRecordingEntry(rootUri))
        }
    }

    suspend fun removeOwnedRecording(uri: Uri) {
        return withContext(Dispatchers.IO) {
            ownedRecordingsStore.remove(uri)
        }
    }

    suspend fun checkRecordingExists(uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            val document = DocumentFile.fromSingleUri(context, uri)
            document?.exists() == true
        }
    }

    fun observeFolderEntries(
        rootUri: Uri,
        parentUri: Uri,
        query: String,
        sortOption: RecordingSortOption,
        filter: RecordingTypeFilter
    ): Flow<PagingData<ManagerListItem>> {
        val rootUriRaw = rootUri.toString()
        val parentUriRaw = normalizeDirectoryUri(parentUri).toString()
        val queryPattern = query.toSearchPattern()
        val sourceFactory = when (sortOption) {
            RecordingSortOption.NAME -> {
                { dao.managerByName(rootUriRaw, parentUriRaw, queryPattern, filter.ordinal) }
            }

            RecordingSortOption.CREATED -> {
                { dao.managerByCreated(rootUriRaw, parentUriRaw, queryPattern, filter.ordinal) }
            }

            RecordingSortOption.UPDATED -> {
                { dao.managerByUpdated(rootUriRaw, parentUriRaw, queryPattern, filter.ordinal) }
            }
        }

        return Pager(
            config = PagingConfig(
                pageSize = 40,
                prefetchDistance = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = sourceFactory
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toManagerListItem() }
        }
    }

    fun observeMoveTargets(
        rootUri: Uri,
        query: String
    ): Flow<List<MoveTargetFolder>> {
        return flow {
            val normalizedPattern = query.toSearchPattern()
            val rootUriRaw = rootUri.toString()
            val items = withContext(Dispatchers.IO) {
                val rootTarget = MoveTargetFolder(
                    uri = normalizeDirectoryUri(rootUri),
                    relativePath = "",
                    displayPath = "/"
                )

                val dbTargets = dao.findMoveTargets(
                    rootUri = rootUriRaw,
                    searchPattern = normalizedPattern
                ).map { projection ->
                    val displayPath = if (projection.relativePath.isBlank()) {
                        "/"
                    } else {
                        "/${projection.relativePath}"
                    }
                    MoveTargetFolder(
                        uri = Uri.parse(projection.docUri),
                        relativePath = projection.relativePath,
                        displayPath = displayPath
                    )
                }

                val includesRoot = dbTargets.any { it.relativePath.isBlank() }
                if (includesRoot) {
                    dbTargets
                } else {
                    listOf(rootTarget) + dbTargets
                }
            }
            emit(items)
        }
    }

    suspend fun reindex(rootUri: Uri): IndexingResult {
        return indexer.reindex(rootUri)
    }

    fun enqueueReindex(rootUri: Uri) {
        val workName = "recordings_reindex_${rootUri.hashCode()}"
        val workData = Data.Builder()
            .putString(RecordingsReindexWorker.KEY_ROOT_URI, rootUri.toString())
            .build()

        val request = OneTimeWorkRequestBuilder<RecordingsReindexWorker>()
            .setInputData(workData)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, request)
    }

    suspend fun createFolder(parentUri: Uri, rawFolderName: String): Boolean {
        return withContext(Dispatchers.IO) {
            val parentFolder = resolveDirectory(parentUri) ?: return@withContext false
            val folderName = RecordingDocumentOps.sanitizeName(rawFolderName)
            if (folderName.isBlank()) {
                return@withContext false
            }
            if (parentFolder.findFile(folderName) != null) {
                return@withContext false
            }
            runCatching { parentFolder.createDirectory(folderName) }.getOrNull() != null
        }
    }

    suspend fun renameEntry(item: ManagerListItem, targetName: String): RenameOutcome {
        return withContext(Dispatchers.IO) {
            if (item.type == ManagerEntryType.AUDIO_FILE) {
                val result = RecordingDocumentOps.renameFileWithFallback(
                    context = context,
                    sourceUri = item.uri,
                    parentFolderUri = item.parentUri,
                    targetName = targetName
                )
                when (result) {
                    RenameOutcome.SUCCESS -> {
                        updateOwnedRecordingAfterRename(
                            sourceUri = item.uri,
                            parentUri = item.parentUri,
                            targetName = targetName,
                            originalMimeType = item.mimeType,
                            sourceRelativePath = item.relativePath
                        )
                    }

                    RenameOutcome.REMOVED -> {
                        ownedRecordingsStore.remove(item.uri)
                    }

                    else -> Unit
                }
                return@withContext result
            }

            val parentFolder = resolveDirectory(item.parentUri) ?: return@withContext RenameOutcome.FAILED
            if (parentFolder.findFile(targetName) != null) {
                return@withContext RenameOutcome.NAME_EXISTS
            }

            val sourceFolder = DocumentFile.fromSingleUri(context, item.uri)
                ?: return@withContext RenameOutcome.REMOVED
            if (!sourceFolder.exists()) {
                return@withContext RenameOutcome.REMOVED
            }

            val renamed = runCatching {
                sourceFolder.renameTo(targetName)
            }.getOrDefault(false)
            if (renamed) RenameOutcome.SUCCESS else RenameOutcome.FAILED
        }
    }

    suspend fun renameEntry(item: RecordingListItem, targetName: String): RenameOutcome {
        return withContext(Dispatchers.IO) {
            val result = RecordingDocumentOps.renameFileWithFallback(
                context = context,
                sourceUri = item.uri,
                parentFolderUri = item.parentUri,
                targetName = targetName
            )

            when (result) {
                RenameOutcome.SUCCESS -> {
                    updateOwnedRecordingAfterRename(
                        sourceUri = item.uri,
                        parentUri = item.parentUri,
                        targetName = targetName,
                        originalMimeType = item.mimeType,
                        sourceRelativePath = item.relativePath
                    )
                }

                RenameOutcome.REMOVED -> {
                    ownedRecordingsStore.remove(item.uri)
                }

                else -> Unit
            }

            result
        }
    }

    suspend fun deleteEntry(item: ManagerListItem): DeleteOutcome {
        return withContext(Dispatchers.IO) {
            val result = deleteDocument(item.uri)
            if (item.type == ManagerEntryType.AUDIO_FILE && result != DeleteOutcome.FAILED) {
                ownedRecordingsStore.remove(item.uri)
            }
            result
        }
    }

    suspend fun deleteEntry(item: RecordingListItem): DeleteOutcome {
        return withContext(Dispatchers.IO) {
            val result = deleteDocument(item.uri)
            if (result != DeleteOutcome.FAILED) {
                ownedRecordingsStore.remove(item.uri)
            }
            result
        }
    }

    suspend fun moveEntry(
        sourceItem: ManagerListItem,
        target: MoveTargetFolder
    ): MoveOutcome {
        return withContext(Dispatchers.IO) {
            if (sourceItem.parentUri == target.uri) {
                return@withContext MoveOutcome.SAME_PARENT
            }

            if (
                sourceItem.type == ManagerEntryType.FOLDER &&
                target.relativePath.isNotBlank() &&
                (target.relativePath == sourceItem.relativePath || target.relativePath.startsWith("${sourceItem.relativePath}/"))
            ) {
                return@withContext MoveOutcome.BLOCKED_SELF_OR_DESCENDANT
            }

            val movedUri = runCatching {
                DocumentsContract.moveDocument(
                    context.contentResolver,
                    sourceItem.uri,
                    sourceItem.parentUri,
                    target.uri
                )
            }.getOrNull()

            if (movedUri != null) {
                if (sourceItem.type == ManagerEntryType.AUDIO_FILE) {
                    updateOwnedRecordingAfterMove(sourceItem, target, movedUri)
                }
                MoveOutcome.SUCCESS
            } else {
                MoveOutcome.FAILED
            }
        }
    }

    suspend fun resolveDirectoryUriByRelativePath(rootUri: Uri, relativePath: String): Uri? {
        if (relativePath.isBlank()) {
            return normalizeDirectoryUri(rootUri)
        }
        return withContext(Dispatchers.IO) {
            dao.findDirectoryByRelativePath(rootUri.toString(), relativePath)?.docUri?.let(Uri::parse)
        }
    }

    suspend fun resolveRootFolderName(rootUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            DocumentFile.fromTreeUri(context, rootUri)?.name
        }
    }

    suspend fun getIndexedCount(rootUri: Uri): Int {
        return withContext(Dispatchers.IO) {
            dao.countByRootUri(rootUri.toString())
        }
    }

    fun normalizeDirectoryUri(uri: Uri): Uri {
        if (!DocumentsContract.isTreeUri(uri)) {
            return uri
        }

        return runCatching {
            val treeDocumentId = DocumentsContract.getTreeDocumentId(uri)
            DocumentsContract.buildDocumentUriUsingTree(uri, treeDocumentId)
        }.getOrDefault(uri)
    }

    private fun resolveDirectory(uri: Uri): DocumentFile? {
        val normalizedUri = normalizeDirectoryUri(uri)

        val asTree = DocumentFile.fromTreeUri(context, uri)
        if (asTree != null && asTree.exists() && asTree.isDirectory) {
            return asTree
        }

        val asSingle = DocumentFile.fromSingleUri(context, normalizedUri)
        if (asSingle != null && asSingle.exists() && asSingle.isDirectory) {
            return asSingle
        }

        return null
    }

    private fun deleteDocument(uri: Uri): DeleteOutcome {
        val document = DocumentFile.fromSingleUri(context, uri)
            ?: return DeleteOutcome.REMOVED
        if (!document.exists()) {
            return DeleteOutcome.REMOVED
        }

        val deleted = runCatching { document.delete() }.getOrDefault(false)
        if (deleted) {
            return DeleteOutcome.SUCCESS
        }

        val stillExists = runCatching { document.exists() }.getOrDefault(true)
        return if (!stillExists) DeleteOutcome.REMOVED else DeleteOutcome.FAILED
    }

    private fun updateOwnedRecordingAfterRename(
        sourceUri: Uri,
        parentUri: Uri,
        targetName: String,
        originalMimeType: String?,
        sourceRelativePath: String
    ) {
        val renamedDocument = resolveDirectory(parentUri)?.findFile(targetName) ?: return
        val parentRelativePath = sourceRelativePath.substringBeforeLast('/', "")
        val updatedRelativePath = if (parentRelativePath.isBlank()) {
            targetName
        } else {
            "$parentRelativePath/$targetName"
        }
        val updatedTimestamp = runCatching { renamedDocument.lastModified() }
            .getOrDefault(System.currentTimeMillis())

        ownedRecordingsStore.updateIfTracked(
            sourceUri = sourceUri,
            updatedUri = renamedDocument.uri,
            updatedName = targetName,
            updatedMimeType = renamedDocument.type ?: originalMimeType,
            updatedParentUri = parentUri,
            updatedRelativePath = updatedRelativePath,
            updatedTimestamp = updatedTimestamp
        )
    }

    private fun updateOwnedRecordingAfterMove(
        sourceItem: ManagerListItem,
        target: MoveTargetFolder,
        movedUri: Uri
    ) {
        val updatedRelativePath = if (target.relativePath.isBlank()) {
            sourceItem.name
        } else {
            "${target.relativePath}/${sourceItem.name}"
        }

        val movedDocument = DocumentFile.fromSingleUri(context, movedUri)
        val updatedTimestamp = movedDocument?.lastModified()
            ?.takeIf { it > 0L }
            ?: System.currentTimeMillis()

        ownedRecordingsStore.updateIfTracked(
            sourceUri = sourceItem.uri,
            updatedUri = movedUri,
            updatedName = sourceItem.name,
            updatedMimeType = movedDocument?.type ?: sourceItem.mimeType,
            updatedParentUri = target.uri,
            updatedRelativePath = updatedRelativePath,
            updatedTimestamp = updatedTimestamp
        )
    }

    private fun String.toSearchPattern(): String {
        val normalized = trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) {
            return "%"
        }
        return "%$normalized%"
    }

    private fun RecordingListItem.toOwnedRecordingEntry(rootUri: Uri): OwnedRecordingEntry {
        return OwnedRecordingEntry(
            rootUri = rootUri.toString(),
            uri = uri.toString(),
            name = name,
            mimeType = mimeType,
            parentUri = parentUri.toString(),
            relativePath = relativePath,
            parentRelativePath = parentRelativePath,
            createdTs = createdTimestamp,
            updatedTs = updatedTimestamp
        )
    }

    companion object {
        @Volatile
        private var instance: RecordingsRepository? = null

        fun getInstance(context: Context): RecordingsRepository {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val db = RecordingsDatabase.getInstance(context.applicationContext)
                    RecordingsRepository(
                        context = context.applicationContext,
                        database = db,
                        indexer = RecordingsIndexer(context.applicationContext, db)
                    ).also { instance = it }
                }
            }
        }
    }
}
