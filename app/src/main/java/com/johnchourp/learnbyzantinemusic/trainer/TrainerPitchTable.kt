package com.johnchourp.learnbyzantinemusic.trainer

import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow

/**
 * Converts between [TrainerPhthong]s and concrete frequencies in Hz using the natural
 * diatonic scale anchored at Νη = 220 Hz over a 72-moria octave. This mirrors the
 * moria→frequency formula the 8 Ήχοι screen uses for its scale diagram so the trainer
 * sounds — and listens for — the same pitches.
 */
object TrainerPitchTable {
    const val BASE_NI_FREQUENCY_HZ = 220.0
    const val MORIA_PER_OCTAVE = 72.0
    private const val CENTS_PER_OCTAVE = 1200.0

    /** Absolute frequency of [phthong], raised/lowered by [octaveShift] whole octaves. */
    fun frequencyHz(phthong: TrainerPhthong, octaveShift: Int = 0): Double {
        val moriaFromNi = phthong.diatonicMoriaFromNi + octaveShift * MORIA_PER_OCTAVE
        return BASE_NI_FREQUENCY_HZ * 2.0.pow(moriaFromNi / MORIA_PER_OCTAVE)
    }

    /** Moria above base Νη for a detected frequency (can be negative or exceed 72). */
    fun moriaFromNi(frequencyHz: Double): Double =
        MORIA_PER_OCTAVE * log2(frequencyHz / BASE_NI_FREQUENCY_HZ)

    fun moriaToCents(moria: Double): Double = moria * (CENTS_PER_OCTAVE / MORIA_PER_OCTAVE)

    /**
     * Nearest phthong (folding octaves away) to a detected frequency, with the signed
     * deviation in moria: positive = the sung pitch is sharp (above the phthong), negative
     * = flat. Returns null for non-positive / non-finite input.
     */
    fun nearestPhthong(frequencyHz: Double): PitchMatch? {
        if (frequencyHz <= 0.0 || frequencyHz.isNaN() || frequencyHz.isInfinite()) {
            return null
        }
        val moria = moriaFromNi(frequencyHz)
        val withinOctave = ((moria % MORIA_PER_OCTAVE) + MORIA_PER_OCTAVE) % MORIA_PER_OCTAVE
        var best = TrainerPhthong.NI
        var bestDeviation = Double.MAX_VALUE
        for (phthong in TrainerPhthong.ascending) {
            // Compare against the phthong and its upper-octave image so pitches near the
            // octave seam (Ζω ↔ Νη') resolve to the genuinely closest phthong.
            val candidates = doubleArrayOf(
                phthong.diatonicMoriaFromNi.toDouble(),
                phthong.diatonicMoriaFromNi + MORIA_PER_OCTAVE
            )
            for (candidateMoria in candidates) {
                val deviation = withinOctave - candidateMoria
                if (abs(deviation) < abs(bestDeviation)) {
                    bestDeviation = deviation
                    best = phthong
                }
            }
        }
        return PitchMatch(best, bestDeviation)
    }
}

/** Result of matching a detected frequency to the nearest phthong. */
data class PitchMatch(
    val phthong: TrainerPhthong,
    /** Signed moria deviation from [phthong]: positive = sharp, negative = flat. */
    val deviationMoria: Double
) {
    val deviationCents: Double get() = TrainerPitchTable.moriaToCents(deviationMoria)
}
