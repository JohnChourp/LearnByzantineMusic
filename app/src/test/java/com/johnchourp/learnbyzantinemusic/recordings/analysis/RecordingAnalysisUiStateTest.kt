package com.johnchourp.learnbyzantinemusic.recordings.analysis

import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.music.PhthongName.GA
import com.johnchourp.learnbyzantinemusic.music.PhthongName.KE
import com.johnchourp.learnbyzantinemusic.music.PhthongName.PA
import com.johnchourp.learnbyzantinemusic.music.PhthongName.VOU
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The melody and its comparison change in the same state update (ClickUp `869f4tqef`), whichever
 * changes first: a melody edit compares with the notes the state has, and notes found by a recompute
 * compare with the melody as it is when they arrive — not as it was when the recompute started.
 */
class RecordingAnalysisUiStateTest {

    private fun sung(vararg phthongs: PhthongName): List<SungNote> =
        phthongs.mapIndexed { i, p -> SungNote(p, octave = 0, startMs = i * 500L, endMs = i * 500L + 400, deviationMoria = 0.0, moria = 0.0) }

    private fun RecordingAnalysisUiState.assertComparedAsItIs() {
        assertEquals(MelodyComparison.of(expected, notes), comparison)
    }

    @Test
    fun aMelodyEditComparesWithTheNotesThereAre() {
        val ready = RecordingAnalysisUiState().withNotes(niHz = 220.0, notes = sung(PA, VOU, GA))
        val typed = ready.withExpected(listOf(PA, VOU, GA))
        typed.assertComparedAsItIs()
        assertEquals(3, typed.comparison!!.alignment.matches)

        val shorter = typed.withExpected(listOf(PA))
        shorter.assertComparedAsItIs()
        assertEquals(listOf(PA), shorter.comparison!!.expected)

        assertNull(shorter.withExpected(emptyList()).comparison)
    }

    @Test
    fun notesThatArriveLaterCompareWithTheMelodyOfThatMoment() {
        // Edits made while the recording was still being analysed: no notes yet, nothing to compare.
        val decoding = RecordingAnalysisUiState().withExpected(listOf(PA, KE))
        assertNull(decoding.comparison)

        val ready = decoding.withNotes(niHz = 220.0, notes = sung(PA, KE))
        ready.assertComparedAsItIs()
        assertEquals(AnalysisStatus.READY, ready.status)

        // A melody taken from an exercise while a recompute runs, then that recompute's notes.
        val replaced = ready.withExpected(listOf(GA, VOU, PA))
        val recomputed = replaced.withNotes(niHz = 230.0, notes = sung(GA, VOU))
        recomputed.assertComparedAsItIs()
        assertEquals(listOf(GA, VOU, PA), recomputed.comparison!!.expected)
    }

    @Test
    fun noNotesMeansNoPitchAndNoComparison() {
        val state = RecordingAnalysisUiState().withExpected(listOf(PA)).withNotes(niHz = null, notes = emptyList())
        assertEquals(AnalysisStatus.NO_PITCH, state.status)
        assertNull(state.comparison)
    }
}
