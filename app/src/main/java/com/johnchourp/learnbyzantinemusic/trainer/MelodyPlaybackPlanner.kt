package com.johnchourp.learnbyzantinemusic.trainer

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
 */
object MelodyPlaybackPlanner {

    fun plan(sequence: MelodySequence, tempo: MelodyTempo): List<PlannedNoteEvent> {
        val durations = sequence.effectiveDurationsBeats()
        var cursor = 0L
        return sequence.notes.mapIndexed { index, note ->
            val durationMillis = tempo.beatsToMillis(durations[index])
            PlannedNoteEvent(
                index = index,
                phthong = note.phthong,
                frequencyHz = note.frequencyHz,
                startMillis = cursor,
                durationMillis = durationMillis
            ).also { cursor += durationMillis }
        }
    }

    fun totalDurationMillis(plan: List<PlannedNoteEvent>): Long =
        plan.lastOrNull()?.endMillis ?: 0L
}
