package com.johnchourp.learnbyzantinemusic.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.YearMonth

/**
 * ClickUp `869f4tprf` (B4): the calendar must not let "no data for this month" look like "no feast
 * today".
 *
 * Coverage has to be derived from the dataset, never from a list of filled months written into the
 * code — such a list is wrong the first time someone fills a month and forgets to edit it. So these
 * tests do both: they pin the three states against hand-built datasets, and they assert the shape of
 * the **real** shipped asset, which is what the user actually sees.
 */
class CalendarMonthCoverageTest {

    private fun celebration(title: String) = CalendarCelebration(
        title = title,
        description = "",
        type = CalendarCelebrationType.RELIGIOUS_OBSERVANCE,
        isOfficialNonWorking = false,
        isHalfDay = false,
        priority = 0,
    )

    private fun repositoryCovering(vararg dates: LocalDate) =
        CalendarCelebrationsRepository(dates.associateWith { listOf(celebration("x")) })

    private fun fullMonth(yearMonth: YearMonth) =
        (1..yearMonth.lengthOfMonth()).map { yearMonth.atDay(it) }.toTypedArray()

    // ---- the three states -----------------------------------------------------------------------

    @Test
    fun aMonthWithNoEntriesAtAllIsNone() {
        val repo = repositoryCovering(LocalDate.of(2025, 1, 5))
        assertEquals(CalendarMonthCoverage.NONE, repo.getMonthCoverage(YearMonth.of(1975, 6)))
    }

    @Test
    fun aMonthWithOnlySeededFeastsIsPartial() {
        // Exactly the shape of the immovable-feast seeding outside the filled range.
        val repo = repositoryCovering(LocalDate.of(1975, 6, 24), LocalDate.of(1975, 6, 29))
        assertEquals(CalendarMonthCoverage.PARTIAL, repo.getMonthCoverage(YearMonth.of(1975, 6)))
    }

    @Test
    fun aMonthWithEveryDayPresentIsComplete() {
        val june = YearMonth.of(1975, 6)
        assertEquals(CalendarMonthCoverage.COMPLETE, repositoryCovering(*fullMonth(june)).getMonthCoverage(june))
    }

    @Test
    fun oneMissingDayIsEnoughToDropOutOfComplete() {
        val june = YearMonth.of(1975, 6)
        val allButOne = fullMonth(june).filterNot { it.dayOfMonth == 17 }.toTypedArray()
        assertEquals(CalendarMonthCoverage.PARTIAL, repositoryCovering(*allButOne).getMonthCoverage(june))
    }

    @Test
    fun februaryLengthIsTakenFromTheYearNotAssumed() {
        val leap = YearMonth.of(2024, 2)   // 29 days
        val plain = YearMonth.of(2025, 2)  // 28 days
        // 28 days of entries completes a normal February but not a leap one.
        val twentyEight = (1..28).map { leap.atDay(it) }.toTypedArray()
        assertEquals(CalendarMonthCoverage.PARTIAL, repositoryCovering(*twentyEight).getMonthCoverage(leap))
        assertEquals(
            CalendarMonthCoverage.COMPLETE,
            repositoryCovering(*fullMonth(plain)).getMonthCoverage(plain)
        )
    }

    // ---- what the panel is allowed to claim ------------------------------------------------------

    @Test
    fun anOrdinaryDayMayOnlyBeClaimedInAFullyCoveredMonth() {
        val june = YearMonth.of(1975, 6)
        val complete = repositoryCovering(*fullMonth(june))
        assertTrue(complete.isOrdinaryDayKnown(june.atDay(17)))

        val seededOnly = repositoryCovering(june.atDay(24), june.atDay(29))
        assertFalse(
            "a day missing from a partially seeded month is unknown, not ordinary",
            seededOnly.isOrdinaryDayKnown(june.atDay(17))
        )
    }

    // ---- the real shipped asset ------------------------------------------------------------------

    private fun shippedDataset(): CalendarCelebrationsRepository {
        val candidates = listOf(
            Paths.get("app/src/main/assets/calendar_celebrations_v1.json"),
            Paths.get("src/main/assets/calendar_celebrations_v1.json"),
        )
        val path: Path = candidates.firstOrNull { Files.exists(it) }
            ?: error("calendar_celebrations_v1.json not found from ${Paths.get("").toAbsolutePath()}")
        val parsed = CalendarCelebrationsRepository.parseDataset(File(path.toString()).readText())
        return CalendarCelebrationsRepository(parsed.celebrationsByDate, parsed.readingsByDate)
    }

    @Test
    fun theShippedDatasetReportsAFilledMonthAsComplete() {
        assertEquals(
            CalendarMonthCoverage.COMPLETE,
            shippedDataset().getMonthCoverage(YearMonth.of(2025, 1))
        )
    }

    @Test
    fun theShippedDatasetDoesNotClaimCoverageItDoesNotHave() {
        val repo = shippedDataset()
        // A year far outside the filled range still carries seeded immovable feasts: partial, and
        // therefore never reported to the user as ordinary days.
        val far = YearMonth.of(1950, 6)
        assertEquals(CalendarMonthCoverage.PARTIAL, repo.getMonthCoverage(far))
        assertFalse(repo.isOrdinaryDayKnown(far.atDay(17)))
    }

    @Test
    fun readingsCoverageIsTrackedSeparatelyFromCelebrations() {
        val repo = shippedDataset()
        val filled = YearMonth.of(2025, 1)
        assertEquals(CalendarMonthCoverage.COMPLETE, repo.getMonthCoverage(filled))
        assertEquals(CalendarMonthCoverage.COMPLETE, repo.getReadingsCoverage(filled))

        // Readings are filled month by month, independently — an unfilled one must say so.
        val unfilled = YearMonth.of(1950, 6)
        assertEquals(CalendarMonthCoverage.NONE, repo.getReadingsCoverage(unfilled))
    }
}
