package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * «φθόγγοι ↔ συλλαβές» (ClickUp `869f5x2cv`, J2): once the παραλλαγή is learnt, the line shows the
 * syllables of the text instead of the names of the φθόγγοι — the old method's step to the μέλος. A
 * note without a syllable keeps its φθόγγος, and a syllable never changes how a note sounds or lasts.
 */
class SyllableLineTest {

    private val line = listOf(
        TrainerNote(PhthongName.PA, syllable = "Κύ"),
        TrainerNote(PhthongName.VOU, octaveShift = 1),
        TrainerNote(PhthongName.GA, syllable = "ε"),
    )

    private fun shown(showSyllables: Boolean): List<String> = line.map { SingAlong.lineLabel(it, showSyllables) }

    @Test
    fun theToggleSwitchesTheLineBetweenPhthongiAndSyllables() {
        assertEquals(listOf("Πα", "Βου΄", "Γα"), shown(showSyllables = false))
        assertEquals("a note with no syllable keeps its φθόγγος", listOf("Κύ", "Βου΄", "ε"), shown(showSyllables = true))
        assertEquals("and back", listOf("Πα", "Βου΄", "Γα"), shown(showSyllables = false))
    }

    @Test
    fun aSyllableIsStoredTrimmedShortAndNeverBlank() {
        assertEquals("Κύ", TrainerNote.cleanSyllable("  Κύ \t"))
        assertEquals("ρι ε", TrainerNote.cleanSyllable("ρι \n  ε"))
        assertEquals("cut to 12 characters", "Κυριε ελέησο", TrainerNote.cleanSyllable("Κυριε ελέησον"))
        assertEquals(TrainerNote.MAX_SYLLABLE_LENGTH, TrainerNote.cleanSyllable("α".repeat(40))!!.length)
        assertNull(TrainerNote.cleanSyllable("   "))
        assertNull(TrainerNote.cleanSyllable(""))
        assertNull(TrainerNote.cleanSyllable(null))
    }

    @Test
    fun typingGoesThroughTheSameCleaningAndBlankTakesTheSyllableOff() {
        val note = TrainerNote(PhthongName.DI)
        assertEquals("λέ", note.withSyllable(" λέ ").syllable)
        assertNull(note.withSyllable("λέ").withSyllable("  ").syllable)
        assertThrows("a note never holds an uncleaned syllable", IllegalArgumentException::class.java) {
            TrainerNote(PhthongName.DI, syllable = " λέ")
        }
    }

    @Test
    fun aSyllableChangesNeitherTheTimingNorThePitch() {
        val plain = MelodySequence(line.map { it.withSyllable(null) })
        val sung = MelodySequence(line)
        assertEquals(plain.durations(), sung.durations())
        assertEquals(plain.notes.map { it.frequencyHz }, sung.notes.map { it.frequencyHz })
        assertEquals(
            MelodyPlaybackPlanner.plan(plain, MelodyTempo(90)),
            MelodyPlaybackPlanner.plan(sung, MelodyTempo(90)),
        )
    }
}
