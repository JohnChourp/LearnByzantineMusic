package com.johnchourp.learnbyzantinemusic.practice

import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * The daily practice reminder's rules (ClickUp `869f5x2dy`), kept pure so they can be tested for any
 * day and any clock.
 *
 * The reminder is opt-in and local: WorkManager wakes the app near the chosen time and a notification
 * is posted only when the user has not completed a practice session yet that day. «Near» is honest —
 * Android batches background work, especially in Doze, so the notification may come some minutes late.
 */
object PracticeReminderPolicy {

    /** The time offered when the reminder is first switched on. */
    val DEFAULT_TIME: LocalTime = LocalTime.of(19, 0)

    /** Notify only when the reminder is on and today has no completed practice yet. */
    fun shouldNotify(enabled: Boolean, practisedToday: Boolean): Boolean = enabled && !practisedToday

    /**
     * The next moment at [time] after [now], in [now]'s time zone: today if it is still ahead, else
     * tomorrow. On the night the clocks spring forward, a time that does not exist moves later by the
     * gap, as `ZonedDateTime` resolves it; a time that happens twice fires at its first occurrence.
     */
    fun nextReminderAt(now: ZonedDateTime, time: LocalTime): ZonedDateTime {
        val today = now.toLocalDate().atTime(time).atZone(now.zone)
        return if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(time).atZone(now.zone)
    }
}
