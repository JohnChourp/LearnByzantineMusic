package com.johnchourp.learnbyzantinemusic.trainer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ComboPitchGateTest {

    @Test
    fun `in-tune pitch returns its phthong`() {
        val match = PitchMatch(TrainerPhthong.DI, deviationMoria = 1.5)
        assertEquals(TrainerPhthong.DI, ComboPitchGate.inTunePhthong(match, toleranceMoria = 4.0))
    }

    @Test
    fun `out-of-tune pitch returns null`() {
        val match = PitchMatch(TrainerPhthong.DI, deviationMoria = 6.0)
        assertNull(ComboPitchGate.inTunePhthong(match, toleranceMoria = 4.0))
    }

    @Test
    fun `flat pitch just inside tolerance still returns its phthong`() {
        val match = PitchMatch(TrainerPhthong.KE, deviationMoria = -4.0)
        assertEquals(TrainerPhthong.KE, ComboPitchGate.inTunePhthong(match, toleranceMoria = 4.0))
    }

    @Test
    fun `silence returns null`() {
        assertNull(ComboPitchGate.inTunePhthong(null))
    }
}
