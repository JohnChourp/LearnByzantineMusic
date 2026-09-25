package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.ByzantineRhythmMapper
import com.johnchourp.learnbyzantinemusic.music.TimeSign

/**
 * One entry in a trainer melody: a phthong at an octave shift, a base duration in
 * χρόνοι (beats), and the time signs it carries (γοργόν, κλάσμα, …) as typed [TimeSign]s.
 * They are timed by [ByzantineRhythmMapper], the one table of time rules that the
 * «Χαρακτήρες Χρόνου» page is checked against too; the Trainer offers only the γοργόν.
 */
data class TrainerNote(
    val phthong: TrainerPhthong,
    val octaveShift: Int = 0,
    val baseDurationBeats: Float = 1f,
    val signs: Set<TimeSign> = emptySet()
) {
    val frequencyHz: Double get() = TrainerPitchTable.frequencyHz(phthong, octaveShift)

    val hasGorgo: Boolean get() = TimeSign.GORGON in signs

    fun withGorgo(enabled: Boolean): TrainerNote = withSign(TimeSign.GORGON, enabled)

    fun withSign(sign: TimeSign, enabled: Boolean): TrainerNote {
        if ((sign in signs) == enabled) return this
        return copy(signs = if (enabled) signs + sign else signs - sign)
    }
}
