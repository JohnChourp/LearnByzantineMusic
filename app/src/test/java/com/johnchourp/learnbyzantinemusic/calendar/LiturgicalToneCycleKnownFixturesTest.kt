package com.johnchourp.learnbyzantinemusic.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Known date → ήχος pairs for the weekly tone cycle (ClickUp `869f4tpqn`).
 *
 * ## Why this file used to prove nothing
 *
 * It held `emptyList()` behind an `assumeTrue`, so it reported success on every run while asserting
 * nothing at all. A test that passes when the fixtures are missing is worse than no test: it occupies
 * the slot where a real one would go.
 *
 * ## Where the expected values come from
 *
 * Not from the production code — that would only prove it agrees with itself. The anchor is the
 * **published Gregorian date of Orthodox Pascha**, a calendar fact independent of this app. Those
 * dates are listed in [PASCHA] and cross-checked against [OrthodoxPaschaCalculator] by
 * [paschaCalculatorMatchesThePublishedDates] below, for eight consecutive years including leap years.
 *
 * From each Pascha the cycle follows by the documented rule, written out here by hand rather than
 * called from production code:
 *
 *     Pentecost   = Pascha + 49 days
 *     cycle start = the Sunday of the week two weeks after Pentecost  → Α΄ Ήχος
 *     tone        = whole weeks since the cycle start, mod 8
 *
 * ## What the fixtures cover
 *
 * Three cycles (2024, 2025, 2026) × eight dates each, chosen where off-by-ones actually live: the
 * cycle start itself, a midweek day, the last week of the eight-tone round, the wrap back to Α΄, the
 * Saturday *before* a cycle start (which must fall back to the previous year's cycle), a Sunday
 * before Pentecost, and a 31 Dec / 1 Jan pair that shares a liturgical week across the year boundary.
 */
class LiturgicalToneCycleKnownFixturesTest {

    private val toneCycle = LiturgicalToneCycle()

    /** Published Gregorian dates of Orthodox Pascha. Independent of this app's arithmetic. */
    private val PASCHA = mapOf(
        2023 to LocalDate.of(2023, 4, 16),
        2024 to LocalDate.of(2024, 5, 5),
        2025 to LocalDate.of(2025, 4, 20),
        2026 to LocalDate.of(2026, 4, 12),
        2027 to LocalDate.of(2027, 5, 2),
        2028 to LocalDate.of(2028, 4, 16),
        2029 to LocalDate.of(2029, 4, 8),
        2030 to LocalDate.of(2030, 4, 28),
    )

    private data class KnownToneFixture(
        val date: LocalDate,
        val toneIndex: Int,
        val why: String,
    )

    private val knownFixtures: List<KnownToneFixture> = listOf(
        // ---- cycle beginning 2024-07-07
        KnownToneFixture(LocalDate.of(2024, 7, 7), 0, "cycle start → Α΄ Ήχος"),
        KnownToneFixture(LocalDate.of(2024, 7, 10), 0, "midweek keeps its Sunday's tone"),
        KnownToneFixture(LocalDate.of(2024, 8, 25), 7, "eighth week → Πλ. Δ΄, end of the round"),
        KnownToneFixture(LocalDate.of(2024, 9, 1), 0, "ninth week wraps back to Α΄"),
        KnownToneFixture(LocalDate.of(2024, 7, 6), 6, "Saturday before the start → previous cycle"),
        KnownToneFixture(LocalDate.of(2024, 6, 16), 4, "Sunday before Pentecost → previous cycle"),
        KnownToneFixture(LocalDate.of(2024, 12, 31), 1, "31 Dec"),
        KnownToneFixture(LocalDate.of(2025, 1, 1), 1, "1 Jan, same liturgical week as 31 Dec"),

        // ---- cycle beginning 2025-06-22
        KnownToneFixture(LocalDate.of(2025, 6, 22), 0, "cycle start → Α΄ Ήχος"),
        KnownToneFixture(LocalDate.of(2025, 6, 25), 0, "midweek keeps its Sunday's tone"),
        KnownToneFixture(LocalDate.of(2025, 8, 10), 7, "eighth week → Πλ. Δ΄, end of the round"),
        KnownToneFixture(LocalDate.of(2025, 8, 17), 0, "ninth week wraps back to Α΄"),
        KnownToneFixture(LocalDate.of(2025, 6, 21), 1, "Saturday before the start → previous cycle"),
        KnownToneFixture(LocalDate.of(2025, 6, 1), 7, "Sunday before Pentecost → previous cycle"),
        KnownToneFixture(LocalDate.of(2025, 12, 31), 3, "31 Dec"),
        KnownToneFixture(LocalDate.of(2026, 1, 1), 3, "1 Jan, same liturgical week as 31 Dec"),

        // ---- cycle beginning 2026-06-14
        KnownToneFixture(LocalDate.of(2026, 6, 14), 0, "cycle start → Α΄ Ήχος"),
        KnownToneFixture(LocalDate.of(2026, 6, 17), 0, "midweek keeps its Sunday's tone"),
        KnownToneFixture(LocalDate.of(2026, 8, 2), 7, "eighth week → Πλ. Δ΄, end of the round"),
        KnownToneFixture(LocalDate.of(2026, 8, 9), 0, "ninth week wraps back to Α΄"),
        KnownToneFixture(LocalDate.of(2026, 6, 13), 2, "Saturday before the start → previous cycle"),
        KnownToneFixture(LocalDate.of(2026, 5, 24), 0, "Sunday before Pentecost → previous cycle"),
        KnownToneFixture(LocalDate.of(2026, 12, 31), 4, "31 Dec"),
        KnownToneFixture(LocalDate.of(2027, 1, 1), 4, "1 Jan, same liturgical week as 31 Dec"),
    )

    @Test
    fun theFixtureTableIsPopulated() {
        // The failure this file used to have: an empty table that made everything else vacuous.
        assertTrue("fixtures must not be empty", knownFixtures.isNotEmpty())
        assertEquals("three cycles × eight dates", 24, knownFixtures.size)
        // Four calendar years appear, because each cycle's block ends on the following 1 January.
        assertEquals(
            "three cycles, spanning four calendar years",
            listOf(2024, 2025, 2026, 2027),
            knownFixtures.map { it.date.year }.distinct().sorted()
        )
    }

    @Test
    fun paschaCalculatorMatchesThePublishedDates() {
        PASCHA.forEach { (year, published) ->
            assertEquals("Pascha $year", published, OrthodoxPaschaCalculator.computePaschaDate(year))
        }
    }

    @Test
    fun matchesKnownDateFixtures() {
        knownFixtures.forEach { fixture ->
            assertEquals(
                "${fixture.date} (${fixture.why})",
                fixture.toneIndex,
                toneCycle.resolveTone(fixture.date).toneIndex
            )
        }
    }

    @Test
    fun theWholeWeekCarriesOneTone() {
        // Sunday through Saturday is one liturgical week; a date's tone must not change midweek.
        val sunday = LocalDate.of(2025, 6, 22)
        val expected = toneCycle.resolveTone(sunday).toneIndex
        (0..6).forEach { offset ->
            val day = sunday.plusDays(offset.toLong())
            assertEquals("$day belongs to the week of $sunday", expected, toneCycle.resolveTone(day).toneIndex)
        }
        assertEquals(
            "the next Sunday must move on",
            (expected + 1) % 8,
            toneCycle.resolveTone(sunday.plusWeeks(1)).toneIndex
        )
    }

    @Test
    fun everyToneIsReachedExactlyOncePerRound() {
        val start = LocalDate.of(2026, 6, 14)
        val round = (0..7).map { toneCycle.resolveTone(start.plusWeeks(it.toLong())).toneIndex }
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7), round)
    }
}
