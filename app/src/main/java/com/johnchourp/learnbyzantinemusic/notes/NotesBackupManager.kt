package com.johnchourp.learnbyzantinemusic.notes

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class NotesBackupManager(
    private val context: Context,
    private val prefs: NotesPrefs
) {
    private val fileNameFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS", Locale.US)

    private val pendingDir: File by lazy {
        File(context.filesDir, "notes_pending_snapshots").apply { mkdirs() }
    }

    suspend fun createAndSyncSnapshot(notes: List<NoteEntity>): BackupRunResult = withContext(Dispatchers.IO) {
        val exportedAt = System.currentTimeMillis()
        val snapshotJson = NotesBackupCodec.encodeSnapshot(notes, exportedAt)
        val snapshotFileName = buildSnapshotFileName(exportedAt)
        val configuredFolderUri = prefs.getFolderUri()

        val writeResult = writeSnapshotToConfiguredFolder(
            folderUri = configuredFolderUri,
            preferredFileName = snapshotFileName,
            payload = snapshotJson
        )

        if (writeResult !is SnapshotWriteResult.Success) {
            val queued = runCatching {
                queuePendingSnapshot(snapshotFileName, snapshotJson)
            }.isSuccess
            if (!queued) {
                prefs.setLastSyncError("pending_snapshot_write_failed")
            }
        }

        val pendingSync = syncPendingSnapshots(configuredFolderUri)

        BackupRunResult(
            primaryWriteResult = writeResult,
            pendingSyncResult = pendingSync,
            pendingCount = pendingCount(),
            lastSyncEpochMs = prefs.getLastSyncEpochMs(),
            lastSyncError = prefs.getLastSyncError()
        )
    }

    suspend fun syncPendingOnly(): BackupRunResult = withContext(Dispatchers.IO) {
        val pendingSync = syncPendingSnapshots(prefs.getFolderUri())
        BackupRunResult(
            primaryWriteResult = SnapshotWriteResult.Skipped,
            pendingSyncResult = pendingSync,
            pendingCount = pendingCount(),
            lastSyncEpochMs = prefs.getLastSyncEpochMs(),
            lastSyncError = prefs.getLastSyncError()
        )
    }

    fun pendingCount(): Int {
        return pendingFiles().size
    }

    private fun writeSnapshotToConfiguredFolder(
        folderUri: Uri?,
        preferredFileName: String,
        payload: String
    ): SnapshotWriteResult {
        if (folderUri == null) {
            prefs.setLastSyncError("backup_folder_not_selected")
            return SnapshotWriteResult.Failure("backup_folder_not_selected")
        }

        val folder = resolveWritableFolder(folderUri)
            ?: run {
                prefs.setLastSyncError("backup_folder_not_accessible")
                return SnapshotWriteResult.Failure("backup_folder_not_accessible")
            }

        val targetName = ensureUniqueName(folder, preferredFileName)
        val targetFile = folder.createFile("application/json", targetName)
            ?: run {
                prefs.setLastSyncError("backup_file_create_failed")
                return SnapshotWriteResult.Failure("backup_file_create_failed")
            }

        val writeSucceeded = runCatching {
            context.contentResolver.openOutputStream(targetFile.uri, "w")?.use { output ->
                output.write(payload.toByteArray(Charsets.UTF_8))
                output.flush()
            } ?: error("open_output_stream_failed")
        }.isSuccess

        if (!writeSucceeded) {
            runCatching { targetFile.delete() }
            prefs.setLastSyncError("backup_file_write_failed")
            return SnapshotWriteResult.Failure("backup_file_write_failed")
        }

        prefs.setLastSyncEpochMs(System.currentTimeMillis())
        prefs.setLastSyncError(null)
        return SnapshotWriteResult.Success(targetName)
    }

    private fun syncPendingSnapshots(folderUri: Uri?): PendingSyncResult {
        if (folderUri == null) {
            prefs.setLastSyncError("backup_folder_not_selected")
            return PendingSyncResult.Failure("backup_folder_not_selected")
        }

        val folder = resolveWritableFolder(folderUri)
            ?: run {
                prefs.setLastSyncError("backup_folder_not_accessible")
                return PendingSyncResult.Failure("backup_folder_not_accessible")
            }

        val files = pendingFiles()
        if (files.isEmpty()) {
            return PendingSyncResult.Success(0)
        }

        var synced = 0
        files.sortedBy { it.lastModified() }.forEach { file ->
            val payload = runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
            if (payload == null) {
                prefs.setLastSyncError("pending_snapshot_read_failed")
                return PendingSyncResult.Failure("pending_snapshot_read_failed")
            }

            val writeResult = writeSnapshotToConfiguredFolder(
                folderUri = folder.uri,
                preferredFileName = file.name,
                payload = payload
            )

            if (writeResult !is SnapshotWriteResult.Success) {
                val reason = (writeResult as? SnapshotWriteResult.Failure)?.reason ?: "pending_snapshot_sync_failed"
                prefs.setLastSyncError(reason)
                return PendingSyncResult.Failure(reason)
            }

            val deleted = runCatching { file.delete() }.getOrDefault(false)
            if (!deleted) {
                prefs.setLastSyncError("pending_snapshot_cleanup_failed")
                return PendingSyncResult.Failure("pending_snapshot_cleanup_failed")
            }

            synced += 1
        }

        prefs.setLastSyncEpochMs(System.currentTimeMillis())
        prefs.setLastSyncError(null)
        return PendingSyncResult.Success(synced)
    }

    private fun resolveWritableFolder(uri: Uri): DocumentFile? {
        val asTree = DocumentFile.fromTreeUri(context, uri)
        if (asTree != null && asTree.exists() && asTree.isDirectory && asTree.canWrite()) {
            return asTree
        }

        val asSingle = DocumentFile.fromSingleUri(context, uri)
        if (asSingle != null && asSingle.exists() && asSingle.isDirectory && asSingle.canWrite()) {
            return asSingle
        }

        return null
    }

    private fun ensureUniqueName(folder: DocumentFile, preferredName: String): String {
        if (folder.findFile(preferredName) == null) {
            return preferredName
        }

        val baseName = preferredName.substringBeforeLast('.', preferredName)
        val extension = preferredName.substringAfterLast('.', "").takeIf { it.isNotBlank() }
        var index = 1
        while (true) {
            val candidate = if (extension == null) {
                "${baseName}_$index"
            } else {
                "${baseName}_$index.$extension"
            }
            if (folder.findFile(candidate) == null) {
                return candidate
            }
            index += 1
        }
    }

    private fun queuePendingSnapshot(fileName: String, payload: String) {
        if (!pendingDir.exists()) {
            pendingDir.mkdirs()
        }
        val safeName = fileName.ifBlank { buildSnapshotFileName(System.currentTimeMillis()) }
        val targetFile = File(pendingDir, safeName)
        runCatching {
            targetFile.writeText(payload, Charsets.UTF_8)
        }.getOrElse {
            throw IOException("unable_to_write_pending_snapshot", it)
        }
    }

    private fun pendingFiles(): List<File> {
        val files = pendingDir.listFiles() ?: return emptyList()
        return files.filter { it.isFile && it.name.endsWith(".json") }
    }

    private fun buildSnapshotFileName(timestampEpochMs: Long): String {
        val token = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestampEpochMs),
            ZoneId.systemDefault()
        ).format(fileNameFormatter)
        return "notes_snapshot_${token}.json"
    }
}

sealed interface SnapshotWriteResult {
    data object Skipped : SnapshotWriteResult
    data class Success(val fileName: String) : SnapshotWriteResult
    data class Failure(val reason: String) : SnapshotWriteResult
}

sealed interface PendingSyncResult {
    data class Success(val syncedCount: Int) : PendingSyncResult
    data class Failure(val reason: String) : PendingSyncResult
}

data class BackupRunResult(
    val primaryWriteResult: SnapshotWriteResult,
    val pendingSyncResult: PendingSyncResult,
    val pendingCount: Int,
    val lastSyncEpochMs: Long?,
    val lastSyncError: String?
)
