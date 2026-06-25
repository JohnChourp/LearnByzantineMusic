package com.johnchourp.learnbyzantinemusic.trainer

import org.junit.Assert.assertEquals
import org.junit.Test

class MelodySequenceTest {

    private fun note(phthong: TrainerPhthong, beats: Float = 1f) =
        TrainerNote(phthong = phthong, baseDurationBeats = beats)

    @Test
    fun `plain notes keep their base durations`() {
        val sequence = MelodySequence(listOf(note(TrainerPhthong.NI), note(TrainerPhthong.PA, 2f)))
        assertEquals(listOf(1f, 2f), sequence.effectiveDurationsBeats())
        assertEquals(3f, sequence.totalBeats())
    }

    @Test
    fun `gorgo halves its note and shortens the previous one`() {
        val sequence = MelodySequence(
            listOf(
                note(TrainerPhthong.NI),
                note(TrainerPhthong.PA).withGorgo(true)
            )
        )
        // Two phthongi share one χρόνο: previous shrinks to 0.5, gorgo note is 0.5.
        assertEquals(listOf(0.5f, 0.5f), sequence.effectiveDurationsBeats())
        assertEquals(1f, sequence.totalBeats())
    }

    @Test
    fun `fraction adds a whole beat`() {
        val sequence = MelodySequence(listOf(note(TrainerPhthong.DI).withFraction(true)))
        assertEquals(listOf(2f), sequence.effectiveDurationsBeats())
        assertEquals(2f, sequence.totalBeats())
    }

    @Test
    fun `empty sequence has no durations`() {
        val sequence = MelodySequence(emptyList())
        assertEquals(emptyList<Float>(), sequence.effectiveDurationsBeats())
        assertEquals(0f, sequence.totalBeats())
        assertEquals(0, sequence.size)
    }
}
