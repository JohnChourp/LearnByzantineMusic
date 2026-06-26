package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DescentCharacterTest {

    @Test
    fun characters_are_ordered_with_the_expected_voice_counts() {
        assertEquals(
            listOf(
                DescentCharacter.APOSTROPHOS,
                DescentCharacter.ELAFRON,
                DescentCharacter.YPORROI,
                DescentCharacter.CHAMILI,
            ),
            DescentCharacter.all,
        )
        assertEquals(listOf(1, 2, 2, 4), DescentCharacter.all.map { it.voices })
    }

    @Test
    fun maxVoices_is_the_deepest_simple_descent() {
        assertEquals(4, DescentCharacter.maxVoices)
        assertEquals(DescentCharacter.CHAMILI.voices, DescentCharacter.maxVoices)
    }

    @Test
    fun only_yporroi_carries_a_definition() {
        val withDefinition = DescentCharacter.all.filter { it.hasDefinition }
        assertEquals(listOf(DescentCharacter.YPORROI), withDefinition)
    }

    @Test
    fun every_simple_character_actually_descends() {
        // There is no zero descent — even the lightest character (Απόστροφος) lowers the voice.
        assertTrue(DescentCharacter.all.all { it.voices >= 1 })
    }

    @Test
    fun yporroi_descends_the_same_two_phthongs_as_elafron() {
        // The υπορροή is written as one neume but is worth two apostrophes, i.e. −2 — the same
        // depth as the ελαφρόν.
        assertEquals(DescentCharacter.ELAFRON.voices, DescentCharacter.YPORROI.voices)
        assertEquals(2, DescentCharacter.YPORROI.voices)
    }
}
