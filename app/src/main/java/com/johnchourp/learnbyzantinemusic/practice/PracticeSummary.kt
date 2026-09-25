package com.johnchourp.learnbyzantinemusic.practice

import java.time.LocalDate

/** What the home card and «Ιστορικό εξάσκησης» show about the user's practice, for one day. */
data class PracticeSummary(
    /** Days in a row, ending today or — while today is still open — yesterday. */
    val streak: Int,
    val practisedToday: Boolean,
    val bestStreak: Int,
    /** The last seven days ending today, oldest first. */
    val week: List<PracticeDayBar>,
) {
    companion object {
        fun of(log: PracticeLog, today: LocalDate): PracticeSummary {
            val days = log.practisedDays
            return PracticeSummary(
                streak = PracticeStreak.current(days, today),
                practisedToday = PracticeStreak.practisedToday(days, today),
                bestStreak = PracticeStreak.best(days),
                week = PracticeWeek.bars(log, today),
            )
        }
    }
}
