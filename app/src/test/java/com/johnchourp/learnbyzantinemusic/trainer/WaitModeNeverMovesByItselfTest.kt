package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.ByzantineTuning
import com.johnchourp.learnbyzantinemusic.music.IntonationProfile
import com.johnchourp.learnbyzantinemusic.music.ModeLadder
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The line never moves on by itself (ClickUp `869f5x2cd`, J1): not on a wrong note, not on silence,
 * not after any length of time. «Παράλειψη» is the one other way, and only a tap calls it — the
 * reviews of apps whose voice exercises «προχωρούν μόνες τους» are why.
 */
class WaitModeNeverMovesByItselfTest {

    private val scale = TrainerScale.DIATONIC
    private val line = listOf(TrainerNote(PhthongName.PA), TrainerNote(PhthongName.GA), TrainerNote(PhthongName.DI))
    private val targets: List<ModeLadder.Step> = line.map { scale.ladder.stepFor(it.pitch)!! }

    private fun evaluator() = WaitModeEvaluator(targets, scale.ladder, IntonationProfile.IN_TUNE_MORIA)

    private fun sung(step: ModeLadder.Step, offsetMoria: Double = 0.0): Double =
        ByzantineTuning.frequencyHz(step.moriaFromNi.value + offsetMoria)

    /** Ten seconds of capture times from [start], one every live window. */
    private fun tenSecondsFrom(start: Long): List<Long> =
        (0 until 216).map { start + (it * IntonationProfile.LIVE_HOP_MS).toLong() }

    @Test
    fun aWrongNoteHeldForTenSecondsNeverMovesIt() {
        val wait = evaluator()
        listOf(targets[1], targets[2]).forEachIndexed { turn, wrong ->
            tenSecondsFrom(turn * 20_000L).forEach { at -> assertFalse(wait.onFrame(sung(wrong), at).advanced) }
        }
        assertEquals(0, wait.currentIndex)
        assertEquals(0, wait.skips)
    }

    @Test
    fun silenceAndOutOfTuneNeverMoveItEither() {
        val wait = evaluator()
        tenSecondsFrom(0L).forEach { at -> assertFalse(wait.onFrame(null, at).advanced) }
        // Just outside ±3, on either side, held just as long.
        tenSecondsFrom(20_000L).forEach { at -> assertFalse(wait.onFrame(sung(targets[0], 3.4), at).advanced) }
        tenSecondsFrom(40_000L).forEach { at -> assertFalse(wait.onFrame(sung(targets[0], -3.4), at).advanced) }
        assertEquals(0, wait.currentIndex)
    }

    @Test
    fun onlyATappedSkipMovesItWithoutTheVoice() {
        val wait = evaluator()
        assertTrue(wait.skip())
        assertEquals(1, wait.currentIndex)
        assertEquals(1, wait.skips)
        // The skipped note is not a found one: no time to lock is recorded for it.
        assertEquals(emptyList<Long>(), wait.result().lockMillis)
    }

    @Test
    fun theRunEndsAfterTheLastNoteAndThenNothingMoves() {
        val wait = evaluator()
        wait.skip()
        var at = 0L
        while (!wait.onFrame(sung(targets[1]), at).advanced) at += 50L
        assertEquals(2, wait.currentIndex)
        wait.skip()
        assertTrue(wait.isComplete)
        assertNull(wait.currentIndex)
        assertFalse("no skip after the end", wait.skip())
        assertFalse(wait.onFrame(sung(targets[0]), at + 10_000).advanced)
        val run = wait.result()
        assertEquals(3, run.noteCount)
        assertEquals(2, run.skips)
        assertEquals("one note was found by the voice", 1, run.lockMillis.size)
    }

    @Test
    fun theCursorItMovesIsTheSharedOne() {
        val wait = evaluator()
        assertEquals(PracticeCursor.start(3), wait.cursor)
        wait.skip()
        assertEquals(PracticeCursor.start(3).advance(), wait.cursor)
    }

    @Test
    fun anEmptyLineHasNothingToWaitOn() {
        val wait = WaitModeEvaluator(emptyList(), scale.ladder, IntonationProfile.IN_TUNE_MORIA)
        assertTrue(wait.isComplete)
        assertNull(wait.currentIndex)
        assertFalse(wait.onFrame(220.0, 0L).advanced)
    }
}
