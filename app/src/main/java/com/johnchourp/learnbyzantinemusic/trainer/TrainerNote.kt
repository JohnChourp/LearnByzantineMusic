package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.ByzantineRhythmMapper
import com.johnchourp.learnbyzantinemusic.music.Phthong
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.music.TimeSign

/**
 * One entry in a trainer melody: a phthong at an octave shift, a base duration in
 * χρόνοι (beats), and the time signs it carries (γοργόν, κλάσμα, …) as typed [TimeSign]s.
 * They are timed by [ByzantineRhythmMapper], the one table of time rules that the
 * «Χαρακτήρες Χρόνου» page is checked against too; the Trainer offers only the γοργόν.
 *
 * A note may also carry the [syllable] of the text sung on it (ClickUp `869f5x2cv`, J2): once the
 * παραλλαγή is learnt, «Ψάλλε μαζί» shows the syllables instead of the φθόγγοι, which is how the
 * old method moves from the παραλλαγή to the μέλος. It never changes how the note sounds or lasts.
 */
data class TrainerNote(
    val phthong: PhthongName,
    val octaveShift: Int = 0,
    val baseDurationBeats: Float = 1f,
    val signs: Set<TimeSign> = emptySet(),
    /** The syllable sung on this note, or null; always in [cleanSyllable]'s form. */
    val syllable: String? = null,
) {
    init {
        require(syllable == cleanSyllable(syllable)) { "a syllable is stored trimmed, short and never blank: '$syllable'" }
    }

    val frequencyHz: Double get() = TrainerPitchTable.frequencyHz(phthong, octaveShift)

    /**
     * The φθόγγος with its octave, as the app's one model. The Trainer shows `pitch.label`, the same
     * renderer as every other screen, instead of the copy it used to build by hand.
     */
    val pitch: Phthong get() = Phthong(phthong, octaveShift)

    val hasGorgo: Boolean get() = TimeSign.GORGON in signs

    fun withGorgo(enabled: Boolean): TrainerNote = withSign(TimeSign.GORGON, enabled)

    fun withSign(sign: TimeSign, enabled: Boolean): TrainerNote {
        if ((sign in signs) == enabled) return this
        return copy(signs = if (enabled) signs + sign else signs - sign)
    }

    /** This note with the syllable the learner typed, cleaned; blank removes it. */
    fun withSyllable(typed: String?): TrainerNote = copy(syllable = cleanSyllable(typed))

    companion object {
        /** The longest syllable a note keeps: a syllable, or a short word, never a line of text. */
        const val MAX_SYLLABLE_LENGTH = 12

        /**
         * [raw] as a note stores it: every run of spaces, line breaks or control characters made one
         * space, none at either end, cut to [MAX_SYLLABLE_LENGTH] characters, and null when nothing is
         * left. Typing, reading a saved melody and importing one all go through here, so a note holds
         * one form only — and a pasted control character can never reach the stored text.
         */
        fun cleanSyllable(raw: String?): String? {
            if (raw == null) return null
            val text = buildString {
                var gap = false
                for (char in raw) {
                    if (char.isWhitespace() || char.isISOControl()) {
                        gap = isNotEmpty()
                    } else {
                        if (gap) append(' ')
                        gap = false
                        append(char)
                    }
                }
            }
            return text.take(MAX_SYLLABLE_LENGTH).trimEnd().takeIf { it.isNotEmpty() }
        }
    }
}
