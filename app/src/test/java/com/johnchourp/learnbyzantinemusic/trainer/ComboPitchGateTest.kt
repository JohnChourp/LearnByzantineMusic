package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.IntonationProfile
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * These used to pass `toleranceMoria = 4.0`, the Trainer's own tolerance at the time. Since ClickUp
 * `869f5x28t` (H1) there is one tolerance for every screen, [IntonationProfile.IN_TUNE_MORIA] (±3),
 * so the tests use the default: they pin what the Trainer actually does, not a number it has dropped.
 */
class ComboPitchGateTest {

    @Test
    fun `in-tune pitch returns its phthong`() {
        val match = PitchMatch(PhthongName.DI, deviationMoria = 1.5)
        assertEquals(PhthongName.DI, ComboPitchGate.inTunePhthong(match))
    }

    @Test
    fun `out-of-tune pitch returns null`() {
        val match = PitchMatch(PhthongName.DI, deviationMoria = 6.0)
        assertNull(ComboPitchGate.inTunePhthong(match))
    }

    @Test
    fun `flat pitch just inside tolerance still returns its phthong`() {
        // Exactly on the boundary, which is inclusive: -3 now, where it was -4.
        val match = PitchMatch(PhthongName.KE, deviationMoria = -IntonationProfile.IN_TUNE_MORIA)
        assertEquals(PhthongName.KE, ComboPitchGate.inTunePhthong(match))
    }

    @Test
    fun `silence returns null`() {
        assertNull(ComboPitchGate.inTunePhthong(null))
    }
}
