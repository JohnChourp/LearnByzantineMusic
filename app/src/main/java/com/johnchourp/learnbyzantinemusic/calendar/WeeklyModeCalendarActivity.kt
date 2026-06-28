package com.johnchourp.learnbyzantinemusic.calendar

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.calendar.ui.CelebrationUi
import com.johnchourp.learnbyzantinemusic.calendar.ui.DayCellUi
import com.johnchourp.learnbyzantinemusic.calendar.ui.MonthGridUi
import com.johnchourp.learnbyzantinemusic.calendar.ui.ReadingRefUi
import com.johnchourp.learnbyzantinemusic.calendar.ui.WeeklyModeCalendarScreen
import com.johnchourp.learnbyzantinemusic.calendar.ui.WeeklyModeCalendarUiState
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min

/**
 * Host for the redesigned «Ημερολόγιο Ήχου Εβδομάδας» screen. Owns the real state (selected date,
 * visible month, repository, tone cycle) and rebuilds an immutable [WeeklyModeCalendarUiState] after
 * every interaction; the Compose [WeeklyModeCalendarScreen] is a pure renderer of it. All locale
 * formatting happens here, where the context is already wrapped with the user's language/font scale.
 */
class WeeklyModeCalendarActivity : BaseActivity() {

    private val toneCycle = LiturgicalToneCycle()
    private lateinit var celebrationsRepository: CalendarCelebrationsRepository

    private lateinit var appLocale: Locale
    private lateinit var monthYearFormatter: DateTimeFormatter
    private lateinit var monthOnlyFormatter: DateTimeFormatter
    private lateinit var fullDateFormatter: DateTimeFormatter
    private lateinit var weekRangeFormatter: DateTimeFormatter
    private lateinit var cellDateFormatter: DateTimeFormatter

    private var selectedDate: LocalDate = LocalDate.now()
    private var visibleMonth: YearMonth = YearMonth.from(selectedDate)

    private var uiState by mutableStateOf(WeeklyModeCalendarUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appLocale = resolveCurrentLocale()
        monthYearFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", appLocale)
        monthOnlyFormatter = DateTimeFormatter.ofPattern("LLLL", appLocale)
        fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", appLocale)
        weekRangeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", appLocale)
        cellDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", appLocale)
        celebrationsRepository = CalendarCelebrationsRepository(this)

        rebuildState()

        setContent {
            LbmTheme {
                WeeklyModeCalendarScreen(
                    state = uiState,
                    onBack = ::finish,
                    onPrevMonth = { moveMonth(-1) },
                    onNextMonth = { moveMonth(1) },
                    onToday = ::jumpToToday,
                    onSelectDay = ::selectDay,
                    onPickMonth = ::pickMonth,
                    onOpenReading = ::openReadingTextPage,
                )
            }
        }
    }

    private fun moveMonth(deltaMonths: Long) {
        visibleMonth = visibleMonth.plusMonths(deltaMonths)
        val clampedDay = min(selectedDate.dayOfMonth, visibleMonth.lengthOfMonth())
        selectedDate = visibleMonth.atDay(clampedDay)
        rebuildState()
    }

    private fun jumpToToday() {
        selectedDate = LocalDate.now()
        visibleMonth = YearMonth.from(selectedDate)
        rebuildState()
    }

    private fun selectDay(day: Int) {
        val clampedDay = day.coerceIn(1, visibleMonth.lengthOfMonth())
        selectedDate = visibleMonth.atDay(clampedDay)
        rebuildState()
    }

    private fun pickMonth(year: Int, month: Int) {
        visibleMonth = YearMonth.of(year, month)
        val clampedDay = min(selectedDate.dayOfMonth, visibleMonth.lengthOfMonth())
        selectedDate = visibleMonth.atDay(clampedDay)
        rebuildState()
    }

    private fun openReadingTextPage(readingId: String) {
        val intent = Intent(this, ReadingTextActivity::class.java).apply {
            putExtra(ReadingTextActivity.EXTRA_READING_ID, readingId)
            putExtra(ReadingTextActivity.EXTRA_DATE, selectedDate.toString())
        }
        startActivity(intent)
    }

    private fun rebuildState() {
        val monthLabel = capitalizeLabel(visibleMonth.format(monthYearFormatter))
        val tone = toneCycle.resolveTone(selectedDate)
        val celebrations = celebrationsRepository.getCelebrations(selectedDate).sortedBy { it.priority }
        val primary = celebrations.first()
        val extras = celebrations.drop(1)
        val readings = celebrationsRepository.getDayReadings(selectedDate)

        uiState = WeeklyModeCalendarUiState(
            monthLabel = monthLabel,
            monthContentDescription = getString(
                R.string.weekly_mode_calendar_open_picker_content_description,
                monthLabel,
            ),
            visibleYear = visibleMonth.year,
            visibleMonthValue = visibleMonth.monthValue,
            monthNames = (1..12).map { capitalizeLabel(YearMonth.of(2000, it).format(monthOnlyFormatter)) },
            minYear = MIN_PICKER_YEAR,
            maxYear = MAX_PICKER_YEAR,
            monthGrid = buildMonthGrid(),
            selectedDay = selectedDate.dayOfMonth,
            toneNameRes = tone.toneNameRes,
            weekRangeLabel = getString(
                R.string.weekly_mode_calendar_week_range_template,
                tone.weekStart.format(weekRangeFormatter),
                tone.weekEnd.format(weekRangeFormatter),
            ),
            selectedDateLabel = capitalizeLabel(selectedDate.format(fullDateFormatter)),
            primaryCelebration = primary.toUi(),
            extraCelebrations = extras.map { it.toUi() },
            apostleReadings = readings.apostle.map { ReadingRefUi(it.id, it.reference) },
            gospelReadings = readings.gospel.map { ReadingRefUi(it.id, it.reference) },
            readingsEmpty = readings.apostle.isEmpty() && readings.gospel.isEmpty(),
        )
    }

    private fun buildMonthGrid(): MonthGridUi {
        val today = LocalDate.now()
        val firstDayOfMonth = visibleMonth.atDay(1)
        val leadingEmptyCells = firstDayOfMonth.dayOfWeek.value % 7

        val cells = ArrayList<DayCellUi?>(leadingEmptyCells + visibleMonth.lengthOfMonth())
        repeat(leadingEmptyCells) { cells.add(null) }
        for (day in 1..visibleMonth.lengthOfMonth()) {
            val date = visibleMonth.atDay(day)
            val markerType = celebrationsRepository.getCelebrations(date)
                .firstOrNull { it.type != CalendarCelebrationType.NORMAL_DAY }
                ?.type
            val baseDescription = date.format(cellDateFormatter)
            val description = if (markerType != null) {
                "$baseDescription, ${getString(typeLabelRes(markerType))}"
            } else {
                baseDescription
            }
            cells.add(
                DayCellUi(
                    day = day,
                    isToday = date == today,
                    markerType = markerType,
                    contentDescription = description,
                ),
            )
        }
        // Pad the final week to a full row of 7 so the grid stays aligned.
        while (cells.size % 7 != 0) cells.add(null)

        return MonthGridUi(
            monthKey = visibleMonth.year * 12 + (visibleMonth.monthValue - 1),
            weeks = cells.chunked(7),
        )
    }

    private fun CalendarCelebration.toUi(): CelebrationUi =
        CelebrationUi(type = type, title = title, description = description)

    private fun typeLabelRes(type: CalendarCelebrationType): Int = when (type) {
        CalendarCelebrationType.PUBLIC_HOLIDAY -> R.string.weekly_mode_calendar_type_public_holiday
        CalendarCelebrationType.HALF_HOLIDAY -> R.string.weekly_mode_calendar_type_half_holiday
        CalendarCelebrationType.RELIGIOUS_OBSERVANCE -> R.string.weekly_mode_calendar_type_religious_observance
        CalendarCelebrationType.NORMAL_DAY -> R.string.weekly_mode_calendar_type_normal_day
    }

    private fun capitalizeLabel(value: String): String = value.replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase(appLocale) else char.toString()
    }

    private fun resolveCurrentLocale(): Locale {
        val locales = resources.configuration.locales
        return if (!locales.isEmpty) locales[0] else Locale.getDefault()
    }

    private companion object {
        const val MIN_PICKER_YEAR = 1900
        const val MAX_PICKER_YEAR = 2100
    }
}
