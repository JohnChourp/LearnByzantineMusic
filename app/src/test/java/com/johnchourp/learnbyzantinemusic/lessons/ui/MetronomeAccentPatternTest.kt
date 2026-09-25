package com.johnchourp.learnbyzantinemusic.lessons.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ClickUp `869f5x2d7`: the vibration is strong on the θέση and light on the άρσεις, in every
 * grouping — and «Πόδι» marks only the θέσεις, so a beginner counts from θέση to θέση.
 *
 * Patterns read `S` = strong, `l` = light, `-` = nothing felt.
 */
class MetronomeAccentPatternTest {

    private fun felt(grouping: BeatGrouping, options: MetronomeOptions): List<String> =
        (0 until grouping.beats * 2).map { index ->
            when (MetronomeBeats.event(0L, 80, grouping.beats, index, options).vibration) {
                BeatStrength.STRONG -> "S"
                BeatStrength.LIGHT -> "l"
                null -> "-"
            }
        }

    @Test
    fun everyGroupingIsStrongOnItsThesisAndLightOnItsArses() {
        val options = MetronomeOptions(vibrate = true)
        assertEquals(listOf("S", "l", "S", "l"), felt(BeatGrouping.DISIMOS, options))
        assertEquals(listOf("S", "l", "l", "S", "l", "l"), felt(BeatGrouping.TRISIMOS, options))
        assertEquals(listOf("S", "l", "l", "l", "S", "l", "l", "l"), felt(BeatGrouping.TETRASIMOS, options))
    }

    @Test
    fun footModeMarksOnlyTheTheses() {
        val foot = MetronomeOptions(vibrate = true, foot = true)
        assertEquals(listOf("S", "-", "S", "-"), felt(BeatGrouping.DISIMOS, foot))
        assertEquals(listOf("S", "-", "-", "S", "-", "-"), felt(BeatGrouping.TRISIMOS, foot))
        assertEquals(listOf("S", "-", "-", "-", "S", "-", "-", "-"), felt(BeatGrouping.TETRASIMOS, foot))
        // The click follows the same rule: it sounds on the θέσεις and nowhere else.
        BeatGrouping.all.forEach { grouping ->
            (0 until grouping.beats * 3).forEach { index ->
                val event = MetronomeBeats.event(0L, 80, grouping.beats, index, foot)
                assertEquals("$grouping beat $index", event.thesis, event.click)
            }
        }
    }

    @Test
    fun footModeStillCountsEveryBeat() {
        // Only the marking thins out: the highlight and the hand still go 1, 2, 3.
        val shown = (0 until 6).map { MetronomeBeats.event(0L, 80, 3, it, MetronomeOptions(foot = true)).beatInGrouping }
        assertEquals(listOf(1, 2, 3, 1, 2, 3), shown)
    }

    @Test
    fun theClickAndTheVibrationAccentTheSameBeat() {
        BeatGrouping.all.forEach { grouping ->
            (0 until grouping.beats * 3).forEach { index ->
                val event = MetronomeBeats.event(0L, 80, grouping.beats, index, MetronomeOptions(vibrate = true))
                assertTrue(event.click)
                assertEquals(MetronomeSchedule.isDownbeat(index, grouping.beats), event.thesis)
                assertEquals("$grouping beat $index", event.thesis, event.vibration == BeatStrength.STRONG)
            }
        }
    }

    @Test
    fun silentDropsTheClickAndKeepsTheVibration() {
        val events = (0 until 8).map { MetronomeBeats.event(0L, 80, 4, it, MetronomeOptions(vibrate = true, silent = true)) }
        assertTrue(events.none { it.click })
        assertEquals(
            listOf(true, false, false, false, true, false, false, false),
            events.map { it.vibration == BeatStrength.STRONG },
        )
        assertTrue(events.all { it.vibration != null })
    }

    @Test
    fun vibrationOffLeavesTheClickAsItWas() {
        val events = (0 until 4).map { MetronomeBeats.event(0L, 80, 2, it, MetronomeOptions(vibrate = false)) }
        assertTrue(events.all { it.click && it.vibration == null })
        assertEquals(listOf(true, false, true, false), events.map { it.thesis })
    }
}
