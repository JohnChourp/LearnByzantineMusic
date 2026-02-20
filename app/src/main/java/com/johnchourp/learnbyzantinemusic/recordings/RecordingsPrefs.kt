package com.johnchourp.learnbyzantinemusic.recordings

import android.content.ContentResolver
import android.content.Context
import android.net.Uri

class RecordingsPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
        private const val PREFS_NAME = "learn_byzantine_music_recordings"
        const val KEY_RECORDINGS_FOLDER_TREE_URI = "recordings_folder_tree_uri"
        const val KEY_RECORDINGS_OUTPUT_FORMAT = "recordings_output_format"
    }
}
