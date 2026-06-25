package com.johnchourp.learnbyzantinemusic.trainer

import kotlin.math.abs

/**
 * Intonation gate for the combined "phthong + time" exercise (Mode 3). It turns a raw
 * detected pitch into the phthong the singer is actually holding *in tune*: the nearest
 * phthong is returned only when the pitch is within [toleranceMoria] of it, otherwise null
 * (read as silence by the timing engine). The engine then matches that phthong against the
 * scheduled note, so a note greens only when the right phthong is sung in tune at the right
 * time.
 */
object ComboPitchGate {
    /** Reuses the voice-check intonation window so the two pitch modes agree. */
    const val DEFAULT_TOLERANCE_MORIA = PitchGreeningEvaluator.DEFAULT_TOLERANCE_MORIA

    fun inTunePhthong(match: PitchMatch?, toleranceMoria: Double = DEFAULT_TOLERANCE_MORIA): TrainerPhthong? {
        if (match == null) return null
        return if (abs(match.deviationMoria) <= toleranceMoria) match.phthong else null
    }
}
