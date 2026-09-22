package com.johnchourp.learnbyzantinemusic.notes

import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
import android.content.ContentResolver
import android.content.Context
import android.net.Uri

class NotesPrefs(context: Context) {
    private val prefs = AppPrefs.open(context, AppPrefs.Store.NOTES)

    fun getFolderUri(): Uri? {
        val raw = prefs.getString(KEY_NOTES_FOLDER_TREE_URI, null) ?: return null
        return runCatching { Uri.parse(raw) }.getOrNull()
    }

    fun setFolderUri(uri: Uri) {
        prefs.edit().putString(KEY_NOTES_FOLDER_TREE_URI, uri.toString()).apply()
    }

    fun clearFolderUri() {
        prefs.edit().remove(KEY_NOTES_FOLDER_TREE_URI).apply()
    }

    fun hasPersistedReadWriteAccess(contentResolver: ContentResolver, uri: Uri): Boolean =
        contentResolver.persistedUriPermissions.any { permission ->
            permission.uri == uri && permission.isReadPermission && permission.isWritePermission
        }

    fun getLastSyncEpochMs(): Long? {
        val value = prefs.getLong(KEY_NOTES_LAST_SYNC_EPOCH_MS, -1L)
        return value.takeIf { it > 0L }
    }

    fun setLastSyncEpochMs(epochMs: Long) {
        prefs.edit().putLong(KEY_NOTES_LAST_SYNC_EPOCH_MS, epochMs).apply()
    }

    fun getLastSyncError(): String? {
        return prefs.getString(KEY_NOTES_LAST_SYNC_ERROR, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun setLastSyncError(error: String?) {
        prefs.edit().putString(KEY_NOTES_LAST_SYNC_ERROR, error?.trim()).apply()
    }

    companion object {
        val KEY_NOTES_FOLDER_TREE_URI = AppPrefs.NotesFolderTreeUri.name
        val KEY_NOTES_LAST_SYNC_EPOCH_MS = AppPrefs.NotesLastSyncEpochMs.name
        val KEY_NOTES_LAST_SYNC_ERROR = AppPrefs.NotesLastSyncError.name
    }
}
