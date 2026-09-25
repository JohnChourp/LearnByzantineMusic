package com.johnchourp.learnbyzantinemusic.lessons.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * ClickUp `869f5x2d7`, first acceptance criterion: the vibration is timed by the **same** absolute
 * schedule as the click — never by a second `delay()` loop, which would accumulate error and drift
 * away from it (the bug fixed in `869f4tpg0`).
 *
 * A played minute is simulated on a fake monotonic clock whose wake-ups come late, as a real
 * scheduler's do. Every click and every vibration is stamped with the clock at the moment it fires,
 * exactly as the page's loop fires them.
 */
class MetronomeOneScheduleTest {

    private data class Fired(val index: Int, val atMillis: Long)

    private class Minute(val run: MetronomeRun, val events: List<BeatEvent>, val clicks: List<Fired>, val vibrations: List<Fired>)

    /** Late by 0–12 ms, never early — and not the same every time. */
    private fun lateness(beat: Int): Long = (beat * 7L) % 13

    private fun playAMinute(bpm: Int, beats: Int, options: MetronomeOptions): Minute {
        val start = 7_200_000L // boot-relative, like SystemClock.elapsedRealtime
        val run = MetronomeRun(start, bpm, beats)
        var now = start
        val events = mutableListOf<BeatEvent>()
        val clicks = mutableListOf<Fired>()
        val vibrations = mutableListOf<Fired>()
        val count = (60_000.0 / MetronomeSchedule.intervalMillis(bpm)).roundToInt()
        repeat(count) { beat ->
            now += run.waitMillis(now) + lateness(beat)
            val event = run.next(options)
            val vibrator = object : BeatVibrator {
                override val available = true
                override fun pulse(strength: BeatStrength) {
                    vibrations += Fired(event.index, now)
                }
            }
            MetronomeBeats.perform(event, { clicks += Fired(event.index, now) }, vibrator)
            events += event
        }
        return Minute(run, events, clicks, vibrations)
    }

    @Test
    fun theVibrationFiresWithTheClickOnEveryBeat() {
        BeatGrouping.all.forEach { grouping ->
            val minute = playAMinute(110, grouping.beats, MetronomeOptions(vibrate = true))
            assertEquals("$grouping: a minute at 110 bpm", 110, minute.events.size)
            assertEquals(grouping.toString(), minute.clicks, minute.vibrations)
        }
    }

    @Test
    fun everyBeatIsTheScheduledOneAndLatenessIsNeverCarriedForward() {
        listOf(40, 80, 110, 160).forEach { bpm ->
            val minute = playAMinute(bpm, 3, MetronomeOptions(vibrate = true, silent = true))
            minute.events.forEach { event ->
                assertEquals(MetronomeSchedule.beatTimeMillis(minute.run.startMillis, bpm, event.index), event.atMillis)
            }
            // Fired no later than that beat's own wake-up delay — even at the end of the minute.
            // Error that accumulated from beat to beat would be far past it by then.
            minute.vibrations.forEach { fired ->
                val late = fired.atMillis - minute.events[fired.index].atMillis
                assertTrue("$bpm bpm, beat ${fired.index}: ${late}ms late", late in 0..lateness(fired.index))
            }
            assertEquals(minute.events.size, minute.vibrations.size)
        }
    }

    @Test
    fun inFootModeTheVibrationAndTheClickShareTheTheses() {
        val minute = playAMinute(80, 4, MetronomeOptions(vibrate = true, foot = true))
        val theses = minute.events.filter { it.thesis }.map { it.index }
        assertEquals(theses, minute.clicks.map { it.index })
        assertEquals(minute.clicks, minute.vibrations)
        assertEquals(20, theses.size)
    }

    @Test
    fun theHandIsAtTheBottomWhenEachThesisIsDue() {
        val minute = playAMinute(110, 3, MetronomeOptions())
        val theses = minute.events.filter { it.thesis }
        theses.forEach { event ->
            val phase = minute.run.handPhase(event.atMillis)
            assertTrue("θέση ${event.index}: phase $phase", min(phase, 1 - phase) < 1e-3)
        }
        assertTrue(theses.size >= 36)
    }
}
