package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.ByzantineTuning
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * «Φωνή μετατοπισμένη κατά −12 μόρια πρασινίζει με μεταφορά −12 και όχι χωρίς αυτήν»
 * (ClickUp `869f5x24v`, F2).
 *
 * A singer at home lower than Νη = 220 Hz sings every note of Νη … Ζω 12 μόρια below where the Trainer
 * used to expect it. Each voice goes through the same path as on the device: the Trainer's scale
 * reads the frequency ([TrainerScale.match]) and the unchanged evaluators judge the result.
 */
class TrainerBaseShiftTest {

    private val melody: List<TrainerNote> = PhthongName.entries.map { TrainerNote(it) }

    /** Each note of [melody], sung [offsetMoria] away from where the default Trainer plays it. */
    private fun sungHz(note: TrainerNote, offsetMoria: Double): Double =
        ByzantineTuning.frequencyHz(
            ByzantineTuning.moriaFromNi(TrainerScale.DIATONIC.frequencyHz(note)) + offsetMoria,
        )

    /** The voice check: how many notes turn green when the melody is sung [offsetMoria] away. */
    private fun voiceCheckGreens(scale: TrainerScale, offsetMoria: Double): Int {
        val evaluator = PitchGreeningEvaluator(melody.map { it.phthong })
        var greens = 0
        melody.forEach { note ->
            val heard = scale.match(sungHz(note, offsetMoria))
            repeat(PitchGreeningEvaluator.DEFAULT_MIN_STABLE_FRAMES) {
                if (evaluator.onFrame(heard)?.matched == true) greens++
            }
            evaluator.onFrame(null)
        }
        return greens
    }

    /** φθόγγος + time: how many notes the gate lets through as the right φθόγγος, in tune. */
    private fun comboGreens(scale: TrainerScale, offsetMoria: Double): Int =
        melody.count { note ->
            ComboPitchGate.inTunePhthong(scale.match(sungHz(note, offsetMoria))) == note.phthong
        }

    @Test
    fun aVoiceTwelveMoriaLowGreensWithTheShiftAndNotWithout() {
        assertEquals("shift −12", 7, voiceCheckGreens(TrainerScale(Mode.FIRST, -12), -12.0))
        assertEquals("the same ήχος at 0", 0, voiceCheckGreens(TrainerScale(Mode.FIRST, 0), -12.0))
        assertEquals("«Διατονικός»", 0, voiceCheckGreens(TrainerScale.DIATONIC, -12.0))

        assertEquals(7, comboGreens(TrainerScale(Mode.FIRST, -12), -12.0))
        assertEquals(0, comboGreens(TrainerScale(Mode.FIRST, 0), -12.0))
    }

    @Test
    fun theSameHoldsAtBothEndsOfTheRange() {
        listOf(BaseShift.MIN_MORIA, BaseShift.MAX_MORIA).forEach { shift ->
            val scale = TrainerScale(Mode.FIRST, shift)
            assertEquals("shift $shift", 7, voiceCheckGreens(scale, shift.toDouble()))
            assertEquals("shift $shift, voice not moved", 0, voiceCheckGreens(scale, 0.0))
        }
    }

    @Test
    fun whatTheShiftedTrainerPlaysIsWhatItListensFor() {
        // Playback and judgement are one ladder: the shifted scale plays exactly the shifted voice.
        val shifted = TrainerScale(Mode.FIRST, -12)
        melody.forEach { note ->
            assertEquals(note.pitch.label, sungHz(note, -12.0), shifted.frequencyHz(note), 1e-9)
        }
    }
}
