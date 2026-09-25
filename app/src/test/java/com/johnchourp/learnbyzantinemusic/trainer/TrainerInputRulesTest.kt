package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.docs.KotlinSource
import com.johnchourp.learnbyzantinemusic.music.Beats
import com.johnchourp.learnbyzantinemusic.music.RhythmProblem
import com.johnchourp.learnbyzantinemusic.music.RhythmProblem.Reason
import com.johnchourp.learnbyzantinemusic.music.TimeSign
import com.johnchourp.learnbyzantinemusic.trainer.TrainerPhthong.NI
import com.johnchourp.learnbyzantinemusic.trainer.TrainerPhthong.PA
import com.johnchourp.learnbyzantinemusic.trainer.TrainerPhthong.VOU
import com.johnchourp.learnbyzantinemusic.trainer.ui.TrainerNoteUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The Melody Trainer's input rules live in [MelodySequence], and the screen only reads them (ClickUp
 * `869f5x29r`, H5). Before H5 «no γοργόν on the first note» was checked in three places of the screen
 * code — the chip, the toggle and the clean-up after a deletion — and «a γοργόν fixes the length» in
 * two, with nothing tying the copies together.
 */
class TrainerInputRulesTest {

    private val half = Beats.of(1, 2)

    private fun note(phthong: TrainerPhthong = NI, beats: Float = 1f) = TrainerNote(phthong, baseDurationBeats = beats)

    private fun melody(vararg notes: TrainerNote) = MelodySequence(notes.toList())

    @Test
    fun theFirstNoteNeverOffersAGorgon() {
        val sequence = melody(note(), note(), note())
        assertFalse(sequence.canToggleGorgon(0))
        assertTrue(sequence.canToggleGorgon(1))
        assertTrue(sequence.canToggleGorgon(2))
        // It is the time rules that say so: there is no previous note to share a χρόνο with.
        assertEquals(
            listOf(RhythmProblem(0, TimeSign.GORGON, Reason.NO_NOTE_BEFORE)),
            melody(note().withGorgo(true), note()).problems(),
        )
    }

    @Test
    fun everyOtherNoteOffersAGorgonWhateverComesBeforeIt() {
        // After a γοργόν note (a run), after a ½ note, after a 4 χρόνοι note.
        val sequence = melody(note(), note().withGorgo(true), note(beats = 0.5f), note(beats = 4f), note())
        (1..4).forEach { assertTrue("note $it", sequence.canToggleGorgon(it)) }
        // Switching one off is always possible; a note that is not there never is.
        assertTrue(sequence.canToggleGorgon(1))
        assertFalse(sequence.canToggleGorgon(5))
        assertFalse(sequence.canToggleGorgon(-1))
    }

    @Test
    fun deletingTheFirstNoteTakesTheGorgonOffTheNewFirstNote() {
        // What the screen does on «remove»: drop the note, then normalise what is left.
        val left = MelodySequence(listOf(note(NI), note(PA).withGorgo(true), note(VOU).withGorgo(true)).drop(1))
        assertFalse(left.isValid)
        val kept = left.normalised()
        assertEquals(listOf(note(PA), note(VOU).withGorgo(true)), kept.notes)
        assertTrue(kept.isValid)
    }

    @Test
    fun normalisingAMelodyTheRulesAcceptChangesNothing() {
        val sequence = melody(note(), note(PA).withGorgo(true), note(VOU, beats = 2.5f), note().withGorgo(true))
        assertEquals(sequence.notes, sequence.normalised().notes)
    }

    @Test
    fun aGorgonNoteLengthIsTheRulesNotTheButtons() {
        val long = note(PA, beats = 2f)
        val sequence = melody(note(), long.withGorgo(true))
        assertTrue(sequence.canChangeLength(0))
        assertFalse(sequence.canChangeLength(1))
        // The 2 χρόνοι it had before are set aside while the γοργόν is on…
        assertEquals(listOf(half, half), sequence.durations())
        // …and come back when it is switched off.
        assertEquals(listOf(Beats.ONE, Beats.whole(2)), melody(note(), long.withGorgo(true).withGorgo(false)).durations())
    }

    @Test
    fun theButtonsWriteHalvesFromHalfABeatToFour() {
        // The ½ floor is the Trainer's, not the rules': the rules themselves time ⅓ and ¼ (TimeRulesTest).
        assertEquals(0.5f, MelodySequence.MIN_LENGTH_BEATS)
        assertEquals(4f, MelodySequence.MAX_LENGTH_BEATS)
        assertEquals(0.5f, MelodySequence.LENGTH_STEP_BEATS)
    }

    @Test
    fun theNoteRowReadsTheRulesAndHasNoCopyOfItsOwn() {
        fun row(index: Int, editable: Boolean = true, length: Boolean = true, gorgon: Boolean = true) = TrainerNoteUi(
            index = index, phthongLabel = "Νη", beatsLabel = "1", hasGorgo = false, editable = editable,
            matched = false, active = false, lengthChangeable = length, gorgoToggleable = gorgon,
        )
        // The chip follows MelodySequence alone — no check of the index of its own…
        assertTrue(row(0, gorgon = true).gorgoEnabled)
        assertFalse(row(3, gorgon = false).gorgoEnabled)
        // …and the ± buttons too — no check of the γοργόν of their own.
        assertTrue(row(2, length = true).durationEditable)
        assertFalse(row(2, length = false).durationEditable)
        // While a mode runs, nothing is editable.
        assertFalse(row(3, editable = false).gorgoEnabled)
        assertFalse(row(3, editable = false).durationEditable)
    }

    /** «Not the first note», in the three spellings the screen code used. */
    private val firstNoteCheck = Regex("""\bindex\s*(?:==\s*0|>\s*0|>=\s*1)\b""")

    /** «A γοργόν fixes the length», in the two spellings the screen code used. */
    private val gorgonLengthCheck = Regex("""hasGorgo\s*\)\s*return\b|&&\s*!\s*(?:\w+\.)?hasGorgo\b""")

    @Test
    fun theScreenCodeAsksTheMelodyInsteadOfCheckingItself() {
        val trainer = File(KotlinSource.mainRoot, "com/johnchourp/learnbyzantinemusic/trainer")
        val screen = listOf(File(trainer, "MelodyTrainerActivity.kt")) +
            File(trainer, "ui").listFiles().orEmpty().filter { it.extension == "kt" }
        assertTrue("found ${screen.map { it.name }}", screen.size >= 3 && screen.all { it.isFile })
        val copies = screen.filter { file ->
            val code = KotlinSource.withoutComments(file.readText())
            firstNoteCheck.containsMatchIn(code) || gorgonLengthCheck.containsMatchIn(code)
        }.map { it.name }
        assertEquals("ask MelodySequence.canToggleGorgon / canChangeLength instead", emptyList<String>(), copies)
    }

    @Test
    fun theChecksStillCatchTheCopiesH5Removed() {
        // The positive control: each copy that was in the screen code before H5.
        listOf(
            "if (index == 0) return // γοργόν needs a previous note to shorten",
            "val gorgoEnabled: Boolean get() = editable && index > 0",
        ).forEach { assertTrue(it, firstNoteCheck.containsMatchIn(KotlinSource.withoutComments(it))) }
        listOf(
            "if (note.hasGorgo) return",
            "val durationEditable: Boolean get() = editable && !hasGorgo",
        ).forEach { assertTrue(it, gorgonLengthCheck.containsMatchIn(KotlinSource.withoutComments(it))) }
        // And the code that replaced them passes.
        listOf(
            "if (!MelodySequence(notes.toList()).canToggleGorgon(index)) return",
            "notes[index] = note.withGorgo(!note.hasGorgo)",
            "val gorgoEnabled: Boolean get() = editable && gorgoToggleable",
            "val active = index == activeIndex",
        ).forEach {
            assertFalse(it, firstNoteCheck.containsMatchIn(it) || gorgonLengthCheck.containsMatchIn(it))
        }
    }
}
