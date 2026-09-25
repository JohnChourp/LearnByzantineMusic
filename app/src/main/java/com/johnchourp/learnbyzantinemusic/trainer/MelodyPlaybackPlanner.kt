package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.PhthongName

/** One scheduled note in a playback plan, with absolute start/duration in milliseconds. */
data class PlannedNoteEvent(
    val index: Int,
    val phthong: PhthongName,
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
 */
object MelodyPlaybackPlanner {

    /** The schedule on the fixed diatonic table ([TrainerNote.frequencyHz], Νη = 220 Hz). */
    fun plan(sequence: MelodySequence, tempo: MelodyTempo): List<PlannedNoteEvent> =
        plan(sequence, tempo) { it.frequencyHz }

    /**
     * The same schedule, with each note's pitch resolved by [frequencyOf]. The Trainer passes its
     * scale ([TrainerScale.frequencyHz]), so it plays the ladder it listens on (ClickUp `869f5x24v`).
     * Timing does not depend on pitch: both overloads produce identical start times and durations.
     */
    fun plan(
        sequence: MelodySequence,
        tempo: MelodyTempo,
        frequencyOf: (TrainerNote) -> Double,
    ): List<PlannedNoteEvent> {
        val durations = sequence.effectiveDurationsBeats()
        var cursor = 0L
        return sequence.notes.mapIndexed { index, note ->
            val durationMillis = tempo.beatsToMillis(durations[index])
            PlannedNoteEvent(
                index = index,
                phthong = note.phthong,
                frequencyHz = frequencyOf(note),
                startMillis = cursor,
                durationMillis = durationMillis
            ).also { cursor += durationMillis }
        }
    }

    fun totalDurationMillis(plan: List<PlannedNoteEvent>): Long =
        plan.lastOrNull()?.endMillis ?: 0L
}
