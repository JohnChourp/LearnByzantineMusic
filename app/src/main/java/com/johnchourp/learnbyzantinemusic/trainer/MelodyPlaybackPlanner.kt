package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.Beats

/** One scheduled note in a playback plan, with absolute start/duration in milliseconds. */
data class PlannedNoteEvent(
    val index: Int,
    val phthong: TrainerPhthong,
    val frequencyHz: Double,
    val startMillis: Long,
    val durationMillis: Long
) {
    val endMillis: Long get() = startMillis + durationMillis
}

/**
 * Pure, deterministic translation of a [MelodySequence] + [MelodyTempo] into an
 * absolute-time schedule of note events. It carries no Android dependency so it is fully
 * unit-testable, and it is shared by Mode 1 playback and (later) the rhythm-timing mode.
 *
 * Every note starts at its **exact** offset in χρόνοι from the start, converted to milliseconds
 * once, so rounding never adds up: three ⅓ notes at 60 bpm end at 1000 ms, not 999, and the
 * melody always ends where its total says (`MelodyTotalIsTheSumOfItsNotesTest`).
 */
object MelodyPlaybackPlanner {

    fun plan(sequence: MelodySequence, tempo: MelodyTempo): List<PlannedNoteEvent> {
        val durations = sequence.durations()
        var elapsed = Beats.ZERO
        return sequence.notes.mapIndexed { index, note ->
            val startMillis = tempo.beatsToMillis(elapsed)
            elapsed += durations[index]
            PlannedNoteEvent(
                index = index,
                phthong = note.phthong,
                frequencyHz = note.frequencyHz,
                startMillis = startMillis,
                durationMillis = tempo.beatsToMillis(elapsed) - startMillis
            )
        }
    }

    fun totalDurationMillis(plan: List<PlannedNoteEvent>): Long =
        plan.lastOrNull()?.endMillis ?: 0L
}
