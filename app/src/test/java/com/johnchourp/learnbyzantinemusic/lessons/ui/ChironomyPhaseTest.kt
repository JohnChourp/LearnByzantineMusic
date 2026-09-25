package com.johnchourp.learnbyzantinemusic.lessons.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.min

/**
 * ClickUp `869f5x2d7`: the moving hand is at its lowest point on every θέση — on the θέση itself,
 * as the click schedules it, not somewhere near it.
 */
class ChironomyPhaseTest {

    /** Distance from the bottom of the circle, measured round it: 0 at the bottom, 0.5 at the top. */
    private fun fromTheBottom(phase: Double) = min(phase, 1 - phase)

    @Test
    fun theHandIsAtItsLowestOnEveryThesis() {
        var checked = 0
        BeatGrouping.all.forEach { grouping ->
            listOf(40, 48, 80, 110, 160).forEach { bpm ->
                (0..60).forEach { k ->
                    val thesis = MetronomeSchedule.beatTimeMillis(0L, bpm, k * grouping.beats)
                    val phase = ChironomyPhase.of(thesis.toDouble(), bpm, grouping.beats)
                    // Beat times are rounded to the millisecond; the hand may be that far off, no more.
                    val tolerance = 0.5 / (MetronomeSchedule.intervalMillis(bpm) * grouping.beats) + 1e-9
                    assertTrue(
                        "$grouping at $bpm bpm, θέση $k: phase $phase",
                        fromTheBottom(phase) <= tolerance,
                    )
                    val (x, y) = ChironomyPhase.position(phase)
                    assertEquals("the lowest point, y growing downwards", 1.0, y, 1e-4)
                    assertEquals(0.0, x, 1e-2)
                    checked++
                }
            }
        }
        // Guards the slice: 3 groupings × 5 tempi × 61 θέσεις.
        assertEquals(3 * 5 * 61, checked)
    }

    @Test
    fun onTheArsesTheHandIsFurtherRound() {
        BeatGrouping.all.forEach { grouping ->
            (1 until grouping.beats).forEach { j ->
                val arsis = MetronomeSchedule.beatTimeMillis(0L, 80, j).toDouble()
                val phase = ChironomyPhase.of(arsis, 80, grouping.beats)
                assertEquals("$grouping χρόνος ${j + 1}", j.toDouble() / grouping.beats, phase, 1e-3)
                assertTrue("$grouping χρόνος ${j + 1} is not the bottom", ChironomyPhase.position(phase).second < 0.9)
            }
        }
    }

    @Test
    fun aDisimosGoesDownThenUp() {
        val arsis = MetronomeSchedule.beatTimeMillis(0L, 60, 1).toDouble()
        assertEquals(-1.0, ChironomyPhase.position(ChironomyPhase.of(arsis, 60, 2)).second, 1e-9)
    }

    @Test
    fun oneCirclePerGroupingAndThenTheNext() {
        val interval = MetronomeSchedule.intervalMillis(80)
        assertEquals(0.5, ChironomyPhase.of(interval * 2, 80, 4), 1e-9)
        assertEquals(0.5, ChironomyPhase.of(interval * 6, 80, 4), 1e-9)
    }

    @Test
    fun atRestTheHandWaitsOnTheThesis() {
        assertEquals(0.0, ChironomyPhase.of(0.0, 80, 3), 0.0)
        assertEquals(0.0, ChironomyPhase.of(-5.0, 80, 3), 0.0)
        assertEquals(0.0, ChironomyPhase.of(1_234.0, 80, 0), 0.0)
    }
}
