package com.johnchourp.learnbyzantinemusic.lessons.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * ClickUp `869f4tpg0` (A2). The acceptance criterion is *«μέτρηση drift σε 60 δευτ.»*, so the drift
 * is measured here rather than asserted by eye.
 */
class MetronomeScheduleTest {

    @Test
    fun theTempoRangeIsTheOneThePageOffers() {
        assertEquals(40, MetronomeSchedule.MIN_BPM)
        assertEquals(160, MetronomeSchedule.MAX_BPM)
        assertEquals(MetronomeSchedule.MIN_BPM, MetronomeSchedule.clampBpm(1))
        assertEquals(MetronomeSchedule.MAX_BPM, MetronomeSchedule.clampBpm(10_000))
    }

    @Test
    fun sixtyBpmIsOneBeatPerSecond() {
        assertEquals(1_000.0, MetronomeSchedule.intervalMillis(60), 1e-9)
    }

    /**
     * The whole point of absolute scheduling. At 110 bpm one beat lasts 545.4545…ms, so a loop that
     * delays a whole number of milliseconds loses a fraction every beat — and never gets it back.
     */
    @Test
    fun absoluteSchedulingDoesNotDriftOverAMinuteWhileNaiveLoopingDoes() {
        val bpm = 110
        val interval = MetronomeSchedule.intervalMillis(bpm)
        val beatsInAMinute = (60_000.0 / interval).toInt()   // 110

        var naive = 0L
        var worstNaive = 0L
        var worstAbsolute = 0L

        for (index in 0..beatsInAMinute) {
            val ideal = index * interval

            val absolute = MetronomeSchedule.beatTimeMillis(0L, bpm, index)
            worstAbsolute = maxOf(worstAbsolute, abs(absolute - ideal).roundToLong())

            worstNaive = maxOf(worstNaive, abs(naive - ideal).roundToLong())
            naive += interval.toLong()   // what `delay(interval.toLong())` in a loop really does
        }

        assertTrue(
            "absolute scheduling must stay within a millisecond over 60s, was ${worstAbsolute}ms",
            worstAbsolute <= 1L,
        )
        assertTrue(
            "the naive loop must actually drift, otherwise this test proves nothing " +
                "(was ${worstNaive}ms)",
            worstNaive >= 25L,
        )
    }

    @Test
    fun aLateWakeUpIsNotCarriedForward() {
        val bpm = 110
        // Beat 10 fired 40ms late; beat 11 must still be scheduled from the START, not from then.
        val ideal11 = MetronomeSchedule.beatTimeMillis(0L, bpm, 11)
        val lateNow = MetronomeSchedule.beatTimeMillis(0L, bpm, 10) + 40
        assertEquals(ideal11 - lateNow, MetronomeSchedule.delayUntilMillis(0L, bpm, 11, lateNow))
    }

    @Test
    fun aBeatAlreadyDueFiresImmediatelyRatherThanSleepingBackwards() {
        val now = MetronomeSchedule.beatTimeMillis(0L, 120, 5) + 5_000
        assertEquals(0L, MetronomeSchedule.delayUntilMillis(0L, 120, 5, now))
    }

    @Test
    fun theHighlightedBeatIsDerivedFromTheIndexNotIncremented() {
        // Derived, so a dropped frame cannot strand the highlight on the wrong beat forever.
        BeatGrouping.all.forEach { grouping ->
            (0..40).forEach { index ->
                val beat = MetronomeSchedule.beatInGrouping(index, grouping.beats)
                assertTrue("$grouping beat $beat out of range", beat in 1..grouping.beats)
                assertEquals((index % grouping.beats) + 1, beat)
            }
        }
    }

    @Test
    fun everyGroupingAccentsOnlyItsDownbeat() {
        BeatGrouping.all.forEach { grouping ->
            val accents = (0 until grouping.beats * 4).count {
                MetronomeSchedule.isDownbeat(it, grouping.beats)
            }
            assertEquals("$grouping must accent once per cycle", 4, accents)
            assertTrue(MetronomeSchedule.isDownbeat(0, grouping.beats))
            assertTrue(!MetronomeSchedule.isDownbeat(1, grouping.beats))
        }
    }

    @Test
    fun theScheduleAgreesWithTheGroupingsOwnCycle() {
        // The existing BeatGrouping.nextBeat and the new index-derived beat must not disagree:
        // one drives the tap-to-select highlight, the other drives playback.
        BeatGrouping.all.forEach { grouping ->
            var walked = 1
            (1..20).forEach { index ->
                walked = grouping.nextBeat(walked)
                assertEquals(
                    "$grouping diverges at index $index",
                    walked,
                    MetronomeSchedule.beatInGrouping(index, grouping.beats),
                )
            }
        }
    }

    @Test
    fun aZeroBeatGroupingCannotDivideByZero() {
        assertEquals(1, MetronomeSchedule.beatInGrouping(7, 0))
    }
}
