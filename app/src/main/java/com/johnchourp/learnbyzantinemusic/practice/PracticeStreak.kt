package com.johnchourp.learnbyzantinemusic.practice

import java.time.LocalDate

/**
 * «Συνέχεια»: how many days in a row the user has completed a practice session (ClickUp `869f5x2dy`).
 * The same counter is meant for every kind of practice (E3's flash cards included), because it reads
 * only the days of [PracticeLog], never which session filled them.
 *
 * **A streak is not lost before the day is over.** Until today has a completed session, the count
 * ends yesterday: at 09:00 a user who practised every day this week still sees their streak, marked
 * «not yet today», and it only drops to 0 once a whole day passes with no practice.
 *
 * Days are compared as calendar dates, never as 24-hour spans, so the 23- and 25-hour days of a
 * daylight-saving change cannot break or double a streak.
 */
object PracticeStreak {

    /** Days in a row ending today — or ending yesterday while today is still open. */
    fun current(practised: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in practised) today else today.minusDays(1)
        var count = 0
        while (day in practised) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    fun practisedToday(practised: Set<LocalDate>, today: LocalDate): Boolean = today in practised

    /** The longest run of consecutive days there has ever been. */
    fun best(practised: Set<LocalDate>): Int {
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (day in practised.sorted()) {
            run = if (previous?.plusDays(1) == day) run + 1 else 1
            best = maxOf(best, run)
            previous = day
        }
        return best
    }
}
