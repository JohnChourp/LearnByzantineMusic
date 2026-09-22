package com.johnchourp.learnbyzantinemusic.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * ClickUp `869f4tpzk` (C4): one test file per asset, and a broken fixture must turn **this** test red.
 *
 * Two halves, and both are needed:
 * 1. the **real shipped asset** satisfies [CalendarDatasetSchema] — that is the thing users get;
 * 2. every rule in the schema actually rejects something — a validator nobody proved can fail is a
 *    comment with extra steps. Each fixture below is a mutation of the real asset, and each asserts
 *    the *specific* message, so a rule cannot pass by being caught accidentally by a different rule.
 */
class CalendarCelebrationsAssetSchemaTest {

    private val assetText: String by lazy {
        val candidates = listOf(
            File("app/src/main/assets/${CalendarDatasetSchema.ASSET_NAME}"),
            File("src/main/assets/${CalendarDatasetSchema.ASSET_NAME}"),
        )
        (candidates.firstOrNull { it.exists() }
            ?: error("${CalendarDatasetSchema.ASSET_NAME} not found from ${File("").absolutePath}"))
            .readText()
    }

    private fun problemsFor(transform: (String) -> String): List<String> =
        CalendarDatasetSchema.validate(transform(assetText))

    /** Asserts a mutation is rejected, and rejected *for the stated reason*. */
    private fun assertRejects(because: String, transform: (String) -> String) {
        val problems = problemsFor(transform)
        assertTrue(
            "expected a problem mentioning \"$because\", got: ${problems.take(3)}",
            problems.any { it.contains(because) },
        )
    }

    // ---- the real asset ---------------------------------------------------------------------

    @Test
    fun theAssetIsActuallyLoaded() {
        // Without this, an empty string would validate as "not a JSON object" and every mutation
        // below would "fail" for the wrong reason.
        assertTrue("asset looks too small: ${assetText.length} chars", assetText.length > 100_000)
        assertTrue(assetText.trimStart().startsWith("{"))
    }

    @Test
    fun theShippedAssetSatisfiesTheSchema() {
        assertEquals(emptyList<String>(), CalendarDatasetSchema.validate(assetText))
    }

    @Test
    fun theVersionFieldMatchesTheFileName() {
        assertEquals("1", CalendarDatasetSchema.VERSION_FROM_FILENAME)
        assertTrue(assetText.contains("\"version\": \"1\"") || assetText.contains("\"version\":\"1\""))
    }

    // ---- each rule must reject something -----------------------------------------------------

    @Test
    fun aVersionThatDisagreesWithTheFileNameIsRejected() {
        assertRejects("the file name says v1") { it.replace("\"version\": \"1\"", "\"version\": \"2\"", ) }
    }

    @Test
    fun aMissingHeaderFieldIsRejected() {
        assertRejects("missing required field: language") {
            it.replace("\"language\"", "\"language_removed\"")
        }
    }

    @Test
    fun anUnknownCelebrationTypeIsRejected() {
        // The runtime parser silently demotes this to normal_day. That is right at runtime and wrong
        // at build time: a typo would quietly turn a public holiday into an ordinary day.
        assertRejects("not one of") { it.replace("\"public_holiday\"", "\"public_holidayy\"", ) }
    }

    @Test
    fun aBlankTitleIsRejected() {
        assertRejects("title is blank") {
            it.replace("\"title\": \"Πρωτοχρονιά\"", "\"title\": \"   \"")
        }
    }

    @Test
    fun aWrongFieldTypeIsRejected() {
        assertRejects("should be boolean") {
            it.replace("\"is_half_day\": false", "\"is_half_day\": \"false\"")
        }
    }

    @Test
    fun aNonIsoDateKeyIsRejected() {
        assertRejects("is not a YYYY-MM-DD date") {
            it.replace("\"2025-01-01\": [", "\"01/01/2025\": [")
        }
    }

    @Test
    fun anEmptyDayListIsRejected() {
        val problems = problemsFor {
            Regex("""("2025-01-02":\s*)\[[^\]]*]""").replace(it, "$1[]")
        }
        assertTrue(
            "expected an empty-day complaint, got: ${problems.take(3)}",
            problems.any { it.contains("is empty") },
        )
    }

    @Test
    fun aProvenanceFieldIsRejectedEvenInsideDays() {
        // The runtime parser checks readings only; the policy is about the whole dataset.
        assertRejects("forbidden provenance field") {
            it.replace("\"title\": \"Πρωτοχρονιά\"", "\"source\": \"somewhere\", \"title\": \"Πρωτοχρονιά\"")
        }
    }

    @Test
    fun aUrlAnywhereInTheFileIsRejected() {
        assertRejects("contains a URL") {
            it.replace("\"country_scope\": \"GR\"", "\"country_scope\": \"https://example.org\"")
        }
    }

    @Test
    fun aDuplicateReadingIdIsRejected() {
        assertRejects("is not unique") {
            it.replace("\"2025-01-01-apostle-1\"", "\"2025-01-01-gospel-1\"")
        }
    }

    @Test
    fun aReadingIdThatDoesNotNameItsDateAndSectionIsRejected() {
        assertRejects("should start with") {
            it.replace("\"id\": \"2025-01-02-apostle-1\"", "\"id\": \"apostle-1-2025-01-02\"")
        }
    }

    @Test
    fun aBlankReadingFieldIsRejected() {
        assertRejects("is missing or blank: reference") {
            it.replace("\"reference\": \"ΠΡΟΣ ΚΟΛΟΣΣΑΕΙΣ Β´ 8 - 12\"", "\"reference\": \"\"")
        }
    }

    @Test
    fun readingsForADateWithNoDayEntryAreRejected() {
        assertRejects("has no matching entry in days") {
            // Move one readings date to a day the calendar does not list at all.
            it.replaceFirst("\"readings\": {\n", "\"readings\": {\n    \"1801-01-01\": {\"apostle\": [], \"gospel\": []},\n")
        }
    }

    @Test
    fun aFileThatIsNotJsonIsRejectedWithoutThrowing() {
        val problems = CalendarDatasetSchema.validate("not json at all")
        assertTrue(problems.any { it.contains("not a JSON object") })
    }
}
