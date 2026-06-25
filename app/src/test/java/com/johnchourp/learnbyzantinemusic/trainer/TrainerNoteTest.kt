package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.analysis.ByzantineRhythmMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainerNoteTest {

    @Test
    fun `note exposes its phthong frequency`() {
        assertEquals(220.0, TrainerNote(TrainerPhthong.NI).frequencyHz, 1e-6)
        assertEquals(440.0, TrainerNote(TrainerPhthong.NI, octaveShift = 1).frequencyHz, 1e-6)
    }

    @Test
    fun `gorgo toggles on and off without duplicating modifiers`() {
        val base = TrainerNote(TrainerPhthong.PA)
        assertFalse(base.hasGorgo)

        val withGorgo = base.withGorgo(true)
        assertTrue(withGorgo.hasGorgo)
        assertEquals(listOf(ByzantineRhythmMapper.MODIFIER_GORGO), withGorgo.modifiers)

        // Toggling on again is idempotent.
        assertEquals(withGorgo, withGorgo.withGorgo(true))

        val withoutGorgo = withGorgo.withGorgo(false)
        assertFalse(withoutGorgo.hasGorgo)
        assertEquals(emptyList<String>(), withoutGorgo.modifiers)
    }

    @Test
    fun `fraction modifier is tracked independently`() {
        val note = TrainerNote(TrainerPhthong.DI).withFraction(true)
        assertTrue(note.hasFraction)
        assertFalse(note.hasGorgo)
        assertTrue(note.modifiers.contains(ByzantineRhythmMapper.MODIFIER_FRACTION))
    }
}
