package com.johnchourp.learnbyzantinemusic.anastasimatarion

import com.johnchourp.learnbyzantinemusic.recordings.session.RecordingTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HymnFoldersTest {
    private val hymn = Hymn(code = "02", incipit = "Τὰς ἑσπερινὰς ἡμῶν εὐχάς", note = null)

    @Test
    fun hymnPathIsRootModeAndCodeWithIncipit() {
        assertEquals(
            listOf("Αναστασιματάριο", "Ήχος Α΄", "02 Τὰς ἑσπερινὰς ἡμῶν εὐχάς"),
            HymnFolders.pathSegments("first", hymn),
        )
    }

    @Test
    fun everyCatalogModeHasAFolderName() {
        assertEquals(AnastasimatarionLabels.MODE_ORDER.toSet(), HymnFolders.modeKeys)
        val names = AnastasimatarionLabels.MODE_ORDER.map { HymnFolders.modeFolder(it) }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun aRenamedFolderIsStillFoundByItsCode() {
        assertTrue(HymnFolders.isHymnFolder("02 Τὰς ἑσπερινὰς ἡμῶν εὐχάς", hymn))
        assertTrue(HymnFolders.isHymnFolder("02 my own name", hymn))
        assertTrue(HymnFolders.isHymnFolder("02", hymn))
        assertFalse(HymnFolders.isHymnFolder("020 other", hymn))
        assertFalse(HymnFolders.isHymnFolder("12 Κυκλώσατε", hymn))
        assertFalse(HymnFolders.isHymnFolder(null, hymn))
    }

    /** ClickUp `1amndjnkdym`: a recording saved for the hymn goes into every folder the hymn page shows. */
    @Test
    fun savingMatchesEveryFolderTheHymnPageShows() {
        val prefix = HymnFolders.hymnFolderPrefix(hymn)
        listOf("02", "02 Τὰς ἑσπερινὰς ἡμῶν εὐχάς", "02 my own name", "020 other", "12 Κυκλώσατε", "0", "").forEach { name ->
            assertEquals(name, HymnFolders.isHymnFolder(name, hymn), RecordingTarget.matchesPrefix(name, prefix))
        }
    }

    /** Two folders for the same hymn (e.g. `02` and `02 …`): both are the hymn's, none is hidden. */
    @Test
    fun everyFolderOfTheHymnIsKept() {
        val folders = listOf("02", "01 Κύριε ἐκέκραξα", "02 Τὰς ἑσπερινὰς ἡμῶν εὐχάς", "020 other", "02 my own name")
        assertEquals(
            listOf("02", "02 Τὰς ἑσπερινὰς ἡμῶν εὐχάς", "02 my own name"),
            HymnFolders.foldersOf(hymn, folders) { it },
        )
        assertEquals(emptyList<String>(), HymnFolders.foldersOf(hymn, listOf("03", "12")) { it })
    }

    /** ClickUp `1amndjnrekd`: the hymn page names the folder the next recording really goes into. */
    @Test
    fun theHymnPageShowsTheFolderThatReceivesTheNextRecording() {
        val generated = HymnFolders.hymnFolder(hymn)
        assertEquals("02", HymnFolders.savingFolderOf(hymn, listOf("01 Κύριε ἐκέκραξα", "02")) { it })
        assertEquals(generated, HymnFolders.savingFolderOf(hymn, listOf("02", generated)) { it })
        assertNull(HymnFolders.savingFolderOf(hymn, listOf("020 other", "12")) { it })
        assertEquals(
            listOf("Αναστασιματάριο", "Ήχος Α΄", "02 my own name"),
            HymnFolders.displaySegments("first", hymn, "02 my own name"),
        )
        assertEquals(HymnFolders.pathSegments("first", hymn), HymnFolders.displaySegments("first", hymn, null))
    }

    @Test
    fun sanitizeRemovesPathAndReservedCharacters() {
        assertEquals("a b c d e", HymnFolders.sanitize("a/b\\c:d?e"))
        assertEquals("Ὦ θαύματος", HymnFolders.sanitize("  Ὦ   θαύματος.. "))
        assertEquals("x y", HymnFolders.sanitize("x\"*<>|y"))
    }

    @Test
    fun sanitizeBoundsTheLength() {
        val long = "λ".repeat(200)
        assertEquals(60, HymnFolders.sanitize(long).length)
    }
}
