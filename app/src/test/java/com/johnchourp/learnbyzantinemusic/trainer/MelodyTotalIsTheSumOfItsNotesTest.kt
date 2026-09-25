package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.Beats
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.summary_theory.ui.TimePageEquations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * A melody lasts exactly the sum of its notes — in χρόνοι, and in the milliseconds it is played for
 * (ClickUp `869f5x29r`, H5).
 *
 * The Trainer shows every note's length and a «Σύνολο»; playback and the timing exercise schedule
 * the notes one after another. With exact lengths the three can only agree if nothing rounds along
 * the way. Rounding each note to whole milliseconds did: at 70 χρόνοι/λεπτό a half lasts 428.57 ms,
 * so a γοργόν pair played 858 ms of a 857 ms χρόνος, and three ⅓ notes at 60 lost a millisecond per
 * χρόνος. Now each note starts at its exact offset, rounded once.
 *
 * Swept over melodies the Trainer's buttons can write (seeded, so a failure repeats) and over every
 * example of the «Χαρακτήρες Χρόνου» page, which has the thirds and quarters the buttons cannot make.
 */
class MelodyTotalIsTheSumOfItsNotesTest {

    private val tempos = listOf(30, 45, 60, 70, 80, 97, 110, 133, 160, 200, 240)

    /** What the ± buttons and the γοργόν chip can write: ½ … 4 χρόνοι, γοργόν anywhere but first. */
    private val trainerMelodies: List<MelodySequence> by lazy {
        val random = Random(869)
        List(400) {
            MelodySequence(
                List(random.nextInt(1, 13)) { index ->
                    val note = TrainerNote(
                        phthong = PhthongName.entries[random.nextInt(PhthongName.entries.size)],
                        baseDurationBeats = MelodySequence.LENGTH_STEP_BEATS * random.nextInt(1, 9),
                    )
                    if (index > 0 && random.nextInt(3) == 0) note.withGorgo(true) else note
                }
            )
        }
    }

    /** Every example of the «Χαρακτήρες Χρόνου» page, played as a melody. */
    private val pageMelodies: List<MelodySequence> by lazy {
        TimePageEquations.all.values.map { equation ->
            MelodySequence(equation.rhythm.map { TrainerNote(PhthongName.NI, signs = it.signs) })
        }
    }

    private val melodies: List<MelodySequence> by lazy { trainerMelodies + pageMelodies }

    @Test
    fun theSweepHasRunsThirdsAndQuarters() {
        // Guards the slice: without them the millisecond check below could pass on whole χρόνοι alone.
        assertTrue(trainerMelodies.all { it.isValid })
        assertTrue(melodies.any { m -> m.durations().any { it.ticks % (Beats.TICKS_PER_BEAT / 2) != 0 } })
        assertTrue(melodies.any { m -> m.durations().any { it == Beats.of(1, 3) } })
        assertTrue(melodies.any { m -> m.notes.zipWithNext().any { (a, b) -> a.hasGorgo && b.hasGorgo } })
    }

    @Test
    fun theTotalIsTheSumOfTheNotes() {
        melodies.forEach { melody ->
            val durations = melody.durations()
            assertEquals(melody.notes.size, durations.size)
            assertTrue("no note lasts nothing: $durations", durations.all { it > Beats.ZERO })
            assertEquals("${melody.notes}", durations.fold(Beats.ZERO) { sum, beats -> sum + beats }, melody.total())
        }
    }

    @Test
    fun theMelodyEndsWhereItsTotalSaysAtEveryTempo() {
        var checked = 0
        melodies.forEach { melody ->
            tempos.forEach { bpm ->
                val tempo = MelodyTempo.of(bpm)
                val plan = MelodyPlaybackPlanner.plan(melody, tempo)
                val where = "$bpm bpm, ${melody.durations()}"
                assertEquals(where, 0L, plan.first().startMillis)
                plan.zipWithNext().forEach { (a, b) -> assertEquals("$where: no gap, no overlap", a.endMillis, b.startMillis) }
                assertEquals(where, tempo.beatsToMillis(melody.total()), MelodyPlaybackPlanner.totalDurationMillis(plan))
                checked++
            }
        }
        assertTrue(checked >= 400 * tempos.size)
    }
}
