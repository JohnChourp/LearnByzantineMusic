package com.johnchourp.learnbyzantinemusic.anastasimatarion

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.johnchourp.learnbyzantinemusic.recordings.RecordingFormatOption
import com.johnchourp.learnbyzantinemusic.recordings.RecordingsPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One audio file inside a hymn's folder. */
data class HymnRecording(
    val name: String,
    val uri: Uri,
    val mimeType: String?,
    val lastModified: Long,
)

/**
 * Reads the hymn folders ([HymnFolders]) inside the recordings folder the user picked on the
 * «Ηχογραφήσεις» page (a SAF tree). Nothing is created here: RecordingsActivity creates a hymn's
 * folder when the first recording for it is saved.
 */
class HymnRecordingsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = RecordingsPrefs(appContext)

    /** The picked recordings folder, or null when none is picked or access was revoked. */
    fun rootFolder(): DocumentFile? {
        val uri = prefs.getFolderUri() ?: return null
        if (!prefs.hasPersistedReadWriteAccess(appContext.contentResolver, uri)) return null
        return DocumentFile.fromTreeUri(appContext, uri)?.takeIf { it.exists() && it.isDirectory }
    }

    /** Recording count per hymn code for one mode; hymns without recordings are absent. */
    suspend fun countsForMode(mode: HymnMode): Map<String, Int> = withContext(Dispatchers.IO) {
        val modeFolder = modeFolder(mode.key) ?: return@withContext emptyMap()
        val hymnFolders = modeFolder.listFiles().filter { it.isDirectory }
        mode.hymns.mapNotNull { hymn ->
            val folder = hymnFolders.firstOrNull { HymnFolders.isHymnFolder(it.name, hymn) } ?: return@mapNotNull null
            val count = folder.listFiles().count { it.isAudioFile() }
            if (count > 0) hymn.code to count else null
        }.toMap()
    }

    /** The hymn's recordings, newest first. */
    suspend fun recordingsFor(modeKey: String, hymn: Hymn): List<HymnRecording> = withContext(Dispatchers.IO) {
        val folder = modeFolder(modeKey)
            ?.listFiles()
            ?.firstOrNull { it.isDirectory && HymnFolders.isHymnFolder(it.name, hymn) }
            ?: return@withContext emptyList()
        folder.listFiles()
            .filter { it.isAudioFile() }
            .map { file ->
                HymnRecording(
                    name = file.name.orEmpty(),
                    uri = file.uri,
                    mimeType = file.type,
                    lastModified = runCatching { file.lastModified() }.getOrDefault(0L),
                )
            }
            .sortedByDescending { it.lastModified }
    }

    private fun modeFolder(modeKey: String): DocumentFile? {
        val root = rootFolder() ?: return null
        val anastasimatarion = root.listFiles().firstOrNull { it.isDirectory && it.name == HymnFolders.ROOT } ?: return null
        val modeName = HymnFolders.modeFolder(modeKey)
        return anastasimatarion.listFiles().firstOrNull { it.isDirectory && it.name == modeName }
    }

    private fun DocumentFile.isAudioFile(): Boolean = isFile && RecordingFormatOption.supportsFileName(name)
}
