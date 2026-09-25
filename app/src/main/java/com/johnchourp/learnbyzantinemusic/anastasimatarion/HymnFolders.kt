package com.johnchourp.learnbyzantinemusic.anastasimatarion

import com.johnchourp.learnbyzantinemusic.music.Mode

/**
 * Where a hymn's recordings live inside the user's recordings folder:
 * `Αναστασιματάριο/<mode folder>/<code> <incipit>/`.
 *
 * The names are fixed Greek, independent of the app language, so switching the language never
 * hides existing recordings. The mode folders are [Mode.anastasimatarionFolder], frozen on users'
 * devices byte for byte (ClickUp `869f5x299`). A hymn folder is found again by its `"<code> "`
 * prefix, so a user rename after the code, or a later fix of an incipit, keeps the recordings
 * attached.
 */
object HymnFolders {
    const val ROOT = "Αναστασιματάριο"

    private const val MAX_NAME_LENGTH = 60

    /** [Mode.anastasimatarionFolder] by key: frozen names, kept byte for byte in [Mode]. */
    private val MODE_FOLDERS: Map<String, String> = Mode.entries.associate { it.key to it.anastasimatarionFolder }

    private val FORBIDDEN = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
    private val SPACES = Regex("\\s+")

    val modeKeys: Set<String> get() = MODE_FOLDERS.keys

    fun modeFolder(modeKey: String): String =
        requireNotNull(MODE_FOLDERS[modeKey]) { "Unknown mode key: $modeKey" }

    fun hymnFolder(hymn: Hymn): String = "${hymn.code} ${sanitize(hymn.incipit)}".trim()

    fun hymnFolderPrefix(hymn: Hymn): String = "${hymn.code} "

    /** True when [folderName] is this hymn's folder, even if the user renamed its incipit part. */
    fun isHymnFolder(folderName: String?, hymn: Hymn): Boolean =
        folderName != null && (folderName == hymn.code || folderName.startsWith(hymnFolderPrefix(hymn)))

    /** Folder path segments from the recordings root to the hymn's folder. */
    fun pathSegments(modeKey: String, hymn: Hymn): List<String> =
        listOf(ROOT, modeFolder(modeKey), hymnFolder(hymn))

    /** A name every document provider accepts: no path or reserved characters, bounded length. */
    fun sanitize(name: String): String =
        name.replace(FORBIDDEN, " ")
            .replace(SPACES, " ")
            .trim()
            .take(MAX_NAME_LENGTH)
            .trimEnd('.', ' ')
}
