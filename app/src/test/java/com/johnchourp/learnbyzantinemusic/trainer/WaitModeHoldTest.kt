package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.ByzantineTuning
import com.johnchourp.learnbyzantinemusic.music.IntonationProfile
import com.johnchourp.learnbyzantinemusic.music.ModeLadder
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * «Παραλλαγή με αναμονή» moves on only after the φθόγγος is held within the tolerance for 300 ms
 * (ClickUp `869f5x2cd`, J1) — 300 ms of the pitch engine's **capture time**, whatever the frame rate,
 * and in one unbroken stretch: a frame off the note, or silent, starts the hold again.
 */
class WaitModeHoldTest {

    private val scale = TrainerScale.DIATONIC
    private val line = listOf(TrainerNote(PhthongName.DI), TrainerNote(PhthongName.KE))
    private val targets: List<ModeLadder.Step> = line.map { scale.ladder.stepFor(it.pitch)!! }

    private fun evaluator() = WaitModeEvaluator(targets, scale.ladder, IntonationProfile.IN_TUNE_MORIA)

    /** The target's frequency moved by [offsetMoria]. */
    private fun sung(step: ModeLadder.Step, offsetMoria: Double = 0.0): Double =
        ByzantineTuning.frequencyHz(step.moriaFromNi.value + offsetMoria)

    @Test
    fun theHoldIsThreeHundredMilliseconds() {
        assertEquals(300.0, IntonationProfile.WAIT_HOLD_MS, 0.0)
    }

    @Test
    fun itMovesOnOnlyOnceTheNoteIsHeldForThreeHundredMilliseconds() {
        val wait = evaluator()
        // Live frames, one window apart, on the note and 2 μόρια sharp — within ±3.
        val frames = (0..8).map { 10_000L + (it * IntonationProfile.LIVE_HOP_MS).toLong() }
        val results = frames.map { at -> wait.onFrame(sung(targets[0], 2.0), at) }
        val first = results.indexOfFirst { it.advanced }
        assertTrue("it moved on", first >= 0)
        assertTrue("not before 300 ms: ${frames[first] - frames[0]} ms", frames[first] - frames[0] >= 300)
        assertTrue("and not a frame later than it had to", frames[first - 1] - frames[0] < 300)
        assertEquals(1, wait.currentIndex)
        results.take(first).forEach { assertFalse(it.advanced) }
    }

    @Test
    fun itIsTheMillisecondsThatCountNotTheFrames() {
        listOf(20L, 46L, 100L, 150L).forEach { spacing ->
            val wait = evaluator()
            var at = 0L
            var frame: WaitFrame
            do {
                frame = wait.onFrame(sung(targets[0]), at)
                at += spacing
            } while (!frame.advanced)
            val lockedAt = at - spacing
            assertTrue("$spacing ms frames lock at $lockedAt", lockedAt >= 300 && lockedAt < 300 + spacing)
        }
    }

    @Test
    fun anyFrameOffTheNoteOrSilentStartsTheHoldAgain() {
        listOf<Double?>(null, sung(targets[0], 3.5), sung(targets[1])).forEach { interruption ->
            val wait = evaluator()
            (0..5).forEach { wait.onFrame(sung(targets[0]), it * 50L) } // 250 ms held
            assertFalse(wait.onFrame(interruption, 300L).advanced)
            // The hold starts again at 350: nothing before 650 moves the line.
            (7..12).forEach { assertFalse("$interruption at ${it * 50}", wait.onFrame(sung(targets[0]), it * 50L).advanced) }
            assertTrue(wait.onFrame(sung(targets[0]), 650L).advanced)
        }
    }

    @Test
    fun theTimeToLockRunsFromWhenTheLineStartedWaiting() {
        val wait = evaluator()
        (0..20).forEach { wait.onFrame(null, it * 50L) } // a second of silence first: it counts too
        var at = 1_050L
        while (!wait.onFrame(sung(targets[0]), at).advanced) at += 50L
        assertEquals(listOf(at - 0L), wait.result().lockMillis)
    }

    @Test
    fun theHoldBarFillsWithTheHold() {
        val wait = evaluator()
        assertEquals(0L, wait.onFrame(sung(targets[0]), 0L).heldMillis)
        assertEquals(100L, wait.onFrame(sung(targets[0]), 100L).heldMillis)
        assertEquals(200L, wait.onFrame(sung(targets[0]), 200L).heldMillis)
        assertEquals("off the note, the bar empties", 0L, wait.onFrame(null, 250L).heldMillis)
    }
}
