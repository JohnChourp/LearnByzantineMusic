package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.ByzantineTuning
import com.johnchourp.learnbyzantinemusic.music.PhthongName

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class TrainerPitchMatchTest {

    @Test
    fun `exact phthong frequencies match with no deviation`() {
        for (phthong in PhthongName.entries) {
            val match = TrainerPitchTable.nearestPhthong(TrainerPitchTable.frequencyHz(phthong))
            assertEquals(phthong, match?.phthong)
            assertTrue("deviation ${match?.deviationMoria}", abs(match!!.deviationMoria) < 1e-6)
        }
    }

    @Test
    fun `octaves fold onto the same phthong`() {
        assertEquals(PhthongName.NI, TrainerPitchTable.nearestPhthong(440.0)?.phthong)
        assertEquals(PhthongName.NI, TrainerPitchTable.nearestPhthong(110.0)?.phthong)
        assertEquals(PhthongName.DI, TrainerPitchTable.nearestPhthong(TrainerPitchTable.frequencyHz(PhthongName.DI, 1))?.phthong)
    }

    @Test
    fun `a slightly sharp pitch reports positive moria deviation`() {
        // 2 moria above Νη.
        val twoMoriaSharp = ByzantineTuning.frequencyHz(2.0)
        val match = TrainerPitchTable.nearestPhthong(twoMoriaSharp)
        assertEquals(PhthongName.NI, match?.phthong)
        assertEquals(2.0, match!!.deviationMoria, 0.05)
    }

    @Test
    fun `invalid frequencies return null`() {
        assertNull(TrainerPitchTable.nearestPhthong(0.0))
        assertNull(TrainerPitchTable.nearestPhthong(-100.0))
        assertNull(TrainerPitchTable.nearestPhthong(Double.NaN))
        assertNull(TrainerPitchTable.nearestPhthong(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `moria converts to cents`() {
        assertEquals(16.667, TrainerPitchTable.moriaToCents(1.0), 0.01)
        assertEquals(1200.0, TrainerPitchTable.moriaToCents(72.0), 1e-6)
    }
}
