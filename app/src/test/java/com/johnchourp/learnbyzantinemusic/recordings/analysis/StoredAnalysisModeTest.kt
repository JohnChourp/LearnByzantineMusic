package com.johnchourp.learnbyzantinemusic.recordings.analysis

import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.recordings.analysis.AnalysisSettingsStore.Companion.resolveMode
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The mode a phthong analysis opens on, read back from a preference that anyone — an old version,
 * a restore, a hand edit — may have left holding anything (ClickUp `869f5x299`).
 *
 * It used to reach the scale lookup as a raw key and turn silently into the diatonic scale from Νη.
 * Now it is parsed at `Mode.fromKey`, and a key that names no mode falls back explicitly, never by
 * throwing: the analysis must still open.
 */
class StoredAnalysisModeTest {

    @Test
    fun theStoredModeWins() {
        assertEquals(Mode.VARYS, resolveMode(stored = "varys", requested = "third"))
    }

    @Test
    fun withNothingStoredItOpensTheModeItWasOpenedWith() {
        assertEquals(Mode.THIRD, resolveMode(stored = null, requested = "third"))
    }

    @Test
    fun aCorruptedStoredModeIsSkippedNotThrownOn() {
        listOf("", "xyz", "First", " first", "first ", "Ήχος Α΄", "0").forEach { corrupted ->
            assertEquals("«$corrupted» with a hymn's mode", Mode.THIRD, resolveMode(stored = corrupted, requested = "third"))
            assertEquals("«$corrupted» alone", Mode.FIRST, resolveMode(stored = corrupted, requested = null))
        }
    }

    @Test
    fun withNothingUsableItOpensOnTheFirstModeAsBefore() {
        assertEquals(Mode.FIRST, resolveMode(stored = null, requested = null))
        assertEquals(Mode.FIRST, resolveMode(stored = "xyz", requested = "abc"))
    }
}
