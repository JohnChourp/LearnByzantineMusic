package com.johnchourp.learnbyzantinemusic.lessons.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class BeatGroupingTest {

    @Test
    fun groupings_are_ordered_with_the_expected_beat_counts() {
        assertEquals(listOf(2, 3, 4), BeatGrouping.all.map { it.beats })
        assertEquals(BeatGrouping.DISIMOS, BeatGrouping.ofBeats(2))
        assertEquals(BeatGrouping.TRISIMOS, BeatGrouping.ofBeats(3))
        assertEquals(BeatGrouping.TETRASIMOS, BeatGrouping.ofBeats(4))
    }

    @Test
    fun beatNumbers_are_one_based_up_to_the_beat_count() {
        assertEquals(listOf(1, 2), BeatGrouping.DISIMOS.beatNumbers)
        assertEquals(listOf(1, 2, 3), BeatGrouping.TRISIMOS.beatNumbers)
        assertEquals(listOf(1, 2, 3, 4), BeatGrouping.TETRASIMOS.beatNumbers)
    }

    @Test
    fun nextBeat_advances_then_cycles_back_to_the_downbeat() {
        // Disimos: 1 -> 2 -> 1 (the group repeats every 2 beats).
        assertEquals(2, BeatGrouping.DISIMOS.nextBeat(1))
        assertEquals(1, BeatGrouping.DISIMOS.nextBeat(2))
        // Trisimos wraps after 3.
        assertEquals(2, BeatGrouping.TRISIMOS.nextBeat(1))
        assertEquals(1, BeatGrouping.TRISIMOS.nextBeat(3))
        // Tetrasimos wraps after 4.
        assertEquals(4, BeatGrouping.TETRASIMOS.nextBeat(3))
        assertEquals(1, BeatGrouping.TETRASIMOS.nextBeat(4))
    }

    @Test
    fun every_grouping_has_a_distinct_beat_count() {
        val counts = BeatGrouping.entries.map { it.beats }
        assertEquals(counts.size, counts.toSet().size)
    }
}
