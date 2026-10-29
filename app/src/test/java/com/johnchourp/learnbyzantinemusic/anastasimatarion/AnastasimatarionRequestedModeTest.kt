package com.johnchourp.learnbyzantinemusic.anastasimatarion

import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.anastasimatarion.AnastasimatarionWeekMode.CurrentMode
import com.johnchourp.learnbyzantinemusic.anastasimatarion.AnastasimatarionWeekMode.modeKeyOf
import com.johnchourp.learnbyzantinemusic.anastasimatarion.AnastasimatarionWeekMode.openingModeKey
import com.johnchourp.learnbyzantinemusic.anastasimatarion.AnastasimatarionWeekMode.showsOtherMode
import com.johnchourp.learnbyzantinemusic.calendar.LiturgicalToneCycle
import com.johnchourp.learnbyzantinemusic.modes.ModeResources
import com.johnchourp.learnbyzantinemusic.music.Mode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The calendar opens the Anastasimatarion on the selected week's mode (ClickUp `869f4tqdf`), and the
 * page keeps the mode it showed across a recreation — a rotation or a process the system killed.
 */
class AnastasimatarionRequestedModeTest {

    @Test
    fun theCalendarsRequestOpensThatMode() {
        AnastasimatarionLabels.MODE_ORDER.forEach { modeKey ->
            assertEquals(modeKey, openingModeKey(savedModeKey = null, requestedModeKey = modeKey))
        }
    }

    @Test
    fun aRecreationKeepsTheModeThatWasOnScreen() {
        // The user opened Πλ. Δ΄ from the calendar, then picked Α΄; after the process dies, Α΄ stays.
        assertEquals("first", openingModeKey(savedModeKey = "first", requestedModeKey = "plagal_fourth"))
        assertEquals("first", openingModeKey(savedModeKey = "first", requestedModeKey = null))
    }

    @Test
    fun anUnknownKeyIsIgnored() {
        assertEquals("varys", openingModeKey(savedModeKey = "ninth", requestedModeKey = "varys"))
        assertNull("no key: the page keeps the current mode", openingModeKey(null, null))
        assertNull(openingModeKey(savedModeKey = "", requestedModeKey = "Varys"))
    }

    @Test
    fun theCalendarsButtonOpensTheModeItsHeadlineNames() {
        // The headline shows tone.toneNameRes; the button opens modeKeyOf(tone). For every date of 2026
        // the button's mode must be the one the headline names, and a week without a tone (Holy
        // Week, the week of Pentecost) has neither.
        val cycle = LiturgicalToneCycle()
        var day = LocalDate.of(2026, 1, 1)
        while (day.year == 2026) {
            val tone = cycle.resolveTone(day)
            val buttonNameRes = modeKeyOf(tone)?.let { ModeResources.nameRes(Mode.fromKey(it)!!) }
            assertEquals("$day", tone.toneNameRes, buttonNameRes)
            day = day.plusDays(1)
        }
        assertNull("Holy Wednesday", modeKeyOf(cycle.resolveTone(LocalDate.of(2026, 4, 8))))
        assertNull("the Monday after Pentecost", modeKeyOf(cycle.resolveTone(LocalDate.of(2026, 6, 1))))
        assertEquals("the week of 27 September", "plagal_fourth", modeKeyOf(cycle.resolveTone(LocalDate.of(2026, 9, 30))))
    }

    @Test
    fun thePageNamesTheShownModeOnlyWhenTheBadgeNamesAnother() {
        val weekOfFirst = CurrentMode("first", R.string.anastasimatarion_week_mode_template)
        assertTrue("opened on Πλ. Δ΄ in a week of Α΄", showsOtherMode("plagal_fourth", weekOfFirst))
        assertFalse("the badge already names it", showsOtherMode("first", weekOfFirst))
        assertFalse("no badge in a week with no tone", showsOtherMode("plagal_fourth", null))
    }
}
