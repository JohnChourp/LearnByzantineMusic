package com.johnchourp.learnbyzantinemusic.modes

import com.johnchourp.learnbyzantinemusic.modes.ui.EIGHT_MODES
import com.johnchourp.learnbyzantinemusic.modes.ui.SCALE_OCTAVES
import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.ByzantineTuning
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.Moria
import com.johnchourp.learnbyzantinemusic.music.PhthongName
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

    private val octaves = SCALE_OCTAVES
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
            // The ison's base, which since ClickUp `869f5x251` is the end of the απήχημα.
            val base = IsonDrone.base(Mode.fromKey(key)!!).label
            val hz = IsonDrone.frequencyHz(labels(scale), frequencies(scale, 0), base)
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
    fun theDroneIsExactlyTheDiagramsKeyForEverySelectablePhthongNotASecondCalculation() {
        // ClickUp `869f5x251`: the ison can now sit on any φθόγγος «Ίσον σε…» offers, so the lookup
        // is held to the diagram's own key for all of them, in every mode, across the whole range
        // the «Μεταφορά βάσης» slider offers.
        var compared = 0
        EIGHT_MODES.forEach { row ->
            val mode = row.mode
            (BaseShift.MIN_MORIA..BaseShift.MAX_MORIA).forEach { shift ->
                val all = labels(row.scale)
                val freqs = frequencies(row.scale, shift)
                val ladder = row.scale.ladder(octaves = SCALE_OCTAVES, baseShift = Moria(shift))
                IsonDrone.choices(mode, ladder)!!.all.forEach { choice ->
                    val index = all.indexOf(choice.label)
                    assertTrue("${mode.key}: ${choice.label} is not on the diagram", index >= 0)
                    val where = "${mode.key} shift=$shift ${choice.label}"
                    // bit-for-bit: it is a lookup, so any difference means a second calculation crept in
                    assertEquals(where, freqs[index], IsonDrone.step(ladder, choice)!!.frequencyHz, 0.0)
                    assertEquals(where, freqs[index], IsonDrone.frequencyHz(all, freqs, choice.label)!!, 0.0)
                    compared++
                }
            }
        }
        // Guards the sweep: every mode × every shift × every φθόγγος, or it proves nothing.
        assertEquals(
            EIGHT_MODES.size * (BaseShift.MAX_MORIA - BaseShift.MIN_MORIA + 1) * PhthongName.entries.size,
            compared,
        )
    }

    @Test
    fun theBaseShiftMovesTheDroneByExactlyThatManyMoria() {
        val scale = EightModeScaleDefinitions.MODE_SCALES.getValue("first")
        val unshifted = IsonDrone.frequencyHz(labels(scale), frequencies(scale, 0), scale.base.phthong)!!
        listOf(BaseShift.MIN_MORIA, -12, -7, -1, 1, 7, 12, BaseShift.MAX_MORIA).forEach { shift ->
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
