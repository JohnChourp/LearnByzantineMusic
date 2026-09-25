package com.johnchourp.learnbyzantinemusic.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The names time signs are stored under (ClickUp `869f5x29r`, H5). Nothing stores a melody yet, but
 * the Melody Trainer has always named γοργόν `gorgo` and κλάσμα `fraction`, and saving melodies (F6)
 * will write those names: once written, a name can never change meaning.
 */
class TimeSignIdsTest {

    @Test
    fun theTrainersNamesStillReadBack() {
        assertEquals(TimeSign.GORGON, TimeSign.fromId("gorgo"))
        assertEquals(TimeSign.KLASMA, TimeSign.fromId("fraction"))
    }

    @Test
    fun everySignHasItsOwnNameAndReadsBack() {
        assertEquals("no two signs share a name", TimeSign.entries.size, TimeSign.entries.map { it.id }.toSet().size)
        TimeSign.entries.forEach { assertEquals(it, TimeSign.fromId(it.id)) }
    }

    @Test
    fun aNameTheAppDoesNotKnowReadsAsNoSign() {
        // «antikeno» belonged to a rule the removed notation scanner used; nothing ever produced it.
        assertNull(TimeSign.fromId("antikeno"))
        assertNull(TimeSign.fromId("GORGO"))
        assertNull(TimeSign.fromId(""))
    }
}
