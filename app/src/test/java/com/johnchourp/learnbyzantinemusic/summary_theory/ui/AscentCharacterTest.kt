package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AscentCharacterTest {

    @Test
    fun characters_are_ordered_with_the_expected_voice_counts() {
        assertEquals(
            listOf(
                AscentCharacter.ISON,
                AscentCharacter.OLIGON,
                AscentCharacter.PETASTI,
                AscentCharacter.KENTIMATA,
                AscentCharacter.KENTIMA,
                AscentCharacter.YPSILI,
            ),
            AscentCharacter.all,
        )
        assertEquals(listOf(0, 1, 1, 1, 2, 4), AscentCharacter.all.map { it.voices })
    }

    @Test
    fun maxVoices_is_the_highest_simple_rise() {
        assertEquals(4, AscentCharacter.maxVoices)
        assertEquals(AscentCharacter.YPSILI.voices, AscentCharacter.maxVoices)
    }

    @Test
    fun only_petasti_and_kentimata_carry_a_definition() {
        val withDefinition = AscentCharacter.all.filter { it.hasDefinition }
        assertEquals(listOf(AscentCharacter.PETASTI, AscentCharacter.KENTIMATA), withDefinition)
    }

    @Test
    fun ison_does_not_raise_the_voice() {
        assertEquals(0, AscentCharacter.ISON.voices)
        assertTrue(AscentCharacter.all.filter { it.voices > 0 }.all { it != AscentCharacter.ISON })
    }
}
