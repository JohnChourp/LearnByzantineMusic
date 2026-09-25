package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.modes.EightModeScaleDefinitions
import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.Moria
import com.johnchourp.learnbyzantinemusic.music.Phthong
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every note the Trainer lets you write has a pitch in every ήχος, at every «Μεταφορά βάσης» the
 * range allows — and the Trainer hears that pitch back as the note it is (ClickUp `869f5x24v`, F2).
 *
 * The range is iterated from [BaseShift], not written out, so when ClickUp `869f5x2dd` (J4) widens
 * it this test widens with it.
 */
class TrainerScaleCoversEveryNoteTest {

    private val notes: List<TrainerNote> = PhthongName.entries.flatMap { phthong ->
        (TrainerScale.MIN_NOTE_OCTAVE..TrainerScale.MAX_NOTE_OCTAVE).map { octave -> TrainerNote(phthong, octave) }
    }

    private val scales: List<TrainerScale> =
        listOf(TrainerScale.DIATONIC) +
            Mode.entries.flatMap { mode -> BaseShift.RANGE.map { shift -> TrainerScale(mode, shift) } }

    @Test
    fun theSweepIsEveryModeAtEveryShift() {
        // Guards the slice: an empty sweep would pass everything below.
        assertEquals(21, notes.size)
        assertEquals(1 + Mode.entries.size * BaseShift.RANGE.count(), scales.size)
    }

    @Test
    fun everyNoteHasAPitchAndIsHeardBackAsItself() {
        scales.forEach { scale ->
            notes.forEach { note ->
                val hz = scale.frequencyHz(note)
                assertTrue("$scale ${note.pitch.label}: $hz", hz.isFinite() && hz > 0.0)
                val heard = scale.match(hz)!!
                assertEquals("$scale ${note.pitch.label}", note.phthong, heard.phthong)
                assertEquals("$scale ${note.pitch.label}", 0.0, heard.deviationMoria, 1e-9)
            }
        }
    }

    @Test
    fun theLowestNiIsOnTheTrainersLadderThoughNotOnTheDiagrams() {
        // The range gap: a Πα-based ladder built the 8 Ήχοι way starts at Πα one octave down.
        val lowestNi = Phthong(PhthongName.NI, TrainerScale.MIN_NOTE_OCTAVE)
        assertNull(EightModeScaleDefinitions.DIATONIC.ladder(octaves = 3).stepFor(lowestNi))
        assertNotNull(TrainerScale.DIATONIC.ladder.stepFor(lowestNi))
    }

    /**
     * One ladder, not a copy: wherever the Trainer's ladder and the 8 Ήχοι diagram's overlap, they sound
     * the same pitch — same ήχος, same shift, to the last bit.
     */
    @Test
    fun theTrainersLadderIsTheDiagramsLadderExtendedDownwards() {
        var compared = 0
        Mode.entries.forEach { mode ->
            listOf(BaseShift.MIN_MORIA, -5, 0, 7, BaseShift.MAX_MORIA).forEach { shift ->
                val diagram = mode.scale.ladder(octaves = 3, baseShift = Moria(shift))
                val trainer = TrainerScale(mode, shift).ladder
                diagram.steps.forEach { step ->
                    val onTrainer = trainer.stepFor(step.phthong)!!.frequencyHz
                    assertEquals("$mode $shift ${step.phthong.label}", step.frequencyHz, onTrainer, 0.0)
                    compared++
                }
            }
        }
        assertTrue("expected every rung of 40 diagram ladders, got $compared", compared >= 40 * 22)
    }
}
