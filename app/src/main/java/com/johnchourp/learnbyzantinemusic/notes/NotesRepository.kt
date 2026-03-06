package com.johnchourp.learnbyzantinemusic.notes

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

class NotesRepository private constructor(
    private val context: Context,
    private val database: NotesDatabase,
    private val prefs: NotesPrefs,
    private val backupManager: NotesBackupManager
) {
    private val dao: NotesDao = database.notesDao()
    private val mutationMutex = Mutex()

    fun observeNotes(searchQuery: String): Flow<List<NoteEntity>> {
        return dao.observeBySearch(searchQuery.toNotesSearchPattern())
    }

    suspend fun createNote(): NotesMutationResult = mutationMutex.withLock {
        val now = System.currentTimeMillis()
        val note = NoteEntity(
            id = UUID.randomUUID().toString(),
            title = "",
            body = "",
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        withContext(Dispatchers.IO) {
            dao.upsert(note)
        }
        return@withLock runSnapshotSync(messageOnSuccess = "created")
    }

    suspend fun saveNote(noteId: String, title: String, body: String): NotesMutationResult = mutationMutex.withLock {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val existing = dao.getById(noteId)
            val normalizedTitle = title.trim()
            val normalizedBody = body.trimEnd()
            val note = NoteEntity(
                id = noteId,
                title = normalizedTitle,
                body = normalizedBody,
                createdAtEpochMs = existing?.createdAtEpochMs ?: now,
                updatedAtEpochMs = now
            )
            dao.upsert(note)
        }
        return@withLock runSnapshotSync(messageOnSuccess = "saved")
    }

    suspend fun deleteNote(noteId: String): NotesMutationResult = mutationMutex.withLock {
        withContext(Dispatchers.IO) {
            dao.deleteById(noteId)
        }
        return@withLock runSnapshotSync(messageOnSuccess = "deleted")
    }

    suspend fun exportNow(): NotesMutationResult = mutationMutex.withLock {
        return@withLock runSnapshotSync(messageOnSuccess = "exported")
    }

    suspend fun importReplace(snapshotUri: Uri): NotesMutationResult = mutationMutex.withLock {
        val rawJson = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(snapshotUri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                reader.readText()
            }
        } ?: return@withLock NotesMutationResult(
            syncState = getSyncState(),
            message = "import_read_failed"
        )

        val importedNotes = try {
            NotesBackupCodec.decodeSnapshot(rawJson)
        } catch (_: IllegalArgumentException) {
            return@withLock NotesMutationResult(
                syncState = getSyncState(),
                message = "import_invalid_json"
            )
        }
        val replacementNotes = replaceAllNotesWithImport(importedNotes)

        withContext(Dispatchers.IO) {
            database.withTransaction {
                dao.deleteAll()
                dao.insertAll(replacementNotes)
            }
        }

        val syncResult = runSnapshotSync(messageOnSuccess = "imported")
        return@withLock syncResult
    }

    suspend fun syncPendingBackups(): NotesMutationResult = mutationMutex.withLock {
        val result = backupManager.syncPendingOnly()
        val message = when (result.pendingSyncResult) {
            is PendingSyncResult.Success -> {
                if (result.pendingSyncResult.syncedCount > 0) "resync_success"
                else "resync_nothing_pending"
            }

            is PendingSyncResult.Failure -> "resync_failed"
        }

        return@withLock NotesMutationResult(
            syncState = buildSyncState(result),
            message = message
        )
    }

    fun getSyncState(): NotesSyncState {
        val folderUri = prefs.getFolderUri()
        val folderName = resolveFolderName(folderUri)
        return NotesSyncState(
            folderName = folderName,
            folderUri = folderUri?.toString(),
            pendingSyncCount = backupManager.pendingCount(),
            lastSyncEpochMs = prefs.getLastSyncEpochMs(),
            lastSyncError = prefs.getLastSyncError()
        )
    }

    fun resolveFolderName(uri: Uri?): String? {
        if (uri == null) {
            return null
        }
        val asTree = DocumentFile.fromTreeUri(context, uri)
        if (asTree != null && asTree.exists() && asTree.isDirectory) {
            return asTree.name
        }
        val asSingle = DocumentFile.fromSingleUri(context, uri)
        if (asSingle != null && asSingle.exists() && asSingle.isDirectory) {
            return asSingle.name
        }
        return null
    }

    private suspend fun runSnapshotSync(messageOnSuccess: String): NotesMutationResult {
        val allNotes = withContext(Dispatchers.IO) {
            dao.getAllForSnapshot()
        }

        val backupResult = backupManager.createAndSyncSnapshot(allNotes)
        val message = when {
            backupResult.primaryWriteResult is SnapshotWriteResult.Failure -> "sync_failed_local_saved"
            backupResult.pendingSyncResult is PendingSyncResult.Failure -> "sync_partial_pending"
            else -> messageOnSuccess
        }

        return NotesMutationResult(
            syncState = buildSyncState(backupResult),
            message = message
        )
    }

    private fun buildSyncState(result: BackupRunResult): NotesSyncState {
        val folderUri = prefs.getFolderUri()
        return NotesSyncState(
            folderName = resolveFolderName(folderUri),
            folderUri = folderUri?.toString(),
            pendingSyncCount = result.pendingCount,
            lastSyncEpochMs = result.lastSyncEpochMs,
            lastSyncError = result.lastSyncError
        )
    }

    companion object {
        @Volatile
        private var instance: NotesRepository? = null

        fun getInstance(context: Context): NotesRepository {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val appContext = context.applicationContext
                    val prefs = NotesPrefs(appContext)
                    NotesRepository(
                        context = appContext,
                        database = NotesDatabase.getInstance(appContext),
                        prefs = prefs,
                        backupManager = NotesBackupManager(appContext, prefs)
                    )
                }.also { instance = it }
            }
        }
    }
}
