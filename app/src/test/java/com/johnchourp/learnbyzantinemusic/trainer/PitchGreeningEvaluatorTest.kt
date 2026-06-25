package com.johnchourp.learnbyzantinemusic.trainer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchGreeningEvaluatorTest {

    private val stableFrames = 3

    private fun match(phthong: TrainerPhthong, deviation: Double = 0.0) = PitchMatch(phthong, deviation)

    /** Feeds the same phthong [stableFrames] times and returns the committing result. */
    private fun PitchGreeningEvaluator.sing(phthong: TrainerPhthong, deviation: Double = 0.0): GreeningResult? {
        var committed: GreeningResult? = null
        repeat(stableFrames) {
            val r = onFrame(match(phthong, deviation))
            if (r != null) committed = r
        }
        onFrame(null) // release the note (silence) before the next segment
        return committed
    }

    @Test
    fun `correctly sung phthongi commit as matched and advance`() {
        val evaluator = PitchGreeningEvaluator(
            listOf(TrainerPhthong.NI, TrainerPhthong.PA, TrainerPhthong.GA),
            minStableFrames = stableFrames
        )

        val first = evaluator.sing(TrainerPhthong.NI)
        assertEquals(GreeningResult(0, true, TrainerPhthong.NI), first)
        assertEquals(1, evaluator.currentTargetIndex)

        evaluator.sing(TrainerPhthong.PA)
        val third = evaluator.sing(TrainerPhthong.GA)
        assertEquals(GreeningResult(2, true, TrainerPhthong.GA), third)
        assertTrue(evaluator.isComplete)
    }

    @Test
    fun `a wrong phthong still advances but is not matched`() {
        val evaluator = PitchGreeningEvaluator(
            listOf(TrainerPhthong.NI, TrainerPhthong.PA),
            minStableFrames = stableFrames
        )
        val result = evaluator.sing(TrainerPhthong.DI) // expected Νη, sang Δι
        assertEquals(GreeningResult(0, false, TrainerPhthong.DI), result)
        assertEquals(1, evaluator.currentTargetIndex)
    }

    @Test
    fun `intonation outside tolerance is not matched`() {
        val evaluator = PitchGreeningEvaluator(
            listOf(TrainerPhthong.NI),
            toleranceMoria = 4.0,
            minStableFrames = stableFrames
        )
        val result = evaluator.sing(TrainerPhthong.NI, deviation = 6.0) // right phthong, too sharp
        assertEquals(GreeningResult(0, false, TrainerPhthong.NI), result)
    }

    @Test
    fun `a phthong held too briefly does not commit`() {
        val evaluator = PitchGreeningEvaluator(
            listOf(TrainerPhthong.NI),
            minStableFrames = stableFrames
        )
        assertNull(evaluator.onFrame(match(TrainerPhthong.NI)))
        assertNull(evaluator.onFrame(match(TrainerPhthong.NI)))
        assertEquals(0, evaluator.currentTargetIndex)
        assertFalse(evaluator.isComplete)
    }

    @Test
    fun `reset returns to the first target`() {
        val evaluator = PitchGreeningEvaluator(listOf(TrainerPhthong.NI, TrainerPhthong.PA), minStableFrames = stableFrames)
        evaluator.sing(TrainerPhthong.NI)
        assertEquals(1, evaluator.currentTargetIndex)
        evaluator.reset()
        assertEquals(0, evaluator.currentTargetIndex)
        assertFalse(evaluator.isComplete)
        assertEquals(TrainerPhthong.NI, evaluator.currentTarget())
    }

    @Test
    fun `a held note advances through repeated identical targets`() {
        val evaluator = PitchGreeningEvaluator(
            listOf(TrainerPhthong.NI, TrainerPhthong.NI),
            minStableFrames = stableFrames
        )
        val results = mutableListOf<GreeningResult>()
        // Sing one continuous Νη with no silence in between.
        repeat(2 * stableFrames) {
            evaluator.onFrame(match(TrainerPhthong.NI))?.let { results.add(it) }
        }
        assertEquals(listOf(0, 1), results.map { it.targetIndex })
        assertTrue(results.all { it.matched })
        assertTrue(evaluator.isComplete)
    }
}
