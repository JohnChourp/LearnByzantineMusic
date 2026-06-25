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
    fun `a note still voiced at the safety cutoff is not counted correct`() {
        // One 8 s note (4 beats at 30 bpm); duration tolerance is a generous 4 s.
        val plan = MelodyPlaybackPlanner.plan(
            MelodySequence(listOf(TrainerNote(TrainerPhthong.NI, baseDurationBeats = 4f))),
            MelodyTempo(30)
        )
        val evaluator = RhythmTimingEvaluator(plan)
        evaluator.onFrame(0, true) // start on time, never release
        // Safety cutoff fires at total + grace ≈ 9500 ms — within the 4 s tolerance, but a
        // never-released note must not count as correctly timed.
        val verdict = evaluator.finish(9500)
        assertEquals(0, verdict?.noteIndex)
        assertFalse(verdict!!.matched)
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
        evaluator.onFrame(2000, false)?.let { verdicts.add(it) } // release at the end

        assertEquals(listOf(0, 1), verdicts.map { it.noteIndex })
        assertTrue(verdicts.all { it.matched })
        assertTrue(evaluator.isComplete)
    }

    @Test
    fun `a correctly timed second note still greens after the first is skipped`() {
        val evaluator = RhythmTimingEvaluator(twoNotePlan())
        val verdicts = mutableListOf<RhythmVerdict>()
        // Sing only the second note, starting slightly early (900 ms) and releasing at 1920.
        var t = 900L
        while (t <= 1900) {
            evaluator.onFrame(t, true)?.let { verdicts.add(it) }
            t += 50
        }
        evaluator.onFrame(1920, false)?.let { verdicts.add(it) }

        // 900 ms is within tolerance of note1's start (1000) so note1 greens; note0 was never
        // sung and stays unjudged.
        assertEquals(listOf(1), verdicts.map { it.noteIndex })
        assertTrue(verdicts.single().matched)
    }

    @Test
    fun `a late onset fails its own note without consuming the next`() {
        val evaluator = RhythmTimingEvaluator(twoNotePlan())
        // note0 started 600 ms late — not within 250 ms of any scheduled start — so it attaches
        // to note0 and fails, instead of being grabbed by the nearer note1 start.
        val first = evaluator.singSegment(600, 980)
        assertEquals(0, first?.noteIndex)
        assertFalse(first!!.matched)
        // note1 sung on time still greens.
        val second = evaluator.singSegment(1000, 1950)
        assertEquals(1, second?.noteIndex)
        assertTrue(second!!.matched)
    }

    @Test
    fun `the final note is left open past its scheduled end while still held`() {
        val evaluator = RhythmTimingEvaluator(twoNotePlan()) // note0 [0,1000), note1 [1000,2000)
        evaluator.singSegment(0, 980) // note0 on time

        // Hold note1 continuously well past its scheduled end (2000) without releasing.
        val verdicts = mutableListOf<RhythmVerdict>()
        var t = 1000L
        while (t <= 2400) {
            evaluator.onFrame(t, true)?.let { verdicts.add(it) }
            t += 50
        }
        // It must NOT be closed/greened at the 2000 ms boundary — no verdict yet.
        assertTrue(verdicts.isEmpty())

        // Only when the safety cutoff fires (still held) is it judged — and not matched.
        val verdict = evaluator.finish(3500)
        assertEquals(1, verdict?.noteIndex)
        assertFalse(verdict!!.matched)
    }
}
