package com.johnchourp.learnbyzantinemusic.practice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The daily reminder (ClickUp `869f5x2dy`): it speaks only when the day has no completed practice,
 * and it keeps its local time across the clock changes. Clocks and moments are injected.
 */
class PracticeReminderPolicyTest {

    private val athens = ZoneId.of("Europe/Athens")
    private val seven = LocalTime.of(19, 0)

    private fun at(text: String): ZonedDateTime = ZonedDateTime.parse(text)

    @Test
    fun itNotifiesOnlyWhenOnAndTodayHasNoPracticeYet() {
        assertTrue(PracticeReminderPolicy.shouldNotify(enabled = true, practisedToday = false))
        assertFalse(PracticeReminderPolicy.shouldNotify(enabled = true, practisedToday = true))
        assertFalse(PracticeReminderPolicy.shouldNotify(enabled = false, practisedToday = false))
        assertFalse(PracticeReminderPolicy.shouldNotify(enabled = false, practisedToday = true))
    }

    @Test
    fun aSessionCompletedEarlierTodaySilencesTheEveningReminder() {
        val reminderTime = Clock.fixed(OffsetDateTime.parse("2026-09-25T19:00:00+03:00").toInstant(), athens)
        val calendar = PracticeCalendar(reminderTime)
        val practisedAtSix = PracticeLog().record(LocalDate.parse("2026-09-25"), 5)
        val practisedYesterday = PracticeLog().record(LocalDate.parse("2026-09-24"), 5)
        assertFalse(PracticeReminderPolicy.shouldNotify(true, calendar.summary(practisedAtSix).practisedToday))
        assertTrue(PracticeReminderPolicy.shouldNotify(true, calendar.summary(practisedYesterday).practisedToday))
        assertTrue(PracticeReminderPolicy.shouldNotify(true, calendar.summary(PracticeLog()).practisedToday))
    }

    @Test
    fun theNextReminderIsTodayWhileItsTimeIsAheadOtherwiseTomorrow() {
        assertEquals(at("2026-09-25T19:00+03:00[Europe/Athens]"), PracticeReminderPolicy.nextReminderAt(at("2026-09-25T18:59+03:00[Europe/Athens]"), seven))
        assertEquals(at("2026-09-26T19:00+03:00[Europe/Athens]"), PracticeReminderPolicy.nextReminderAt(at("2026-09-25T19:00+03:00[Europe/Athens]"), seven))
        assertEquals(at("2026-09-26T19:00+03:00[Europe/Athens]"), PracticeReminderPolicy.nextReminderAt(at("2026-09-25T23:30+03:00[Europe/Athens]"), seven))
    }

    @Test
    fun itKeepsItsLocalTimeAcrossTheSpringForwardNight() {
        // 28 March 19:30 (UTC+2) → 29 March 19:00 (UTC+3): 22½ hours later, still 19:00 on the clock.
        val next = PracticeReminderPolicy.nextReminderAt(at("2026-03-28T19:30+02:00[Europe/Athens]"), seven)
        assertEquals(at("2026-03-29T19:00+03:00[Europe/Athens]"), next)
    }

    @Test
    fun aTimeThatDoesNotExistThatNightMovesLater() {
        // 29 March 2026 in Athens has no 03:30: the clocks jump from 03:00 to 04:00.
        val next = PracticeReminderPolicy.nextReminderAt(at("2026-03-29T01:00+02:00[Europe/Athens]"), LocalTime.of(3, 30))
        assertEquals(at("2026-03-29T04:30+03:00[Europe/Athens]"), next)
    }

    @Test
    fun aTimeThatHappensTwiceFiresTheFirstTime() {
        // 25 October 2026 in Athens has two 03:30s; the reminder takes the first (still UTC+3).
        val next = PracticeReminderPolicy.nextReminderAt(at("2026-10-25T01:00+03:00[Europe/Athens]"), LocalTime.of(3, 30))
        assertEquals(OffsetDateTime.parse("2026-10-25T03:30+03:00").toInstant(), next.toInstant())
    }
}
