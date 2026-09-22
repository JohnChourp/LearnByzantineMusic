package com.johnchourp.learnbyzantinemusic.modes

import com.johnchourp.learnbyzantinemusic.music.ByzantineTuning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ClickUp `869f4tpeu` (A1). The acceptance criterion that actually matters is that the drone follows
 * the «Μεταφορά βάσης» — a drone a comma away from the scale it accompanies is worse than none.
 */
class IsonDroneTest {

    private val octaves = 3
    private val reference = "Νη"

    private fun labels(scale: ModeScaleDefinition) =
        scale.ascendingPhthongs(octaves).reversed()

    private fun frequencies(scale: ModeScaleDefinition, shift: Int) =
        ModeScaleFrequencies.topToBottom(
            scale.repeatedIntervals(octaves),
            scale.referenceMoriaFromBottom(reference, octaves),
            shift,
        )

    @Test
    fun theBaseResolvesForEveryOneOfTheEightModes() {
        EightModeScaleDefinitions.MODE_SCALES.forEach { (key, scale) ->
            val hz = IsonDrone.frequencyHz(labels(scale), frequencies(scale, 0), scale.base.phthong)
            assertNotNull("no ison resolved for $key", hz)
            assertTrue("$key ison out of audible range: $hz", hz!! > 60.0 && hz < 900.0)
        }
    }

    @Test
    fun theBareLabelPicksTheMiddleOctaveNotTheDecoratedOnes() {
        val scale = EightModeScaleDefinitions.MODE_SCALES.getValue("first")
        val all = labels(scale)
        val base = scale.base.phthong

        // The base appears once per octave, decorated by octave; only one occurrence is bare.
        val bareOccurrences = all.count { it == base }
        assertEquals("exactly one undecorated occurrence", 1, bareOccurrences)

        val decorated = all.count { it != base && it.trimEnd('΄', ',') == base }
        assertTrue("the decorated octave twins must exist, otherwise this proves nothing", decorated >= 2)

        assertEquals(all.indexOf(base), IsonDrone.baseLabelIndex(all, base))
    }

    @Test
    fun theDroneIsExactlyTheDiagramsBaseKeyNotASecondCalculation() {
        val scale = EightModeScaleDefinitions.MODE_SCALES.getValue("first")
        (-12..12).forEach { shift ->
            val all = labels(scale)
            val freqs = frequencies(scale, shift)
            val index = IsonDrone.baseLabelIndex(all, scale.base.phthong)
            assertEquals(
                "shift=$shift",
                freqs[index],
                IsonDrone.frequencyHz(all, freqs, scale.base.phthong)!!,
                0.0, // bit-for-bit: it is a lookup, so any difference means a second calculation crept in
            )
        }
    }

    @Test
    fun theBaseShiftMovesTheDroneByExactlyThatManyMoria() {
        val scale = EightModeScaleDefinitions.MODE_SCALES.getValue("first")
        val unshifted = IsonDrone.frequencyHz(labels(scale), frequencies(scale, 0), scale.base.phthong)!!
        listOf(-12, -7, -1, 1, 7, 12).forEach { shift ->
            val shifted =
                IsonDrone.frequencyHz(labels(scale), frequencies(scale, shift), scale.base.phthong)!!
            assertEquals(
                "a shift of $shift μόρια",
                shift.toDouble(),
                ByzantineTuning.moriaFromNi(shifted) - ByzantineTuning.moriaFromNi(unshifted),
                1e-9,
            )
        }
    }

    @Test
    fun anAbsentBaseYieldsNullSoTheCallerCannotGuessAPitch() {
        assertNull(IsonDrone.frequencyHz(listOf("Πα", "Βου"), listOf(220.0, 240.0), "Δι"))
        assertEquals(-1, IsonDrone.baseLabelIndex(listOf("Πα", "Βου"), "Δι"))
    }

    @Test
    fun anIndexBeyondTheFrequencyListIsRefusedRatherThanThrowing() {
        // Labels and frequencies always arrive from the same source, but a mismatch must not crash
        // the audio path.
        assertNull(IsonDrone.frequencyHz(listOf("Πα", "Βου", "Γα"), listOf(220.0), "Γα"))
    }
}
