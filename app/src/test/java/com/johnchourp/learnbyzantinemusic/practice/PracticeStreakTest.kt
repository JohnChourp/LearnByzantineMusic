package com.johnchourp.learnbyzantinemusic.practice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * The streak of «Πεντάλεπτο της ημέρας» (ClickUp `869f5x2dy`): consecutive calendar days with a
 * completed session, as the user lived them — which is what must survive a daylight-saving night and a
 * flight. Every clock here is injected.
 */
class PracticeStreakTest {

    /** A clock the test moves: the instant, and the zone the phone is in. */
    private class MovableClock(var instant: Instant, var zoneId: ZoneId) : Clock() {
        override fun getZone(): ZoneId = zoneId
        override fun withZone(zone: ZoneId): Clock = MovableClock(instant, zone)
        override fun instant(): Instant = instant
    }

    private val athens = ZoneId.of("Europe/Athens")
    private val newYork = ZoneId.of("America/New_York")

    private fun day(text: String) = LocalDate.parse(text)

    /** Completes a five-minute session ending at [at], on [clock] as it is set. */
    private fun MovableClock.completeAt(log: PracticeLog, at: String, zone: ZoneId = zoneId): PracticeLog {
        zoneId = zone
        instant = OffsetDateTime.parse(at).toInstant()
        return PracticeCalendar(this).complete(log, startedAt = instant.minusSeconds(5 * 60))
    }

    private fun MovableClock.summaryAt(log: PracticeLog, at: String, zone: ZoneId = zoneId): PracticeSummary {
        zoneId = zone
        instant = OffsetDateTime.parse(at).toInstant()
        return PracticeCalendar(this).summary(log)
    }

    @Test
    fun consecutiveDaysEndingTodayCount() {
        val days = setOf(day("2026-09-23"), day("2026-09-24"), day("2026-09-25"))
        assertEquals(3, PracticeStreak.current(days, day("2026-09-25")))
        assertTrue(PracticeStreak.practisedToday(days, day("2026-09-25")))
    }

    @Test
    fun aMissedDayEndsTheStreak() {
        val days = setOf(day("2026-09-20"), day("2026-09-21"), day("2026-09-23"), day("2026-09-24"))
        assertEquals(2, PracticeStreak.current(days, day("2026-09-24")))
        assertEquals("the longest run is kept apart", 2, PracticeStreak.best(days))
        // A whole day with nothing: the streak is gone, not merely «not yet today».
        assertEquals(0, PracticeStreak.current(days, day("2026-09-26")))
    }

    @Test
    fun notYetTodayKeepsTheStreakUntilTheDayIsOver() {
        val days = setOf(day("2026-09-23"), day("2026-09-24"))
        assertEquals(2, PracticeStreak.current(days, day("2026-09-25")))
        assertFalse(PracticeStreak.practisedToday(days, day("2026-09-25")))
        assertEquals("today's session extends it", 3, PracticeStreak.current(days + day("2026-09-25"), day("2026-09-25")))
    }

    @Test
    fun nothingYetIsZero() {
        assertEquals(0, PracticeStreak.current(emptySet(), day("2026-09-25")))
        assertEquals(0, PracticeStreak.best(emptySet()))
    }

    @Test
    fun theSpringForwardNightNeitherBreaksNorMergesDays() {
        // Athens, 29 March 2026: 03:00 becomes 04:00. The first two sessions are 23 hours apart, the
        // last two only one hour — yet they are three calendar days, so three days in a row.
        val clock = MovableClock(Instant.EPOCH, athens)
        var log = PracticeLog()
        log = clock.completeAt(log, "2026-03-28T23:30:00+02:00")
        log = clock.completeAt(log, "2026-03-29T23:30:00+03:00")
        log = clock.completeAt(log, "2026-03-30T00:30:00+03:00")
        assertEquals(setOf(day("2026-03-28"), day("2026-03-29"), day("2026-03-30")), log.practisedDays)
        assertEquals(3, clock.summaryAt(log, "2026-03-30T12:00:00+03:00").streak)
    }

    @Test
    fun theFallBackNightKeepsTwoDaysApartEvenWhenMoreThan48HoursPass() {
        // Athens, 25 October 2026: 04:00 becomes 03:00, a 25-hour day. Early on the 24th and late on
        // the 25th are 48 hours 40 minutes apart, and still two days in a row.
        val clock = MovableClock(Instant.EPOCH, athens)
        var log = PracticeLog()
        log = clock.completeAt(log, "2026-10-24T00:10:00+03:00")
        log = clock.completeAt(log, "2026-10-25T23:50:00+02:00")
        assertEquals(2, clock.summaryAt(log, "2026-10-25T23:55:00+02:00").streak)
    }

    @Test
    fun flyingWestTheDayIsTheOneTheUserLived() {
        val clock = MovableClock(Instant.EPOCH, athens)
        var log = clock.completeAt(PracticeLog(), "2026-03-01T23:30:00+02:00") // 1 March, Athens
        // Landed in New York the same evening: it is still 1 March there, and that day is done.
        val sameEvening = clock.summaryAt(log, "2026-03-01T18:00:00-05:00", zone = newYork)
        assertTrue(sameEvening.practisedToday)
        assertEquals(1, sameEvening.streak)
        // The next evening in New York, 2 March — already 3 March in UTC. Two days in a row.
        log = clock.completeAt(log, "2026-03-02T20:00:00-05:00", zone = newYork)
        assertEquals(setOf(day("2026-03-01"), day("2026-03-02")), log.practisedDays)
        assertEquals(2, clock.summaryAt(log, "2026-03-02T21:00:00-05:00", zone = newYork).streak)
    }

    @Test
    fun flyingEastNotYetTodayStillHoldsYesterday() {
        val clock = MovableClock(Instant.EPOCH, newYork)
        var log = clock.completeAt(PracticeLog(), "2026-03-01T20:00:00-05:00") // 1 March, New York
        // Landed in Athens on 2 March: nothing yet today, and the streak still stands.
        val landed = clock.summaryAt(log, "2026-03-02T15:00:00+02:00", zone = athens)
        assertFalse(landed.practisedToday)
        assertEquals(1, landed.streak)
        log = clock.completeAt(log, "2026-03-02T19:00:00+02:00", zone = athens)
        assertEquals(2, clock.summaryAt(log, "2026-03-02T19:10:00+02:00", zone = athens).streak)
    }
}
