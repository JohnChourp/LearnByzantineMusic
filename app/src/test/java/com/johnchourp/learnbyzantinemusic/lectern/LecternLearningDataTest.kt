package com.johnchourp.learnbyzantinemusic.lectern

import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.Phthong
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile.Reason
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile.Result.Accepted
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile.Result.Rejected
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The lectern in the «Δεδομένα μάθησης» file (ClickUp `869f5x2e7` in F5's `869f5x25w`): each PDF's
 * page → ήχος map travels, named by the file's SHA-256; the library of URIs never does; and an import
 * writes only the PDFs it carries, so the maps a new phone already has are kept.
 */
class LecternLearningDataTest {

    private val exportedAt = 1_758_800_000_000L
    private val pagesStore = "learn_byzantine_music_lectern_pages"

    private val anthology = LecternFileKey.sha256Hex("%PDF anthology".byteInputStream())
    private val doxastarion = LecternFileKey.sha256Hex("%PDF doxastarion".byteInputStream())

    private val anthologyMap = LecternPagesCodec.encode(
        listOf(
            PageAssignment(0, Mode.FIRST, 0),
            PageAssignment(12, Mode.PLAGAL_FOURTH, -2, Phthong(PhthongName.GA)),
        )
    )
    private val doxastarionMap = LecternPagesCodec.encode(listOf(PageAssignment(3, Mode.SECOND, 4)))

    private val library = LecternLibrary(
        listOf(LibraryEntry("content://com.android.providers.downloads.documents/document/42", "Ανθολογία.pdf", 11, exportedAt))
    ).encode()

    private fun accepted(file: String): Accepted {
        val result = LearningDataFile.decode(file)
        assertTrue("expected the file to be accepted, got $result", result is Accepted)
        return result as Accepted
    }

    private fun fileWith(name: String, value: Any): String =
        JSONObject()
            .put("schemaVersion", 1)
            .put("exportedAt", exportedAt)
            .put("appVersion", "1.17.0")
            .put("stores", JSONObject().put(pagesStore, JSONObject().put(name, JSONObject().put("type", "STRING").put("value", value))))
            .toString()

    @Test
    fun `the page maps travel and come back exactly, while the library of URIs stays behind`() {
        val snapshot = mapOf(
            AppPrefs.Store.LECTERN_PAGES to mapOf(
                AppPrefs.lecternPagesKeyName(anthology) to anthologyMap,
                AppPrefs.lecternPagesKeyName(doxastarion) to doxastarionMap,
            ),
            AppPrefs.Store.LECTERN_LIBRARY to mapOf(AppPrefs.LecternLibraryEntries.name to library),
        )

        val file = LearningDataFile.encode(snapshot, exportedAt, "1.17.0")

        assertFalse("a URI reached the file", "content://" in file)
        assertFalse("the library reached the file", "lectern_library" in file)
        assertEquals(
            mapOf(
                AppPrefs.Store.LECTERN_PAGES to mapOf(
                    AppPrefs.lecternPagesKeyName(anthology) to anthologyMap,
                    AppPrefs.lecternPagesKeyName(doxastarion) to doxastarionMap,
                )
            ),
            accepted(file).changes,
        )
    }

    @Test
    fun `an import writes only the PDFs it carries, so a map only the new phone has is kept`() {
        val onlyTheAnthology = LearningDataFile.encode(
            mapOf(AppPrefs.Store.LECTERN_PAGES to mapOf(AppPrefs.lecternPagesKeyName(anthology) to anthologyMap)),
            exportedAt,
            "1.17.0",
        )
        val commits = mutableListOf<Pair<AppPrefs.Store, Map<String, Any>>>()

        LearningDataFile.write(accepted(onlyTheAnthology), LearningDataFile.Writer { store, values -> commits += store to values; true })

        assertEquals(listOf(AppPrefs.Store.LECTERN_PAGES to mapOf(AppPrefs.lecternPagesKeyName(anthology) to anthologyMap)), commits)
    }

    @Test
    fun `the dialog counts PDFs`() {
        val file = LearningDataFile.encode(
            mapOf(
                AppPrefs.Store.LECTERN_PAGES to mapOf(
                    AppPrefs.lecternPagesKeyName(anthology) to anthologyMap,
                    AppPrefs.lecternPagesKeyName(doxastarion) to doxastarionMap,
                )
            ),
            exportedAt,
            "1.17.0",
        )
        assertEquals(
            listOf(LearningDataFile.Line(LearningDataFile.Item.LECTERN_PAGES, 2)),
            LearningDataFile.summary(accepted(file)),
        )
    }

    @Test
    fun `a stored map the app cannot use is not exported, and a messy one is exported clean`() {
        val messy = JSONObject(anthologyMap).toString(2)
        val unusable = mapOf(
            AppPrefs.lecternPagesKeyName(doxastarion) to "not a map",
            AppPrefs.lecternPagesKeyName("a".repeat(64)) to LecternPagesCodec.encode(emptyList()),
            AppPrefs.lecternPagesKeyName("b".repeat(64)) to anthologyMap.replace("\"schemaVersion\":1", "\"schemaVersion\":2"),
        )
        val file = LearningDataFile.encode(
            mapOf(AppPrefs.Store.LECTERN_PAGES to unusable + (AppPrefs.lecternPagesKeyName(anthology) to messy)),
            exportedAt,
            "1.17.0",
        )
        assertEquals(
            mapOf(AppPrefs.Store.LECTERN_PAGES to mapOf(AppPrefs.lecternPagesKeyName(anthology) to anthologyMap)),
            accepted(file).changes,
        )
    }

    @Test
    fun `a map named by anything but a SHA-256 is not importable`() {
        listOf(
            "lectern_pages_content://com.android.providers.downloads.documents/document/42",
            "lectern_pages_${anthology.uppercase()}",
            "lectern_pages_${anthology.dropLast(2)}",
            "lectern_library",
        ).forEach { name ->
            assertEquals(name, Rejected(Reason.NOT_IMPORTABLE, name), LearningDataFile.decode(fileWith(name, anthologyMap)))
        }
    }

    @Test
    fun `a map the app would not store rejects the whole file`() {
        val name = AppPrefs.lecternPagesKeyName(anthology)
        listOf<Any>(
            JSONObject(anthologyMap).toString(2),
            anthologyMap.replace("\"shiftMoria\":-2", "\"shiftMoria\":400"),
            anthologyMap.replace("\"schemaVersion\":1", "\"schemaVersion\":2"),
            LecternPagesCodec.encode(emptyList()),
            "not a map",
            42,
        ).forEach { value ->
            assertEquals("$value", Rejected(Reason.BAD_VALUE, name), LearningDataFile.decode(fileWith(name, value)))
        }
    }
}
