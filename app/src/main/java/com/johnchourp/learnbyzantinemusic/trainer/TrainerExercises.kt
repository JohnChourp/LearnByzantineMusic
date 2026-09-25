package com.johnchourp.learnbyzantinemusic.trainer

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** One saved exercise: a melody under the name the user gave it. */
data class TrainerExercise(
    val name: String,
    val savedAtMillis: Long,
    val melody: TrainerMelody,
)

/**
 * «Οι ασκήσεις μου» — the saved exercises, the rules for changing them, and their stored form
 * (ClickUp `869f5x261`, F6). Pure Kotlin, so every rule is tested without a device.
 *
 * ## Rules
 *
 * - A name is trimmed, must not be blank, and is at most [MAX_NAME_LENGTH] characters.
 * - Names are unique, ignoring case: saving or renaming onto a taken name is refused, never an
 *   overwrite. (Renaming «αρχή» to «Αρχή» is fine: it is the same exercise.)
 * - At most [MAX_EXERCISES] are kept; saving one more is refused until one is deleted.
 * - The newest is listed first.
 *
 * Every change returns a new book in [ExerciseChange.Done], or the reason it was refused.
 *
 * ## Stored form
 *
 * ```
 * {"schemaVersion": 1, "exercises": [{"name": "…", "savedAt": 1727300000000, "melody": {…}}]}
 * ```
 *
 * `melody` is a `TrainerMelodyCodec` object, with its own `schemaVersion`. Two cases keep data that
 * cannot be read, instead of losing it:
 *
 * - **An entry that cannot be read** — a melody a newer app wrote, an unknown φθόγγος, a duplicate
 *   name — is left out of the list, and written back **unchanged** on the next save. A user who goes
 *   back to an older version and then saves does not lose what the newer one wrote.
 * - **A whole list in a newer format** (another `schemaVersion`) is shown as unreadable and is
 *   **never written**: [isNewerFormat], and every change is refused with [ExerciseChange.NewerFormat].
 *
 * A stored value that is not JSON at all holds nothing that can be kept, and is treated as empty.
 */
class ExerciseBook private constructor(
    /** The exercises that could be read, newest first. */
    val exercises: List<TrainerExercise>,
    /** Stored entries that could not be read, kept as they were. */
    private val unreadable: List<Any>,
    /** A newer app wrote the whole list: it is read-only here. */
    val isNewerFormat: Boolean,
) {
    /** How many entries the stored list holds, readable or not: what [MAX_EXERCISES] limits. */
    val storedCount: Int get() = exercises.size + unreadable.size

    /** The exercise named [name], ignoring case. */
    fun find(name: String): TrainerExercise? {
        val wanted = name.trim()
        return exercises.firstOrNull { it.name.equals(wanted, ignoreCase = true) }
    }

    fun saveAs(name: String, melody: TrainerMelody, nowMillis: Long): ExerciseChange {
        if (isNewerFormat) return ExerciseChange.NewerFormat
        val clean = name.trim()
        nameProblem(clean)?.let { return it }
        if (find(clean) != null) return ExerciseChange.NameTaken
        if (storedCount >= MAX_EXERCISES) return ExerciseChange.Full
        return ExerciseChange.Done(copyWith(exercises + TrainerExercise(clean, nowMillis, melody)))
    }

    fun rename(from: String, to: String): ExerciseChange {
        if (isNewerFormat) return ExerciseChange.NewerFormat
        val current = find(from) ?: return ExerciseChange.NotFound
        val clean = to.trim()
        nameProblem(clean)?.let { return it }
        val holder = find(clean)
        if (holder != null && holder !== current) return ExerciseChange.NameTaken
        return ExerciseChange.Done(copyWith(exercises.map { if (it === current) it.copy(name = clean) else it }))
    }

    fun delete(name: String): ExerciseChange {
        if (isNewerFormat) return ExerciseChange.NewerFormat
        val current = find(name) ?: return ExerciseChange.NotFound
        return ExerciseChange.Done(copyWith(exercises.filterNot { it === current }))
    }

    /** The stored form. Not for a list in a newer format, which must never be written back. */
    fun encode(): String {
        check(!isNewerFormat) { "a list written by a newer app is never overwritten" }
        val array = JSONArray()
        exercises.forEach { exercise ->
            array.put(
                JSONObject().apply {
                    put(NAME, exercise.name)
                    put(SAVED_AT, exercise.savedAtMillis)
                    put(MELODY, TrainerMelodyCodec.encode(exercise.melody))
                },
            )
        }
        unreadable.forEach { array.put(it) }
        return JSONObject().apply {
            put(SCHEMA_VERSION_FIELD, SCHEMA_VERSION)
            put(EXERCISES, array)
        }.toString()
    }

    private fun nameProblem(clean: String): ExerciseChange? = when {
        clean.isEmpty() -> ExerciseChange.NameBlank
        clean.length > MAX_NAME_LENGTH -> ExerciseChange.NameTooLong
        else -> null
    }

    private fun copyWith(exercises: List<TrainerExercise>): ExerciseBook =
        ExerciseBook(newestFirst(exercises), unreadable, isNewerFormat = false)

    companion object {
        const val SCHEMA_VERSION = 1

        /** The most exercises kept. With [MelodySequence.MAX_NOTES] it bounds the stored size. */
        const val MAX_EXERCISES = 50

        const val MAX_NAME_LENGTH = 40

        private const val SCHEMA_VERSION_FIELD = "schemaVersion"
        private const val EXERCISES = "exercises"
        private const val NAME = "name"
        private const val SAVED_AT = "savedAt"
        private const val MELODY = "melody"

        val EMPTY = ExerciseBook(emptyList(), emptyList(), isNewerFormat = false)

        fun decode(raw: String?): ExerciseBook = read(raw) ?: EMPTY

        /**
         * [raw] exactly as the Trainer would write it back, or null when it writes nothing from it: a
         * value that is not a list at all, or a list in a newer format. This is what the «Δεδομένα
         * μάθησης» file carries, and the only form its import accepts (ClickUp `869f5x25w`).
         */
        fun normalized(raw: String): String? = read(raw)?.takeUnless { it.isNewerFormat }?.encode()

        /** The book [raw] holds; null when it is not JSON at all, so there is nothing to keep. */
        private fun read(raw: String?): ExerciseBook? {
            if (raw.isNullOrBlank()) return null
            val root = try {
                JSONObject(raw)
            } catch (_: JSONException) {
                return null
            }
            if (root.opt(SCHEMA_VERSION_FIELD) != SCHEMA_VERSION) {
                return ExerciseBook(emptyList(), emptyList(), isNewerFormat = true)
            }
            val entries = root.optJSONArray(EXERCISES) ?: return EMPTY
            val readable = ArrayList<TrainerExercise>()
            val unreadable = ArrayList<Any>()
            for (index in 0 until entries.length()) {
                val entry = entries.opt(index) ?: continue
                val exercise = (entry as? JSONObject)?.let(::decodeExercise)
                val duplicate = exercise != null && readable.any { it.name.equals(exercise.name, ignoreCase = true) }
                if (exercise == null || duplicate) unreadable += entry else readable += exercise
            }
            return ExerciseBook(newestFirst(readable), unreadable, isNewerFormat = false)
        }

        private fun decodeExercise(json: JSONObject): TrainerExercise? {
            val name = (json.opt(NAME) as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val savedAt = (json.opt(SAVED_AT) as? Number)?.toLong() ?: return null
            val melody = TrainerMelodyCodec.decode(json.optJSONObject(MELODY)) ?: return null
            return TrainerExercise(name, savedAt, melody)
        }

        private fun newestFirst(exercises: List<TrainerExercise>): List<TrainerExercise> =
            exercises.sortedWith(compareByDescending<TrainerExercise> { it.savedAtMillis }.thenBy { it.name })
    }
}

/** What happened to a change asked of an [ExerciseBook]. */
sealed interface ExerciseChange {
    data class Done(val book: ExerciseBook) : ExerciseChange
    data object NameBlank : ExerciseChange
    data object NameTooLong : ExerciseChange
    data object NameTaken : ExerciseChange
    data object Full : ExerciseChange
    data object NotFound : ExerciseChange
    data object NewerFormat : ExerciseChange
}
