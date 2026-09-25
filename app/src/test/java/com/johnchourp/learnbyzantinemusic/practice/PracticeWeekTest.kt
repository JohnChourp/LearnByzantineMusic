package com.johnchourp.learnbyzantinemusic.practice

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate

/** The weekly chart of «Ιστορικό εξάσκησης» and what one session adds to it (ClickUp `869f5x2dy`). */
class PracticeWeekTest {

    private fun day(text: String) = LocalDate.parse(text)

    @Test
    fun theWeekIsTheLastSevenDaysEndingToday() {
        val log = PracticeLog()
            .record(day("2026-09-18"), 5) // eight days back: outside the week
            .record(day("2026-09-19"), 6)
            .record(day("2026-09-22"), 4)
            .record(day("2026-09-25"), 5)
        val bars = PracticeWeek.bars(log, today = day("2026-09-25"))
        assertEquals((19..25).map { day("2026-09-%02d".format(it)) }, bars.map { it.date })
        assertEquals(listOf(6, 0, 0, 4, 0, 0, 5), bars.map { it.minutes })
        assertEquals(3, PracticeWeek.daysPractised(bars))
        assertEquals(15, PracticeWeek.totalMinutes(bars))
    }

    @Test
    fun twoSessionsOnOneDayAddUpAndCountAsOneDay() {
        val log = PracticeLog().record(day("2026-09-25"), 5).record(day("2026-09-25"), 7)
        val today = PracticeWeek.bars(log, today = day("2026-09-25")).last()
        assertEquals(2, today.sessions)
        assertEquals(12, today.minutes)
        assertEquals(1, PracticeWeek.daysPractised(PracticeWeek.bars(log, day("2026-09-25"))))
    }

    @Test
    fun anEmptyHistoryIsSevenEmptyBars() {
        val bars = PracticeWeek.bars(PracticeLog(), today = day("2026-09-25"))
        assertEquals(PracticeWeek.DAYS, bars.size)
        assertEquals(0, PracticeWeek.totalMinutes(bars))
        assertEquals(0, PracticeWeek.daysPractised(bars))
    }

    @Test
    fun aSessionCountsWholeMinutesAtLeastOneAndAtMostThirty() {
        assertEquals(1, PracticeLog.sessionMinutes(Duration.ofSeconds(10)))
        assertEquals(5, PracticeLog.sessionMinutes(Duration.ofMinutes(5)))
        assertEquals("rounded up", 6, PracticeLog.sessionMinutes(Duration.ofSeconds(5 * 60 + 1)))
        assertEquals("a forgotten session", PracticeLog.MAX_SESSION_MINUTES, PracticeLog.sessionMinutes(Duration.ofHours(3)))
        assertEquals("a clock that went back", 1, PracticeLog.sessionMinutes(Duration.ofMinutes(-4)))
        assertEquals(30, PracticeLog().record(day("2026-09-25"), 90).days.getValue(day("2026-09-25")).minutes)
    }
}
