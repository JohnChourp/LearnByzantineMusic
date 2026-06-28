package com.johnchourp.learnbyzantinemusic.calendar.ui

import androidx.compose.runtime.Immutable
import com.johnchourp.learnbyzantinemusic.calendar.CalendarCelebrationType

/**
 * Immutable snapshot of everything the redesigned «Ημερολόγιο Ήχου Εβδομάδας» screen renders.
 * The host [com.johnchourp.learnbyzantinemusic.calendar.WeeklyModeCalendarActivity] owns the real
 * state (selected date, visible month, repository, tone cycle) and rebuilds this object after every
 * interaction; the Compose [WeeklyModeCalendarScreen] is a pure function of it. Because it is a data
 * class the screen only recomposes when something actually changed (structural equality).
 *
 * All locale-dependent text (month label, full date, week range, month names) is pre-formatted by the
 * Activity, which holds the locale-wrapped context — the screen never touches `java.time` formatting.
 */
@Immutable
data class WeeklyModeCalendarUiState(
    /** Capitalised "month year" label, e.g. "Ιούνιος 2026". */
    val monthLabel: String = "",
    /** Accessibility description for the tappable month label (opens the picker). */
    val monthContentDescription: String = "",
    val visibleYear: Int = 2000,
    val visibleMonthValue: Int = 1,
    /** 12 localised, capitalised month names for the picker dialog (index 0 = January). */
    val monthNames: List<String> = emptyList(),
    val minYear: Int = 1900,
    val maxYear: Int = 2100,
    /** The day grid for the visible month (selection is tracked separately in [selectedDay]). */
    val monthGrid: MonthGridUi = MonthGridUi(),
    /** Day-of-month of the selected date within the visible month (always inside it). */
    val selectedDay: Int = 1,
    /** @StringRes id of the resolved weekly tone name. */
    val toneNameRes: Int = 0,
    /** Pre-formatted week range, e.g. "28 Ιουν 2026 – 4 Ιουλ 2026". */
    val weekRangeLabel: String = "",
    /** Pre-formatted full selected date, e.g. "Κυριακή, 28 Ιουνίου 2026". */
    val selectedDateLabel: String = "",
    val primaryCelebration: CelebrationUi = CelebrationUi(),
    val extraCelebrations: List<CelebrationUi> = emptyList(),
    val apostleReadings: List<ReadingRefUi> = emptyList(),
    val gospelReadings: List<ReadingRefUi> = emptyList(),
    val readingsEmpty: Boolean = false,
)

/** The visible month's grid, used as the [AnimatedContent] target so month changes slide cleanly. */
@Immutable
data class MonthGridUi(
    /** year * 12 + (monthValue - 1) — drives the directional slide animation. */
    val monthKey: Int = 0,
    /** Rows of 7 cells; null = leading/trailing placeholder for alignment. */
    val weeks: List<List<DayCellUi?>> = emptyList(),
)

/** One day cell as the grid needs to draw it (selection is applied by the screen from [WeeklyModeCalendarUiState.selectedDay]). */
@Immutable
data class DayCellUi(
    val day: Int,
    val isToday: Boolean,
    /** Highest-priority special celebration type for the day, or null when the day has no marker. */
    val markerType: CalendarCelebrationType?,
    /** Accessibility description, e.g. "28 Ιουνίου 2026, Θρησκευτική εορτή". */
    val contentDescription: String,
)

/** A celebration / εορτολόγιο entry for the selected day. */
@Immutable
data class CelebrationUi(
    val type: CalendarCelebrationType = CalendarCelebrationType.NORMAL_DAY,
    val title: String = "",
    val description: String = "",
)

/** A tappable reading reference that launches the reading-text screen. */
@Immutable
data class ReadingRefUi(
    val id: String,
    val reference: String,
)
