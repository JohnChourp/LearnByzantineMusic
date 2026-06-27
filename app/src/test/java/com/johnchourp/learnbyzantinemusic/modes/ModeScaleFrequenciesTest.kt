package com.johnchourp.learnbyzantinemusic.modes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeScaleFrequenciesTest {

    @Test
    fun `reference Ni sounds at the base frequency`() {
        // One octave (72 μόρια) with the reference Νη at the bottom: bottom = 220Hz, top = 440Hz.
        val freqs = ModeScaleFrequencies.topToBottom(
            ascendingIntervals = listOf(72),
            referenceMoriaFromBottom = 0,
            baseShiftMoria = 0,
        )
        // Ordered top → bottom: 440 then 220.
        assertEquals(2, freqs.size)
        assertEquals(440.0, freqs[0], 1e-6)
        assertEquals(220.0, freqs[1], 1e-6)
    }

    @Test
    fun `each octave doubles the frequency`() {
        // Two octaves stacked: 0, 72, 144 μόρια from the bottom reference Νη.
        val freqs = ModeScaleFrequencies.topToBottom(
            ascendingIntervals = listOf(72, 72),
            referenceMoriaFromBottom = 0,
            baseShiftMoria = 0,
        )
        assertEquals(listOf(880.0, 440.0, 220.0), freqs.map { it })
    }

    @Test
    fun `reference offset places Ni in the middle of the run`() {
        // Reference Νη 72 μόρια up: the φθόγγος below it is an octave lower (110Hz).
        val freqs = ModeScaleFrequencies.topToBottom(
            ascendingIntervals = listOf(72),
            referenceMoriaFromBottom = 72,
            baseShiftMoria = 0,
        )
        assertEquals(220.0, freqs[0], 1e-6) // top boundary == reference Νη
        assertEquals(110.0, freqs[1], 1e-6) // bottom boundary, an octave below
    }

    @Test
    fun `base shift transposes the whole ladder without changing ratios`() {
        val base = ModeScaleFrequencies.topToBottom(listOf(72), 0, 0)
        val up = ModeScaleFrequencies.topToBottom(listOf(72), 0, 72) // +1 octave
        val down = ModeScaleFrequencies.topToBottom(listOf(72), 0, -72) // -1 octave
        for (i in base.indices) {
            assertEquals(base[i] * 2.0, up[i], 1e-6)
            assertEquals(base[i] / 2.0, down[i], 1e-6)
        }
        // The interval ratio between adjacent φθόγγοι is unchanged by the shift.
        assertEquals(base[0] / base[1], up[0] / up[1], 1e-9)
    }

    @Test
    fun `frequencies are ordered high to low and match the diatonic scale octave`() {
        val scale = EightModeScaleDefinitions.DIATONIC
        val octaves = 3
        val freqs = ModeScaleFrequencies.topToBottom(
            ascendingIntervals = scale.repeatedIntervals(octaves),
            referenceMoriaFromBottom = scale.referenceMoriaFromBottom("Νη", octaves),
            baseShiftMoria = 0,
        )
        // 3 octaves of the 7-step scale → 21 intervals → 22 boundaries.
        assertEquals(22, freqs.size)
        // Strictly descending top → bottom.
        for (i in 0 until freqs.size - 1) {
            assertTrue("freq[$i] should be above freq[${i + 1}]", freqs[i] > freqs[i + 1])
        }
        // The reference Νη is present at exactly the base frequency.
        assertTrue(freqs.any { kotlin.math.abs(it - 220.0) < 1e-6 })
    }
}
