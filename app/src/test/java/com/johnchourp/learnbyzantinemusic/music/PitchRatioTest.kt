package com.johnchourp.learnbyzantinemusic.music

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.pow

/**
 * [ByzantineTuning.ratioForMoria], the factor behind the in-app player's «μετατόπιση» (ClickUp
 * `869f5x268`): an octave is 72 μόρια, so +72 doubles the frequency, −72 halves it and 0 leaves it —
 * from the one tuning source, never a second copy of the 72. And [ByzantineTuning.frequencyHz] is Νη
 * times the same factor, to the last bit, so the player and the scale can never disagree.
 */
class PitchRatioTest {

    @Test
    fun anOctaveOfMoriaDoublesTheFrequency() {
        assertEquals(2.0, ByzantineTuning.ratioForMoria(72.0), 1e-12)
    }

    @Test
    fun anOctaveDownHalvesIt() {
        assertEquals(0.5, ByzantineTuning.ratioForMoria(-72.0), 1e-12)
    }

    @Test
    fun noShiftLeavesItAlone() {
        assertEquals(1.0, ByzantineTuning.ratioForMoria(0.0), 0.0)
    }

    @Test
    fun twelveMoriaIsASixthOfAnOctave() {
        assertEquals(2.0.pow(1.0 / 6.0), ByzantineTuning.ratioForMoria(12.0), 1e-12)
        assertEquals(1.0 / 2.0.pow(1.0 / 6.0), ByzantineTuning.ratioForMoria(-12.0), 1e-12)
    }

    @Test
    fun theFrequencyOfAPitchIsNiTimesItsRatio() {
        for (moria in -216..216 step 7) {
            assertEquals(
                "at $moria μόρια",
                ByzantineTuning.NI_BASE_HZ * ByzantineTuning.ratioForMoria(moria.toDouble()),
                ByzantineTuning.frequencyHz(moria),
                0.0,
            )
        }
    }
}
