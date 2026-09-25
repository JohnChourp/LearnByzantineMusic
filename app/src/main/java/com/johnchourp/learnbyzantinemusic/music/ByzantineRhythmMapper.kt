package com.johnchourp.learnbyzantinemusic.music

import com.johnchourp.learnbyzantinemusic.music.RhythmProblem.Reason

/**
 * The time rules — how long each sung note lasts under the time signs it carries — written **once**,
 * as the tables below, and applied by [durations] (ClickUp `869f5x29r`, H5).
 *
 * The Melody Trainer plays and scores by these rules (`MelodySequence`), and every worked example of
 * the «Χαρακτήρες Χρόνου» page is checked against them (`TimeCharactersFollowTheRulesTest`), so the
 * page and the Trainer cannot disagree. Before H5 the rules lived in `analysis/`, a package left over
 * from the removed notation scanner: free strings instead of signs, three rules that were written
 * nowhere, and a floor of ½ χρόνου that made ⅓ and ¼ impossible.
 *
 * A melody is a list of [RhythmNote]s in sung order. Every note starts with its own length — one
 * χρόνος, unless the Trainer set another — plus what its additive signs add; the dividers then make
 * groups of notes share one χρόνο. Lengths are exact [Beats]: ⅓ is a third, not 0.333…. Every row and
 * every edge case below has its own test in `TimeRulesTest`, named after it.
 *
 * ## The rules
 *
 * The *carrier* is the note the sign is written on; *previous* and *next* are its neighbours.
 *
 * | Sign ([TimeSign]) | Times | Durations |
 * |---|---|---|
 * | none | the note | its own length: 1 |
 * | κλάσμα, απλή | the carrier | + 1 |
 * | διπλή | the carrier | + 2 |
 * | τριπλή | the carrier | + 3 |
 * | γοργόν | previous + carrier share one χρόνο | ½ + ½ |
 * | παρεστιγμένο γοργόν, στιγμή αριστερά / δεξιά | previous + carrier | ¾ + ¼ / ¼ + ¾ — the dot's side gets ¾ |
 * | δίγοργον | previous + carrier + next | ⅓ + ⅓ + ⅓ |
 * | παρεστιγμένο δίγοργον, στιγμή κάτω / μέση / πάνω | previous + carrier + next | ½ ¼ ¼ / ¼ ½ ¼ / ¼ ¼ ½ — the dotted note gets ½ |
 * | τρίγοργον | previous + carrier + the next two | ¼ + ¼ + ¼ + ¼ |
 * | αργόν | the two notes before the carrier share one χρόνο; the carrier (the ολίγον) is lengthened | ½ + ½ + 2 |
 * | δίαργον | as αργόν | ½ + ½ + 3 |
 * | τρίαργον | as αργόν | ½ + ½ + 4 |
 *
 * **Sharing keeps the rest.** A note that shares a χρόνο gives that one χρόνος to the group and keeps
 * whatever it lasts beyond it. The notes before the carrier give their *last* χρόνο, the carrier and
 * the notes after it their *first*: a note with κλάσμα (2) followed by a γοργόν lasts 1½, and a
 * γοργόν note that also carries a κλάσμα lasts ½ + 1.
 *
 * The αργόν family is timed exactly as the «Χαρακτήρες Χρόνου» page reads it: ίσον ½, κεντήματα ½
 * (as with a γοργόν), ολίγον 2 — three χρόνοι in all, the ολίγον alone lasting two (δίαργον 3,
 * τρίαργον 4). In other words αργόν = a γοργόν on the κεντήματα + a κλάσμα on the ολίγον, and δίαργον
 * and τρίαργον put a διπλή or τριπλή there instead.
 *
 * ## Edge cases — each one a decision
 *
 * | Case | Decision |
 * |---|---|
 * | γοργόν on the first note | **invalid**: there is no previous note to share a χρόνο with ([Reason.NO_NOTE_BEFORE]) |
 * | γοργόν + κλάσμα on the same note | **additive**: previous ½, the carrier ½ + 1 = 1½ |
 * | consecutive γοργά | a **run of ½**: the note before the first γοργόν ½, and every γοργόν note ½ |
 * | a γοργόν after a note with no whole χρόνος left to give — it already shares its only one, or is shorter than one (the Trainer's ½) | the γοργόν takes nothing from it and the run of halves goes on: this is what makes consecutive γοργά a run |
 * | a divider missing a neighbour: δίγοργον on the last note, αργόν on the second | **invalid** ([Reason.NO_NOTE_BEFORE], [Reason.NO_NOTE_AFTER]) |
 * | two dividers on one note | **invalid** ([Reason.TWO_DIVIDERS]) |
 * | a divider asking for a χρόνο that is already shared (a δίγοργον right after a γοργόν note) | **invalid** ([Reason.NO_BEAT_TO_SHARE]); only the plain γοργόν continues a run |
 * | shorter than ½ | **allowed**: ⅓ and ¼ are ordinary lengths here. The ½ floor is the Trainer's input rule — what its ± buttons let you write — and lives in `MelodySequence` |
 *
 * An invalid divider is listed by [problems] and shares nothing; the χρόνοι a sign adds always count.
 * Whoever builds a melody decides what to do about a problem: the Trainer never keeps one.
 */
object ByzantineRhythmMapper {

    /** How long each note of [notes] lasts, in sung order. */
    fun durations(notes: List<RhythmNote>): List<Beats> = Timing(notes).durations

    /** Every sign in [notes] that breaks a rule, in note order; empty when the melody is valid. */
    fun problems(notes: List<RhythmNote>): List<RhythmProblem> = Timing(notes).problems

    /** The length of the whole melody: the sum of its notes. */
    fun total(notes: List<RhythmNote>): Beats = durations(notes).fold(Beats.ZERO) { sum, beats -> sum + beats }

    /** One pass over the melody, carrier by carrier, left to right. */
    private class Timing(notes: List<RhythmNote>) {
        val durations: List<Beats>
        val problems: List<RhythmProblem>

        init {
            val length = notes.map { note ->
                note.signs.fold(note.base) { sum, sign -> sum + Beats.whole(sign.addsBeats) }
            }.toMutableList()
            // The whole χρόνοι a note can still give to a shared one: its first and its last. Under two
            // χρόνοι they are the same χρόνος, so giving it either way gives it away.
            val single = BooleanArray(notes.size) { length[it] < Beats.whole(2) }
            val firstFree = BooleanArray(notes.size) { length[it] >= Beats.ONE }
            val lastFree = firstFree.copyOf()
            val found = mutableListOf<RhythmProblem>()

            fun give(index: Int, last: Boolean) {
                if (single[index] || last) lastFree[index] = false
                if (single[index] || !last) firstFree[index] = false
            }

            notes.forEachIndexed { i, note ->
                val dividers = note.signs.filter { it.isDivider }.sortedBy { it.ordinal }
                if (dividers.size > 1) {
                    dividers.forEach { found += RhythmProblem(i, it, Reason.TWO_DIVIDERS) }
                    return@forEachIndexed
                }
                val sign = dividers.singleOrNull() ?: return@forEachIndexed
                val missing = when {
                    i < sign.notesBefore -> Reason.NO_NOTE_BEFORE
                    i + sign.notesAfter > notes.lastIndex -> Reason.NO_NOTE_AFTER
                    else -> null
                }
                if (missing != null) {
                    found += RhythmProblem(i, sign, missing)
                    return@forEachIndexed
                }
                val first = i + sign.firstShareOffset
                val group = sign.shares.indices.map { first + it }
                // A γοργόν after a note with no whole χρόνος left to give — a γοργόν note itself, or a
                // note shorter than a χρόνος — takes nothing from it: the run of halves goes on.
                val givers = if (sign == TimeSign.GORGON && !lastFree[i - 1]) listOf(i) else group
                if (!givers.all { j -> if (j < i) lastFree[j] else firstFree[j] }) {
                    found += RhythmProblem(i, sign, Reason.NO_BEAT_TO_SHARE)
                    return@forEachIndexed
                }
                givers.forEach { j ->
                    give(j, last = j < i)
                    length[j] = length[j] - Beats.ONE + sign.shares[j - first]
                }
            }
            durations = length.toList()
            problems = found.toList()
        }
    }
}

/** One sung note for the time rules: the [TimeSign]s written on it and its own length before them. */
data class RhythmNote(
    val signs: Set<TimeSign> = emptySet(),
    /** One χρόνος in written music. The Melody Trainer lets you set others: its input rules, `MelodySequence`. */
    val base: Beats = Beats.ONE,
) {
    init {
        require(base > Beats.ZERO) { "a note lasts some time, not $base" }
    }
}

/** A sign that cannot do its work where it is written, and why. [noteIndex] is the note it is written on. */
data class RhythmProblem(val noteIndex: Int, val sign: TimeSign, val reason: Reason) {
    enum class Reason {
        /** It needs a note before its own that the melody does not have: a γοργόν on the first note. */
        NO_NOTE_BEFORE,

        /** It needs a note after its own that the melody does not have: a δίγοργον on the last note. */
        NO_NOTE_AFTER,

        /** Its note carries a second divider, and nothing says which of the two times the note. */
        TWO_DIVIDERS,

        /** A note it would take a χρόνο from has already given that χρόνος to another shared one. */
        NO_BEAT_TO_SHARE,
    }
}
