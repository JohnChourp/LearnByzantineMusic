package com.johnchourp.learnbyzantinemusic.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ClickUp `869f4tpxj`: μόρια as a value rather than a bare Int.
 *
 * The type earns its place by making `octave + moria` — which used to compile and produce a
 * plausible wrong pitch — impossible to write.
 */
class MoriaTest {

    @Test
    fun anOctaveIsTheSameSeventyTwoTheTuningDefines() {
        // Not a second copy of the number: if these ever diverged, the ladder and the frequency
        // formula would be measuring in different units.
        assertEquals(ByzantineTuning.MORIA_PER_OCTAVE, Moria.OCTAVE.value)
        assertEquals(72, Moria.OCTAVE.value)
    }

    @Test
    fun arithmeticBehavesLikeASignedDistance() {
        assertEquals(Moria(22), Moria(10) + Moria(12))
        assertEquals(Moria(-2), Moria(10) - Moria(12))
        assertEquals(Moria(-10), -Moria(10))
        assertEquals(Moria(144), Moria.OCTAVE * 2)
        assertEquals(Moria.ZERO, Moria(12) - Moria(12))
    }

    @Test
    fun negativeDistancesAreOrdinaryBecausePitchesSitBelowNi() {
        assertTrue(Moria(-12) < Moria.ZERO)
        assertEquals(listOf(Moria(-12), Moria.ZERO, Moria(12)), listOf(Moria(12), Moria(-12), Moria.ZERO).sorted())
    }

    @Test
    fun aDistanceResolvesToAPitchThroughTheSingleTuningSource() {
        assertEquals(ByzantineTuning.NI_BASE_HZ, Moria.ZERO.toFrequencyHz(), 1e-9)
        assertEquals(ByzantineTuning.NI_BASE_HZ * 2, Moria.OCTAVE.toFrequencyHz(), 1e-9)
        assertEquals(ByzantineTuning.NI_BASE_HZ / 2, (-Moria.OCTAVE).toFrequencyHz(), 1e-9)
        assertEquals(ByzantineTuning.frequencyHz(42), Moria(42).toFrequencyHz(), 0.0)
    }

    @Test
    fun readingAnIntAtABoundaryIsExplicit() {
        assertEquals(Moria(-12), (-12).moria())
    }
}
