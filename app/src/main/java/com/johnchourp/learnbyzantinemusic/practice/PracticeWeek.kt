package com.johnchourp.learnbyzantinemusic.practice

import java.time.LocalDate

/** One bar of the weekly chart in «Ιστορικό εξάσκησης». */
data class PracticeDayBar(val date: LocalDate, val minutes: Int, val sessions: Int)

/**
 * The weekly chart of «Ιστορικό εξάσκησης» (ClickUp `869f5x2dy`): the last seven days ending today,
 * oldest first, so today is always the last bar and no week-start convention (Monday, or the Sunday
 * of the liturgical week) has to be chosen.
 */
object PracticeWeek {
    const val DAYS = 7

    fun bars(log: PracticeLog, today: LocalDate): List<PracticeDayBar> =
        (DAYS - 1 downTo 0).map { back ->
            val date = today.minusDays(back.toLong())
            val day = log.days[date]
            PracticeDayBar(date = date, minutes = day?.minutes ?: 0, sessions = day?.sessions ?: 0)
        }

    fun totalMinutes(bars: List<PracticeDayBar>): Int = bars.sumOf { it.minutes }

    fun daysPractised(bars: List<PracticeDayBar>): Int = bars.count { it.sessions > 0 }
}
