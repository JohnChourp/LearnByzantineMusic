package com.johnchourp.learnbyzantinemusic.trainer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RhythmTimingEvaluatorTest {

    // Two notes, 1 beat each at 60 bpm -> note0 [0,1000), note1 [1000,2000).
    private fun twoNotePlan(): List<PlannedNoteEvent> {
        val sequence = MelodySequence(
            listOf(TrainerNote(TrainerPhthong.NI), TrainerNote(TrainerPhthong.PA))
        )
        return MelodyPlaybackPlanner.plan(sequence, MelodyTempo(60))
    }

    /** Simulates a voiced sung segment from [onset] to [offset]; returns the verdict. */
    private fun RhythmTimingEvaluator.singSegment(onset: Long, offset: Long): RhythmVerdict? {
        onFrame(onset, true)
        return onFrame(offset, false)
    }

    @Test
    fun `notes sung on time turn matched`() {
        val evaluator = RhythmTimingEvaluator(twoNotePlan())
        val first = evaluator.singSegment(0, 975)
        assertEquals(0, first?.noteIndex)
        assertTrue(first!!.matched)

        val second = evaluator.singSegment(1000, 1975)
        assertEquals(1, second?.noteIndex)
        assertTrue(second!!.matched)
        assertTrue(evaluator.isComplete)
    }

    @Test
    fun `a late onset is not matched`() {
        val evaluator = RhythmTimingEvaluator(twoNotePlan())
        val verdict = evaluator.singSegment(400, 1200) // note0 starts at 0; 400 ms late > 250
        assertEquals(0, verdict?.noteIndex)
        assertFalse(verdict!!.matched)
    }

    @Test
    fun `too-short duration is not matched even with a good onset`() {
        val evaluator = RhythmTimingEvaluator(twoNotePlan())
        val verdict = evaluator.singSegment(0, 200) // held 200 ms vs 1000 ms expected
        assertEquals(0, verdict?.noteIndex)
        assertFalse(verdict!!.matched)
    }

    @Test
    fun `a skipped note is passed over and the next is evaluated`() {
        val evaluator = RhythmTimingEvaluator(twoNotePlan())
        // Singer ignores note0 entirely and sings note1 on time.
        val verdict = evaluator.singSegment(1000, 1975)
        assertEquals(1, verdict?.noteIndex)
        assertTrue(verdict!!.matched)
        // note0 was never sung, so it stays unjudged (and ungreened).
        assertFalse(evaluator.isComplete)
    }

    @Test
    fun `finish closes a segment still voiced at the end`() {
        val evaluator = RhythmTimingEvaluator(twoNotePlan())
        evaluator.onFrame(0, true)
        val verdict = evaluator.finish(1000)
        assertEquals(0, verdict?.noteIndex)
        assertTrue(verdict!!.matched)
    }

    @Test
    fun `active note index tracks the schedule`() {
        val evaluator = RhythmTimingEvaluator(twoNotePlan())
        assertEquals(0, evaluator.activeNoteIndex(500))
        assertEquals(1, evaluator.activeNoteIndex(1500))
        assertEquals(-1, evaluator.activeNoteIndex(5000))
    }

    @Test
    fun `silence alone yields no verdict`() {
        val evaluator = RhythmTimingEvaluator(twoNotePlan())
        assertNull(evaluator.onFrame(100, false))
        assertNull(evaluator.onFrame(200, false))
        assertFalse(evaluator.isComplete)
    }

    @Test
    fun `a legato run is split at note boundaries and greens every note`() {
        val evaluator = RhythmTimingEvaluator(twoNotePlan())
        val verdicts = mutableListOf<RhythmVerdict>()
        // Continuous voicing from 0 to ~1975 ms with no silence between the two notes.
        var t = 0L
        while (t <= 1975) {
            evaluator.onFrame(t, true)?.let { verdicts.add(it) }
            t += 50
        }
        evaluator.finish(2000)?.let { verdicts.add(it) }

        assertEquals(listOf(0, 1), verdicts.map { it.noteIndex })
        assertTrue(verdicts.all { it.matched })
        assertTrue(evaluator.isComplete)
    }
}
