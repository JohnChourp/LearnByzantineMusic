package com.johnchourp.learnbyzantinemusic.trainer

import kotlin.math.pow

/**
 * Converts a [TrainerPhthong] (optionally shifted by whole octaves) to a concrete
 * frequency in Hz, using the natural diatonic scale anchored at Νη = 220 Hz over a
 * 72-moria octave. This mirrors the moria→frequency formula the 8 Ήχοι screen uses for
 * its scale diagram so the trainer sounds the same pitches.
 */
object TrainerPitchTable {
    const val BASE_NI_FREQUENCY_HZ = 220.0
    const val MORIA_PER_OCTAVE = 72.0

    /** Absolute frequency of [phthong], raised/lowered by [octaveShift] whole octaves. */
    fun frequencyHz(phthong: TrainerPhthong, octaveShift: Int = 0): Double {
        val moriaFromNi = phthong.diatonicMoriaFromNi + octaveShift * MORIA_PER_OCTAVE
        return BASE_NI_FREQUENCY_HZ * 2.0.pow(moriaFromNi / MORIA_PER_OCTAVE)
    }
}
