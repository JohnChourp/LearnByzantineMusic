package com.johnchourp.learnbyzantinemusic.calendar

import org.json.JSONArray
import org.json.JSONObject

/**
 * The written schema of `calendar_celebrations_v1.json`, and the validator that enforces it
 * (ClickUp `869f4tpzk`).
 *
 * ## Why this lives in test sources
 *
 * The acceptance criterion is *«ώστε ένα χαλασμένο dataset να πέφτει στο **build**, όχι στο χέρι του
 * χρήστη»*. A build-time contract belongs where the build checks it. Shipping it in `main` would put
 * a second, never-called copy of the rules into the APK — and the runtime parser
 * ([CalendarCelebrationsRepository]) deliberately stays lenient, because a user with a slightly odd
 * dataset should still get a working calendar rather than a crash. **That split is the design:**
 * strict here, forgiving there.
 *
 * ## Scope
 *
 * This was written for six assets. Five of them left with the scanner (ClickUp `869f4tppv`), so one
 * remains. The structure below is per-asset on purpose: a second asset gets its own object and its
 * own test file, not another branch inside this one.
 *
 * ## The versioning rule
 *
 * The `_vN` suffix in the file name is part of the **contract**, not a changelog:
 *
 * - **Adding an optional field, or adding rows** — no bump. Older code ignores what it does not read.
 * - **Adding a required field, removing or renaming a field, changing a field's type, or narrowing
 *   an allowed-value set** — **bump `_vN`** and ship the new file under the new name. Old and new
 *   must be able to coexist, because an installed app keeps reading the asset it was built with.
 * - **Changing what an existing value *means* without changing its shape** — bump. This is the one
 *   people skip, and it is the one that silently misleads: the parser keeps working and starts
 *   lying.
 * - The `version` field inside the file mirrors the suffix. If they disagree, one of them was edited
 *   by hand and neither can be trusted — [validate] therefore checks them against each other.
 *
 * ## The schema
 *
 * ```
 * {
 *   "version":       string,  required, must equal the _vN suffix of the file name
 *   "country_scope": string,  required, non-empty      e.g. "GR"
 *   "language":      string,  required, non-empty      e.g. "el"
 *   "days": {                 required
 *     "<YYYY-MM-DD>": [       required, at least one entry
 *       {
 *         "title":                   string,  required, non-blank
 *         "type":                    string,  required, one of TYPES
 *         "is_official_non_working": boolean, required
 *         "is_half_day":             boolean, required
 *         "description":             string,  required (may be empty)
 *         "priority":                int,     required
 *       }
 *     ]
 *   },
 *   "readings": {             optional
 *     "<YYYY-MM-DD>": {
 *       "apostle": [ ReadingEntry ],   optional, defaults to empty
 *       "gospel":  [ ReadingEntry ],   optional, defaults to empty
 *     }
 *   }
 * }
 *
 * ReadingEntry = {
 *   "id":           string, required, unique across the file, prefixed "<date>-<section>-"
 *   "reference":    string, required, non-blank
 *   "text_ancient": string, required, non-blank
 *   "text_modern":  string, required, non-blank
 * }
 * ```
 *
 * **No provenance anywhere.** [FORBIDDEN_KEYS] must not appear on any object, and no `http(s)://`
 * may appear anywhere in the file. The runtime parser enforces this for readings only; the schema
 * enforces it for the whole document, because the confidential policy is about the dataset, not
 * about one section of it.
 */
object CalendarDatasetSchema {

    const val ASSET_NAME = "calendar_celebrations_v1.json"

    /** The `_vN` suffix of [ASSET_NAME], which the file's own `version` field must match. */
    val VERSION_FROM_FILENAME: String =
        Regex("""_v(\d+)\.json$""").find(ASSET_NAME)?.groupValues?.get(1)
            ?: error("$ASSET_NAME does not carry a _vN suffix")

    val TYPES = setOf("public_holiday", "half_holiday", "religious_observance", "normal_day")

    /** Provenance fields. Not allowed on any object in the file. */
    val FORBIDDEN_KEYS = setOf("source", "source_url", "source_label", "url", "domain")

    private val ISO_DATE = Regex("""^\d{4}-\d{2}-\d{2}$""")
    private val URL = Regex("""https?://""")

    private val DAY_FIELDS = mapOf(
        "title" to FieldType.STRING,
        "type" to FieldType.STRING,
        "is_official_non_working" to FieldType.BOOLEAN,
        "is_half_day" to FieldType.BOOLEAN,
        "description" to FieldType.STRING,
        "priority" to FieldType.INT,
    )

    private val READING_FIELDS = listOf("id", "reference", "text_ancient", "text_modern")

    private enum class FieldType { STRING, BOOLEAN, INT }

    /**
     * Returns every problem found, rather than throwing on the first one: a broken dataset usually
     * has the same mistake repeated, and fixing them one build at a time is miserable.
     */
    fun validate(rawJson: String): List<String> {
        val problems = mutableListOf<String>()
        val root = runCatching { JSONObject(rawJson) }.getOrElse {
            return listOf("the file is not a JSON object: ${it.message}")
        }

        // ---- header ------------------------------------------------------------------------
        val version = root.optString("version", "")
        when {
            version.isBlank() -> problems += "missing required field: version"
            version != VERSION_FROM_FILENAME ->
                problems += "version is \"$version\" but the file name says v$VERSION_FROM_FILENAME " +
                    "— one of the two was hand-edited, so neither can be trusted"
        }
        listOf("country_scope", "language").forEach { field ->
            if (root.optString(field, "").isBlank()) problems += "missing required field: $field"
        }

        // ---- days --------------------------------------------------------------------------
        val days = root.optJSONObject("days")
        if (days == null) {
            problems += "missing required field: days"
        } else {
            days.keys().forEach { dateKey ->
                if (!ISO_DATE.matches(dateKey)) {
                    problems += "days: \"$dateKey\" is not a YYYY-MM-DD date"
                }
                val entries = days.optJSONArray(dateKey)
                if (entries == null) {
                    problems += "days[$dateKey] is not an array"
                } else if (entries.length() == 0) {
                    problems += "days[$dateKey] is empty — a listed day must carry at least one entry"
                } else {
                    validateDayEntries(dateKey, entries, problems)
                }
            }
        }

        // ---- readings ----------------------------------------------------------------------
        val readings = root.optJSONObject("readings")
        if (readings != null) {
            val seenIds = mutableSetOf<String>()
            readings.keys().forEach { dateKey ->
                if (!ISO_DATE.matches(dateKey)) {
                    problems += "readings: \"$dateKey\" is not a YYYY-MM-DD date"
                }
                if (days != null && !days.has(dateKey)) {
                    problems += "readings[$dateKey] has no matching entry in days"
                }
                val dayObject = readings.optJSONObject(dateKey)
                if (dayObject == null) {
                    problems += "readings[$dateKey] is not an object"
                    return@forEach
                }
                checkForbidden("readings[$dateKey]", dayObject, problems)
                listOf("apostle", "gospel").forEach { section ->
                    val array = dayObject.optJSONArray(section) ?: return@forEach
                    validateReadingEntries(dateKey, section, array, seenIds, problems)
                }
            }
        }

        // ---- provenance, document-wide -----------------------------------------------------
        if (URL.containsMatchIn(rawJson)) {
            problems += "the dataset contains a URL — no provenance may ship inside an app asset"
        }

        return problems
    }

    private fun validateDayEntries(dateKey: String, entries: JSONArray, problems: MutableList<String>) {
        for (index in 0 until entries.length()) {
            val where = "days[$dateKey][$index]"
            val entry = entries.optJSONObject(index)
            if (entry == null) {
                problems += "$where is not an object"
                continue
            }
            checkForbidden(where, entry, problems)
            DAY_FIELDS.forEach { (field, type) ->
                if (!entry.has(field)) {
                    problems += "$where is missing required field: $field"
                    return@forEach
                }
                val value = entry.get(field)
                val ok = when (type) {
                    FieldType.STRING -> value is String
                    FieldType.BOOLEAN -> value is Boolean
                    FieldType.INT -> value is Int
                }
                if (!ok) {
                    problems += "$where.$field should be ${type.name.lowercase()} but is " +
                        value::class.simpleName
                }
            }
            if (entry.optString("title", "").isBlank()) {
                problems += "$where.title is blank"
            }
            val type = entry.optString("type", "")
            if (type.isNotEmpty() && type !in TYPES) {
                // The runtime parser silently falls back to normal_day here. That is right at
                // runtime and wrong at build time: a typo would quietly demote a feast.
                problems += "$where.type is \"$type\", not one of $TYPES"
            }
        }
    }

    private fun validateReadingEntries(
        dateKey: String,
        section: String,
        array: JSONArray,
        seenIds: MutableSet<String>,
        problems: MutableList<String>,
    ) {
        for (index in 0 until array.length()) {
            val where = "readings[$dateKey].$section[$index]"
            val entry = array.optJSONObject(index)
            if (entry == null) {
                problems += "$where is not an object"
                continue
            }
            checkForbidden(where, entry, problems)
            READING_FIELDS.forEach { field ->
                if (entry.optString(field, "").isBlank()) {
                    problems += "$where is missing or blank: $field"
                }
            }
            val id = entry.optString("id", "")
            if (id.isNotBlank()) {
                if (!seenIds.add(id)) {
                    problems += "$where.id \"$id\" is not unique — ids address a single reading"
                }
                val expectedPrefix = "$dateKey-$section-"
                if (!id.startsWith(expectedPrefix)) {
                    problems += "$where.id \"$id\" should start with \"$expectedPrefix\""
                }
            }
        }
    }

    private fun checkForbidden(where: String, obj: JSONObject, problems: MutableList<String>) {
        FORBIDDEN_KEYS.forEach { key ->
            if (obj.has(key)) problems += "$where carries forbidden provenance field \"$key\""
        }
    }
}
