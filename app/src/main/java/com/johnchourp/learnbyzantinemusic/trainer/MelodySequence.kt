package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.analysis.ByzantineRhythmMapper
import com.johnchourp.learnbyzantinemusic.analysis.NeumeRhythmInput

/**
 * An ordered melody of [TrainerNote]s. Effective per-note durations are derived from
 * each note's base duration plus its Byzantine rhythm modifiers, reusing the same
 * [ByzantineRhythmMapper] rules the notation analyzer applies: γοργόν makes its own note
 * 0.5 χρόνου and shortens the previous note by 0.5 (two phthongi in one χρόνο), κλάσμα
 * (fraction) adds a whole χρόνο, and so on.
 */
class MelodySequence(
    val notes: List<TrainerNote>,
    private val rhythmMapper: ByzantineRhythmMapper = ByzantineRhythmMapper()
) {
    /** Effective duration in χρόνοι for each note after applying rhythm rules. */
    fun effectiveDurationsBeats(): List<Float> =
        rhythmMapper.map(
            notes.map { note ->
                NeumeRhythmInput(modifiers = note.modifiers, baseDurationBeats = note.baseDurationBeats)
            }
        )

    /** Total length of the melody in χρόνοι once rhythm rules are applied. */
    fun totalBeats(): Float = effectiveDurationsBeats().sum()

    val size: Int get() = notes.size
    fun isEmpty(): Boolean = notes.isEmpty()
}
