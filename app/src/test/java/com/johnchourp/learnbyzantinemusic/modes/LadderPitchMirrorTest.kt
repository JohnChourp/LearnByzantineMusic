package com.johnchourp.learnbyzantinemusic.modes

import com.johnchourp.learnbyzantinemusic.music.ByzantineTuning
import com.johnchourp.learnbyzantinemusic.music.ModeLadder
import com.johnchourp.learnbyzantinemusic.music.Moria
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * ClickUp `869f4tqad` (E1): the live pitch mirror reads against the mode's own ladder.
 *
 * The cases that matter are the ones a fixed diatonic table gets wrong — a transposed base and a
 * non-diatonic genus — so each of those has a test that **fails** if the mirror is reimplemented
 * against `TrainerPitchTable`.
 */
class LadderPitchMirrorTest {

    private val scales: List<ModeScaleDefinition> =
        EightModeScaleDefinitions.SCALE_BY_MODE.values.toList()

    private fun ladderFor(modeIndex: Int, baseShiftMoria: Int = 0): ModeLadder =
        scales[modeIndex].ladder(octaves = OCTAVES, baseShift = Moria(baseShiftMoria))

    /** The exact pitch of one rung, so a reading on it must come back with ~0 deviation. */
    private fun exactHzOf(ladder: ModeLadder, label: String): Double =
        ladder.steps.first { it.phthong.label == label }.frequencyHz

    @Test
    fun theLadderFixtureIsReal() {
        // Guards every assertion below: an empty ladder would make `read` return null and the
        // "is null" expectations would pass for the wrong reason.
        val ladder = ladderFor(0)
        assertTrue("expected a populated ladder, got ${ladder.steps.size}", ladder.steps.size >= 8)
        assertTrue(ladder.steps.all { it.frequencyHz > 0.0 })
    }

    @Test
    fun aPitchExactlyOnARungReadsAsThatRungWithNoDeviation() {
        val ladder = ladderFor(0)
        ladder.steps.forEach { step ->
            val reading = LadderPitchMirror.read(ladder, step.frequencyHz)!!
            assertEquals(step.phthong, reading.step.phthong)
            assertTrue("expected ~0 μόρια, got ${reading.deviationMoria}", abs(reading.deviationMoria) < 1e-6)
            assertTrue(reading.isInTune())
        }
    }

    @Test
    fun theSignSaysSharpOrFlatInMoria() {
        val ladder = ladderFor(0)
        val step = ladder.steps[ladder.steps.size / 2]
        val sharpBy4 = ByzantineTuning.frequencyHz(ByzantineTuning.moriaFromNi(step.frequencyHz) + 4.0)
        val flatBy4 = ByzantineTuning.frequencyHz(ByzantineTuning.moriaFromNi(step.frequencyHz) - 4.0)

        val sharp = LadderPitchMirror.read(ladder, sharpBy4)!!
        val flat = LadderPitchMirror.read(ladder, flatBy4)!!
        assertEquals(4.0, sharp.deviationMoria, 1e-6)
        assertEquals(-4.0, flat.deviationMoria, 1e-6)
        assertFalse("4 μόρια is outside the default tolerance", sharp.isInTune())
        assertTrue("2 μόρια is inside it", LadderPitchMirror.Reading(step, 2.0).isInTune())
    }

    /**
     * The transposed case. With a base shift the whole ladder moves, so a singer following it is in
     * tune — a fixed table would call them 8 μόρια flat on every single note.
     */
    @Test
    fun aTransposedLadderIsReadAgainstItsOwnShiftedPitches() {
        val shifted = ladderFor(0, baseShiftMoria = -8)
        val unshifted = ladderFor(0, baseShiftMoria = 0)
        assertNotEquals(
            "the fixture must actually be transposed, or this test proves nothing",
            unshifted.steps.first().frequencyHz,
            shifted.steps.first().frequencyHz,
        )
        shifted.steps.forEach { step ->
            val reading = LadderPitchMirror.read(shifted, step.frequencyHz)!!
            assertTrue(
                "shifted rung ${step.phthong.label} read as ${reading.deviationMoria} μόρια off",
                abs(reading.deviationMoria) < 1e-6,
            )
        }
    }

    /**
     * The genus case. A non-diatonic mode puts its φθόγγοι at different distances; reading it
     * against diatonic positions misreports a correctly sung note.
     */
    @Test
    fun aNonDiatonicModeIsReadAgainstItsOwnIntervals() {
        val diatonic = ladderFor(0)
        val other = scales.indices
            .map { ladderFor(it) }
            .firstOrNull { candidate ->
                candidate.steps.any { step ->
                    diatonic.steps.none { abs(it.frequencyHz - step.frequencyHz) < 0.01 }
                }
            }
        assertTrue("expected at least one mode whose pitches differ from the diatonic ladder", other != null)
        other!!.steps.forEach { step ->
            val reading = LadderPitchMirror.read(other, step.frequencyHz)!!
            assertTrue(abs(reading.deviationMoria) < 1e-6)
        }
    }

    @Test
    fun unusablePitchesReadAsNothingRatherThanAsNi() {
        val ladder = ladderFor(0)
        assertNull(LadderPitchMirror.read(ladder, 0.0))
        assertNull(LadderPitchMirror.read(ladder, -110.0))
        assertNull(LadderPitchMirror.read(ladder, Double.NaN))
        assertNull(LadderPitchMirror.read(ladder, Double.POSITIVE_INFINITY))
    }

    private companion object {
        const val OCTAVES = 3
    }
}
