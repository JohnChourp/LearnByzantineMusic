package com.johnchourp.learnbyzantinemusic.recordings.analysis

import com.johnchourp.learnbyzantinemusic.modes.EightModeScaleDefinitions
import com.johnchourp.learnbyzantinemusic.music.ByzantineTuning
import com.johnchourp.learnbyzantinemusic.modes.ModeScaleBase
import com.johnchourp.learnbyzantinemusic.modes.ModeScaleDefinition
import com.johnchourp.learnbyzantinemusic.trainer.TrainerPhthong

/**
 * Where the seven phthongs sit in a mode's scale, in moria above Νη (0 ≤ position < 72), taken
 * from the same interval tables the 8 Ήχοι screen uses ([EightModeScaleDefinitions]). Indexed by
 * [TrainerPhthong.ordinal] (Νη, Πα, Βου, Γα, Δι, Κε, Ζω). For the diatonic genus this is exactly
 * [TrainerPhthong.diatonicMoriaFromNi].
 */
object ModeScalePositions {
    /** The octave size is declared once, in ByzantineTuning — never re-stated here. */
    const val MORIA_PER_OCTAVE = ByzantineTuning.MORIA_PER_OCTAVE

    fun forMode(modeKey: String): IntArray =
        forDefinition(EightModeScaleDefinitions.MODE_SCALES[modeKey] ?: EightModeScaleDefinitions.DIATONIC)

    fun forDefinition(definition: ModeScaleDefinition): IntArray {
        val intervals = definition.intervals
        require(intervals.size == 7 && intervals.sum() == MORIA_PER_OCTAVE) { "A scale needs 7 intervals summing to 72 moria" }
        return when (definition.base) {
            // Νη-based tables start at Νη: Νη 0, Πα = i0, Βου = i0 + i1, …
            ModeScaleBase.NI -> IntArray(7) { index -> intervals.take(index).sum() }
            // Πα-based tables start at Πα; Νη sits one interval (the last) below it.
            ModeScaleBase.PA -> {
                val niBelowPa = intervals.last()
                IntArray(7) { index -> if (index == 0) 0 else niBelowPa + intervals.take(index - 1).sum() }
            }
        }
    }

    /**
     * The phthong the melody is assumed to start on unless the user picks another: the phthong of
     * the mode's martyria in the app's mode theory (Α΄/Β΄ Πα, Γ΄ Γα, Δ΄ Βου, πλ. Α΄ Κε, πλ. Β΄ Δι,
     * Βαρύς Ζω, πλ. Δ΄ Νη).
     */
    fun defaultStartPhthong(modeKey: String): TrainerPhthong = when (modeKey) {
        "first", "second" -> TrainerPhthong.PA
        "third" -> TrainerPhthong.GA
        "fourth" -> TrainerPhthong.VOU
        "plagal_first" -> TrainerPhthong.KE
        "plagal_second" -> TrainerPhthong.DI
        "varys" -> TrainerPhthong.ZO
        else -> TrainerPhthong.NI
    }
}
