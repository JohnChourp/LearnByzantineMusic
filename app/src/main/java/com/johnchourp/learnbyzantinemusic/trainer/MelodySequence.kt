package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.Beats
import com.johnchourp.learnbyzantinemusic.music.ByzantineRhythmMapper
import com.johnchourp.learnbyzantinemusic.music.RhythmNote
import com.johnchourp.learnbyzantinemusic.music.RhythmProblem

/**
 * An ordered melody of [TrainerNote]s, and the one place the Trainer asks what its time signs
 * allow (ClickUp `869f5x29r`, H5). Effective durations come from [ByzantineRhythmMapper], the
 * written table of time rules that the «Χαρακτήρες Χρόνου» page is checked against too: γοργόν
 * makes its note and the previous one share a χρόνο (½ + ½), κλάσμα adds a whole χρόνο, and so on.
 *
 * On top of those rules the Trainer has **input rules** — what its buttons let you write. They
 * live here, and the screen only reads them:
 *
 * | Input rule | Where |
 * |---|---|
 * | a note's own length is ½ … 4 χρόνοι, in steps of ½ | [MIN_LENGTH_BEATS], [MAX_LENGTH_BEATS], [LENGTH_STEP_BEATS] — the rules themselves know ⅓ and ¼ |
 * | γοργόν only where the rules allow one — never on the first note | [canToggleGorgon] |
 * | a γοργόν note's length is set by the rule: its ± buttons are off, and a length set before the γοργόν waits for it to be switched off | [canChangeLength] |
 * | a deletion never leaves a γοργόν on the first note | [normalised] |
 * | a melody holds at most [MAX_NOTES] notes, so every melody can be saved (ClickUp `869f5x261`) | [canAddNote] |
 *
 * The screen used to hold its own copies of the first-note check, in three places.
 */
class MelodySequence(val notes: List<TrainerNote>) {

    private val rhythm: List<RhythmNote> by lazy { notes.map { it.toRhythmNote() } }

    /** Effective duration of each note, in sung order, once the rhythm rules are applied. */
    fun durations(): List<Beats> = ByzantineRhythmMapper.durations(rhythm)

    /** Total length of the melody: the sum of its notes. */
    fun total(): Beats = ByzantineRhythmMapper.total(rhythm)

    /** Every time sign that breaks a rule; empty for any melody the Trainer lets you write. */
    fun problems(): List<RhythmProblem> = ByzantineRhythmMapper.problems(rhythm)

    val isValid: Boolean get() = problems().isEmpty()

    /**
     * Whether the γοργόν chip of the note at [index] can be pressed: switching one off always can,
     * switching one on only where the rules accept it — which is every note but the first.
     */
    fun canToggleGorgon(index: Int): Boolean {
        val note = notes.getOrNull(index) ?: return false
        if (note.hasGorgo) return true
        return MelodySequence(notes.toMutableList().also { it[index] = note.withGorgo(true) }).isValid
    }

    /** Whether the ± buttons of the note at [index] work: not while a γοργόν sets its length. */
    fun canChangeLength(index: Int): Boolean = notes.getOrNull(index)?.hasGorgo == false

    /** Whether one more note fits: a melody is capped so that it can always be saved. */
    fun canAddNote(): Boolean = notes.size < MAX_NOTES

    /**
     * This melody with every sign that breaks a rule taken off, and nothing else changed — e.g. the
     * γοργόν a deletion moved onto the first note.
     */
    fun normalised(): MelodySequence {
        var current = this
        while (true) {
            val problems = current.problems()
            if (problems.isEmpty()) return current
            current = MelodySequence(
                current.notes.mapIndexed { index, note ->
                    problems.filter { it.noteIndex == index }.fold(note) { kept, problem -> kept.withSign(problem.sign, false) }
                }
            )
        }
    }

    val size: Int get() = notes.size
    fun isEmpty(): Boolean = notes.isEmpty()

    companion object {
        /** The shortest length the ± buttons write. A Trainer input rule, not a rhythm rule. */
        const val MIN_LENGTH_BEATS = 0.5f

        /** The longest length the ± buttons write. */
        const val MAX_LENGTH_BEATS = 4f

        /** One press of a ± button. */
        const val LENGTH_STEP_BEATS = 0.5f

        /**
         * The most notes one melody holds. Generous for a line of a hymn, and it bounds what a saved
         * exercise can weigh in preferences: 50 exercises at the cap stay well under half a megabyte.
         */
        const val MAX_NOTES = 100

        /** A γοργόν note's own length is the rule's, whatever the ± buttons had set before. */
        private fun TrainerNote.toRhythmNote(): RhythmNote =
            RhythmNote(signs = signs, base = if (hasGorgo) Beats.ONE else Beats.nearest(baseDurationBeats))
    }
}
