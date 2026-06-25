package com.johnchourp.learnbyzantinemusic.trainer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class TrainerPitchMatchTest {

    @Test
    fun `exact phthong frequencies match with no deviation`() {
        for (phthong in TrainerPhthong.ascending) {
            val match = TrainerPitchTable.nearestPhthong(TrainerPitchTable.frequencyHz(phthong))
            assertEquals(phthong, match?.phthong)
            assertTrue("deviation ${match?.deviationMoria}", abs(match!!.deviationMoria) < 1e-6)
        }
    }

    @Test
    fun `octaves fold onto the same phthong`() {
        assertEquals(TrainerPhthong.NI, TrainerPitchTable.nearestPhthong(440.0)?.phthong)
        assertEquals(TrainerPhthong.NI, TrainerPitchTable.nearestPhthong(110.0)?.phthong)
        assertEquals(TrainerPhthong.DI, TrainerPitchTable.nearestPhthong(TrainerPitchTable.frequencyHz(TrainerPhthong.DI, 1))?.phthong)
    }

    @Test
    fun `a slightly sharp pitch reports positive moria deviation`() {
        // 2 moria above Νη.
        val twoMoriaSharp = TrainerPitchTable.BASE_NI_FREQUENCY_HZ * Math.pow(2.0, 2.0 / 72.0)
        val match = TrainerPitchTable.nearestPhthong(twoMoriaSharp)
        assertEquals(TrainerPhthong.NI, match?.phthong)
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
