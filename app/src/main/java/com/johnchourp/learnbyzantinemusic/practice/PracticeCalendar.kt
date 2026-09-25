package com.johnchourp.learnbyzantinemusic.practice

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * The clock-dependent rules of practice (ClickUp `869f5x2dy`), pure so they can be tested on any
 * clock: which day a completed session counts for, how long it took, and what the streak is today.
 *
 * **The day is the device's calendar date when the session is completed.** Null [clock] reads the
 * device's clock and time zone at every call, so after a flight the new zone applies from then on,
 * and the sessions already recorded keep the dates the user lived them on.
 */
class PracticeCalendar(private val clock: Clock? = null) {

    private fun clock(): Clock = clock ?: Clock.systemDefaultZone()

    fun today(): LocalDate = LocalDate.now(clock())

    fun now(): Instant = clock().instant()

    /** [log] with one more completed session: it started at [startedAt] and ends now. */
    fun complete(log: PracticeLog, startedAt: Instant): PracticeLog =
        log.record(today(), PracticeLog.sessionMinutes(Duration.between(startedAt, now())))

    fun summary(log: PracticeLog): PracticeSummary = PracticeSummary.of(log, today())
}
