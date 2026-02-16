package com.johnchourp.learnbyzantinemusic.analysis

import org.junit.Assert.assertEquals
import org.junit.Test

class ByzantineRhythmMapperTest {
    @Test
    fun `applies fraction and antikeno_apli and gorgo redistribution`() {
        val mapper = ByzantineRhythmMapper()

        val inputs = listOf(
            NeumeRhythmInput(modifiers = emptyList(), baseDurationBeats = 1f),
            NeumeRhythmInput(modifiers = emptyList(), baseDurationBeats = 1f),
            NeumeRhythmInput(modifiers = emptyList(), baseDurationBeats = 1f),
            NeumeRhythmInput(modifiers = emptyList(), baseDurationBeats = 1f),
            NeumeRhythmInput(modifiers = listOf("fraction"), baseDurationBeats = 1f),
            NeumeRhythmInput(modifiers = listOf("fraction"), baseDurationBeats = 1f),
            NeumeRhythmInput(modifiers = listOf("antikeno", "apli"), baseDurationBeats = 1f),
            NeumeRhythmInput(modifiers = listOf("gorgo"), baseDurationBeats = 1f)
        )

        val durations = mapper.map(inputs)

        assertEquals(listOf(1f, 1f, 1f, 1f, 2f, 2f, 1.5f, 0.5f), durations)
    }
}
