package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.ByzantineTuning
import kotlin.math.abs

/**
 * Converts between [TrainerPhthong]s and concrete frequencies in Hz on the natural diatonic scale.
 *
 * It does not carry its own tuning: both directions go through [ByzantineTuning], the one place that
 * knows where Νη sounds and how many μόρια an octave holds. That is what guarantees the trainer
 * listens for exactly the pitches the 8 Ήχοι diagram plays — a second copy of the formula would let
 * the two drift apart silently.
 */
object TrainerPitchTable {
    /** μόρια in an octave, re-exported so trainer code reads one name for the octave's size. */
    const val MORIA_PER_OCTAVE = ByzantineTuning.MORIA_PER_OCTAVE

    /** Absolute frequency of [phthong], raised/lowered by [octaveShift] whole octaves. */
    fun frequencyHz(phthong: TrainerPhthong, octaveShift: Int = 0): Double {
        val moriaFromNi = phthong.diatonicMoriaFromNi + octaveShift * MORIA_PER_OCTAVE
        return ByzantineTuning.frequencyHz(moriaFromNi)
    }

    /** Moria above base Νη for a detected frequency (can be negative or exceed 72). */
    fun moriaFromNi(frequencyHz: Double): Double = ByzantineTuning.moriaFromNi(frequencyHz)

    fun moriaToCents(moria: Double): Double = ByzantineTuning.moriaToCents(moria)

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
        val octaveMoria = MORIA_PER_OCTAVE.toDouble()
        val withinOctave = ((moria % octaveMoria) + octaveMoria) % octaveMoria
        var best = TrainerPhthong.NI
        var bestDeviation = Double.MAX_VALUE
        for (phthong in TrainerPhthong.ascending) {
            // Compare against the phthong and its upper-octave image so pitches near the
            // octave seam (Ζω ↔ Νη') resolve to the genuinely closest phthong.
            val candidates = doubleArrayOf(
                phthong.diatonicMoriaFromNi.toDouble(),
                phthong.diatonicMoriaFromNi + octaveMoria
            )
            for (candidateMoria in candidates) {
                val deviation = withinOctave - candidateMoria
                if (abs(deviation) < abs(bestDeviation)) {
                    bestDeviation = deviation
                    best = phthong
                }
            }
        }
        return PitchMatch(best, bestDeviation, frequencyHz)
    }
}

/** Result of matching a detected frequency to the nearest phthong. */
data class PitchMatch(
    val phthong: TrainerPhthong,
    /** Signed moria deviation from [phthong]: positive = sharp, negative = flat. */
    val deviationMoria: Double,
    /**
     * The frequency this match was made from, unrounded and **with its octave**.
     *
     * [phthong] and [deviationMoria] fold octaves away, which is what the trainer wants — a Δι is a
     * Δι wherever it is sung. A caller matching against a ladder that spans several octaves needs
     * the pitch itself, and reconstructing it from the folded pair silently lands an octave out.
     * Defaulted so the existing test fixtures, which care only about the deviation, are unchanged.
     */
    val frequencyHz: Double = 0.0,
) {
    val deviationCents: Double get() = TrainerPitchTable.moriaToCents(deviationMoria)
}
