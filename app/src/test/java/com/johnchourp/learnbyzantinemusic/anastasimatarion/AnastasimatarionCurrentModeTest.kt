package com.johnchourp.learnbyzantinemusic.anastasimatarion

import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.anastasimatarion.AnastasimatarionWeekMode.CurrentMode
import com.johnchourp.learnbyzantinemusic.anastasimatarion.AnastasimatarionWeekMode.currentMode
import com.johnchourp.learnbyzantinemusic.calendar.WeeklyToneAnnouncement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

/**
 * The mode the Anastasimatarion opens on at a given moment, and the badge that names it (ClickUp
 * `869f5x24r`). The badge must be true in every kind: «Ήχος της εβδομάδας», «Ήχος της ημέρας» in
 * Bright Week, «Από τον εσπερινό απόψε» once the vespers hour has passed — and none at all when the
 * page falls back to the first mode because there is no tone.
 *
 * Dates in 2026 (Pascha 12 April); the week of 20 September is Βαρύς, 27 September Πλ. Δ΄.
 */
class AnastasimatarionCurrentModeTest {

    private fun at(moment: String) = currentMode(LocalDateTime.parse(moment))

    @Test
    fun anOrdinaryWeekNamesTheToneOfTheWeek() {
        assertEquals(CurrentMode("varys", R.string.anastasimatarion_week_mode_template), at("2026-09-23T18:00"))
        assertEquals(CurrentMode("varys", R.string.anastasimatarion_week_mode_template), at("2026-09-26T11:59"))
    }

    @Test
    fun fromSaturdayNoonItOpensTonightsVespersTone() {
        assertEquals(CurrentMode("plagal_fourth", R.string.anastasimatarion_vespers_mode_template), at("2026-09-26T12:00"))
        assertEquals(CurrentMode("plagal_fourth", R.string.anastasimatarion_vespers_mode_template), at("2026-09-26T23:59"))
        // Sunday: the new week's tone, named as the week's again.
        assertEquals(CurrentMode("plagal_fourth", R.string.anastasimatarion_week_mode_template), at("2026-09-27T00:00"))
    }

    @Test
    fun brightWeekNamesTheToneOfTheDayUntilTheEvening() {
        assertEquals(CurrentMode("third", R.string.anastasimatarion_day_mode_template), at("2026-04-14T10:00"))
        assertEquals(CurrentMode("fourth", R.string.anastasimatarion_vespers_mode_template), at("2026-04-14T18:00"))
        // Bright Saturday evening: Thomas Sunday's Α΄.
        assertEquals(CurrentMode("first", R.string.anastasimatarion_vespers_mode_template), at("2026-04-18T18:00"))
    }

    @Test
    fun holyWeekHasNoModeUntilHolySaturdayAfternoon() {
        assertNull("the first mode, and no badge", at("2026-04-08T18:00"))
        assertNull(at("2026-04-11T11:59"))
        assertEquals(CurrentMode("first", R.string.anastasimatarion_vespers_mode_template), at("2026-04-11T15:00"))
    }

    @Test
    fun lazarusSaturdayEveningKeepsTheWeeksTone() {
        assertEquals(CurrentMode("first", R.string.anastasimatarion_week_mode_template), at("2026-04-04T18:00"))
    }

    @Test
    fun itAlwaysOpensTheToneTheHomeCardAnnounces() {
        // Every six hours for a year: the page and the home card must never disagree.
        val announcement = WeeklyToneAnnouncement()
        var moment = LocalDateTime.parse("2026-01-01T00:00")
        while (moment.year == 2026) {
            val expected = announcement.at(moment).currentToneIndex?.let { AnastasimatarionLabels.MODE_ORDER[it] }
            assertEquals("$moment", expected, currentMode(moment)?.modeKey)
            moment = moment.plusHours(6)
        }
    }
}
