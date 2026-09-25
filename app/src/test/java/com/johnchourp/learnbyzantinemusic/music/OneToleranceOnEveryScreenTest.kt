package com.johnchourp.learnbyzantinemusic.music

import com.johnchourp.learnbyzantinemusic.modes.EightModeScaleDefinitions
import com.johnchourp.learnbyzantinemusic.modes.LadderPitchMirror
import com.johnchourp.learnbyzantinemusic.recordings.analysis.SungNote
import com.johnchourp.learnbyzantinemusic.trainer.ComboPitchGate
import com.johnchourp.learnbyzantinemusic.trainer.GreeningResult
import com.johnchourp.learnbyzantinemusic.trainer.PitchGreeningEvaluator
import com.johnchourp.learnbyzantinemusic.trainer.PitchMatch
import com.johnchourp.learnbyzantinemusic.trainer.TrainerPhthong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * «Διάγραμμα ανάλυσης = Γυμναστής» (ClickUp `869f5x28t`, H1): for the same deviation from the same
 * φθόγγος, every screen that judges a voice gives the same verdict. Before H1 the analysis said
 * «εντός» up to 4 μόρια, the Trainer up to 4 and «Πού είμαι» up to 3.
 *
 * Each screen is asked through its own public entry point with its **default** tolerance — the one
 * the app runs — so a screen that grows its own number again disagrees here on the 3–4 μόρια band.
 * The analysis diagram, chips and summary all colour by [SungNote.isInTune].
 */
class OneToleranceOnEveryScreenTest {

    /** −6 … +6 μόρια in quarter steps: in, on the ±3 boundary, in the old 3–4 band, and out. */
    private val deviations: List<Double> = (-24..24).map { it * 0.25 }

    private fun analysisDiagram(deviation: Double): Boolean =
        SungNote(TrainerPhthong.DI, octave = 0, startMs = 0, endMs = 500, deviationMoria = deviation, moria = 42.0 + deviation)
            .isInTune

    private fun trainerPhthongPlusTime(deviation: Double): Boolean =
        ComboPitchGate.inTunePhthong(PitchMatch(TrainerPhthong.DI, deviation)) != null

    private fun trainerVoiceCheck(deviation: Double): Boolean {
        val evaluator = PitchGreeningEvaluator(listOf(TrainerPhthong.DI))
        var verdict: GreeningResult? = null
        repeat(PitchGreeningEvaluator.DEFAULT_MIN_STABLE_FRAMES) {
            evaluator.onFrame(PitchMatch(TrainerPhthong.DI, deviation))?.let { verdict = it }
        }
        return verdict!!.matched
    }

    private val diatonicDi = EightModeScaleDefinitions.DIATONIC.ladder(octaves = 3).stepFor(Phthong(PhthongName.DI))!!

    private fun whereAmI(deviation: Double): Boolean = LadderPitchMirror.Reading(diatonicDi, deviation).isInTune()

    @Test
    fun theSweepCrossesTheBoundaryBothWays() {
        // Guards the slice: a sweep that were all "in" or all "out" would make agreement trivial.
        assertTrue(deviations.any { IntonationProfile.isInTune(it) })
        assertTrue(deviations.any { !IntonationProfile.isInTune(it) })
        assertTrue("the old 3–4 band is probed", deviations.any { it > IntonationProfile.IN_TUNE_MORIA && it <= 4.0 })
    }

    @Test
    fun theAnalysisDiagramJudgesExactlyLikeTheTrainer() {
        deviations.forEach { deviation ->
            assertEquals("voice check, $deviation μόρια", trainerVoiceCheck(deviation), analysisDiagram(deviation))
            assertEquals("φθόγγος + time, $deviation μόρια", trainerPhthongPlusTime(deviation), analysisDiagram(deviation))
        }
    }

    @Test
    fun whereAmIJudgesExactlyLikeTheTrainer() {
        deviations.forEach { deviation ->
            assertEquals("$deviation μόρια", trainerVoiceCheck(deviation), whereAmI(deviation))
        }
    }

    @Test
    fun everyScreenAgreesWithTheProfile() {
        deviations.forEach { deviation ->
            val expected = IntonationProfile.isInTune(deviation)
            assertEquals("analysis, $deviation", expected, analysisDiagram(deviation))
            assertEquals("voice check, $deviation", expected, trainerVoiceCheck(deviation))
            assertEquals("φθόγγος + time, $deviation", expected, trainerPhthongPlusTime(deviation))
            assertEquals("«Πού είμαι», $deviation", expected, whereAmI(deviation))
        }
    }
}
