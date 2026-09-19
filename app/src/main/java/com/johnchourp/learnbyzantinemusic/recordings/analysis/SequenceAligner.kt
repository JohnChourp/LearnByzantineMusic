package com.johnchourp.learnbyzantinemusic.recordings.analysis

import com.johnchourp.learnbyzantinemusic.trainer.TrainerPhthong

/** One step of lining the sung notes up with the expected melody. */
sealed interface AlignmentStep {
    /** Expected note [expected] was sung as note [sung]. */
    data class Match(val expected: Int, val sung: Int) : AlignmentStep

    /** Expected note [expected] was sung as a different phthong, note [sung]. */
    data class Substitution(val expected: Int, val sung: Int) : AlignmentStep

    /** Expected note [expected] has no sung counterpart. */
    data class Missing(val expected: Int) : AlignmentStep

    /** Sung note [sung] has no expected counterpart. */
    data class Extra(val sung: Int) : AlignmentStep
}

data class AlignmentResult(
    val steps: List<AlignmentStep>,
    val expectedCount: Int,
) {
    val matches: Int get() = steps.count { it is AlignmentStep.Match }
    val extras: Int get() = steps.count { it is AlignmentStep.Extra }

    /** Share of the expected notes sung right, 0..1. */
    val accuracy: Double get() = if (expectedCount == 0) 0.0 else matches.toDouble() / expectedCount
}

/**
 * «How correct was I»: the minimum-edit alignment (Levenshtein) of the phthongs the singer was
 * supposed to sing against the ones that were recognised. Octaves are ignored, as in the Melody
 * Trainer's voice check; order matters, timing does not.
 */
object SequenceAligner {
    fun align(expected: List<TrainerPhthong>, sung: List<TrainerPhthong>): AlignmentResult {
        val rows = expected.size + 1
        val cols = sung.size + 1
        val cost = Array(rows) { IntArray(cols) }
        for (i in 0 until rows) cost[i][0] = i
        for (j in 0 until cols) cost[0][j] = j
        for (i in 1 until rows) {
            for (j in 1 until cols) {
                val diagonal = cost[i - 1][j - 1] + if (expected[i - 1] == sung[j - 1]) 0 else 1
                cost[i][j] = minOf(diagonal, cost[i - 1][j] + 1, cost[i][j - 1] + 1)
            }
        }
        // Walk back, preferring a match/substitution over a gap on ties so notes stay paired.
        val steps = ArrayList<AlignmentStep>()
        var i = expected.size
        var j = sung.size
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0) {
                val same = expected[i - 1] == sung[j - 1]
                if (cost[i][j] == cost[i - 1][j - 1] + if (same) 0 else 1) {
                    steps += if (same) AlignmentStep.Match(i - 1, j - 1) else AlignmentStep.Substitution(i - 1, j - 1)
                    i--
                    j--
                    continue
                }
            }
            if (i > 0 && cost[i][j] == cost[i - 1][j] + 1) {
                steps += AlignmentStep.Missing(i - 1)
                i--
            } else {
                steps += AlignmentStep.Extra(j - 1)
                j--
            }
        }
        return AlignmentResult(steps.asReversed().toList(), expected.size)
    }
}
