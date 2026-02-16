package com.johnchourp.learnbyzantinemusic.calendar

import com.johnchourp.learnbyzantinemusic.R
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class LiturgicalToneCycle {
    private val toneNameResIds = listOf(
        R.string.mode_first,
        R.string.mode_second,
        R.string.mode_third,
        R.string.mode_fourth,
        R.string.mode_plagal_first,
        R.string.mode_plagal_second,
        R.string.mode_varys,
        R.string.mode_plagal_fourth
    )

    fun resolveTone(date: LocalDate): WeeklyToneResult {
        val weekStart = startOfWeekSunday(date)
        val weekEnd = weekStart.plusDays(6)
        val cycleStart = computeCycleStartForDate(date)
        val weeksBetween = ChronoUnit.WEEKS.between(cycleStart, weekStart).toInt()
        val toneIndex = Math.floorMod(weeksBetween, toneNameResIds.size)

        return WeeklyToneResult(
            selectedDate = date,
            weekStart = weekStart,
            weekEnd = weekEnd,
            toneIndex = toneIndex,
            toneNameRes = toneNameResIds[toneIndex]
        )
    }

    internal fun computeCycleStartForDate(date: LocalDate): LocalDate {
        val weekStart = startOfWeekSunday(date)
        val cycleStartCurrentYear = computeCycleStartForYear(date.year)
        if (!weekStart.isBefore(cycleStartCurrentYear)) {
            return cycleStartCurrentYear
        }
        return computeCycleStartForYear(date.year - 1)
    }

    internal fun computeCycleStartForYear(year: Int): LocalDate {
        val pascha = OrthodoxPaschaCalculator.computePaschaDate(year)
        val pentecost = pascha.plusDays(49)
        val secondSundayAfterPentecost = pentecost.plusWeeks(2)
        return startOfWeekSunday(secondSundayAfterPentecost)
    }

    internal fun startOfWeekSunday(date: LocalDate): LocalDate {
        val daysFromSunday = date.dayOfWeek.value % 7
        return date.minusDays(daysFromSunday.toLong())
    }
}

data class WeeklyToneResult(
    val selectedDate: LocalDate,
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val toneIndex: Int,
    val toneNameRes: Int
)
