package com.johnchourp.learnbyzantinemusic.calendar

data class CalendarReadingText(
    val id: String,
    val reference: String,
    val textAncient: String,
    val textModern: String,
)

data class CalendarDayReadings(
    val apostle: List<CalendarReadingText>,
    val gospel: List<CalendarReadingText>,
)
