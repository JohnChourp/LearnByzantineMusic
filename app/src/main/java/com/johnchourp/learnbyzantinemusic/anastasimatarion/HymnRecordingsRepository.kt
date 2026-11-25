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

/** What a hymn's folders hold: its recordings and the name of the folder that receives the next one. */
data class HymnFolderContents(
    val recordings: List<HymnRecording> = emptyList(),
    val savingFolder: String? = null,
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
        // Each name is a provider query: read it once per folder, not once per hymn.
        val hymnFolders = modeFolder.listFiles().filter { it.isDirectory }.map { it to it.name }
        mode.hymns.mapNotNull { hymn ->
            val count = HymnFolders.foldersOf(hymn, hymnFolders) { it.second }
                .sumOf { (folder, _) -> folder.listFiles().count { it.isAudioFile() } }
            if (count > 0) hymn.code to count else null
        }.toMap()
    }

    /**
     * The hymn's recordings from every folder of it, newest first, and the name of the folder the
     * next recording goes into ([HymnFolders.savingFolderOf]; null when saving would create one).
     */
    suspend fun recordingsFor(modeKey: String, hymn: Hymn): HymnFolderContents = withContext(Dispatchers.IO) {
        // Each name is a provider query: read it once per folder.
        val hymnFolders = modeFolder(modeKey)
            ?.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it to it.name }
            ?: return@withContext HymnFolderContents()
        val recordings = HymnFolders.foldersOf(hymn, hymnFolders) { it.second }
            .flatMap { (folder, _) -> folder.listFiles().asList() }
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
        HymnFolderContents(recordings, HymnFolders.savingFolderOf(hymn, hymnFolders) { it.second }?.second)
    }

    private fun modeFolder(modeKey: String): DocumentFile? {
        val root = rootFolder() ?: return null
        val anastasimatarion = root.listFiles().firstOrNull { it.isDirectory && it.name == HymnFolders.ROOT } ?: return null
        val modeName = HymnFolders.modeFolder(modeKey)
        return anastasimatarion.listFiles().firstOrNull { it.isDirectory && it.name == modeName }
    }

    private fun DocumentFile.isAudioFile(): Boolean = isFile && RecordingFormatOption.supportsFileName(name)
}
