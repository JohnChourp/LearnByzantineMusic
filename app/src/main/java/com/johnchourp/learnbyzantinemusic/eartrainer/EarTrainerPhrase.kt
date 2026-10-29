package com.johnchourp.learnbyzantinemusic.eartrainer

import com.johnchourp.learnbyzantinemusic.modes.IsonDrone
import com.johnchourp.learnbyzantinemusic.modes.ModeLadders
import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.ModeLadder

/**
 * What a question of «Μάντεψε τον ήχο» plays (ClickUp `869f4tqb9`): the mode's scale around the base
 * of its ison, step by step — base, three φθόγγοι up and back, two down and back:
 *
 *     0 → +1 → +2 → +3 → +2 → +1 → 0 → −1 → −2 → −1 → 0
 *
 * ## Why not the απήχημα
 *
 * It was the first idea, and it cannot tell the ήχοι apart: in 11 of the 28 pairs it has the same
 * shape. Β΄, Γ΄, Δ΄, Βαρύς and Πλ.Δ΄ all go up 12 μόρια and back, so they differ only in absolute
 * pitch. Around the base, two φθόγγοι below and three above make all eight different; above the base
 * alone, Δ΄ is Πλ.Δ΄ and Α΄ is Πλ.Α΄. `EarTrainerPhraseTest` pins both.
 *
 * ## Where the pitches come from
 *
 * The ladder the 8 Ήχοι page draws ([ModeLadders]) and the ison's base ([IsonDrone.base]), nothing
 * written by hand. **No base shift** (owner decision, 2026-10-29): each ήχος sounds at its own
 * pitch, because the register is part of what tells some of them apart — Γ΄ and Πλ.Δ΄ are only 2
 * μόρια apart around the base, while their bases are 30 μόρια apart, and a user's «Μεταφορά βάσης»
 * moves one mode and not another. The voice's global shift is left out too, by the same decision.
 */
object EarTrainerPhrase {

    /** φθόγγοι above the base the phrase reaches. */
    const val STEPS_ABOVE = 3

    /** φθόγγοι below the base the phrase reaches. */
    const val STEPS_BELOW = 2

    /** How long each φθόγγος of the phrase is held. */
    const val NOTE_MILLIS = 450L

    /** The last φθόγγος, the base, is held longer: the phrase comes to rest there. */
    const val LAST_NOTE_MILLIS = 900L

    /** The phrase as offsets from the base, in rungs: see the class header. */
    private val OFFSETS = listOf(0, 1, 2, 3, 2, 1, 0, -1, -2, -1, 0)

    /** The rungs [mode]'s question plays, in order. */
    fun rungs(mode: Mode): List<ModeLadder.Step> {
        val window = window(mode)
        return OFFSETS.map { window.getValue(it) }
    }

    /** The rung the ison holds under [mode]'s question: the base of its απήχημα. */
    fun ison(mode: Mode): ModeLadder.Step = window(mode).getValue(0)

    /**
     * Where each φθόγγος of the window sits from the base, in μόρια, lowest first — the numbers the
     * answer shows, so the learner sees why the ήχοι differ.
     */
    fun moriaFromBase(mode: Mode): List<Int> {
        val window = window(mode)
        val base = window.getValue(0).moriaFromNi
        return (-STEPS_BELOW..STEPS_ABOVE).map { (window.getValue(it).moriaFromNi - base).value }
    }

    /** The rungs from [STEPS_BELOW] below the base to [STEPS_ABOVE] above it, by offset. */
    private fun window(mode: Mode): Map<Int, ModeLadder.Step> {
        val ascending = ModeLadders.ladder(mode, BaseShift.DEFAULT_MORIA).steps.reversed()
        val baseIndex = ascending.indexOfFirst { it.phthong == IsonDrone.base(mode) }
        check(baseIndex - STEPS_BELOW >= 0 && baseIndex + STEPS_ABOVE <= ascending.lastIndex) {
            "the ladder of $mode does not hold its base with $STEPS_BELOW φθόγγοι below and $STEPS_ABOVE above"
        }
        return (-STEPS_BELOW..STEPS_ABOVE).associateWith { ascending[baseIndex + it] }
    }
}
