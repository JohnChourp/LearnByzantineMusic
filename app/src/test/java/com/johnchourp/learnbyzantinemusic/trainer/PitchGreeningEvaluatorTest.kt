package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchGreeningEvaluatorTest {

    private val stableFrames = 3

    private fun match(phthong: PhthongName, deviation: Double = 0.0) = PitchMatch(phthong, deviation)

    /** Feeds the same phthong [stableFrames] times and returns the committing result. */
    private fun PitchGreeningEvaluator.sing(phthong: PhthongName, deviation: Double = 0.0): GreeningResult? {
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
            listOf(PhthongName.NI, PhthongName.PA, PhthongName.GA),
            minStableFrames = stableFrames
        )

        val first = evaluator.sing(PhthongName.NI)
        assertEquals(GreeningResult(0, true, PhthongName.NI), first)
        assertEquals(1, evaluator.currentTargetIndex)

        evaluator.sing(PhthongName.PA)
        val third = evaluator.sing(PhthongName.GA)
        assertEquals(GreeningResult(2, true, PhthongName.GA), third)
        assertTrue(evaluator.isComplete)
    }

    @Test
    fun `a wrong phthong still advances but is not matched`() {
        val evaluator = PitchGreeningEvaluator(
            listOf(PhthongName.NI, PhthongName.PA),
            minStableFrames = stableFrames
        )
        val result = evaluator.sing(PhthongName.DI) // expected Νη, sang Δι
        assertEquals(GreeningResult(0, false, PhthongName.DI), result)
        assertEquals(1, evaluator.currentTargetIndex)
    }

    @Test
    fun `intonation outside tolerance is not matched`() {
        // This passed toleranceMoria = 4.0, the Trainer's own value until ClickUp 869f5x28t (H1). It now
        // uses the default, the one tolerance of IntonationProfile (±3 μόρια), which is what the app runs.
        val evaluator = PitchGreeningEvaluator(
            listOf(PhthongName.NI),
            minStableFrames = stableFrames
        )
        val result = evaluator.sing(PhthongName.NI, deviation = 6.0) // right phthong, too sharp
        assertEquals(GreeningResult(0, false, PhthongName.NI), result)
    }

    @Test
    fun `a phthong held too briefly does not commit`() {
        val evaluator = PitchGreeningEvaluator(
            listOf(PhthongName.NI),
            minStableFrames = stableFrames
        )
        assertNull(evaluator.onFrame(match(PhthongName.NI)))
        assertNull(evaluator.onFrame(match(PhthongName.NI)))
        assertEquals(0, evaluator.currentTargetIndex)
        assertFalse(evaluator.isComplete)
    }

    @Test
    fun `reset returns to the first target`() {
        val evaluator = PitchGreeningEvaluator(listOf(PhthongName.NI, PhthongName.PA), minStableFrames = stableFrames)
        evaluator.sing(PhthongName.NI)
        assertEquals(1, evaluator.currentTargetIndex)
        evaluator.reset()
        assertEquals(0, evaluator.currentTargetIndex)
        assertFalse(evaluator.isComplete)
        assertEquals(PhthongName.NI, evaluator.currentTarget())
    }

    @Test
    fun `a held note advances through repeated identical targets`() {
        val evaluator = PitchGreeningEvaluator(
            listOf(PhthongName.NI, PhthongName.NI),
            minStableFrames = stableFrames
        )
        val results = mutableListOf<GreeningResult>()
        // Sing one continuous Νη with no silence in between.
        repeat(2 * stableFrames) {
            evaluator.onFrame(match(PhthongName.NI))?.let { results.add(it) }
        }
        assertEquals(listOf(0, 1), results.map { it.targetIndex })
        assertTrue(results.all { it.matched })
        assertTrue(evaluator.isComplete)
    }
}
