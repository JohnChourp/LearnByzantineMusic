package com.johnchourp.learnbyzantinemusic.practice

import java.time.Duration
import java.time.LocalDate

/**
 * The app's one record of practice (ClickUp `869f5x2dy`): for each calendar day, how many practice
 * sessions the user COMPLETED and the minutes they took.
 *
 * «Πεντάλεπτο της ημέρας» writes it today. The phthong flash cards of E3 (ClickUp `869f4tqc7`, not
 * built and out of scope here) are meant to write this same record, so the app has one streak, not
 * two: [PracticeStreak] reads only which days have a completed session, not what the session was.
 *
 * A day is the [LocalDate] on the device's clock where the session was completed — the calendar day
 * the user lived, not a UTC day and not a 24-hour slice. That is what keeps a streak honest across a
 * daylight-saving change or a flight (see `PracticeStreakTest`).
 *
 * Immutable: [record] returns a new log. Stored as JSON by [PracticeLogCodec].
 */
data class PracticeLog(val days: Map<LocalDate, PracticeDay> = emptyMap()) {

    /** The days with at least one completed session. */
    val practisedDays: Set<LocalDate> get() = days.filterValues { it.sessions > 0 }.keys

    /** One more completed session on [date], of [minutes] minutes. */
    fun record(date: LocalDate, minutes: Int): PracticeLog {
        val before = days[date] ?: PracticeDay(sessions = 0, minutes = 0)
        val after = PracticeDay(sessions = before.sessions + 1, minutes = before.minutes + minutes.coerceIn(0, MAX_SESSION_MINUTES))
        return copy(days = days + (date to after))
    }

    companion object {
        /**
         * A session left open for an hour counts this much at most, so one forgotten screen cannot
         * swamp the weekly chart.
         */
        const val MAX_SESSION_MINUTES = 30

        /** Whole minutes a session took, rounded up: a completed session is never «0 λεπτά». */
        fun sessionMinutes(elapsed: Duration): Int =
            ((elapsed.seconds.coerceAtLeast(0) + 59) / 60).toInt().coerceIn(1, MAX_SESSION_MINUTES)
    }
}

/** One day of the log. */
data class PracticeDay(val sessions: Int, val minutes: Int)
