package com.johnchourp.learnbyzantinemusic.calendar

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class OrthodoxPaschaCalculatorTest {
    @Test
    fun computesKnownPaschaDates() {
        assertEquals(LocalDate.of(2024, 5, 5), OrthodoxPaschaCalculator.computePaschaDate(2024))
        assertEquals(LocalDate.of(2025, 4, 20), OrthodoxPaschaCalculator.computePaschaDate(2025))
        assertEquals(LocalDate.of(2026, 4, 12), OrthodoxPaschaCalculator.computePaschaDate(2026))
        assertEquals(LocalDate.of(2027, 5, 2), OrthodoxPaschaCalculator.computePaschaDate(2027))
        assertEquals(LocalDate.of(2028, 4, 16), OrthodoxPaschaCalculator.computePaschaDate(2028))
    }
}
