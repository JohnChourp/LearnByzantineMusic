package com.johnchourp.learnbyzantinemusic.calendar

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeParseException

class CalendarCelebrationsRepository private constructor(
    private val context: Context?,
    private val injectedCelebrations: Map<LocalDate, List<CalendarCelebration>>?,
    private val injectedReadings: Map<LocalDate, CalendarDayReadings>?,
) {
    private val celebrationsByDate: Map<LocalDate, List<CalendarCelebration>> by lazy {
        injectedCelebrations ?: loadParsedDataset().celebrationsByDate
    }

    private val readingsByDate: Map<LocalDate, CalendarDayReadings> by lazy {
        injectedReadings ?: loadParsedDataset().readingsByDate
    }

    constructor(context: Context) : this(context, null, null)

    internal constructor(
        celebrationEntries: Map<LocalDate, List<CalendarCelebration>>,
        readingEntries: Map<LocalDate, CalendarDayReadings> = emptyMap(),
    ) : this(null, celebrationEntries, readingEntries)

    fun getCelebrations(date: LocalDate): List<CalendarCelebration> {
        val entries = celebrationsByDate[date].orEmpty()
        return if (entries.isNotEmpty()) {
            entries.sortedBy { it.priority }
        } else {
            listOf(defaultNormalDayCelebration())
        }
    }

    fun hasSpecialCelebration(date: LocalDate): Boolean {
        return celebrationsByDate[date].orEmpty().any { it.type != CalendarCelebrationType.NORMAL_DAY }
    }

    fun getDayReadings(date: LocalDate): CalendarDayReadings {
        return readingsByDate[date] ?: CalendarDayReadings(
            apostle = emptyList(),
            gospel = emptyList(),
        )
    }

    fun getReadingById(date: LocalDate, readingId: String): CalendarReadingText? {
        val dayReadings = getDayReadings(date)
        return (dayReadings.apostle + dayReadings.gospel).firstOrNull { it.id == readingId }
    }

    private fun loadParsedDataset(): ParsedDataset {
        val nonNullContext = context
            ?: throw IllegalStateException("Context is required when loading celebrations from assets.")
        val rawJson = nonNullContext.assets.open(DATASET_ASSET_FILE).bufferedReader().use { it.readText() }
        return parseDataset(rawJson)
    }

    companion object {
        private const val DATASET_ASSET_FILE = "calendar_celebrations_v1.json"
        private val FORBIDDEN_READING_KEYS = setOf("source", "source_url", "source_label", "url", "domain")

        fun parseDataset(rawJson: String): ParsedDataset {
            val rootObject = JSONObject(rawJson)
            val celebrations = parseCelebrations(rootObject)
            val readings = parseReadings(rootObject)
            return ParsedDataset(
                celebrationsByDate = celebrations,
                readingsByDate = readings,
            )
        }

        fun parseCelebrationsJson(rawJson: String): Map<LocalDate, List<CalendarCelebration>> {
            return parseDataset(rawJson).celebrationsByDate
        }

        private fun parseCelebrations(rootObject: JSONObject): Map<LocalDate, List<CalendarCelebration>> {
            val daysObject = rootObject.optJSONObject("days")
                ?: throw IllegalArgumentException("Missing required field: days")
            val result = mutableMapOf<LocalDate, List<CalendarCelebration>>()
            val dayKeys = daysObject.keys()
            while (dayKeys.hasNext()) {
                val dayKey = dayKeys.next()
                val date = parseDate(dayKey)
                val entriesArray = daysObject.optJSONArray(dayKey)
                    ?: throw IllegalArgumentException("Day $dayKey must contain a JSON array.")
                result[date] = parseCelebrationEntries(entriesArray)
            }
            return result.toSortedMap()
        }

        private fun parseReadings(rootObject: JSONObject): Map<LocalDate, CalendarDayReadings> {
            val readingsObject = rootObject.optJSONObject("readings") ?: return emptyMap()
            val result = mutableMapOf<LocalDate, CalendarDayReadings>()
            val dayKeys = readingsObject.keys()
            while (dayKeys.hasNext()) {
                val dayKey = dayKeys.next()
                val date = parseDate(dayKey)
                val dayObject = readingsObject.optJSONObject(dayKey)
                    ?: throw IllegalArgumentException("Readings for $dayKey must be an object.")
                validateForbiddenReadingFields(dayObject)
                val apostleEntries = parseReadingEntries(dayObject.optJSONArray("apostle"))
                val gospelEntries = parseReadingEntries(dayObject.optJSONArray("gospel"))
                result[date] = CalendarDayReadings(
                    apostle = apostleEntries,
                    gospel = gospelEntries,
                )
            }
            return result.toSortedMap()
        }

        private fun parseCelebrationEntries(entriesArray: JSONArray): List<CalendarCelebration> {
            val entries = mutableListOf<CalendarCelebration>()
            for (index in 0 until entriesArray.length()) {
                val entryObject = entriesArray.optJSONObject(index)
                    ?: throw IllegalArgumentException("Entry at index $index is not a JSON object.")
                val title = entryObject.optString("title", "").trim()
                if (title.isEmpty()) {
                    throw IllegalArgumentException("Entry at index $index has empty title.")
                }
                val type = CalendarCelebrationType.fromWireValue(
                    entryObject.optString("type", CalendarCelebrationType.NORMAL_DAY.wireValue)
                )
                entries.add(
                    CalendarCelebration(
                        title = title,
                        type = type,
                        isOfficialNonWorking = entryObject.optBoolean("is_official_non_working", false),
                        isHalfDay = entryObject.optBoolean("is_half_day", false),
                        description = entryObject.optString("description", "").trim(),
                        priority = entryObject.optInt("priority", 100),
                    )
                )
            }
            if (entries.isEmpty()) {
                throw IllegalArgumentException("Day entries list cannot be empty.")
            }
            return entries.sortedBy { it.priority }
        }

        private fun parseReadingEntries(entriesArray: JSONArray?): List<CalendarReadingText> {
            if (entriesArray == null) {
                return emptyList()
            }
            val result = mutableListOf<CalendarReadingText>()
            for (index in 0 until entriesArray.length()) {
                val entryObject = entriesArray.optJSONObject(index)
                    ?: throw IllegalArgumentException("Reading entry at index $index is not a JSON object.")
                validateForbiddenReadingFields(entryObject)
                val id = entryObject.optString("id", "").trim()
                val reference = entryObject.optString("reference", "").trim()
                val textAncient = entryObject.optString("text_ancient", "").trim()
                val textModern = entryObject.optString("text_modern", "").trim()

                if (id.isEmpty() || reference.isEmpty() || textAncient.isEmpty() || textModern.isEmpty()) {
                    throw IllegalArgumentException("Reading entry at index $index is missing required fields.")
                }

                result.add(
                    CalendarReadingText(
                        id = id,
                        reference = reference,
                        textAncient = textAncient,
                        textModern = textModern,
                    )
                )
            }
            return result
        }

        private fun validateForbiddenReadingFields(obj: JSONObject) {
            FORBIDDEN_READING_KEYS.forEach { key ->
                if (obj.has(key)) {
                    throw IllegalArgumentException("Forbidden field '$key' is not allowed in readings dataset.")
                }
            }
        }

        private fun parseDate(value: String): LocalDate {
            return try {
                LocalDate.parse(value)
            } catch (ex: DateTimeParseException) {
                throw IllegalArgumentException("Invalid date key in celebrations dataset: $value", ex)
            }
        }

        fun defaultNormalDayCelebration(): CalendarCelebration {
            return CalendarCelebration(
                title = "Δεν υπάρχει καταχωρημένη εορτή",
                type = CalendarCelebrationType.NORMAL_DAY,
                isOfficialNonWorking = false,
                isHalfDay = false,
                description = "Δεν βρέθηκε ειδική αργία ή βασική θρησκευτική εορτή για αυτή την ημέρα.",
                priority = 1000
            )
        }
    }
}

data class ParsedDataset(
    val celebrationsByDate: Map<LocalDate, List<CalendarCelebration>>,
    val readingsByDate: Map<LocalDate, CalendarDayReadings>,
)
