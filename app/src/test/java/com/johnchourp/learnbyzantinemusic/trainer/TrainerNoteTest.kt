package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.music.TimeSign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainerNoteTest {

    @Test
    fun `note exposes its phthong frequency`() {
        assertEquals(220.0, TrainerNote(PhthongName.NI).frequencyHz, 1e-6)
        assertEquals(440.0, TrainerNote(PhthongName.NI, octaveShift = 1).frequencyHz, 1e-6)
    }

    @Test
    fun `gorgo toggles on and off without duplicating modifiers`() {
        val base = TrainerNote(PhthongName.PA)
        assertFalse(base.hasGorgo)

        val withGorgo = base.withGorgo(true)
        assertTrue(withGorgo.hasGorgo)
        assertEquals(setOf(TimeSign.GORGON), withGorgo.signs)

        // Toggling on again is idempotent.
        assertEquals(withGorgo, withGorgo.withGorgo(true))

        val withoutGorgo = withGorgo.withGorgo(false)
        assertFalse(withoutGorgo.hasGorgo)
        assertEquals(emptySet<TimeSign>(), withoutGorgo.signs)
    }

    @Test
    fun `fraction modifier is tracked independently`() {
        val note = TrainerNote(PhthongName.DI).withSign(TimeSign.KLASMA, true)
        assertTrue(TimeSign.KLASMA in note.signs)
        assertFalse(note.hasGorgo)
        assertEquals(setOf(TimeSign.KLASMA, TimeSign.GORGON), note.withGorgo(true).signs)
    }
}
