package com.johnchourp.learnbyzantinemusic.calendar

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.GridLayout
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
    private lateinit var calendarDaysGrid: GridLayout
    private lateinit var selectedDateText: TextView
    private lateinit var weekRangeText: TextView
    private lateinit var toneText: TextView

    private val greekLocale = Locale("el", "GR")
    private val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", greekLocale)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", greekLocale)
    private val toneCycle = LiturgicalToneCycle()

    private var selectedDate: LocalDate = LocalDate.now()
    private var visibleMonth: YearMonth = YearMonth.from(selectedDate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_weekly_mode_calendar)

        monthLabel = findViewById(R.id.weekly_calendar_month_label)
        prevMonthButton = findViewById(R.id.weekly_calendar_prev_month_btn)
        nextMonthButton = findViewById(R.id.weekly_calendar_next_month_btn)
        calendarDaysGrid = findViewById(R.id.weekly_calendar_days_grid)
        selectedDateText = findViewById(R.id.weekly_calendar_selected_date)
        weekRangeText = findViewById(R.id.weekly_calendar_week_range)
        toneText = findViewById(R.id.weekly_calendar_tone_label)

        prevMonthButton.setOnClickListener { moveMonth(-1) }
        nextMonthButton.setOnClickListener { moveMonth(1) }

        renderCalendar()
        renderToneInfo()
    }

    private fun moveMonth(deltaMonths: Long) {
        visibleMonth = visibleMonth.plusMonths(deltaMonths)
        val clampedDay = min(selectedDate.dayOfMonth, visibleMonth.lengthOfMonth())
        selectedDate = visibleMonth.atDay(clampedDay)
        renderCalendar()
        renderToneInfo()
    }

    private fun renderCalendar() {
        monthLabel.text = formatMonthLabel(visibleMonth)
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
        selectedDateText.text = getString(
            R.string.weekly_mode_calendar_selected_date_template,
            selectedDate.format(dateFormatter)
        )
        weekRangeText.text = getString(
            R.string.weekly_mode_calendar_week_range_template,
            toneResult.weekStart.format(dateFormatter),
            toneResult.weekEnd.format(dateFormatter)
        )
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
            minHeight = dp(38)
        }
    }

    private fun createDayCell(date: LocalDate): TextView {
        val isSelected = date == selectedDate
        val selectedBackground = ContextCompat.getColor(this, R.color.mode_hard_chromatic_blue)
        val selectedTextColor = ContextCompat.getColor(this, android.R.color.white)
        val defaultTextColor = ContextCompat.getColor(this, R.color.black)

        return TextView(this).apply {
            text = date.dayOfMonth.toString()
            gravity = Gravity.CENTER
            minHeight = dp(38)
            isClickable = true
            isFocusable = true
            layoutParams = createCellLayoutParams()
            setPadding(dp(4), dp(8), dp(4), dp(8))

            if (isSelected) {
                setBackgroundColor(selectedBackground)
                setTextColor(selectedTextColor)
            } else {
                setBackgroundColor(Color.TRANSPARENT)
                setTextColor(defaultTextColor)
            }

            setOnClickListener {
                selectedDate = date
                visibleMonth = YearMonth.from(date)
                renderCalendar()
                renderToneInfo()
            }
        }
    }

    private fun createCellLayoutParams(): GridLayout.LayoutParams {
        return GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED, 1f),
            GridLayout.spec(GridLayout.UNDEFINED, 1f)
        ).apply {
            width = 0
            setMargins(dp(2), dp(2), dp(2), dp(2))
        }
    }

    private fun formatMonthLabel(month: YearMonth): String {
        val raw = month.format(monthFormatter)
        return raw.replaceFirstChar {
            if (it.isLowerCase()) {
                it.titlecase(greekLocale)
            } else {
                it.toString()
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
