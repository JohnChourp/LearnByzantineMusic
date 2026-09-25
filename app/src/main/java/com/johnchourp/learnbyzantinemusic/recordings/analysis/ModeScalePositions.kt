package com.johnchourp.learnbyzantinemusic.recordings.analysis

import com.johnchourp.learnbyzantinemusic.modes.EightModeScaleDefinitions
import com.johnchourp.learnbyzantinemusic.music.ByzantineTuning
import com.johnchourp.learnbyzantinemusic.modes.ModeScaleBase
import com.johnchourp.learnbyzantinemusic.modes.ModeScaleDefinition
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.PhthongName

/**
 * Where the seven phthongs sit in a mode's scale, in moria above Νη (0 ≤ position < 72), taken
 * from the same interval tables the 8 Ήχοι screen uses ([EightModeScaleDefinitions]). Indexed by
 * [PhthongName.ordinal] (Νη, Πα, Βου, Γα, Δι, Κε, Ζω) — which is why that order is frozen. For the
 * diatonic genus this is exactly the Melody Trainer's table (`diatonicMoriaFromNi` in
 * `TrainerPitchTable.kt`).
 *
 * Both functions take a [Mode], not a key (ClickUp `869f5x299`): an unknown key used to fall through
 * here silently to the diatonic scale from Νη. Keys are now parsed where they enter the app, and a
 * stored one that names no mode falls back there, explicitly — see `AnalysisSettingsStore.resolveMode`.
 */
object ModeScalePositions {
    /** The octave size is declared once, in ByzantineTuning — never re-stated here. */
    const val MORIA_PER_OCTAVE = ByzantineTuning.MORIA_PER_OCTAVE

    fun forMode(mode: Mode): IntArray = forDefinition(mode.scale)

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
     * The phthong the melody is assumed to start on unless the user picks another: [Mode.martyria],
     * the φθόγγος of the mode's martyria (Α΄/Β΄ Πα, Γ΄ Γα, Δ΄ Βου, πλ. Α΄ Κε, πλ. Β΄ Δι, Βαρύς Ζω,
     * πλ. Δ΄ Νη) — the same values this used to list by hand.
     *
     * Deliberately the martyria, not a style's base: the theory gives some modes another sticheraric
     * base (Β΄ on Δι, for one). Whether that should drive the analysis is the owner's call, not
     * something a refactor changes.
     */
    fun defaultStartPhthong(mode: Mode): PhthongName = mode.martyria
}
