package com.johnchourp.learnbyzantinemusic.anastasimatarion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
