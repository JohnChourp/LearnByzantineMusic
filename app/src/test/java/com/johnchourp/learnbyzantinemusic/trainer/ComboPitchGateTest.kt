package com.johnchourp.learnbyzantinemusic.trainer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComboPitchGateTest {

    @Test
    fun `correct phthong in tune passes`() {
        val match = PitchMatch(TrainerPhthong.DI, deviationMoria = 1.5)
        assertTrue(ComboPitchGate.isCorrectPhthong(match, TrainerPhthong.DI, toleranceMoria = 4.0))
    }

    @Test
    fun `wrong phthong fails even if perfectly in tune`() {
        val match = PitchMatch(TrainerPhthong.PA, deviationMoria = 0.0)
        assertFalse(ComboPitchGate.isCorrectPhthong(match, TrainerPhthong.DI, toleranceMoria = 4.0))
    }

    @Test
    fun `right phthong but out of tune fails`() {
        val match = PitchMatch(TrainerPhthong.DI, deviationMoria = 6.0)
        assertFalse(ComboPitchGate.isCorrectPhthong(match, TrainerPhthong.DI, toleranceMoria = 4.0))
    }

    @Test
    fun `silence or missing target fails`() {
        assertFalse(ComboPitchGate.isCorrectPhthong(null, TrainerPhthong.DI))
        assertFalse(ComboPitchGate.isCorrectPhthong(PitchMatch(TrainerPhthong.DI, 0.0), null))
    }
}
