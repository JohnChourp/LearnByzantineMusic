package com.johnchourp.learnbyzantinemusic.calendar

import java.time.LocalDate

object OrthodoxPaschaCalculator {
    fun computePaschaDate(year: Int): LocalDate {
        require(year >= 1583) { "Year must be >= 1583" }

        // Meeus Julian algorithm for Orthodox Pascha, then convert to Gregorian.
        val a = year % 4
        val b = year % 7
        val c = year % 19
        val d = (19 * c + 15) % 30
        val e = (2 * a + 4 * b - d + 34) % 7

        val julianMonth = (d + e + 114) / 31
        val julianDay = ((d + e + 114) % 31) + 1
        val julianDate = LocalDate.of(year, julianMonth, julianDay)

        val julianToGregorianShiftDays = year / 100 - year / 400 - 2
        return julianDate.plusDays(julianToGregorianShiftDays.toLong())
    }
}
