package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.Beats
import com.johnchourp.learnbyzantinemusic.music.ByzantineRhythmMapper
import com.johnchourp.learnbyzantinemusic.music.RhythmNote
import com.johnchourp.learnbyzantinemusic.music.TimeSign

/**
 * The numbers of the Trainer's «Κανόνες χρόνου» card, read from the time rules instead of typed into
 * its text (ClickUp `869f5x29r`, H5).
 *
 * The card's sentences carry placeholders only — `TimingRulesHelpTextTest` fails if a digit creeps
 * back into them, in any language — and each number is what [ByzantineRhythmMapper] does to a small
 * melody, so the card cannot say one thing while the Trainer plays another. The screen prints them
 * with [BeatsLabel], like the lengths of the note rows.
 */
object TimingRulesHelp {

    private val plain = RhythmNote()
    private val gorgonPair = ByzantineRhythmMapper.durations(listOf(plain, RhythmNote(setOf(TimeSign.GORGON))))

    /** «Κάθε φθόγγος κρατάει …»: a note that carries no sign. */
    val defaultLength: Beats = ByzantineRhythmMapper.durations(listOf(plain)).single()

    /** «Ο φθόγγος γίνεται μισός χρόνος (…)»: the γοργόν note. */
    val gorgonNote: Beats = gorgonPair[1]

    /** «… και «τραβάει» … από τον προηγούμενο»: what the γοργόν takes from the note before it. */
    val gorgonTakes: Beats = defaultLength - gorgonPair[0]

    /** «Προσθέτει … ολόκληρο χρόνο»: what a κλάσμα adds to its note. */
    val klasmaAdds: Beats =
        ByzantineRhythmMapper.durations(listOf(RhythmNote(setOf(TimeSign.KLASMA)))).single() - defaultLength
}
