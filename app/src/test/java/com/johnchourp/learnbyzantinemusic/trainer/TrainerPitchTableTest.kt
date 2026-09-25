package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainerPitchTableTest {

    @Test
    fun `base ni is 220 hz`() {
        assertEquals(220.0, TrainerPitchTable.frequencyHz(PhthongName.NI), 1e-6)
    }

    @Test
    fun `octave shifts double and halve the frequency`() {
        assertEquals(440.0, TrainerPitchTable.frequencyHz(PhthongName.NI, 1), 1e-6)
        assertEquals(110.0, TrainerPitchTable.frequencyHz(PhthongName.NI, -1), 1e-6)
    }

    @Test
    fun `diatonic phthongi land on expected frequencies`() {
        // 220 * 2^(moria/72) for the diatonic moria positions.
        assertEquals(246.94, TrainerPitchTable.frequencyHz(PhthongName.PA), 0.05)
        assertEquals(293.66, TrainerPitchTable.frequencyHz(PhthongName.GA), 0.05)
        assertEquals(329.63, TrainerPitchTable.frequencyHz(PhthongName.DI), 0.05)
        assertEquals(369.99, TrainerPitchTable.frequencyHz(PhthongName.KE), 0.05)
    }

    @Test
    fun `frequencies increase strictly across the ascending phthongi`() {
        val frequencies = PhthongName.entries.map { TrainerPitchTable.frequencyHz(it) }
        for (i in 1 until frequencies.size) {
            assertTrue(
                "expected ${frequencies[i]} > ${frequencies[i - 1]}",
                frequencies[i] > frequencies[i - 1]
            )
        }
        // Ζω stays below the next octave Νη'.
        assertTrue(frequencies.last() < TrainerPitchTable.frequencyHz(PhthongName.NI, 1))
    }
}
