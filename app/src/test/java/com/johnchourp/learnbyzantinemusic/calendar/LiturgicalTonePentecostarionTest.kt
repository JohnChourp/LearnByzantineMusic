package com.johnchourp.learnbyzantinemusic.calendar

import com.johnchourp.learnbyzantinemusic.calendar.LiturgicalToneKind.BRIGHT_WEEK_DAY
import com.johnchourp.learnbyzantinemusic.calendar.LiturgicalToneKind.HOLY_WEEK
import com.johnchourp.learnbyzantinemusic.calendar.LiturgicalToneKind.PENTECOST_WEEK
import com.johnchourp.learnbyzantinemusic.calendar.LiturgicalToneKind.WEEKLY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/**
 * The tone from Palm Sunday to the 2nd Sunday after Pentecost (ClickUp `869f5x26r`).
 *
 * The cycle restarts at every Pascha. Until this fix the app continued the previous year's cycle up
 * to the 2nd Sunday after Pentecost, so the six Sundays from the Myrrhbearers to All Saints (and
 * their weeks) were wrong in every year, and the weeks that have no tone showed one anyway.
 *
 * ## Where the expected values come from
 *
 * Not from the production code. Every literal below is either the Tone column of published Sunday
 * liturgical charts for 2024, 2025 and 2026, or a Pentecostarion rubric for what those charts leave
 * blank. The three years have Pascha on 5 May, 20 Apr and 12 Apr, so the number of weeks since the
 * previous Pascha differs — exactly the case the old rule got wrong.
 *
 * - Palm Sunday and Holy Week: the Triodion, not the Octoechos — no tone.
 * - Bright Week: each day sings the Sunday hymns of another tone — Α΄ Β΄ Γ΄ Δ΄ Πλ.Α΄ Πλ.Β΄, and Πλ.Δ΄
 *   on Saturday, Βαρύς skipped.
 * - Thomas Sunday: back to Α΄, and the weekly cycle runs on from there.
 * - Pentecost and its week: the feast — no tone. The week still counts, so All Saints is Πλ. Δ΄.
 *
 * The charts are cited generically on purpose: this public repository names no calendar source.
 */
class LiturgicalTonePentecostarionTest {

    private val toneCycle = LiturgicalToneCycle()

    private data class ChartSunday(val date: LocalDate, val toneIndex: Int, val name: String)

    /** The Sundays this ticket is about, with the tone the charts print for them. */
    private val pentecostarionSundays = listOf(
        // ---- 2024, Pascha 5 May
        ChartSunday(LocalDate.of(2024, 4, 21), PLAGAL_FIRST, "5th of Lent, still the previous cycle"),
        ChartSunday(LocalDate.of(2024, 5, 19), SECOND, "Myrrhbearers"),
        ChartSunday(LocalDate.of(2024, 5, 26), THIRD, "Paralytic"),
        ChartSunday(LocalDate.of(2024, 6, 2), FOURTH, "Samaritan Woman"),
        ChartSunday(LocalDate.of(2024, 6, 9), PLAGAL_FIRST, "Blind Man"),
        ChartSunday(LocalDate.of(2024, 6, 16), PLAGAL_SECOND, "Holy Fathers"),
        ChartSunday(LocalDate.of(2024, 6, 30), PLAGAL_FOURTH, "All Saints"),
        ChartSunday(LocalDate.of(2024, 7, 7), FIRST, "2nd after Pentecost"),
        ChartSunday(LocalDate.of(2024, 7, 14), SECOND, "3rd after Pentecost"),

        // ---- 2025, Pascha 20 Apr
        ChartSunday(LocalDate.of(2025, 4, 6), PLAGAL_FOURTH, "5th of Lent, still the previous cycle"),
        ChartSunday(LocalDate.of(2025, 5, 4), SECOND, "Myrrhbearers"),
        ChartSunday(LocalDate.of(2025, 5, 11), THIRD, "Paralytic"),
        ChartSunday(LocalDate.of(2025, 5, 18), FOURTH, "Samaritan Woman"),
        ChartSunday(LocalDate.of(2025, 5, 25), PLAGAL_FIRST, "Blind Man"),
        ChartSunday(LocalDate.of(2025, 6, 1), PLAGAL_SECOND, "Holy Fathers"),
        ChartSunday(LocalDate.of(2025, 6, 15), PLAGAL_FOURTH, "All Saints"),
        ChartSunday(LocalDate.of(2025, 6, 22), FIRST, "2nd after Pentecost"),

        // ---- 2026, Pascha 12 Apr
        ChartSunday(LocalDate.of(2026, 3, 29), FIRST, "5th of Lent, still the previous cycle"),
        ChartSunday(LocalDate.of(2026, 4, 26), SECOND, "Myrrhbearers"),
        ChartSunday(LocalDate.of(2026, 5, 3), THIRD, "Paralytic"),
        ChartSunday(LocalDate.of(2026, 5, 10), FOURTH, "Samaritan Woman"),
        ChartSunday(LocalDate.of(2026, 5, 17), PLAGAL_FIRST, "Blind Man"),
        ChartSunday(LocalDate.of(2026, 5, 24), PLAGAL_SECOND, "Holy Fathers"),
        ChartSunday(LocalDate.of(2026, 6, 7), PLAGAL_FOURTH, "All Saints"),
        ChartSunday(LocalDate.of(2026, 6, 14), FIRST, "2nd after Pentecost"),
        ChartSunday(LocalDate.of(2026, 6, 21), SECOND, "3rd after Pentecost"),
    )

    /**
     * Every Sunday of the year, month by month, exactly as the charts print the tone: 1 … 8
     * (5 = Πλ. Α΄, 6 = Πλ. Β΄, 7 = Βαρύς, 8 = Πλ. Δ΄), and "-" where they print none.
     */
    private val publishedCharts = mapOf(
        2024 to listOf("6781", "2345", "67812", "345-", "--23", "456-8", "1234", "5678", "12345", "6781", "2345", "67812"),
        2025 to listOf("3456", "7812", "34567", "8---", "2345", "6-812", "3456", "78123", "4-67", "8123", "45678", "1234"),
        2026 to listOf("5678", "1234", "56781", "---2", "3456-", "8123", "4567", "81234", "5678", "1234", "56781", "2345"),
    )

    /** What the app shows on each Sunday the charts leave blank: every one a decision, not a gap. */
    private val chartBlanks: Map<LocalDate, Pair<LiturgicalToneKind, Int?>> = mapOf(
        LocalDate.of(2024, 4, 28) to (HOLY_WEEK to null),
        LocalDate.of(2024, 5, 5) to (BRIGHT_WEEK_DAY to FIRST),
        LocalDate.of(2024, 5, 12) to (WEEKLY to FIRST),
        LocalDate.of(2024, 6, 23) to (PENTECOST_WEEK to null),
        LocalDate.of(2025, 4, 13) to (HOLY_WEEK to null),
        LocalDate.of(2025, 4, 20) to (BRIGHT_WEEK_DAY to FIRST),
        LocalDate.of(2025, 4, 27) to (WEEKLY to FIRST),
        LocalDate.of(2025, 6, 8) to (PENTECOST_WEEK to null),
        // The Elevation of the Cross fell on a Sunday: the feast replaces that Sunday's resurrection
        // hymns, but the week keeps its place in the cycle — the chart's next Sunday is Πλ. Β΄.
        LocalDate.of(2025, 9, 14) to (WEEKLY to PLAGAL_FIRST),
        LocalDate.of(2026, 4, 5) to (HOLY_WEEK to null),
        LocalDate.of(2026, 4, 12) to (BRIGHT_WEEK_DAY to FIRST),
        LocalDate.of(2026, 4, 19) to (WEEKLY to FIRST),
        LocalDate.of(2026, 5, 31) to (PENTECOST_WEEK to null),
    )

    @Test
    fun thePentecostarionSundaysAndTheirWeeksMatchThePublishedCharts() {
        pentecostarionSundays.forEach { sunday ->
            (0L..6L).forEach { offset ->
                val day = sunday.date.plusDays(offset)
                val result = toneCycle.resolveTone(day)
                assertEquals("$day (week of ${sunday.name} ${sunday.date.year})", WEEKLY, result.kind)
                assertEquals("$day (week of ${sunday.name} ${sunday.date.year})", sunday.toneIndex, result.toneIndex)
            }
        }
    }

    @Test
    fun brightWeekHasATonePerDayAndSkipsVarys() {
        val perDay = listOf(FIRST, SECOND, THIRD, FOURTH, PLAGAL_FIRST, PLAGAL_SECOND, PLAGAL_FOURTH)
        listOf(LocalDate.of(2024, 5, 5), LocalDate.of(2025, 4, 20), LocalDate.of(2026, 4, 12)).forEach { pascha ->
            perDay.forEachIndexed { offset, tone ->
                val day = pascha.plusDays(offset.toLong())
                val result = toneCycle.resolveTone(day)
                assertEquals("$day kind", BRIGHT_WEEK_DAY, result.kind)
                assertEquals("$day tone of the day", tone, result.toneIndex)
                assertNotNull("$day must name its tone", result.toneNameRes)
                assertEquals("$day week", pascha, result.weekStart)
                assertEquals("$day week", pascha.plusDays(6), result.weekEnd)
            }
        }
    }

    @Test
    fun palmSundayAndHolyWeekHaveNoTone() {
        listOf(LocalDate.of(2024, 4, 28), LocalDate.of(2025, 4, 13), LocalDate.of(2026, 4, 5)).forEach { palmSunday ->
            (0L..6L).forEach { offset -> assertNoTone(palmSunday.plusDays(offset), HOLY_WEEK) }
        }
        // The day before Palm Sunday still belongs to the week of the 5th Sunday of Lent.
        assertEquals(PLAGAL_FIRST, toneCycle.resolveTone(LocalDate.of(2024, 4, 27)).toneIndex)
        assertEquals(PLAGAL_FOURTH, toneCycle.resolveTone(LocalDate.of(2025, 4, 12)).toneIndex)
        assertEquals(FIRST, toneCycle.resolveTone(LocalDate.of(2026, 4, 4)).toneIndex)
    }

    @Test
    fun thomasSundayAndItsWeekAreFirstTone() {
        listOf(LocalDate.of(2024, 5, 12), LocalDate.of(2025, 4, 27), LocalDate.of(2026, 4, 19)).forEach { thomas ->
            (0L..6L).forEach { offset ->
                val day = thomas.plusDays(offset)
                val result = toneCycle.resolveTone(day)
                assertEquals("$day kind", WEEKLY, result.kind)
                assertEquals("$day is in the week of Thomas Sunday", FIRST, result.toneIndex)
            }
        }
    }

    @Test
    fun pentecostAndItsWeekHaveNoTone() {
        listOf(LocalDate.of(2024, 6, 23), LocalDate.of(2025, 6, 8), LocalDate.of(2026, 5, 31)).forEach { pentecost ->
            (0L..6L).forEach { offset -> assertNoTone(pentecost.plusDays(offset), PENTECOST_WEEK) }
            // Both neighbours keep their tone: the Holy Fathers week before, All Saints after.
            assertEquals(PLAGAL_SECOND, toneCycle.resolveTone(pentecost.minusDays(1)).toneIndex)
            assertEquals(PLAGAL_FOURTH, toneCycle.resolveTone(pentecost.plusDays(7)).toneIndex)
        }
    }

    @Test
    fun everySundayOf2024To2026MatchesThePublishedCharts() {
        var checked = 0
        val blanksSeen = mutableSetOf<LocalDate>()
        publishedCharts.forEach { (year, months) ->
            assertEquals("$year: twelve months", 12, months.size)
            months.forEachIndexed { monthIndex, printedTones ->
                val sundays = sundaysOf(YearMonth.of(year, monthIndex + 1))
                assertEquals("Sundays of $year-${monthIndex + 1}", sundays.size, printedTones.length)
                sundays.zip(printedTones.toList()).forEach { (sunday, printed) ->
                    val result = toneCycle.resolveTone(sunday)
                    if (printed == '-') {
                        val (kind, tone) = chartBlanks[sunday]
                            ?: error("$sunday: the chart prints no tone and no decision is recorded")
                        assertEquals("$sunday kind", kind, result.kind)
                        assertEquals("$sunday tone", tone, result.toneIndex)
                        blanksSeen += sunday
                    } else {
                        assertEquals("$sunday kind", WEEKLY, result.kind)
                        assertEquals("$sunday (the chart prints $printed)", printed.digitToInt() - 1, result.toneIndex)
                    }
                    checked++
                }
            }
        }
        assertEquals("every Sunday of three years", 156, checked)
        assertEquals("every recorded decision is for a blank on the charts", chartBlanks.keys, blanksSeen)
    }

    @Test
    fun theRuleHoldsForEveryPaschaFrom1901To2100() {
        val fixedSundays = mapOf(
            7L to FIRST, // Thomas
            14L to SECOND, // Myrrhbearers
            21L to THIRD, // Paralytic
            28L to FOURTH, // Samaritan Woman
            35L to PLAGAL_FIRST, // Blind Man
            42L to PLAGAL_SECOND, // Holy Fathers
            56L to PLAGAL_FOURTH, // All Saints
            63L to FIRST, // 2nd after Pentecost
        )
        val brightWeek = listOf(FIRST, SECOND, THIRD, FOURTH, PLAGAL_FIRST, PLAGAL_SECOND, PLAGAL_FOURTH)
        for (year in 1901..2100) {
            val pascha = OrthodoxPaschaCalculator.computePaschaDate(year)
            (-7L..-1L).forEach { assertNoTone(pascha.plusDays(it), HOLY_WEEK) }
            brightWeek.forEachIndexed { offset, tone ->
                val result = toneCycle.resolveTone(pascha.plusDays(offset.toLong()))
                assertEquals("$year Bright Week day $offset", BRIGHT_WEEK_DAY, result.kind)
                assertEquals("$year Bright Week day $offset", tone, result.toneIndex)
            }
            fixedSundays.forEach { (offset, tone) ->
                val result = toneCycle.resolveTone(pascha.plusDays(offset))
                assertEquals("$year Pascha+$offset", WEEKLY, result.kind)
                assertEquals("$year Pascha+$offset", tone, result.toneIndex)
            }
            (49L..55L).forEach { assertNoTone(pascha.plusDays(it), PENTECOST_WEEK) }
        }
    }

    @Test
    fun everyWeekFrom1901To2100IsWholeAndTheCycleNeverSkips() {
        var sunday = LocalDate.of(1901, 1, 1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val last = LocalDate.of(2100, 12, 31)
        var weeks = 0
        while (!sunday.isAfter(last)) {
            val week = toneCycle.resolveTone(sunday)
            assertEquals("$sunday opens its own week", sunday, week.weekStart)
            assertEquals("$sunday week ends on Saturday", sunday.plusDays(6), week.weekEnd)
            // A week's days share one rule, and one tone except in Bright Week.
            (1L..6L).forEach { offset ->
                val day = toneCycle.resolveTone(sunday.plusDays(offset))
                assertEquals("${sunday.plusDays(offset)} kind", week.kind, day.kind)
                if (week.kind != BRIGHT_WEEK_DAY) {
                    assertEquals("${sunday.plusDays(offset)} tone", week.toneIndex, day.toneIndex)
                }
            }
            // No day is left without a label: a tone and its name, or a kind that has none.
            if (week.kind == HOLY_WEEK || week.kind == PENTECOST_WEEK) {
                assertNull("$sunday", week.toneIndex)
            } else {
                assertTrue("$sunday tone in range", week.toneIndex in 0..7)
                assertNotNull("$sunday tone name", week.toneNameRes)
            }
            // Between two ordinary weeks the cycle moves exactly one tone, across 31 Dec too.
            val next = toneCycle.resolveTone(sunday.plusWeeks(1))
            if (week.kind == WEEKLY && next.kind == WEEKLY) {
                assertEquals("${next.selectedDate} follows $sunday", (week.toneIndex!! + 1) % 8, next.toneIndex)
            }
            sunday = sunday.plusWeeks(1)
            weeks++
        }
        assertTrue("two centuries of weeks were walked, not $weeks", weeks > 10_000)
    }

    @Test
    fun exactlyTwoWeeksAYearHaveNoTone() {
        for (year in 1901..2100) {
            var day = LocalDate.of(year, 1, 1)
            var noTone = 0
            while (day.year == year) {
                if (toneCycle.resolveTone(day).toneIndex == null) noTone++
                day = day.plusDays(1)
            }
            assertEquals("$year: Holy Week and the week of Pentecost", 14, noTone)
        }
    }

    private fun assertNoTone(day: LocalDate, kind: LiturgicalToneKind) {
        val result = toneCycle.resolveTone(day)
        assertEquals("$day kind", kind, result.kind)
        assertNull("$day has no tone", result.toneIndex)
        assertNull("$day has no tone name", result.toneNameRes)
    }

    private fun sundaysOf(month: YearMonth): List<LocalDate> =
        generateSequence(month.atDay(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))) { it.plusWeeks(1) }
            .takeWhile { YearMonth.from(it) == month }
            .toList()

    private companion object {
        // Tone indices in the order of the cycle; the charts print them as 1 … 8.
        const val FIRST = 0
        const val SECOND = 1
        const val THIRD = 2
        const val FOURTH = 3
        const val PLAGAL_FIRST = 4
        const val PLAGAL_SECOND = 5
        const val PLAGAL_FOURTH = 7
    }
}
