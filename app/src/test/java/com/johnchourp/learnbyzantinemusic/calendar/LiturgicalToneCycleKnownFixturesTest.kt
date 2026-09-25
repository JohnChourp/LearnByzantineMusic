package com.johnchourp.learnbyzantinemusic.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Known date → ήχος pairs for the weekly tone cycle (ClickUp `869f4tpqn`, corrected by `869f5x26r`).
 *
 * ## Why this file used to prove nothing
 *
 * It held `emptyList()` behind an `assumeTrue`, so it reported success on every run while asserting
 * nothing at all. A test that passes when the fixtures are missing is worse than no test: it occupies
 * the slot where a real one would go.
 *
 * ## Why it then pinned a bug, for six of its lines
 *
 * The Pascha dates in [PASCHA] are published calendar facts, cross-checked against
 * [OrthodoxPaschaCalculator] by [paschaCalculatorMatchesThePublishedDates]. The tones, though, were
 * worked out by hand from a rule this header used to state — "the 2nd Sunday after Pentecost is Α΄,
 * and every date before it continues the previous year's cycle". That was the production code's own
 * rule, so the file could only agree with the code, and the rule is wrong in the Pentecostarion: the
 * cycle restarts at Thomas Sunday every year. Six lines below (the Holy Fathers Sunday and the
 * All Saints week of each year) pinned the wrong tone until `869f5x26r`.
 *
 * ## Where the expected values come from now
 *
 * An independent source: the Tone column of published Sunday liturgical charts for 2024, 2025 and
 * 2026. Every tone below was checked against those charts; a weekday takes the tone of the Sunday
 * that begins its week. The six corrected lines say so. The Pentecostarion itself — Bright Week, the
 * weeks with no tone, the rule over two centuries — is covered by LiturgicalTonePentecostarionTest.
 *
 * ## What the fixtures cover
 *
 * Three years (2024, 2025, 2026) × eight dates each, chosen where off-by-ones actually live: the
 * 2nd Sunday after Pentecost (Α΄ again, eight weeks after Thomas Sunday), a midweek day, the last
 * week of the eight-tone round, the wrap back to Α΄, the Saturday before that Sunday (the All Saints
 * week), the Sunday before Pentecost, and a 31 Dec / 1 Jan pair that shares a liturgical week across
 * the year boundary.
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

    // The six lines marked "corrected" held 6 and 4 (2024), 1 and 7 (2025), 2 and 0 (2026): the
    // output of the old "continue last year's cycle" rule. They now carry what the published charts
    // print for those years: Πλ. Β΄ (index 5) on the Holy Fathers Sunday and Πλ. Δ΄ (index 7) for the
    // All Saints week, the same in all three years because the cycle restarts at every Pascha.
    private val knownFixtures: List<KnownToneFixture> = listOf(
        // ---- 2024, Pascha 5 May: Thomas Sunday 12 May is Α΄, the 2nd after Pentecost 7 Jul is Α΄ again
        KnownToneFixture(LocalDate.of(2024, 7, 7), 0, "2nd Sunday after Pentecost → Α΄ Ήχος"),
        KnownToneFixture(LocalDate.of(2024, 7, 10), 0, "midweek keeps its Sunday's tone"),
        KnownToneFixture(LocalDate.of(2024, 8, 25), 7, "eighth week → Πλ. Δ΄, end of the round"),
        KnownToneFixture(LocalDate.of(2024, 9, 1), 0, "ninth week wraps back to Α΄"),
        KnownToneFixture(LocalDate.of(2024, 7, 6), 7, "corrected: Saturday of the All Saints week → Πλ. Δ΄"),
        KnownToneFixture(LocalDate.of(2024, 6, 16), 5, "corrected: Holy Fathers, the Sunday before Pentecost → Πλ. Β΄"),
        KnownToneFixture(LocalDate.of(2024, 12, 31), 1, "31 Dec"),
        KnownToneFixture(LocalDate.of(2025, 1, 1), 1, "1 Jan, same liturgical week as 31 Dec"),

        // ---- 2025, Pascha 20 Apr: Thomas Sunday 27 Apr is Α΄, the 2nd after Pentecost 22 Jun is Α΄ again
        KnownToneFixture(LocalDate.of(2025, 6, 22), 0, "2nd Sunday after Pentecost → Α΄ Ήχος"),
        KnownToneFixture(LocalDate.of(2025, 6, 25), 0, "midweek keeps its Sunday's tone"),
        KnownToneFixture(LocalDate.of(2025, 8, 10), 7, "eighth week → Πλ. Δ΄, end of the round"),
        KnownToneFixture(LocalDate.of(2025, 8, 17), 0, "ninth week wraps back to Α΄"),
        KnownToneFixture(LocalDate.of(2025, 6, 21), 7, "corrected: Saturday of the All Saints week → Πλ. Δ΄"),
        KnownToneFixture(LocalDate.of(2025, 6, 1), 5, "corrected: Holy Fathers, the Sunday before Pentecost → Πλ. Β΄"),
        KnownToneFixture(LocalDate.of(2025, 12, 31), 3, "31 Dec"),
        KnownToneFixture(LocalDate.of(2026, 1, 1), 3, "1 Jan, same liturgical week as 31 Dec"),

        // ---- 2026, Pascha 12 Apr: Thomas Sunday 19 Apr is Α΄, the 2nd after Pentecost 14 Jun is Α΄ again
        KnownToneFixture(LocalDate.of(2026, 6, 14), 0, "2nd Sunday after Pentecost → Α΄ Ήχος"),
        KnownToneFixture(LocalDate.of(2026, 6, 17), 0, "midweek keeps its Sunday's tone"),
        KnownToneFixture(LocalDate.of(2026, 8, 2), 7, "eighth week → Πλ. Δ΄, end of the round"),
        KnownToneFixture(LocalDate.of(2026, 8, 9), 0, "ninth week wraps back to Α΄"),
        KnownToneFixture(LocalDate.of(2026, 6, 13), 7, "corrected: Saturday of the All Saints week → Πλ. Δ΄"),
        KnownToneFixture(LocalDate.of(2026, 5, 24), 5, "corrected: Holy Fathers, the Sunday before Pentecost → Πλ. Β΄"),
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
        val expected = checkNotNull(toneCycle.resolveTone(sunday).toneIndex) { "$sunday is an ordinary week" }
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
