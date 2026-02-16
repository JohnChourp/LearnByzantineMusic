package com.johnchourp.learnbyzantinemusic.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LiturgicalToneCycleTest {
    private val toneCycle = LiturgicalToneCycle()

    @Test
    fun returnsSameToneInsideSameWeek() {
        val monday = LocalDate.of(2026, 2, 2)
        val thursday = LocalDate.of(2026, 2, 5)

        val mondayTone = toneCycle.resolveTone(monday)
        val thursdayTone = toneCycle.resolveTone(thursday)

        assertEquals(mondayTone.toneIndex, thursdayTone.toneIndex)
        assertEquals(mondayTone.weekStart, thursdayTone.weekStart)
        assertEquals(mondayTone.weekEnd, thursdayTone.weekEnd)
    }

    @Test
    fun changesToneExactlyOnSundayBoundary() {
        val saturday = LocalDate.of(2026, 2, 7)
        val sunday = LocalDate.of(2026, 2, 8)

        val saturdayTone = toneCycle.resolveTone(saturday)
        val sundayTone = toneCycle.resolveTone(sunday)

        assertNotEquals(saturdayTone.toneIndex, sundayTone.toneIndex)
        assertEquals(saturday.plusDays(1), sunday)
    }

    @Test
    fun januaryUsesCycleStartedInPreviousSummer() {
        val januaryDate = LocalDate.of(2026, 1, 15)

        val cycleStartForDate = toneCycle.computeCycleStartForDate(januaryDate)
        val cycleStartForCurrentYear = toneCycle.computeCycleStartForYear(2026)
        val cycleStartForPreviousYear = toneCycle.computeCycleStartForYear(2025)

        assertEquals(cycleStartForPreviousYear, cycleStartForDate)
        assertTrue(cycleStartForDate.isBefore(cycleStartForCurrentYear))
    }
}
