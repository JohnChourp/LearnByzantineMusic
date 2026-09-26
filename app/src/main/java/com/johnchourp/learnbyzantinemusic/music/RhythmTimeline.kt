package com.johnchourp.learnbyzantinemusic.music

/**
 * A rhythm laid out on **one** clock: every note, rest and metronome click at its absolute time from
 * the start (ClickUp `869f5x25n`, F4 — «Άκου τον ρυθμό» on the «Χαρακτήρες Χρόνου» page).
 *
 * The note lengths are [ByzantineRhythmMapper]'s, the one table of time rules, and each note starts at
 * its **exact** offset in χρόνοι converted to milliseconds once ([Beats.millisAt]): a ⅓ never becomes
 * 333 ms three times over, and the example ends where its total says. The clicks are placed on the
 * same clock, one per χρόνος from the start, the first accented. A player that fires each cue at its
 * own time — never after the one before it — keeps the notes, the clicks and the highlight together;
 * they cannot drift apart because nothing counts on anything else.
 *
 * Pure Kotlin, so the whole schedule is unit-tested (`TimeExampleScheduleTest`).
 */
object RhythmTimeline {

    /** One thing that happens at [atMillis] after the start. */
    sealed interface Cue {
        val atMillis: Long
    }

    /** Note [note] starts sounding at [frequencyHz]; the one before it stops. */
    data class Sound(val note: Int, val frequencyHz: Double, override val atMillis: Long) : Cue

    /** Note [note] is a rest: whatever sounds stops, and nothing starts. */
    data class Silence(val note: Int, override val atMillis: Long) : Cue

    /** χρόνος [beat] is counted: a click, [accented] on the first. */
    data class Click(val beat: Int, val accented: Boolean, override val atMillis: Long) : Cue

    /** The rhythm is over: everything stops. */
    data class End(override val atMillis: Long) : Cue

    /**
     * The cues of [notes] at [beatsPerMinute], in time order. [pitches] gives each note's frequency, and
     * null exactly for the rests. At the same instant a note comes before its click and the end last,
     * so the highlight and the sound of a χρόνος's first note arrive together.
     */
    fun of(notes: List<RhythmNote>, pitches: List<Double?>, beatsPerMinute: Int): List<Cue> {
        require(pitches.size == notes.size) { "${notes.size} notes but ${pitches.size} pitches" }
        require(ByzantineRhythmMapper.problems(notes).isEmpty()) { "only a melody the time rules accept can be played" }
        notes.forEachIndexed { index, note ->
            require(note.isRest == (pitches[index] == null)) { "note $index: a rest has no pitch, a note has one" }
        }
        val cues = mutableListOf<Cue>()
        var elapsed = Beats.ZERO
        ByzantineRhythmMapper.durations(notes).forEachIndexed { index, duration ->
            val at = elapsed.millisAt(beatsPerMinute)
            val hz = pitches[index]
            cues += if (hz == null) Silence(index, at) else Sound(index, hz, at)
            elapsed += duration
        }
        val beats = (elapsed.ticks + Beats.TICKS_PER_BEAT - 1) / Beats.TICKS_PER_BEAT
        (0 until beats).forEach { beat ->
            cues += Click(beat, accented = beat == 0, atMillis = Beats.whole(beat).millisAt(beatsPerMinute))
        }
        cues += End(elapsed.millisAt(beatsPerMinute))
        return cues.sortedWith(compareBy({ it.atMillis }, { order(it) }))
    }

    private fun order(cue: Cue): Int = when (cue) {
        is Sound, is Silence -> 0
        is Click -> 1
        is End -> 2
    }
}
