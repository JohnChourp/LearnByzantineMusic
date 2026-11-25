package com.johnchourp.learnbyzantinemusic.recordings.session

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which existing folder a recording with a match prefix is saved into (ClickUp `1amndjnkdym`). A
 * folder named only by the code (`01`) is the hymn's folder too, so the next recording goes into it
 * instead of creating a second folder beside it.
 */
class RecordingTargetMatchTest {

    @Test
    fun aFolderNamedOnlyByTheCodeMatches() {
        assertTrue(RecordingTarget.matchesPrefix("01", "01 "))
    }

    @Test
    fun aFolderStartingWithThePrefixMatches() {
        assertTrue(RecordingTarget.matchesPrefix("01 Κύριε ἐκέκραξα", "01 "))
        assertTrue(RecordingTarget.matchesPrefix("01 my own name", "01 "))
        assertTrue(RecordingTarget.matchesPrefix("01 ", "01 "))
    }

    @Test
    fun otherFoldersDoNotMatch() {
        assertFalse(RecordingTarget.matchesPrefix("010 other", "01 "))
        assertFalse(RecordingTarget.matchesPrefix("011", "01 "))
        assertFalse(RecordingTarget.matchesPrefix("0", "01 "))
        assertFalse(RecordingTarget.matchesPrefix("", "01 "))
        assertFalse(RecordingTarget.matchesPrefix("10 Κυκλώσατε", "01 "))
        assertFalse(RecordingTarget.matchesPrefix(null, "01 "))
    }

    /** A blank prefix (none given) never matches every folder. */
    @Test
    fun aBlankPrefixMatchesNothing() {
        assertFalse(RecordingTarget.matchesPrefix("01", ""))
        assertFalse(RecordingTarget.matchesPrefix("", ""))
        assertFalse(RecordingTarget.matchesPrefix(" ", " "))
    }
}
