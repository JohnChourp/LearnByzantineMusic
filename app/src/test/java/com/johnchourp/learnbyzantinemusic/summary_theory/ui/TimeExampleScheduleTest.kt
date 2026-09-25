package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import com.johnchourp.learnbyzantinemusic.music.RhythmTimeline
import com.johnchourp.learnbyzantinemusic.music.RhythmTimeline.Click
import com.johnchourp.learnbyzantinemusic.music.RhythmTimeline.End
import com.johnchourp.learnbyzantinemusic.music.RhythmTimeline.Silence
import com.johnchourp.learnbyzantinemusic.music.RhythmTimeline.Sound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * «Άκου» fires every note, rest and click at its own time on one clock (ClickUp `869f5x25n`, F4).
 *
 * The expected milliseconds are written out for two tempos. At 80 χρόνοι a minute a χρόνος is 750 ms
 * and every share is a whole number; at 70 it is 857.14… ms, where a note placed by adding up rounded
 * lengths drifts: three ⅓ notes would end at 858 and the second would start at 286 + 286 = 572 — the
 * exact offsets, rounded once, are 571 and 857. The clicks fall on the χρόνοι of the same clock.
 */
class TimeExampleScheduleTest {

    private fun cues(name: String, bpm: Int) = TimeExamplePlayback.cues(TimePageEquations.all.getValue(name), bpm)

    private fun starts(cues: List<RhythmTimeline.Cue>) = cues.filter { it is Sound || it is Silence }.map { it.atMillis }

    private fun clicks(cues: List<RhythmTimeline.Cue>) = cues.filterIsInstance<Click>().map { it.atMillis }

    private fun end(cues: List<RhythmTimeline.Cue>) = cues.filterIsInstance<End>().single().atMillis

    @Test
    fun at80TheNotesAndTheClicksFallOnTheirExactTimes() {
        cues("digorgo", 80).let {
            assertEquals(listOf(0L, 250L, 500L), starts(it))
            assertEquals(listOf(0L), clicks(it))
            assertEquals(750L, end(it))
        }
        cues("argo", 80).let {
            assertEquals(listOf(0L, 375L, 750L), starts(it))
            assertEquals(listOf(0L, 750L, 1500L), clicks(it))
            assertEquals(2250L, end(it))
        }
    }

    @Test
    fun at70NothingDriftsFromNoteToNote() {
        cues("digorgo", 70).let {
            assertEquals(listOf(0L, 286L, 571L), starts(it))
            assertEquals(857L, end(it))
        }
        cues("trigorgo", 70).let {
            assertEquals(listOf(0L, 214L, 429L, 643L), starts(it))
            assertEquals(857L, end(it))
        }
        cues("triargo", 70).let {
            assertEquals(listOf(0L, 429L, 857L), starts(it))
            assertEquals(listOf(0L, 857L, 1714L, 2571L, 3429L), clicks(it))
            assertEquals(4286L, end(it))
        }
    }

    @Test
    fun theClicksCountEveryBeatAndAccentTheFirst() {
        TimePageEquations.all.forEach { (name, equation) ->
            val clicks = TimeExamplePlayback.cues(equation, 80).filterIsInstance<Click>()
            assertEquals(name, clicks.indices.toList(), clicks.map { it.beat })
            assertEquals(name, listOf(true) + List(clicks.size - 1) { false }, clicks.map { it.accented })
            assertEquals(name, clicks.map { it.beat * 750L }, clicks.map { it.atMillis })
        }
    }

    @Test
    fun aBeatThatStartsWithANoteSoundsAndClicksAtTheSameInstant() {
        // The ολίγον of the αργόν starts on the second χρόνος: its note and that click share a time,
        // and the note comes first, so the highlight and the sound of the χρόνος arrive together.
        val argo = cues("argo", 70)
        val oligon = argo.filterIsInstance<Sound>().single { it.note == 2 }
        val second = argo.filterIsInstance<Click>().single { it.beat == 1 }
        assertEquals(oligon.atMillis, second.atMillis)
        assertTrue(argo.indexOf(oligon) < argo.indexOf(second))
    }

    @Test
    fun theCuesAreInTimeOrderAndEndLast() {
        listOf(80, 70, 40, 160).forEach { bpm ->
            (TimePageEquations.all.values.map { TimeExamplePlayback.cues(it, bpm) } +
                (TimeCharacters.examples + TimeCharacters.pauses).map { TimeExamplePlayback.cues(it, bpm) })
                .forEach { cues ->
                    assertEquals("$bpm", cues.sortedBy { it.atMillis }.map { it.atMillis }, cues.map { it.atMillis })
                    assertTrue("$bpm: the end comes last", cues.last() is End)
                    assertTrue("$bpm: nothing after the end", cues.all { it.atMillis <= cues.last().atMillis })
                }
        }
    }
}
