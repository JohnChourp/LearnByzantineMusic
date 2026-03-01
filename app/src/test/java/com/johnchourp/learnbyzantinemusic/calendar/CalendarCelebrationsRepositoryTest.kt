package com.johnchourp.learnbyzantinemusic.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CalendarCelebrationsRepositoryTest {
    @Test
    fun parsesDatasetAndSortsByPriority() {
        val rawJson = """
            {
              "version": "1",
              "country_scope": "GR",
              "language": "el",
              "days": {
                "2025-01-01": [
                  {
                    "title": "Β",
                    "type": "religious_observance",
                    "is_official_non_working": false,
                    "is_half_day": false,
                    "description": "Δεύτερη εγγραφή",
                    "priority": 20
                  },
                  {
                    "title": "Α",
                    "type": "public_holiday",
                    "is_official_non_working": true,
                    "is_half_day": false,
                    "description": "Πρώτη εγγραφή",
                    "priority": 10
                  }
                ]
              }
            }
        """.trimIndent()

        val parsed = CalendarCelebrationsRepository.parseCelebrationsJson(rawJson)
        val celebrations = parsed[LocalDate.of(2025, 1, 1)].orEmpty()

        assertEquals(2, celebrations.size)
        assertEquals("Α", celebrations[0].title)
        assertEquals(CalendarCelebrationType.PUBLIC_HOLIDAY, celebrations[0].type)
        assertTrue(celebrations[0].isOfficialNonWorking)
    }

    @Test(expected = IllegalArgumentException::class)
    fun throwsWhenDateKeyIsInvalid() {
        val rawJson = """
            {
              "days": {
                "2025-13-01": [
                  {
                    "title": "Invalid",
                    "type": "normal_day",
                    "description": "Invalid date",
                    "priority": 100
                  }
                ]
              }
            }
        """.trimIndent()

        CalendarCelebrationsRepository.parseCelebrationsJson(rawJson)
    }

    @Test
    fun returnsFallbackWhenDateMissing() {
        val repository = CalendarCelebrationsRepository(
            mapOf(
                LocalDate.of(2025, 1, 1) to listOf(
                    CalendarCelebration(
                        title = "Πρωτοχρονιά",
                        type = CalendarCelebrationType.PUBLIC_HOLIDAY,
                        isOfficialNonWorking = true,
                        isHalfDay = false,
                        description = "Επίσημη αργία",
                        priority = 10
                    )
                )
            )
        )

        val fallback = repository.getCelebrations(LocalDate.of(2025, 1, 2))
        assertEquals(1, fallback.size)
        assertEquals(CalendarCelebrationType.NORMAL_DAY, fallback[0].type)
        assertEquals("Δεν υπάρχει καταχωρημένη εορτή", fallback[0].title)
    }

    @Test
    fun hasSpecialCelebrationReturnsTrueOnlyForNonNormalEntries() {
        val repository = CalendarCelebrationsRepository(
            mapOf(
                LocalDate.of(2025, 1, 1) to listOf(
                    CalendarCelebration(
                        title = "Θεοφάνεια",
                        type = CalendarCelebrationType.PUBLIC_HOLIDAY,
                        isOfficialNonWorking = true,
                        isHalfDay = false,
                        description = "Αργία",
                        priority = 10
                    )
                ),
                LocalDate.of(2025, 1, 2) to listOf(
                    CalendarCelebration(
                        title = "Δεν υπάρχει καταχωρημένη εορτή",
                        type = CalendarCelebrationType.NORMAL_DAY,
                        isOfficialNonWorking = false,
                        isHalfDay = false,
                        description = "Κανονική ημέρα",
                        priority = 1000
                    )
                )
            )
        )

        assertTrue(repository.hasSpecialCelebration(LocalDate.of(2025, 1, 1)))
        assertFalse(repository.hasSpecialCelebration(LocalDate.of(2025, 1, 2)))
        assertFalse(repository.hasSpecialCelebration(LocalDate.of(2025, 1, 3)))
    }

    @Test
    fun parsesReadingsAndSupportsLookupByDateAndId() {
        val rawJson = """
            {
              "version": "1",
              "country_scope": "GR",
              "language": "el",
              "days": {
                "2025-01-01": [
                  {
                    "title": "Πρωτοχρονιά",
                    "type": "public_holiday",
                    "is_official_non_working": true,
                    "is_half_day": false,
                    "description": "Επίσημη αργία",
                    "priority": 10
                  }
                ]
              },
              "readings": {
                "2025-01-01": {
                  "apostle": [
                    {
                      "id": "2025-01-01-apostle-1",
                      "reference": "Προς Εφεσίους 5:8-19",
                      "text_ancient": "Ancient apostle text",
                      "text_modern": "Modern apostle text"
                    }
                  ],
                  "gospel": [
                    {
                      "id": "2025-01-01-gospel-1",
                      "reference": "Κατά Ιωάννην 1:1-17",
                      "text_ancient": "Ancient gospel text",
                      "text_modern": "Modern gospel text"
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val parsed = CalendarCelebrationsRepository.parseDataset(rawJson)
        val repository = CalendarCelebrationsRepository(parsed.celebrationsByDate, parsed.readingsByDate)

        val dayReadings = repository.getDayReadings(LocalDate.of(2025, 1, 1))
        assertEquals(1, dayReadings.apostle.size)
        assertEquals(1, dayReadings.gospel.size)
        assertEquals("Προς Εφεσίους 5:8-19", dayReadings.apostle.first().reference)
        assertEquals("Κατά Ιωάννην 1:1-17", dayReadings.gospel.first().reference)

        val reading = repository.getReadingById(LocalDate.of(2025, 1, 1), "2025-01-01-gospel-1")
        assertEquals("Modern gospel text", reading?.textModern)
    }

    @Test
    fun readingsLookupReturnsEmptyForUnknownDateAndNullForUnknownId() {
        val repository = CalendarCelebrationsRepository(
            celebrationEntries = emptyMap(),
            readingEntries = mapOf(
                LocalDate.of(2025, 1, 1) to CalendarDayReadings(
                    apostle = listOf(
                        CalendarReadingText(
                            id = "a-1",
                            reference = "Προς Κολοσσαείς 3:4-11",
                            textAncient = "Ancient",
                            textModern = "Modern",
                        )
                    ),
                    gospel = emptyList(),
                )
            ),
        )

        val missingDateReadings = repository.getDayReadings(LocalDate.of(2025, 1, 2))
        assertTrue(missingDateReadings.apostle.isEmpty())
        assertTrue(missingDateReadings.gospel.isEmpty())
        assertNull(repository.getReadingById(LocalDate.of(2025, 1, 1), "missing-id"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun throwsWhenReadingsContainForbiddenSourceFields() {
        val rawJson = """
            {
              "version": "1",
              "country_scope": "GR",
              "language": "el",
              "days": {
                "2025-01-01": [
                  {
                    "title": "Κανονική ημέρα",
                    "type": "normal_day",
                    "is_official_non_working": false,
                    "is_half_day": false,
                    "description": "Test",
                    "priority": 1000
                  }
                ]
              },
              "readings": {
                "2025-01-01": {
                  "apostle": [
                    {
                      "id": "x-1",
                      "reference": "Προς Εφεσίους 5:8-19",
                      "text_ancient": "Ancient",
                      "text_modern": "Modern",
                      "source_url": "https://example.com/private"
                    }
                  ],
                  "gospel": []
                }
              }
            }
        """.trimIndent()

        CalendarCelebrationsRepository.parseDataset(rawJson)
    }
}
