package com.johnchourp.learnbyzantinemusic.anastasimatarion

import com.johnchourp.learnbyzantinemusic.anastasimatarion.AnastasimatarionWeekMode.modeKeyOf
import com.johnchourp.learnbyzantinemusic.anastasimatarion.AnastasimatarionWeekMode.openingModeKey
import com.johnchourp.learnbyzantinemusic.anastasimatarion.AnastasimatarionWeekMode.weekModeKey
import com.johnchourp.learnbyzantinemusic.calendar.LiturgicalToneCycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun theCalendarsButtonOpensTheModeOfItsOwnHeadline() {
        // The calendar builds its headline and its button from the same resolved tone, so they agree
        // on every date; a week without a tone (Holy Week, the week of Pentecost) has no button.
        val cycle = LiturgicalToneCycle()
        var day = LocalDate.of(2026, 1, 1)
        while (day.year == 2026) {
            assertEquals("$day", weekModeKey(day), modeKeyOf(cycle.resolveTone(day)))
            day = day.plusDays(1)
        }
        assertNull("Holy Wednesday", modeKeyOf(cycle.resolveTone(LocalDate.of(2026, 4, 8))))
        assertNull("the Monday after Pentecost", modeKeyOf(cycle.resolveTone(LocalDate.of(2026, 6, 1))))
        assertEquals("the week of 27 September", "plagal_fourth", modeKeyOf(cycle.resolveTone(LocalDate.of(2026, 9, 30))))
    }
}
