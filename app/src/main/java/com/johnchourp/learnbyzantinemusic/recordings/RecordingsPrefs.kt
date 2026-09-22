package com.johnchourp.learnbyzantinemusic.recordings

import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
import android.content.ContentResolver
import android.content.Context
import android.net.Uri

class RecordingsPrefs(context: Context) {
    private val prefs = AppPrefs.open(context, AppPrefs.Store.RECORDINGS)

    fun getFolderUri(): Uri? {
        val raw = prefs.getString(KEY_RECORDINGS_FOLDER_TREE_URI, null) ?: return null
        return runCatching { Uri.parse(raw) }.getOrNull()
    }

    fun setFolderUri(uri: Uri) {
        prefs.edit().putString(KEY_RECORDINGS_FOLDER_TREE_URI, uri.toString()).apply()
    }

    fun getSelectedFormat(): RecordingFormatOption =
        RecordingFormatOption.fromStoredValue(prefs.getString(KEY_RECORDINGS_OUTPUT_FORMAT, RecordingFormatOption.FLAC.name))

    fun setSelectedFormat(format: RecordingFormatOption) {
        prefs.edit().putString(KEY_RECORDINGS_OUTPUT_FORMAT, format.name).apply()
    }

    fun hasPersistedReadWriteAccess(contentResolver: ContentResolver, uri: Uri): Boolean =
        contentResolver.persistedUriPermissions.any { permission ->
            permission.uri == uri && permission.isReadPermission && permission.isWritePermission
        }

    companion object {
        val KEY_RECORDINGS_FOLDER_TREE_URI = AppPrefs.RecordingsFolderTreeUri.name
        val KEY_RECORDINGS_OUTPUT_FORMAT = AppPrefs.RecordingsOutputFormat.name
    }
}
