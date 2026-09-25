package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.ByzantineTuning
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * «Με την προεπιλογή, κάθε υπάρχον test του Γυμναστή περνά αμετάβλητο» (ClickUp `869f5x24v`, F2).
 *
 * The default scale, «Διατονικός», is a [TrainerScale] — the diatonic ladder at shift 0 — and no
 * longer the fixed table the Trainer used before. It must be that table in everything but name: the
 * same frequency for each of the 21 notes the Trainer can write, the same verdict for every pitch a
 * singer can produce, and therefore the same playback plan.
 */
class TrainerScaleDefaultIsTodaysTableTest {

    private val notes: List<TrainerNote> = PhthongName.entries.flatMap { phthong ->
        (TrainerScale.MIN_NOTE_OCTAVE..TrainerScale.MAX_NOTE_OCTAVE).map { octave -> TrainerNote(phthong, octave) }
    }

    @Test
    fun theTrainerWritesTwentyOneNotes() {
        assertEquals(21, notes.size)
    }

    @Test
    fun everyNoteSoundsAtExactlyTheOldFrequency() {
        notes.forEach { note ->
            assertEquals(
                note.pitch.label,
                TrainerPitchTable.frequencyHz(note.phthong, note.octaveShift),
                TrainerScale.DIATONIC.frequencyHz(note),
                0.0,
            )
        }
    }

    @Test
    fun theDefaultPlaybackPlanIsTheOldPlan() {
        val melody = MelodySequence(notes)
        val tempo = MelodyTempo.of(MelodyTempo.DEFAULT_BPM)
        assertEquals(
            MelodyPlaybackPlanner.plan(melody, tempo),
            MelodyPlaybackPlanner.plan(melody, tempo, TrainerScale.DIATONIC::frequencyHz),
        )
    }

    /**
     * Every 0.05 μόρια from Νη two octaves down to Νη three octaves up — past both ends of the
     * Trainer's ladder, so the folding is exercised too. The probes are offset so that none sits on
     * the exact midpoint between two rungs, the one place the two could break a tie differently.
     */
    @Test
    fun everySungPitchIsJudgedAsTheOldTableJudgedIt() {
        val probes = (0 until 7_200).map { index -> -144.0 + 0.0125 + index * 0.05 }
        probes.forEach { moria ->
            val hz = ByzantineTuning.frequencyHz(moria)
            val before = TrainerPitchTable.nearestPhthong(hz)!!
            val now = TrainerScale.DIATONIC.match(hz)!!
            assertEquals("φθόγγος at $moria μόρια", before.phthong, now.phthong)
            assertEquals("deviation at $moria μόρια", before.deviationMoria, now.deviationMoria, 1e-9)
            assertEquals("the pitch as sung is kept", hz, now.frequencyHz, 0.0)
        }
        assertTrue("the sweep must cross the ladder's ends", probes.first() < -132.0 && probes.last() > 156.0)
    }

    @Test
    fun whatIsNotAPitchIsSilenceAsBefore() {
        listOf(0.0, -220.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { hz ->
            assertNull(TrainerPitchTable.nearestPhthong(hz))
            assertNull("$hz", TrainerScale.DIATONIC.match(hz))
        }
    }
}
