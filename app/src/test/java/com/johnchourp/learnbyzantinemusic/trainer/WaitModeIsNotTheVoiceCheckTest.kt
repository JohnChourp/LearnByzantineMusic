package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.docs.KotlinSource
import com.johnchourp.learnbyzantinemusic.music.IntonationProfile
import com.johnchourp.learnbyzantinemusic.music.Phthong
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * «Παραλλαγή με αναμονή» is a new evaluator, not a flag on the voice check's (ClickUp `869f5x2cd`, J1).
 *
 * The voice check («Σωστός φθόγγος», `PitchGreeningEvaluator`) moves on whether the voice was right
 * or wrong — that is what it is for, and it keeps doing it. The wait mode never does. Fed the very
 * same wrong note, the two must part ways; and the wait mode must not lean on the voice check's code,
 * so a change to one cannot quietly change the other.
 */
class WaitModeIsNotTheVoiceCheckTest {

    private val scale = TrainerScale.DIATONIC
    private val line = listOf(PhthongName.PA, PhthongName.VOU)

    @Test
    fun onTheSameWrongNoteTheVoiceCheckMovesOnAndTheWaitModeWaits() {
        val wrongHz = scale.ladder.stepFor(Phthong(PhthongName.DI))!!.frequencyHz
        val voiceCheck = PitchGreeningEvaluator(line)
        val wait = WaitModeEvaluator(line.map { scale.ladder.stepFor(Phthong(it))!! }, scale.ladder, IntonationProfile.IN_TUNE_MORIA)

        val verdicts = (0..40).mapNotNull { frame ->
            val at = (frame * IntonationProfile.LIVE_HOP_MS).toLong()
            assertFalse(wait.onFrame(wrongHz, at).advanced)
            voiceCheck.onFrame(scale.match(wrongHz))
        }

        val first = verdicts.firstOrNull()
        assertNotNull("the voice check commits a verdict on a held wrong note", first)
        assertFalse("and it is a wrong one", first!!.matched)
        assertEquals("then moves on anyway: that is the voice check", 1, voiceCheck.currentTargetIndex)
        assertEquals("the wait mode is still on the first note", 0, wait.currentIndex)
    }

    @Test
    fun theWaitModeDoesNotUseTheVoiceChecksCode() {
        val file = File(KotlinSource.mainRoot, "com/johnchourp/learnbyzantinemusic/trainer/WaitModeEvaluator.kt")
        assertTrue("$file is missing", file.isFile)
        val code = KotlinSource.withoutComments(file.readText())
        // Guards the scan: the file really is the evaluator.
        assertTrue(code.contains("class WaitModeEvaluator"))
        assertFalse("the wait mode reuses the voice check", code.contains("PitchGreeningEvaluator"))
        assertFalse("the wait mode reuses the voice check's result", code.contains("GreeningResult"))
    }

    @Test
    fun theVoiceChecksOwnRuleIsUnchanged() {
        // Its frames are still counted the way H1 left them: 3 live frames of a φθόγγος commit it.
        assertEquals(3, PitchGreeningEvaluator.DEFAULT_MIN_STABLE_FRAMES)
        assertEquals(IntonationProfile.IN_TUNE_MORIA, PitchGreeningEvaluator(line).toleranceMoria, 0.0)
        // And a right note still greens: the evaluator was not bent to fit the wait mode.
        val right = PitchGreeningEvaluator(listOf(PhthongName.PA))
        val paHz = scale.ladder.stepFor(Phthong(PhthongName.PA))!!.frequencyHz
        val verdict = (0..5).firstNotNullOfOrNull { right.onFrame(scale.match(paHz)) }
        assertTrue(verdict!!.matched)
    }
}
