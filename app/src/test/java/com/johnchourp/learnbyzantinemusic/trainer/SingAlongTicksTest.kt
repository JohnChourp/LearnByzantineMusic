package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.lessons.ui.MetronomeSchedule
import com.johnchourp.learnbyzantinemusic.music.Beats
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The «Ψάλλε μαζί» metronome (ClickUp `869f5x2cv`, J2) clicks every χρόνος, on the same timeline as
 * the notes: each click is part of the round's plan, at the start of a χρόνος, and the first click of
 * every round — where the round's first note starts — is the accented one.
 *
 * It runs at the Trainer's tempo, 30 … 240. The lessons' metronome stops at 40 … 160
 * ([MetronomeSchedule]), so the Trainer's clicks cannot be borrowed from it.
 */
class SingAlongTicksTest {

    private val hz: (TrainerNote) -> Double = { 220.0 }

    /** 2½ χρόνοι: every round is 3, and its last half χρόνος is a rest. */
    private val melody = MelodySequence(
        listOf(
            TrainerNote(PhthongName.NI),
            TrainerNote(PhthongName.PA, baseDurationBeats = 0.5f),
            TrainerNote(PhthongName.VOU),
        )
    )

    /** χρόνος [beat] from the start at [bpm], in whole milliseconds rounded half up — exactly. */
    private fun beatStart(beat: Long, bpm: Int): Long = Math.floorDiv(2L * beat * 60_000 + bpm, 2L * bpm)

    @Test
    fun everyClickLandsOnTheStartOfAChronosFrom30To240() {
        (MelodyTempo.MIN_BPM..MelodyTempo.MAX_BPM).forEach { bpm ->
            (0..4).forEach { k ->
                val round = MelodyPlaybackPlanner.planRound(melody, MelodyTempo(bpm), k, hz)
                assertEquals("$bpm bpm: one click per χρόνος of the round", 3, round.ticks.size)
                round.ticks.forEachIndexed { j, tick ->
                    assertEquals("$bpm bpm, round $k, click $j", beatStart(3L * k + j, bpm), tick.startMillis)
                    assertEquals("only a round's first click is accented", j == 0, tick.downbeat)
                }
            }
        }
    }

    @Test
    fun theAccentedClickIsTheRoundsFirstNote() {
        listOf(30, 75, 110, 240).forEach { bpm ->
            (0..6).forEach { k ->
                val round = MelodyPlaybackPlanner.planRound(melody, MelodyTempo(bpm), k, hz)
                val accent = round.ticks.single { it.downbeat }
                assertEquals(round.startMillis, accent.startMillis)
                assertEquals(round.notes.first().startMillis, accent.startMillis)
            }
        }
    }

    @Test
    fun aClickAndANoteOnTheSameChronosStartTogether() {
        // The notes start at 0, 1 and 1½ χρόνοι: the first two on a click, the third between two.
        val plan = MelodyPlaybackPlanner.planRound(melody, MelodyTempo(97), 3, hz)
        val clicks = plan.ticks.map { it.startMillis }.toSet()
        val onTheBeat = plan.notes.filter { note ->
            val offsetTicks = melody.durations().take(note.index).fold(Beats.ZERO) { a, b -> a + b }.ticks
            offsetTicks % Beats.TICKS_PER_BEAT == 0
        }
        assertTrue(onTheBeat.isNotEmpty())
        onTheBeat.forEach { assertTrue("note ${it.index} starts on a click", it.startMillis in clicks) }
    }

    @Test
    fun theTrainersWholeTempoRangeIsWiderThanTheLessonsMetronome() {
        // Guards the premise: if the lessons' metronome ever covered the Trainer's range, borrowing it
        // would be an option again.
        assertTrue(MelodyTempo.MIN_BPM < MetronomeSchedule.MIN_BPM)
        assertTrue(MelodyTempo.MAX_BPM > MetronomeSchedule.MAX_BPM)
        val slowest = MelodyPlaybackPlanner.planRound(melody, MelodyTempo(MelodyTempo.MIN_BPM), 0, hz).ticks
        val fastest = MelodyPlaybackPlanner.planRound(melody, MelodyTempo(MelodyTempo.MAX_BPM), 0, hz).ticks
        assertEquals(listOf(0L, 2_000L, 4_000L), slowest.map { it.startMillis })
        assertEquals(listOf(0L, 250L, 500L), fastest.map { it.startMillis })
    }
}
