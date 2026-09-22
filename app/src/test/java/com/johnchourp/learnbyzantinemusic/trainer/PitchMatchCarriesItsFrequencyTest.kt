package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.ByzantineTuning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * `PitchMatch.frequencyHz` carries the pitch the match was made from (ClickUp `869f4tqad`, E1).
 *
 * The live pitch mirror on the «8 Ήχοι» page reads against a ladder spanning three octaves, so it
 * needs the pitch **with its octave**. `phthong` + `deviationMoria` deliberately fold octaves away.
 * This file exists to pin that the raw value is carried, and to show what reconstructing it from
 * the folded pair actually produces — which is why the field was added rather than derived.
 */
class PitchMatchCarriesItsFrequencyTest {

    @Test
    fun theMatchCarriesTheExactFrequencyItWasMadeFrom() {
        listOf(110.0, 220.0, 293.7, 440.0, 880.0).forEach { hz ->
            val match = TrainerPitchTable.nearestPhthong(hz)!!
            assertEquals("carried frequency for $hz", hz, match.frequencyHz, 1e-9)
        }
    }

    /**
     * The failure the field prevents, stated as an assertion rather than as a comment: an octave
     * above Νη, the folded pair reconstructs to a pitch an octave away from what was sung.
     */
    @Test
    fun reconstructingFromTheFoldedPairLosesTheOctave() {
        val sungHz = ByzantineTuning.NI_BASE_HZ * 2.0 // Νη, one octave up
        val match = TrainerPitchTable.nearestPhthong(sungHz)!!
        val reconstructed = ByzantineTuning.frequencyHz(
            match.phthong.diatonicMoriaFromNi + match.deviationMoria
        )
        assertNotEquals(
            "if this ever equals the sung pitch, the mirror could derive it and the field is redundant",
            sungHz,
            reconstructed,
            1.0,
        )
        assertEquals("but the carried value is right", sungHz, match.frequencyHz, 1e-9)
    }

    @Test
    fun anUnusablePitchIsStillNoMatchAtAll() {
        assertTrue(TrainerPitchTable.nearestPhthong(0.0) == null)
        assertTrue(TrainerPitchTable.nearestPhthong(Double.NaN) == null)
    }

    @Test
    fun theDeviationIsUnchangedByTheNewField() {
        // The field must not have altered what the trainer already relied on.
        val onNi = TrainerPitchTable.nearestPhthong(ByzantineTuning.NI_BASE_HZ)!!
        assertEquals(TrainerPhthong.NI, onNi.phthong)
        assertTrue(abs(onNi.deviationMoria) < 1e-6)
    }
}
