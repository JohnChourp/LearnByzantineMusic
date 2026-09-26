package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.Beats
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * «Ψάλλε μαζί» loops the line round after round (ClickUp `869f5x2cv`, J2). Round k starts exactly k
 * round lengths after the first — counted in χρόνοι and turned into milliseconds once — never at the
 * previous round's start plus a rounded length, which would slip a little further every round.
 *
 * The expected times are computed here in exact integer arithmetic, not with the planner's own
 * conversion, so a test cannot agree with the planner merely by repeating it.
 */
class SingAlongRoundsTest {

    private val hz: (TrainerNote) -> Double = { 220.0 }

    private fun melody(vararg lengths: Float): MelodySequence =
        MelodySequence(lengths.map { TrainerNote(PhthongName.NI, baseDurationBeats = it) })

    /** [ticks] twelfths of a χρόνος at [bpm], in whole milliseconds rounded half up — exactly. */
    private fun exactMillis(ticks: Long, bpm: Int): Long =
        Math.floorDiv(2L * ticks * 60_000 + Beats.TICKS_PER_BEAT.toLong() * bpm, 2L * Beats.TICKS_PER_BEAT * bpm)

    private fun round(sequence: MelodySequence, bpm: Int, k: Int): PlannedRound =
        MelodyPlaybackPlanner.planRound(sequence, MelodyTempo(bpm), k, hz)

    @Test
    fun roundKStartsExactlyKRoundLengthsAfterTheFirst() {
        // 3 χρόνοι, with a half in it, so a round is not a whole number of milliseconds at most tempos.
        val sequence = melody(1f, 0.5f, 1.5f)
        listOf(30, 61, 80, 97, 110, 133, 179, 240).forEach { bpm ->
            (0..60).forEach { k ->
                val round = round(sequence, bpm, k)
                val roundTicks = 3L * Beats.TICKS_PER_BEAT
                assertEquals("$bpm bpm, round $k", exactMillis(k * roundTicks, bpm), round.startMillis)
                assertEquals("$bpm bpm, round $k ends where the next begins", round(sequence, bpm, k + 1).startMillis, round.endMillis)
                assertEquals(round.startMillis, round.notes.first().startMillis)
            }
        }
    }

    @Test
    fun everyNoteOfTenRoundsAt110SitsWhereTheTempoPutsIt() {
        // 110 χρόνοι per minute: 545.45… ms each, so every rounded length is a little short.
        val sequence = melody(1f, 1f, 1f)
        (0..10).forEach { k ->
            round(sequence, 110, k).notes.forEachIndexed { j, note ->
                val ticks = (3L * k + j) * Beats.TICKS_PER_BEAT
                assertEquals("round $k, note $j", exactMillis(ticks, 110), note.startMillis)
            }
        }
        val tenth = round(sequence, 110, 10)
        val exact = 30 * 60_000.0 / 110
        assertTrue("round 10 starts ${tenth.startMillis}, exactly $exact", abs(tenth.startMillis - exact) <= 0.5)

        // Negative control: adding the rounded round length instead would already be this far off —
        // so the check above can see a drift, and there is none.
        val naive = (1..10).fold(0L) { at, _ -> at + MelodyTempo(110).beatsToMillis(Beats.whole(3)) }
        assertTrue("the naive loop drifts ${abs(naive - exact)} ms", abs(naive - exact) > 3)
    }

    @Test
    fun aRoundLastsTheMelodyRoundedUpToWholeBeats() {
        assertEquals(Beats.whole(3), MelodyPlaybackPlanner.roundLength(melody(1f, 1.5f)))
        assertEquals(Beats.whole(3), MelodyPlaybackPlanner.roundLength(melody(1f, 2f)))
        assertEquals(Beats.whole(1), MelodyPlaybackPlanner.roundLength(melody(0.5f)))

        // 2½ χρόνοι of melody, then half a χρόνος of rest, and the next round on the beat.
        val withRest = round(melody(1f, 1.5f), 60, 0)
        assertEquals(2_500L, withRest.melodyEndMillis)
        assertEquals(3_000L, withRest.endMillis)
        val whole = round(melody(1f, 2f), 60, 0)
        assertEquals("a melody of whole χρόνοι loops with no rest", whole.endMillis, whole.melodyEndMillis)
    }

    @Test
    fun everyRoundPlaysTheSameNotesInTheSameOrder() {
        val sequence = MelodySequence(
            listOf(
                TrainerNote(PhthongName.NI),
                TrainerNote(PhthongName.PA).withGorgo(true),
                TrainerNote(PhthongName.VOU, octaveShift = 1, baseDurationBeats = 2f),
            )
        )
        val first = round(sequence, 97, 0)
        (1..5).forEach { k ->
            val later = round(sequence, 97, k)
            assertEquals(first.notes.map { Triple(it.index, it.phthong, it.frequencyHz) }, later.notes.map { Triple(it.index, it.phthong, it.frequencyHz) })
            later.notes.zip(first.notes).forEach { (note, original) ->
                // The same length, give or take the one rounding each end gets.
                assertTrue(abs(note.durationMillis - original.durationMillis) <= 1)
            }
        }
    }

    @Test
    fun anEmptyLineHasNothingToLoop() {
        val round = round(MelodySequence(emptyList()), 80, 0)
        assertTrue(round.notes.isEmpty())
        assertTrue(round.ticks.isEmpty())
    }
}
