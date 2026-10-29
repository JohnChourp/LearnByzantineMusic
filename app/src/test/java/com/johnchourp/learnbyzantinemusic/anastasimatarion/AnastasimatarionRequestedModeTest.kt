package com.johnchourp.learnbyzantinemusic.anastasimatarion

import com.johnchourp.learnbyzantinemusic.anastasimatarion.AnastasimatarionWeekMode.requestedModeKey
import com.johnchourp.learnbyzantinemusic.anastasimatarion.AnastasimatarionWeekMode.weekModeKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * The calendar opens the Anastasimatarion on the selected week's mode (ClickUp `869f4tqdf`). The
 * page accepts only one of the eight mode keys; anything else keeps the current mode.
 */
class AnastasimatarionRequestedModeTest {

    @Test
    fun everyModeKeyIsAccepted() {
        AnastasimatarionLabels.MODE_ORDER.forEach { modeKey ->
            assertEquals(modeKey, requestedModeKey(modeKey))
        }
    }

    @Test
    fun noKeyOrAnUnknownKeyKeepsTheCurrentMode() {
        assertNull(requestedModeKey(null))
        assertNull(requestedModeKey(""))
        assertNull(requestedModeKey("ninth"))
        assertNull("keys are case-sensitive", requestedModeKey("Varys"))
    }

    @Test
    fun theCalendarsModeForAnyDateIsAcceptedOrAbsent() {
        // Every date of 2026: a week with a tone yields a key the page accepts; a week without one
        // (Holy Week, the week of Pentecost) yields none, and the calendar shows no button.
        var day = LocalDate.of(2026, 1, 1)
        while (day.year == 2026) {
            val modeKey = weekModeKey(day)
            if (modeKey != null) assertEquals("$day", modeKey, requestedModeKey(modeKey))
            day = day.plusDays(1)
        }
        assertNull("Holy Wednesday", weekModeKey(LocalDate.of(2026, 4, 8)))
        assertEquals("the week of 27 September", "plagal_fourth", weekModeKey(LocalDate.of(2026, 9, 30)))
    }
}
