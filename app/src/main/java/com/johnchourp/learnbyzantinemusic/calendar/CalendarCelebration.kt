package com.johnchourp.learnbyzantinemusic.calendar

enum class CalendarCelebrationType(val wireValue: String) {
    PUBLIC_HOLIDAY("public_holiday"),
    HALF_HOLIDAY("half_holiday"),
    RELIGIOUS_OBSERVANCE("religious_observance"),
    NORMAL_DAY("normal_day");

    companion object {
        fun fromWireValue(value: String): CalendarCelebrationType {
            return entries.firstOrNull { it.wireValue == value } ?: NORMAL_DAY
        }
    }
}

data class CalendarCelebration(
    val title: String,
    val type: CalendarCelebrationType,
    val isOfficialNonWorking: Boolean,
    val isHalfDay: Boolean,
    val description: String,
    val priority: Int
)
