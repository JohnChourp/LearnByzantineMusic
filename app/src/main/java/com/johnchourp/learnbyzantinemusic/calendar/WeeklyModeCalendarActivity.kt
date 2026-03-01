package com.johnchourp.learnbyzantinemusic.calendar

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min

class WeeklyModeCalendarActivity : BaseActivity() {
    private lateinit var monthLabel: TextView
    private lateinit var prevMonthButton: Button
    private lateinit var nextMonthButton: Button
    private lateinit var todayButton: Button
    private lateinit var calendarDaysGrid: GridLayout
    private lateinit var toneText: TextView
    private lateinit var celebrationPrimaryText: TextView
    private lateinit var celebrationSecondaryText: TextView
    private lateinit var apostleRefsContainer: LinearLayout
    private lateinit var gospelRefsContainer: LinearLayout
    private lateinit var readingsEmptyText: TextView

    private lateinit var appLocale: Locale
    private lateinit var monthFormatter: DateTimeFormatter
    private lateinit var monthOnlyFormatter: DateTimeFormatter
    private val toneCycle = LiturgicalToneCycle()
    private lateinit var celebrationsRepository: CalendarCelebrationsRepository

    private var selectedDate: LocalDate = LocalDate.now()
    private var visibleMonth: YearMonth = YearMonth.from(selectedDate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_weekly_mode_calendar)

        monthLabel = findViewById(R.id.weekly_calendar_month_label)
        prevMonthButton = findViewById(R.id.weekly_calendar_prev_month_btn)
        nextMonthButton = findViewById(R.id.weekly_calendar_next_month_btn)
        todayButton = findViewById(R.id.weekly_calendar_today_btn)
        calendarDaysGrid = findViewById(R.id.weekly_calendar_days_grid)
        toneText = findViewById(R.id.weekly_calendar_tone_label)
        celebrationPrimaryText = findViewById(R.id.weekly_calendar_celebration_primary)
        celebrationSecondaryText = findViewById(R.id.weekly_calendar_celebration_secondary)
        apostleRefsContainer = findViewById(R.id.weekly_calendar_apostle_refs_container)
        gospelRefsContainer = findViewById(R.id.weekly_calendar_gospel_refs_container)
        readingsEmptyText = findViewById(R.id.weekly_calendar_readings_empty)

        appLocale = resolveCurrentLocale()
        monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", appLocale)
        monthOnlyFormatter = DateTimeFormatter.ofPattern("LLLL", appLocale)
        celebrationsRepository = CalendarCelebrationsRepository(this)

        monthLabel.setOnClickListener { showMonthYearPickerDialog() }
        prevMonthButton.setOnClickListener { moveMonth(-1) }
        nextMonthButton.setOnClickListener { moveMonth(1) }
        todayButton.setOnClickListener { jumpToToday() }

        renderScreen()
    }

    private fun moveMonth(deltaMonths: Long) {
        visibleMonth = visibleMonth.plusMonths(deltaMonths)
        val clampedDay = min(selectedDate.dayOfMonth, visibleMonth.lengthOfMonth())
        selectedDate = visibleMonth.atDay(clampedDay)
        renderScreen()
    }

    private fun jumpToToday() {
        selectedDate = LocalDate.now()
        visibleMonth = YearMonth.from(selectedDate)
        renderScreen()
    }

    private fun showMonthYearPickerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_month_year_picker, null)
        val monthPicker = dialogView.findViewById<NumberPicker>(R.id.weekly_calendar_picker_month)
        val yearPicker = dialogView.findViewById<NumberPicker>(R.id.weekly_calendar_picker_year)

        val monthNames = (1..12).map { month ->
            val monthName = YearMonth.of(2000, month).format(monthOnlyFormatter)
            capitalizeLabel(monthName)
        }.toTypedArray()

        monthPicker.minValue = 1
        monthPicker.maxValue = 12
        monthPicker.displayedValues = monthNames
        monthPicker.value = visibleMonth.monthValue
        monthPicker.wrapSelectorWheel = true
        monthPicker.contentDescription = getString(R.string.weekly_mode_calendar_month_picker_content_description)

        yearPicker.minValue = 1900
        yearPicker.maxValue = 2100
        yearPicker.value = visibleMonth.year.coerceIn(yearPicker.minValue, yearPicker.maxValue)
        yearPicker.wrapSelectorWheel = false
        yearPicker.contentDescription = getString(R.string.weekly_mode_calendar_year_picker_content_description)

        AlertDialog.Builder(this)
            .setTitle(R.string.weekly_mode_calendar_picker_title)
            .setView(dialogView)
            .setPositiveButton(R.string.weekly_mode_calendar_picker_confirm) { _, _ ->
                val selectedYear = yearPicker.value
                val selectedMonth = monthPicker.value
                visibleMonth = YearMonth.of(selectedYear, selectedMonth)
                val clampedDay = min(selectedDate.dayOfMonth, visibleMonth.lengthOfMonth())
                selectedDate = visibleMonth.atDay(clampedDay)
                renderScreen()
            }
            .setNegativeButton(R.string.weekly_mode_calendar_picker_cancel, null)
            .show()
    }

    private fun renderScreen() {
        renderCalendar()
        renderToneInfo()
        renderCelebrationInfo()
        renderReadingsInfo()
    }

    private fun renderCalendar() {
        val monthTitle = formatMonthLabel(visibleMonth)
        monthLabel.text = monthTitle
        monthLabel.contentDescription = getString(
            R.string.weekly_mode_calendar_open_picker_content_description,
            monthTitle
        )
        calendarDaysGrid.removeAllViews()

        val firstDayOfMonth = visibleMonth.atDay(1)
        val leadingEmptyCells = firstDayOfMonth.dayOfWeek.value % 7
        repeat(leadingEmptyCells) {
            calendarDaysGrid.addView(createPlaceholderCell())
        }

        for (day in 1..visibleMonth.lengthOfMonth()) {
            val date = visibleMonth.atDay(day)
            calendarDaysGrid.addView(createDayCell(date))
        }
    }

    private fun renderToneInfo() {
        val toneResult = toneCycle.resolveTone(selectedDate)
        toneText.text = getString(
            R.string.weekly_mode_calendar_tone_template,
            getString(toneResult.toneNameRes)
        )
    }

    private fun createPlaceholderCell(): TextView {
        return TextView(this).apply {
            text = ""
            visibility = TextView.INVISIBLE
            layoutParams = createCellLayoutParams()
            minHeight = dp(42)
        }
    }

    private fun createDayCell(date: LocalDate): View {
        val isSelected = date == selectedDate
        val isToday = date == LocalDate.now()
        val selectedBackground = ContextCompat.getDrawable(this, R.drawable.weekly_calendar_day_selected_bg)
        val todayBackground = ContextCompat.getDrawable(this, R.drawable.weekly_calendar_day_today_bg)
        val defaultBackground = ContextCompat.getDrawable(this, R.drawable.weekly_calendar_day_default_bg)
        val selectedTextColor = ContextCompat.getColor(this, android.R.color.white)
        val todayTextColor = ContextCompat.getColor(this, R.color.weekly_calendar_today_text)
        val defaultTextColor = ContextCompat.getColor(this, R.color.black)
        val contentDescriptionDateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", appLocale)
        val specialType = celebrationsRepository.getCelebrations(date)
            .firstOrNull { it.type != CalendarCelebrationType.NORMAL_DAY }
            ?.type

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            minimumHeight = dp(42)
            isClickable = true
            isFocusable = true
            layoutParams = createCellLayoutParams()
            setPadding(dp(4), dp(6), dp(4), dp(6))
            contentDescription = date.format(contentDescriptionDateFormatter)

            val dayNumberText = TextView(this@WeeklyModeCalendarActivity).apply {
                text = date.dayOfMonth.toString()
                gravity = Gravity.CENTER
                textSize = 14f
                if (isSelected) {
                    setTextColor(selectedTextColor)
                } else if (isToday) {
                    setTextColor(todayTextColor)
                } else {
                    setTextColor(defaultTextColor)
                }
            }

            val dotView = View(this@WeeklyModeCalendarActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(6), dp(6)).apply {
                    topMargin = dp(4)
                }
                if (specialType == null) {
                    visibility = View.INVISIBLE
                } else {
                    visibility = View.VISIBLE
                    setBackgroundResource(dotDrawableForType(specialType))
                }
            }

            if (isSelected) {
                background = selectedBackground
            } else if (isToday) {
                background = todayBackground
            } else {
                background = defaultBackground
            }

            addView(dayNumberText)
            addView(dotView)

            setOnClickListener {
                selectedDate = date
                visibleMonth = YearMonth.from(date)
                renderScreen()
            }
        }
    }

    private fun createCellLayoutParams(): GridLayout.LayoutParams {
        return GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED, 1f),
            GridLayout.spec(GridLayout.UNDEFINED, 1f)
        ).apply {
            width = 0
            setMargins(dp(3), dp(3), dp(3), dp(3))
        }
    }

    private fun formatMonthLabel(month: YearMonth): String {
        val raw = month.format(monthFormatter)
        return capitalizeLabel(raw)
    }

    private fun renderCelebrationInfo() {
        val celebrations = celebrationsRepository.getCelebrations(selectedDate).sortedBy { it.priority }
        val primary = celebrations.first()
        val primaryTypeLabel = getString(typeLabelRes(primary.type))
        celebrationPrimaryText.text = getString(
            R.string.weekly_mode_calendar_celebration_primary_template,
            "$primaryTypeLabel: ${primary.title}",
            primary.description
        )
        celebrationPrimaryText.setBackgroundResource(celebrationBackgroundRes(primary.type))

        val extraCelebrations = celebrations.drop(1)
        if (extraCelebrations.isEmpty()) {
            celebrationSecondaryText.visibility = View.GONE
            return
        }

        val extraRows = extraCelebrations.joinToString(separator = "\n\n") { extra ->
            val typeLabel = getString(typeLabelRes(extra.type))
            getString(
                R.string.weekly_mode_calendar_celebration_extra_row,
                "$typeLabel: ${extra.title}",
                extra.description
            )
        }
        celebrationSecondaryText.text = getString(
            R.string.weekly_mode_calendar_celebration_extra_title
        ) + "\n\n" + extraRows
        celebrationSecondaryText.visibility = View.VISIBLE
    }

    private fun renderReadingsInfo() {
        val dayReadings = celebrationsRepository.getDayReadings(selectedDate)
        apostleRefsContainer.removeAllViews()
        gospelRefsContainer.removeAllViews()

        renderReadingsList(
            container = apostleRefsContainer,
            readings = dayReadings.apostle,
            emptyLabelRes = R.string.weekly_mode_calendar_readings_none_for_kind
        )
        renderReadingsList(
            container = gospelRefsContainer,
            readings = dayReadings.gospel,
            emptyLabelRes = R.string.weekly_mode_calendar_readings_none_for_kind
        )

        readingsEmptyText.visibility = if (dayReadings.apostle.isEmpty() && dayReadings.gospel.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun renderReadingsList(
        container: LinearLayout,
        readings: List<CalendarReadingText>,
        emptyLabelRes: Int,
    ) {
        if (readings.isEmpty()) {
            val emptyTextView = TextView(this).apply {
                text = getString(emptyLabelRes)
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@WeeklyModeCalendarActivity, R.color.black))
                setPadding(0, dp(2), 0, dp(2))
            }
            container.addView(emptyTextView)
            return
        }

        readings.forEach { reading ->
            container.addView(createReadingReferenceView(reading))
        }
    }

    private fun createReadingReferenceView(reading: CalendarReadingText): TextView {
        return TextView(this).apply {
            text = "• ${reading.reference}"
            textSize = 14f
            setPadding(0, dp(4), 0, dp(4))
            setTextColor(ContextCompat.getColor(this@WeeklyModeCalendarActivity, R.color.mode_hard_chromatic_blue))
            paint.isUnderlineText = true
            isClickable = true
            isFocusable = true
            setOnClickListener { openReadingTextPage(reading.id) }
        }
    }

    private fun openReadingTextPage(readingId: String) {
        val intent = Intent(this, ReadingTextActivity::class.java).apply {
            putExtra(ReadingTextActivity.EXTRA_READING_ID, readingId)
            putExtra(ReadingTextActivity.EXTRA_DATE, selectedDate.toString())
        }
        startActivity(intent)
    }

    private fun dotDrawableForType(type: CalendarCelebrationType): Int {
        return when (type) {
            CalendarCelebrationType.PUBLIC_HOLIDAY -> R.drawable.weekly_calendar_dot_public
            CalendarCelebrationType.HALF_HOLIDAY -> R.drawable.weekly_calendar_dot_half
            CalendarCelebrationType.RELIGIOUS_OBSERVANCE -> R.drawable.weekly_calendar_dot_religious
            CalendarCelebrationType.NORMAL_DAY -> R.drawable.weekly_calendar_dot_religious
        }
    }

    private fun celebrationBackgroundRes(type: CalendarCelebrationType): Int {
        return when (type) {
            CalendarCelebrationType.PUBLIC_HOLIDAY -> R.drawable.weekly_calendar_celebration_public_bg
            CalendarCelebrationType.HALF_HOLIDAY -> R.drawable.weekly_calendar_celebration_half_bg
            CalendarCelebrationType.RELIGIOUS_OBSERVANCE -> R.drawable.weekly_calendar_celebration_religious_bg
            CalendarCelebrationType.NORMAL_DAY -> R.drawable.weekly_calendar_celebration_normal_bg
        }
    }

    private fun typeLabelRes(type: CalendarCelebrationType): Int {
        return when (type) {
            CalendarCelebrationType.PUBLIC_HOLIDAY -> R.string.weekly_mode_calendar_type_public_holiday
            CalendarCelebrationType.HALF_HOLIDAY -> R.string.weekly_mode_calendar_type_half_holiday
            CalendarCelebrationType.RELIGIOUS_OBSERVANCE -> R.string.weekly_mode_calendar_type_religious_observance
            CalendarCelebrationType.NORMAL_DAY -> R.string.weekly_mode_calendar_type_normal_day
        }
    }

    private fun capitalizeLabel(value: String): String {
        return value.replaceFirstChar { char ->
            if (char.isLowerCase()) {
                char.titlecase(appLocale)
            } else {
                char.toString()
            }
        }
    }

    private fun resolveCurrentLocale(): Locale {
        val locales = resources.configuration.locales
        return if (!locales.isEmpty) locales[0] else Locale.getDefault()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
