package com.johnchourp.learnbyzantinemusic.anastasimatarion

import com.johnchourp.learnbyzantinemusic.anastasimatarion.AnastasimatarionWeekMode.weekModeKey
import com.johnchourp.learnbyzantinemusic.calendar.LiturgicalToneCycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * The mode the Anastasimatarion opens on (ClickUp `869f5x26r`): the same weeks the calendar got
 * wrong, it opened on the wrong mode — the Myrrhbearers' week of 2026 opened on Πλ. Α΄, not Β΄.
 *
 * Expected modes are the tones printed on published Sunday liturgical charts for 2026 (Pascha
 * 12 April) and the Pentecostarion rubrics for Bright Week, not the output of the code.
 */
class AnastasimatarionWeekModeTest {

    @Test
    fun aPentecostarionWeekOpensOnItsOwnTone() {
        assertEquals("Wednesday after the Myrrhbearers", "second", weekModeKey(LocalDate.of(2026, 4, 29)))
        assertEquals("Wednesday after the Holy Fathers", "plagal_second", weekModeKey(LocalDate.of(2026, 5, 27)))
        assertEquals("Wednesday after All Saints", "plagal_fourth", weekModeKey(LocalDate.of(2026, 6, 10)))
        assertEquals("the week of Thomas Sunday", "first", weekModeKey(LocalDate.of(2026, 4, 22)))
    }

    @Test
    fun brightWeekOpensOnTheToneOfTheDay() {
        val expected = listOf("first", "second", "third", "fourth", "plagal_first", "plagal_second", "plagal_fourth")
        expected.forEachIndexed { offset, modeKey ->
            val day = LocalDate.of(2026, 4, 12).plusDays(offset.toLong())
            assertEquals("$day", modeKey, weekModeKey(day))
        }
    }

    @Test
    fun holyWeekAndTheWeekOfPentecostHaveNoModeOfTheWeek() {
        // Null keeps the screen's fallback: the first mode, and no «mode of the week» badge.
        (0L..6L).forEach { offset ->
            assertNull("Holy Week", weekModeKey(LocalDate.of(2026, 4, 5).plusDays(offset)))
            assertNull("the week of Pentecost", weekModeKey(LocalDate.of(2026, 5, 31).plusDays(offset)))
        }
    }

    @Test
    fun anOrdinaryWeekOpensOnTheToneOfTheWeek() {
        // The chart prints Βαρύς for Sunday 20 September 2026.
        assertEquals("varys", weekModeKey(LocalDate.of(2026, 9, 25)))
    }

    @Test
    fun theModeOpenedIsTheToneTheCalendarNames() {
        // The index must go through MODE_ORDER. The 8 Ήχοι list is in genus order, where the same
        // index names another mode, so a key's name has to be the calendar's tone name, every day.
        val toneCycle = LiturgicalToneCycle()
        var day = LocalDate.of(2026, 1, 1)
        while (day.year == 2026) {
            val modeName = weekModeKey(day)?.let { AnastasimatarionLabels.MODE_NAMES.getValue(it) }
            assertEquals("$day", toneCycle.resolveTone(day).toneNameRes, modeName)
            day = day.plusDays(1)
        }
    }
}
