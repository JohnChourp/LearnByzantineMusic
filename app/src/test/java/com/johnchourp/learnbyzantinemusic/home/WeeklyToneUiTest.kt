package com.johnchourp.learnbyzantinemusic.home

import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.calendar.WeeklyToneAnnouncement
import com.johnchourp.learnbyzantinemusic.modes.ui.EIGHT_MODES
import com.johnchourp.learnbyzantinemusic.modes.ui.eightModesIndexOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

/**
 * What the «tone of the week» card on home shows, and what its buttons open (ClickUp `869f5x24r`).
 *
 * Built through the same [WeeklyToneUi.from] the home screen uses, from real dates of 2026 (Pascha
 * 12 April; the week of 20 September is Βαρύς, 27 September Πλ. Δ΄ on the published charts).
 */
class WeeklyToneUiTest {

    private val announcement = WeeklyToneAnnouncement()
    private val openedModes = mutableListOf<String>()
    private var anastasimatarionOpened = 0

    private fun card(moment: String): WeeklyToneUi = WeeklyToneUi.from(
        announcement = announcement.at(LocalDateTime.parse(moment)),
        openEightModes = { openedModes += it },
        openAnastasimatarion = { anastasimatarionOpened++ },
    )

    /** The mode key «Άνοιξε στους 8 Ήχους» would open, or null when the button is hidden. */
    private fun opensInEightModes(card: WeeklyToneUi): String? {
        val open = card.onOpenEightModes ?: return null
        openedModes.clear()
        open()
        return openedModes.single()
    }

    @Test
    fun anOrdinaryWeekSaysThisWeekAndOpensItsTone() {
        val card = card("2026-09-23T10:00")
        assertEquals(R.string.home_weekly_tone_this_week, card.headlineRes)
        assertEquals(R.string.mode_varys, card.headlineToneRes)
        assertNull(card.vespersToneRes)
        assertEquals("varys", opensInEightModes(card))
        // A weekday evening: tomorrow keeps the same tone, so there is no second line to show.
        assertNull(card("2026-09-23T18:00").vespersToneRes)
    }

    @Test
    fun fromSaturdayNoonTheSecondLineAndTheButtonMoveToTonightsTone() {
        val morning = card("2026-09-26T11:59")
        assertNull(morning.vespersToneRes)
        assertEquals("varys", opensInEightModes(morning))

        val afternoon = card("2026-09-26T12:00")
        assertEquals("the headline is still this week", R.string.mode_varys, afternoon.headlineToneRes)
        assertEquals(R.string.mode_plagal_fourth, afternoon.vespersToneRes)
        assertEquals("plagal_fourth", opensInEightModes(afternoon))
    }

    @Test
    fun brightWeekNamesTheToneOfTheDay() {
        val card = card("2026-04-13T10:00") // Bright Monday
        assertEquals(R.string.weekly_mode_calendar_tone_bright_week, card.headlineRes)
        assertEquals(R.string.mode_second, card.headlineToneRes)
        assertEquals("second", opensInEightModes(card))
    }

    @Test
    fun theWeeksWithoutAToneSaySoAndOfferNoModeToOpen() {
        val holyWeek = card("2026-04-08T10:00")
        assertEquals(R.string.weekly_mode_calendar_tone_none_holy_week, holyWeek.headlineRes)
        assertNull(holyWeek.headlineToneRes)
        assertNull("no «Άνοιξε στους 8 Ήχους» without a tone", holyWeek.onOpenEightModes)

        val pentecostWeek = card("2026-06-03T10:00")
        assertEquals(R.string.weekly_mode_calendar_tone_none_pentecost_week, pentecostWeek.headlineRes)
        assertNull(pentecostWeek.headlineToneRes)
        assertNull(pentecostWeek.onOpenEightModes)

        // The Anastasimatarion stays reachable in every kind of week.
        holyWeek.onOpenAnastasimatarion()
        pentecostWeek.onOpenAnastasimatarion()
        assertEquals(2, anastasimatarionOpened)
    }

    @Test
    fun holySaturdayAfternoonAnnouncesPaschaAndOpensItsTone() {
        val card = card("2026-04-11T15:00")
        assertEquals(R.string.weekly_mode_calendar_tone_none_holy_week, card.headlineRes)
        assertEquals(R.string.mode_first, card.vespersToneRes)
        assertEquals("first", opensInEightModes(card))
    }

    @Test
    fun lazarusSaturdayEveningAnnouncesNothing() {
        val card = card("2026-04-04T18:00")
        assertEquals(R.string.mode_first, card.headlineToneRes)
        assertNull(card.vespersToneRes)
        assertEquals("first", opensInEightModes(card))
    }

    @Test
    fun everyToneOpensItsOwnModeInTheGenusOrderedEightModesList() {
        // Eight consecutive Sundays from the 2nd after Pentecost 2026: Α΄ … Πλ. Δ΄ in cycle order.
        val sundays = (0L..7L).map { LocalDateTime.parse("2026-06-14T10:00").plusWeeks(it) }
        val rows = sundays.map { sunday ->
            val today = announcement.at(sunday).today
            val key = checkNotNull(opensInEightModes(card(sunday.toString()))) { "$sunday has a tone to open" }
            val row = eightModesIndexOf(key)
            assertNotNull("$sunday: «$key» must be a row of the 8 Ήχοι", row)
            assertEquals("$sunday opens the tone the card names", today.toneNameRes, EIGHT_MODES[row!!].nameRes)
            row
        }
        assertEquals("all eight rows, once each", (0..7).toSet(), rows.toSet())
        // The trap is real: the 8 Ήχοι list is by genus, so a tone index is not its row.
        assertNotEquals("tone Β΄ is not row 1 there", 1, rows[1])
    }
}
