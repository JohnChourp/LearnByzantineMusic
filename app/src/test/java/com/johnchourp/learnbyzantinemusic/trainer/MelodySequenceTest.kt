package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.Beats
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.music.TimeSign
import org.junit.Assert.assertEquals
import org.junit.Test

class MelodySequenceTest {

    private fun note(phthong: PhthongName, beats: Float = 1f) =
        TrainerNote(phthong = phthong, baseDurationBeats = beats)

    private val half = Beats.of(1, 2)

    @Test
    fun `plain notes keep their base durations`() {
        val sequence = MelodySequence(listOf(note(PhthongName.NI), note(PhthongName.PA, 2f)))
        assertEquals(listOf(Beats.ONE, Beats.whole(2)), sequence.durations())
        assertEquals(Beats.whole(3), sequence.total())
    }

    @Test
    fun `gorgo halves its note and shortens the previous one`() {
        val sequence = MelodySequence(
            listOf(
                note(PhthongName.NI),
                note(PhthongName.PA).withGorgo(true)
            )
        )
        // Two phthongi share one χρόνο: previous shrinks to 0.5, gorgo note is 0.5.
        assertEquals(listOf(half, half), sequence.durations())
        assertEquals(Beats.ONE, sequence.total())
    }

    @Test
    fun `fraction adds a whole beat`() {
        val sequence = MelodySequence(listOf(note(PhthongName.DI).withSign(TimeSign.KLASMA, true)))
        assertEquals(listOf(Beats.whole(2)), sequence.durations())
        assertEquals(Beats.whole(2), sequence.total())
    }

    @Test
    fun `empty sequence has no durations`() {
        val sequence = MelodySequence(emptyList())
        assertEquals(emptyList<Beats>(), sequence.durations())
        assertEquals(Beats.ZERO, sequence.total())
        assertEquals(0, sequence.size)
    }
}
