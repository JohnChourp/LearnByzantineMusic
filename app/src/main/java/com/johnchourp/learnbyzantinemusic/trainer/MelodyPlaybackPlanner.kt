package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.Beats
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

/** One click of the «Ψάλλε μαζί» metronome; [downbeat] is the first χρόνος of a round, accented. */
data class PlannedTick(val startMillis: Long, val downbeat: Boolean)

/**
 * One round of the «Ψάλλε μαζί» loop (ClickUp `869f5x2cv`, J2): the melody's notes, the
 * metronome's clicks and the guide's level, all on one absolute timeline measured from the moment
 * the loop started.
 *
 * [melodyEndMillis] is where the last note ends; [endMillis] is where the next round starts. They
 * differ only when the melody does not fill whole χρόνοι — see [MelodyPlaybackPlanner.roundLength].
 */
data class PlannedRound(
    val index: Int,
    val startMillis: Long,
    val melodyEndMillis: Long,
    val endMillis: Long,
    /** How loud the guide melody plays in this round, 0 … 1: [MelodyPlaybackPlanner.guideGain]. */
    val guideGain: Float,
    val notes: List<PlannedNoteEvent>,
    val ticks: List<PlannedTick>,
)

/**
 * Pure, deterministic translation of a [MelodySequence] + [MelodyTempo] into an
 * absolute-time schedule of note events. It carries no Android dependency so it is fully
 * unit-testable, and it is shared by Mode 1 playback, the rhythm-timing mode and «Ψάλλε μαζί».
 *
 * Every note starts at its **exact** offset in χρόνοι from the start, converted to milliseconds
 * once, so rounding never adds up: three ⅓ notes at 60 bpm end at 1000 ms, not 999, and the
 * melody always ends where its total says (`MelodyTotalIsTheSumOfItsNotesTest`).
 *
 * «Ψάλλε μαζί» loops the melody without end, so it asks for one round at a time ([planRound]).
 * Round k is not the previous round plus its length in milliseconds — that would add a rounding
 * error every round — but k rounds from the start, in χρόνοι, rounded once: every note and every
 * click of every round sits exactly where the tempo puts it, however long the loop runs. The
 * metronome is part of the same plan, so it cannot drift from the notes either.
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
    ): List<PlannedNoteEvent> = planFrom(sequence, tempo, Beats.ZERO, frequencyOf)

    /** [plan], with the whole melody moved later by the exact length [startAt]: where a round starts. */
    private fun planFrom(
        sequence: MelodySequence,
        tempo: MelodyTempo,
        startAt: Beats,
        frequencyOf: (TrainerNote) -> Double,
    ): List<PlannedNoteEvent> {
        val durations = sequence.durations()
        var elapsed = startAt
        return sequence.notes.mapIndexed { index, note ->
            val startMillis = tempo.beatsToMillis(elapsed)
            elapsed += durations[index]
            PlannedNoteEvent(
                index = index,
                phthong = note.phthong,
                frequencyHz = frequencyOf(note),
                startMillis = startMillis,
                durationMillis = tempo.beatsToMillis(elapsed) - startMillis
            )
        }
    }

    fun totalDurationMillis(plan: List<PlannedNoteEvent>): Long =
        plan.lastOrNull()?.endMillis ?: 0L

    /**
     * How long one round of «Ψάλλε μαζί» lasts: the melody, rounded **up** to whole χρόνοι.
     *
     * The metronome clicks every χρόνος, and each round must start on a click — the accented one —
     * or the loop would begin off the beat. A melody of 2½ χρόνοι therefore loops every 3, the last
     * half χρόνος a rest. A melody that fills whole χρόνοι loops with no rest at all.
     */
    fun roundLength(sequence: MelodySequence): Beats {
        val total = sequence.total().ticks
        val wholeBeats = (total + Beats.TICKS_PER_BEAT - 1) / Beats.TICKS_PER_BEAT
        return Beats.whole(wholeBeats)
    }

    /**
     * The guide melody's level in [round] (0-based): full, half, then silent — and silent from then
     * on, for as long as the loop runs. The ison and the metronome are not on this curve.
     */
    fun guideGain(round: Int): Float = GUIDE_GAINS.getOrElse(round) { GUIDE_GAINS.last() }

    /**
     * Round [round] of «Ψάλλε μαζί», on the timeline that starts with round 0. Empty notes and
     * ticks for an empty melody, which has nothing to loop.
     */
    fun planRound(
        sequence: MelodySequence,
        tempo: MelodyTempo,
        round: Int,
        frequencyOf: (TrainerNote) -> Double,
    ): PlannedRound {
        require(round >= 0) { "round $round" }
        val length = roundLength(sequence)
        val start = length * round
        val notes = planFrom(sequence, tempo, start, frequencyOf)
        val beats = length.ticks / Beats.TICKS_PER_BEAT
        val ticks = List(beats) { beat ->
            PlannedTick(startMillis = tempo.beatsToMillis(start + Beats.whole(beat)), downbeat = beat == 0)
        }
        val startMillis = tempo.beatsToMillis(start)
        return PlannedRound(
            index = round,
            startMillis = startMillis,
            melodyEndMillis = notes.lastOrNull()?.endMillis ?: startMillis,
            endMillis = tempo.beatsToMillis(start + length),
            guideGain = guideGain(round),
            notes = notes,
            ticks = ticks,
        )
    }

    /** 100 % → 50 % → 0 %, then 0 % until «Στάση» (the J2 decision; nothing stops by itself). */
    private val GUIDE_GAINS = floatArrayOf(1f, 0.5f, 0f)
}
