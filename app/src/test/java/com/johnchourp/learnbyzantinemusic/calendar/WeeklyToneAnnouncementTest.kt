package com.johnchourp.learnbyzantinemusic.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * The tone announced «from tonight's vespers» (ClickUp `869f5x24r`).
 *
 * The tone of a day comes from [LiturgicalToneCycle]; what is tested here is only the evening rule:
 * from [WeeklyToneAnnouncement.VESPERS] on, tomorrow's tone is announced when it exists and differs
 * from today's. Dates are in 2026 (Pascha 12 April); the ordinary week is the one of Sunday 20
 * September, which the published charts give Βαρύς, followed by Πλ. Δ΄ on 27 September.
 */
class WeeklyToneAnnouncementTest {

    private val announcement = WeeklyToneAnnouncement()

    private fun at(text: String) = announcement.at(LocalDateTime.parse(text))

    @Test
    fun vespersIsTheOwnersNoon() {
        // An advance-notice hour, not the time of the service; changing it is the owner's decision.
        assertEquals(LocalTime.of(12, 0), WeeklyToneAnnouncement.VESPERS)
    }

    @Test
    fun saturdayMorningStillShowsTheWeekThatIsEnding() {
        val now = at("2026-09-26T11:59")
        assertEquals(VARYS, now.today.toneIndex)
        assertNull("before the vespers hour nothing is announced", now.fromVespers)
        assertEquals(VARYS, now.currentToneIndex)
    }

    @Test
    fun fromSaturdayNoonTheNextWeeksToneIsAnnounced() {
        listOf("2026-09-26T12:00", "2026-09-26T18:30", "2026-09-26T23:59").forEach { moment ->
            val now = at(moment)
            assertEquals("$moment today", VARYS, now.today.toneIndex)
            assertEquals("$moment from vespers", PLAGAL_FOURTH, now.fromVespers?.toneIndex)
            assertEquals("$moment current", PLAGAL_FOURTH, now.currentToneIndex)
        }
    }

    @Test
    fun sundayMidnightIsTheNewWeekWithNothingMoreToAnnounce() {
        val now = at("2026-09-27T00:00")
        assertEquals(PLAGAL_FOURTH, now.today.toneIndex)
        assertNull(now.fromVespers)
        // Sunday evening: Monday keeps the week's tone, so there is still nothing to announce.
        assertNull(at("2026-09-27T19:00").fromVespers)
    }

    @Test
    fun anOrdinaryWeekdayEveningAnnouncesNothing() {
        assertNull(at("2026-09-23T18:00").fromVespers)
    }

    @Test
    fun everyBrightWeekEveningAnnouncesTomorrowsTone() {
        val morning = at("2026-04-14T10:00") // Bright Tuesday
        assertEquals(LiturgicalToneKind.BRIGHT_WEEK_DAY, morning.today.kind)
        assertEquals(THIRD, morning.today.toneIndex)
        assertNull(morning.fromVespers)

        val evening = at("2026-04-14T18:00")
        assertEquals("Bright Wednesday's tone from Tuesday's vespers", FOURTH, evening.fromVespers?.toneIndex)
        assertEquals(FOURTH, evening.currentToneIndex)

        // Bright Saturday evening: the vespers of Thomas Sunday, Α΄, where the weekly cycle starts.
        val brightSaturday = at("2026-04-18T18:00")
        assertEquals(PLAGAL_FOURTH, brightSaturday.today.toneIndex)
        assertEquals(LiturgicalToneKind.WEEKLY, brightSaturday.fromVespers?.kind)
        assertEquals(FIRST, brightSaturday.fromVespers?.toneIndex)
    }

    @Test
    fun holySaturdayAfternoonAnnouncesPaschasFirstTone() {
        val morning = at("2026-04-11T11:59")
        assertEquals(LiturgicalToneKind.HOLY_WEEK, morning.today.kind)
        assertNull(morning.currentToneIndex)

        val afternoon = at("2026-04-11T15:00")
        assertNull("Holy Saturday itself has no tone", afternoon.today.toneIndex)
        assertEquals(LiturgicalToneKind.BRIGHT_WEEK_DAY, afternoon.fromVespers?.kind)
        assertEquals(FIRST, afternoon.fromVespers?.toneIndex)
        assertEquals(FIRST, afternoon.currentToneIndex)
    }

    @Test
    fun nothingIsAnnouncedWhenTomorrowHasNoTone() {
        // Lazarus Saturday: Palm Sunday begins no tone, the 5th week of Lent's Α΄ just stops.
        val lazarus = at("2026-04-04T18:00")
        assertEquals(FIRST, lazarus.today.toneIndex)
        assertNull(lazarus.fromVespers)
        assertEquals(FIRST, lazarus.currentToneIndex)

        // The Saturday before Pentecost: the Holy Fathers' Πλ. Β΄ stops, Pentecost has none.
        val beforePentecost = at("2026-05-30T18:00")
        assertEquals(PLAGAL_SECOND, beforePentecost.today.toneIndex)
        assertNull(beforePentecost.fromVespers)

        // Inside Holy Week an evening announces nothing either.
        assertNull(at("2026-04-08T18:00").currentToneIndex)
    }

    @Test
    fun theSaturdayOfPentecostWeekAnnouncesAllSaints() {
        val now = at("2026-06-06T18:00")
        assertEquals(LiturgicalToneKind.PENTECOST_WEEK, now.today.kind)
        assertEquals(PLAGAL_FOURTH, now.fromVespers?.toneIndex)
    }

    @Test
    fun theInjectedClockIsReadInItsOwnTimeZone() {
        // 09:00 UTC is noon in Athens in September (UTC+3): the same instant, two answers.
        val instant = Instant.parse("2026-09-26T09:00:00Z")
        val athens = WeeklyToneAnnouncement(Clock.fixed(instant, ZoneId.of("Europe/Athens"))).now()
        val utc = WeeklyToneAnnouncement(Clock.fixed(instant, ZoneId.of("UTC"))).now()
        assertEquals(PLAGAL_FOURTH, athens.fromVespers?.toneIndex)
        assertNull(utc.fromVespers)
    }

    private companion object {
        const val FIRST = 0
        const val THIRD = 2
        const val FOURTH = 3
        const val PLAGAL_SECOND = 5
        const val VARYS = 6
        const val PLAGAL_FOURTH = 7
    }
}
