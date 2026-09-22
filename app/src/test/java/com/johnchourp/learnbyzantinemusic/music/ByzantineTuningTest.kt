package com.johnchourp.learnbyzantinemusic.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the frequency formula to known μόρια→Hz pairs, including octaves far from the reference,
 * and proves the round trip is exact.
 */
class ByzantineTuningTest {

    private val tolerance = 1e-9

    @Test
    fun niSoundsAtTheReferenceFrequency() {
        assertEquals(220.0, ByzantineTuning.frequencyHz(0), tolerance)
    }

    @Test
    fun oneOctaveUpDoublesAndOneOctaveDownHalves() {
        assertEquals(440.0, ByzantineTuning.frequencyHz(72), tolerance)
        assertEquals(110.0, ByzantineTuning.frequencyHz(-72), tolerance)
    }

    @Test
    fun extremeOctavesStayOnThePowerOfTwo() {
        // The scale diagram spans three octaves; these are past its edges on both sides.
        assertEquals(220.0 * 8, ByzantineTuning.frequencyHz(3 * 72), tolerance)
        assertEquals(220.0 / 8, ByzantineTuning.frequencyHz(-3 * 72), tolerance)
    }

    @Test
    fun knownDiatonicPhthongiMatchTheirPublishedFrequencies() {
        // Νη 0, Πα 12, Βου 22, Γα 30, Δι 42, Κε 54, Ζω 64 μόρια above Νη.
        val expected = mapOf(
            0 to 220.000000,
            12 to 246.941651,
            22 to 271.896783,
            30 to 293.664768,
            42 to 329.627557,
            54 to 369.994423,
            64 to 407.384873,
        )
        expected.forEach { (moria, hz) ->
            assertEquals("μόρια=$moria", hz, ByzantineTuning.frequencyHz(moria), 1e-6)
        }
    }

    @Test
    fun halfMoriaResolutionIsSupported() {
        val half = ByzantineTuning.frequencyHz(0.5)
        assertTrue(half > 220.0 && half < ByzantineTuning.frequencyHz(1))
    }

    @Test
    fun moriaFromNiInvertsFrequencyHz() {
        listOf(-80.0, -12.0, 0.0, 7.5, 42.0, 144.0).forEach { moria ->
            assertEquals(moria, ByzantineTuning.moriaFromNi(ByzantineTuning.frequencyHz(moria)), 1e-9)
        }
    }

    @Test
    fun moriaTranslateToCentsAtSixteenAndTwoThirdsEach() {
        assertEquals(1200.0, ByzantineTuning.moriaToCents(72.0), tolerance)
        assertEquals(100.0, ByzantineTuning.moriaToCents(6.0), tolerance)
    }
}
