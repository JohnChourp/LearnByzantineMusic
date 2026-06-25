package com.johnchourp.learnbyzantinemusic.trainer

import kotlin.math.abs

/**
 * Pitch gate for the combined "phthong + time" exercise (Mode 3). It decides whether a
 * detected pitch counts as singing the *expected* phthong: the nearest phthong must equal
 * the expected one (octave-independent) and be within [toleranceMoria] of it. The timing
 * engine then only "hears" voicing while the right phthong is being sung, so a note greens
 * only when the singer hits the correct phthong at the correct time.
 */
object ComboPitchGate {
    /** Reuses the voice-check intonation window so the two pitch modes agree. */
    const val DEFAULT_TOLERANCE_MORIA = PitchGreeningEvaluator.DEFAULT_TOLERANCE_MORIA

    fun isCorrectPhthong(
        match: PitchMatch?,
        expected: TrainerPhthong?,
        toleranceMoria: Double = DEFAULT_TOLERANCE_MORIA
    ): Boolean {
        if (match == null || expected == null) return false
        return match.phthong == expected && abs(match.deviationMoria) <= toleranceMoria
    }
}
