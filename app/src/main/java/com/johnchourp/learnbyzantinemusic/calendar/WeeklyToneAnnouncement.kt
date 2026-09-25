package com.johnchourp.learnbyzantinemusic.calendar

import java.time.Clock
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * «Which tone now?» for the home card and the Anastasimatarion (ClickUp `869f5x24r`): today's tone,
 * and — from [VESPERS] on — the tone that tonight's vespers begins, when that is a different one.
 *
 * ## Why an evening announcement
 *
 * A tone does not begin at midnight. It begins at the vespers of the evening before: the tone of the
 * week at Saturday's vespers, each day's tone in Bright Week at the previous evening's, Pascha's Α΄ at
 * the vespers of Holy Saturday, Thomas Sunday's Α΄ at the vespers of Bright Saturday.
 * [LiturgicalToneCycle] answers per DATE, so on a Saturday afternoon it still names the week that is
 * ending — exactly when a chanter opens the app to prepare. One rule covers every one of those cases:
 * after [VESPERS], if tomorrow has a tone and it is not today's, announce it.
 *
 * Nothing is announced when tomorrow has no tone: on Lazarus Saturday (→ Palm Sunday) and on the
 * Saturday before Pentecost nothing new begins at vespers — the weekly tone just stops.
 *
 * ## [VESPERS] is an advance-notice hour, not the time of the service
 *
 * 12:00 local time, a value the owner delegated (`869f5x24r`). Vespers itself is sung in the evening;
 * noon is when the app starts naming the evening's tone, so that someone preparing in the afternoon
 * already sees it. This constant is the only place the hour lives.
 *
 * The tone comes from [LiturgicalToneCycle] and nowhere else — no second rule. The clock is injected,
 * and [at] takes the moment explicitly, so every boundary is testable without waiting for a Saturday.
 */
class WeeklyToneAnnouncement(
    /** Null reads the device's clock and time zone at every call, so a zone change is picked up. */
    private val clock: Clock? = null,
    private val toneCycle: LiturgicalToneCycle = LiturgicalToneCycle(),
) {
    fun now(): Announcement = at(LocalDateTime.now(clock ?: Clock.systemDefaultZone()))

    fun at(moment: LocalDateTime): Announcement {
        val today = toneCycle.resolveTone(moment.toLocalDate())
        val fromVespers = if (moment.toLocalTime().isBefore(VESPERS)) {
            null
        } else {
            toneCycle.resolveTone(moment.toLocalDate().plusDays(1))
                .takeIf { tomorrow -> tomorrow.toneIndex != null && tomorrow.toneIndex != today.toneIndex }
        }
        return Announcement(today = today, fromVespers = fromVespers)
    }

    data class Announcement(
        /** Today's date resolved by the cycle: its kind, and its tone (null in a week without one). */
        val today: WeeklyToneResult,
        /** Tomorrow, when tonight's vespers begins a tone that is not today's; otherwise null. */
        val fromVespers: WeeklyToneResult?,
    ) {
        /**
         * The tone to open things on: tonight's once it is announced, otherwise today's. Null only
         * when there is neither — in Holy Week and in the week of Pentecost, until [VESPERS] on
         * their Saturday.
         */
        val currentToneIndex: Int? get() = fromVespers?.toneIndex ?: today.toneIndex
    }

    companion object {
        /**
         * From this local time on, the tone of tonight's vespers is announced. An advance-notice
         * hour chosen by the owner, not the time of the service.
         */
        val VESPERS: LocalTime = LocalTime.NOON
    }
}
