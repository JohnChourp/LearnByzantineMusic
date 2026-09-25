package com.johnchourp.learnbyzantinemusic

import com.johnchourp.learnbyzantinemusic.docs.KotlinSource
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * What Android Auto Backup and a device-to-device transfer keep of the user's data (ClickUp
 * `869f5x2a2`). Android reads two files: `backup_rules.xml` on Android 7-11, and
 * `data_extraction_rules.xml` on 12+, which has one rule set for the cloud backup and one for device
 * transfer. Three rule sets that nothing kept in step — until now each was an Android Studio template.
 *
 * The decisions live here as data: which preferences files travel, and what else stays on the device.
 * The three rule sets must exclude exactly that, and nothing may be `<include>`d — one include switches
 * Android to include-only mode, and the notes would silently stop being backed up.
 */
class BackupRulesTest {

    private data class Rule(val domain: String, val path: String)

    private enum class Backup { KEPT, LEFT_OUT }

    /**
     * Every preferences file, decided. A new [AppPrefs.Store] without an entry here fails
     * [everyPreferencesStoreHasABackupDecision]: somebody has to decide whether it travels.
     */
    private val storeDecisions = mapOf(
        // Font size, language, learning progress, favourites, metronome, theme.
        AppPrefs.Store.SETTINGS to Backup.KEPT,
        // The recordings folder URI (and the chosen format, which falls back to FLAC).
        AppPrefs.Store.RECORDINGS to Backup.LEFT_OUT,
        // The notes folder URI and the sync bookkeeping of this device.
        AppPrefs.Store.NOTES to Backup.LEFT_OUT,
        // Up to 300 recording URIs for the «recent» list.
        AppPrefs.Store.OWNED_RECORDINGS to Backup.LEFT_OUT,
        // Base shift per mode, last mode, timbre.
        AppPrefs.Store.EIGHT_MODES to Backup.KEPT,
        // Expected melodies per hymn — the user's own work.
        AppPrefs.Store.RECORDING_ANALYSIS to Backup.KEPT,
        // The practice history behind the streak, and the reminder's time — the user's own history.
        AppPrefs.Store.PRACTICE to Backup.KEPT,
    )

    /** Everything else that stays on the device, by the name the code gives it on disk. */
    private val otherExclusions = listOf(
        // The recordings index: rebuilt from the folder.
        Rule("database", "recordings_index.db"),
        Rule("database", "recordings_index.db-wal"),
        Rule("database", "recordings_index.db-shm"),
        Rule("database", "recordings_index.db-journal"),
        // Notes snapshots still waiting for the folder; notes.db holds the same notes.
        Rule("file", "notes_pending_snapshots"),
        // Raw recording audio (ClickUp 869f5x26x).
        Rule("file", "recordings_capture"),
        Rule("file", "recordings_pending"),
    )

    private val decidedExclusions: Set<Rule> =
        (otherExclusions + storeDecisions.filterValues { it == Backup.LEFT_OUT }.keys.map(::prefsRule)).toSet()

    private fun prefsRule(store: AppPrefs.Store) = Rule("sharedpref", "${store.fileName}.xml")

    // ---- the files --------------------------------------------------------------------------------

    private val resDir: File by lazy {
        listOf(File("app/src/main/res"), File("src/main/res")).firstOrNull { it.isDirectory }
            ?: error("Cannot locate the resources from ${File("").absolutePath}")
    }

    private fun root(fileName: String): Element =
        DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(resDir, "xml/$fileName")).documentElement

    private fun section(parent: Element, tag: String): Element {
        val found = parent.getElementsByTagName(tag)
        assertEquals("exactly one <$tag> in <${parent.tagName}>", 1, found.length)
        return found.item(0) as Element
    }

    private fun rules(set: Element, tag: String): Set<Rule> {
        val found = set.getElementsByTagName(tag)
        return (0 until found.length).map { found.item(it) as Element }
            .map { Rule(it.getAttribute("domain"), it.getAttribute("path")) }
            .toSet()
    }

    /** The three rule sets Android reads, by the Android versions that read them. */
    private val ruleSets: Map<String, Element> by lazy {
        val modern = root("data_extraction_rules.xml")
        mapOf(
            "Android 7-11 (backup_rules.xml)" to root("backup_rules.xml").also {
                assertEquals("full-backup-content", it.tagName)
            },
            "Android 12+ cloud backup" to section(modern, "cloud-backup"),
            "Android 12+ device transfer" to section(modern, "device-transfer"),
        )
    }

    // ---- the decisions ----------------------------------------------------------------------------

    @Test
    fun everyPreferencesStoreHasABackupDecision() {
        assertEquals(AppPrefs.Store.entries.toSet(), storeDecisions.keys)
    }

    @Test
    fun everyRuleSetExcludesExactlyTheDecidedItems() {
        ruleSets.forEach { (name, set) ->
            assertEquals(name, decidedExclusions, rules(set, "exclude"))
        }
    }

    @Test
    fun noRuleSetIncludesAnything() {
        ruleSets.forEach { (name, set) ->
            assertEquals("$name: an <include> would drop everything else, the notes too", emptySet<Rule>(), rules(set, "include"))
        }
    }

    @Test
    fun theNotesAndTheKeptPreferencesAreNeverLeftOut() {
        val kept = listOf("notes.db", "notes.db-wal", "notes.db-shm").map { Rule("database", it) } +
            storeDecisions.filterValues { it == Backup.KEPT }.keys.map(::prefsRule)
        ruleSets.forEach { (name, set) ->
            val excluded = rules(set, "exclude")
            kept.forEach { rule ->
                assertTrue("$name leaves out $rule", excluded.none { it.domain == rule.domain && coversPath(it.path, rule.path) })
            }
        }
    }

    /** An exclude of a directory, or of ".", covers everything beneath it. */
    private fun coversPath(excluded: String, path: String): Boolean =
        excluded == "." || excluded == path || path.startsWith("$excluded/")

    @Test
    fun theExcludedNamesAreTheOnesTheCodeWrites() {
        // A rename in the code would leave the rule behind and back the renamed file up after all.
        val code = KotlinSource.mainRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { KotlinSource.withoutComments(it.readText()) }
        val onDisk = otherExclusions.map { it.path.removeSuffix("-wal").removeSuffix("-shm").removeSuffix("-journal") }
            .toSet() + "notes.db"
        onDisk.forEach { name ->
            assertTrue("no code writes \"$name\" any more", code.contains("\"$name\""))
        }
    }

    @Test
    fun theManifestUsesTheseRuleFiles() {
        val application = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(resDir.parentFile, "AndroidManifest.xml"))
            .documentElement.getElementsByTagName("application").item(0) as Element
        assertEquals("true", application.getAttribute("android:allowBackup"))
        assertEquals("@xml/backup_rules", application.getAttribute("android:fullBackupContent"))
        assertEquals("@xml/data_extraction_rules", application.getAttribute("android:dataExtractionRules"))
    }
}
