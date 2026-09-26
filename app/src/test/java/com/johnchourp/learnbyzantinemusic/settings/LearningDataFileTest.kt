package com.johnchourp.learnbyzantinemusic.settings

import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.practice.PracticeDay
import com.johnchourp.learnbyzantinemusic.practice.PracticeLog
import com.johnchourp.learnbyzantinemusic.practice.PracticeLogCodec
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs.Store.DEVICE
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs.Store.EIGHT_MODES
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs.Store.LECTERN_PAGES
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs.Store.NOTES
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs.Store.OWNED_RECORDINGS
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs.Store.PRACTICE
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs.Store.RECORDINGS
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs.Store.RECORDING_ANALYSIS
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs.Store.SETTINGS
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs.Store.TRAINER
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile.Item
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile.Line
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile.Reason
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile.Result.Accepted
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile.Result.Rejected
import com.johnchourp.learnbyzantinemusic.trainer.ExerciseBook
import com.johnchourp.learnbyzantinemusic.trainer.ExerciseChange
import com.johnchourp.learnbyzantinemusic.trainer.TrainerMelody
import com.johnchourp.learnbyzantinemusic.trainer.TrainerMelodyCodec
import com.johnchourp.learnbyzantinemusic.trainer.TrainerNote
import com.johnchourp.learnbyzantinemusic.trainer.TrainerScale
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The «Δεδομένα μάθησης» file (ClickUp `869f5x25w`): what travels, what the import accepts, and that
 * a refused file writes nothing while an accepted one writes only its own keys.
 */
class LearningDataFileTest {

    private val exportedAt = 1_758_800_000_000L

    /** A practice history as the app writes it (ClickUp `869f5x2dy`): two days practised. */
    private val practiceLog = PracticeLogCodec.encode(
        PracticeLog(mapOf(LocalDate.of(2026, 9, 24) to PracticeDay(1, 5), LocalDate.of(2026, 9, 25) to PracticeDay(2, 11)))
    )

    /** Written by the Trainer's own codecs, so each is already in the form the export writes. */
    private val trainerMelody = TrainerMelody(
        notes = listOf(TrainerNote(PhthongName.PA), TrainerNote(PhthongName.VOU, octaveShift = 1, baseDurationBeats = 1.5f)),
        bpm = 96,
        scale = TrainerScale(Mode.SECOND, -4),
    )
    private val trainerExercises = listOf("Άσκηση α", "Άσκηση β")
        .foldIndexed(ExerciseBook.EMPTY) { index, book, name ->
            (book.saveAs(name, trainerMelody, nowMillis = exportedAt + index) as ExerciseChange.Done).book
        }
        .encode()

    /** A valid value for every key that travels — every registry key and family is here at least once. */
    private val everythingThatTravels: Map<AppPrefs.Store, Map<String, Any>> = mapOf(
        SETTINGS to mapOf(
            "app_font_step" to 80,
            "app_language_code" to "en",
            "app_theme_mode" to "high_contrast",
            "metronome_bpm" to 96,
            "metronome_vibrate" to false,
            "metronome_silent" to true,
            "metronome_foot_mode" to true,
            // A topic this build may not have: the registry keeps unknown favourites on purpose.
            "favorite_topic_ids" to setOf("first_mode", "a_topic_of_a_newer_build"),
            "learning_completed_step_ids" to setOf("phthongs_names", "ascents", "eight_modes"),
            // The voice's global shift from «Βρες τη φωνή σου» (ClickUp `869f5x2dd`).
            "global_base_shift_moria" to -20,
        ),
        EIGHT_MODES to mapOf(
            "selected_mode_key" to "plagal_first",
            "selected_tone_timbre" to "CRYSTAL",
            "ison_in_background" to true,
            "mode_base_shift_moria_first" to -3,
            "mode_base_shift_moria_varys" to 12,
            "mode_base_shift_moria_plagal_fourth" to -12,
        ),
        RECORDINGS to mapOf("recordings_output_format" to "OPUS"),
        RECORDING_ANALYSIS to mapOf(
            "hymn:first:01|expected" to "NI,PA,VOU",
            "hymn:first:01|mode" to "first",
            "hymn:first:01|start" to "PA",
            "hymn:varys:42|expected" to "",
        ),
        PRACTICE to mapOf("practice_log" to practiceLog),
        TRAINER to mapOf(
            "trainer_exercises" to trainerExercises,
            "trainer_last_melody" to TrainerMelodyCodec.encodeString(trainerMelody),
        ),
        // Two PDFs' page → ήχος maps (ClickUp 869f5x2e7), named by SHA-256, in their canonical text.
        LECTERN_PAGES to mapOf(
            "lectern_pages_0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef" to
                """{"schemaVersion":1,"assignments":[{"pageIndex":0,"mode":"first","shiftMoria":0},""" +
                """{"pageIndex":4,"mode":"plagal_first","shiftMoria":-3,"ison":{"phthong":"PA","octave":0}}]}""",
            "lectern_pages_ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff" to
                """{"schemaVersion":1,"assignments":[{"pageIndex":2,"mode":"varys","shiftMoria":12}]}""",
        ),
    )

    /** What each device keeps for itself: URIs, bookkeeping, once-per-install flags, leftovers. */
    private val keptOnDevice: Map<AppPrefs.Store, Map<String, Any>> = mapOf(
        SETTINGS to mapOf(
            "app_language_onboarding_completed" to true,
            "a_key_no_build_registers" to "leftover",
        ),
        DEVICE to mapOf("notifications_permission_asked" to true),
        RECORDINGS to mapOf("recordings_folder_tree_uri" to "content://com.android.externalstorage.documents/tree/primary%3AMusic"),
        NOTES to mapOf(
            "notes_folder_tree_uri" to "content://com.android.externalstorage.documents/tree/primary%3ANotes",
            "notes_last_sync_epoch_ms" to 1_758_700_000_000L,
            "notes_last_sync_error" to "backup_file_write_failed",
        ),
        OWNED_RECORDINGS to mapOf("owned_recordings" to """["content://media/external/audio/1"]"""),
        PRACTICE to mapOf("practice_reminder_enabled" to true, "practice_reminder_minute_of_day" to 1140),
        // «Βρες τη φωνή σου» offered and the tour shown: once per phone (ClickUp `869f5x2dd`).
        EIGHT_MODES to mapOf("voice_range_offered" to true, "eight_modes_tour_shown" to true),
        RECORDING_ANALYSIS to mapOf(
            "recording:content://com.android.externalstorage.documents/document/primary%3AMusic%2F1.flac|expected" to "NI,PA",
            "recording:content://com.android.externalstorage.documents/document/primary%3AMusic%2F1.flac|mode" to "first",
            "recording:content://com.android.externalstorage.documents/document/primary%3AMusic%2F1.flac|start" to "NI",
        ),
    )

    private fun accepted(file: String): Accepted {
        val result = LearningDataFile.decode(file)
        assertTrue("expected the file to be accepted, got $result", result is Accepted)
        return result as Accepted
    }

    private fun entry(type: String, value: Any?) = JSONObject().put("type", type).put("value", value ?: JSONObject.NULL)

    /** A version 1 file with exactly these entries, each (preferences file, stored key, entry). */
    private fun fileWith(vararg entries: Triple<String, String, JSONObject>): String {
        val stores = JSONObject()
        entries.groupBy { it.first }.forEach { (file, inFile) ->
            stores.put(file, JSONObject().apply { inFile.forEach { (_, key, value) -> put(key, value) } })
        }
        return JSONObject()
            .put("schemaVersion", 1)
            .put("exportedAt", exportedAt)
            .put("appVersion", "1.17.0")
            .put("stores", stores)
            .toString()
    }

    // ---- what travels -----------------------------------------------------------------------------

    @Test
    fun `every key that travels has a sample here, and a line in the import dialog`() {
        val travelling = AppPrefs.all.filter { it.export != AppPrefs.Export.NO }.toSet()
        val sampled = everythingThatTravels.flatMap { (store, values) ->
            values.keys.mapNotNull { LearningDataFile.exportedKey(store, it) }
        }.toSet()
        assertEquals("a key that travels without a sample is never proven to round-trip", travelling, sampled)
        travelling.forEach { assertNotNull("${it.name} has no line in the import dialog", LearningDataFile.itemOf(it)) }
    }

    @Test
    fun `export then import gives back every key exactly`() {
        val result = accepted(LearningDataFile.encode(everythingThatTravels, exportedAt, "1.17.0"))

        assertEquals(everythingThatTravels, result.changes)
        assertEquals(exportedAt, result.exportedAtEpochMs)
        assertEquals("1.17.0", result.appVersion)
    }

    @Test
    fun `nothing a device keeps for itself is exported, and no URI at all`() {
        val snapshot = AppPrefs.Store.entries.associateWith { store ->
            everythingThatTravels[store].orEmpty() + keptOnDevice[store].orEmpty()
        }

        val file = LearningDataFile.encode(snapshot, exportedAt, "1.17.0")

        assertFalse("a URI reached the file", "content://" in file)
        assertEquals(everythingThatTravels, accepted(file).changes)
    }

    @Test
    fun `the export writes what the app itself uses, so it always imports back`() {
        val stored = mapOf(
            SETTINGS to mapOf(
                "app_font_step" to 55,
                "metronome_bpm" to 400,
                "learning_completed_step_ids" to setOf("phthongs_names", "a_step_the_path_no_longer_has"),
                "global_base_shift_moria" to BaseShift.MIN_MORIA - 9,
            ),
            // Past the shared range, which the file follows (±36 since ClickUp `869f5x2dd`).
            EIGHT_MODES to mapOf("mode_base_shift_moria_first" to BaseShift.MAX_MORIA + 14, "selected_tone_timbre" to "NO_SUCH_TIMBRE"),
            RECORDINGS to mapOf("recordings_output_format" to "opus"),
            RECORDING_ANALYSIS to mapOf("hymn:first:01|expected" to "NI,??,PA"),
        )

        val changes = accepted(LearningDataFile.encode(stored, exportedAt, "1.17.0")).changes

        assertEquals(
            mapOf(
                SETTINGS to mapOf(
                    "app_font_step" to 60,
                    "metronome_bpm" to 160,
                    "learning_completed_step_ids" to setOf("phthongs_names"),
                    "global_base_shift_moria" to BaseShift.MIN_MORIA,
                ),
                EIGHT_MODES to mapOf("mode_base_shift_moria_first" to BaseShift.MAX_MORIA),
                RECORDINGS to mapOf("recordings_output_format" to "OPUS"),
                RECORDING_ANALYSIS to mapOf("hymn:first:01|expected" to "NI,PA"),
            ),
            changes
        )
    }

    // ---- what the import refuses --------------------------------------------------------------------

    @Test
    fun `a file from a newer app is refused as newer, a file without a usable version as not one of ours`() {
        val file = JSONObject(LearningDataFile.encode(everythingThatTravels, exportedAt, "1.17.0"))

        assertEquals(Rejected(Reason.NEWER_VERSION), LearningDataFile.decode(JSONObject(file.toString()).put("schemaVersion", 2).toString()))
        listOf<Any?>(0, -1, "1", 1.5, null).forEach { version ->
            val broken = JSONObject(file.toString()).apply { if (version == null) remove("schemaVersion") else put("schemaVersion", version) }
            assertEquals("schemaVersion $version", Rejected(Reason.NOT_A_LEARNING_DATA_FILE), LearningDataFile.decode(broken.toString()))
        }
        listOf("exportedAt", "appVersion", "stores").forEach { field ->
            val broken = JSONObject(file.toString()).apply { remove(field) }
            assertEquals("without $field", Rejected(Reason.NOT_A_LEARNING_DATA_FILE), LearningDataFile.decode(broken.toString()))
        }
        listOf("", "{ not json", "[]", "{}").forEach { text ->
            assertEquals(text, Rejected(Reason.NOT_A_LEARNING_DATA_FILE), LearningDataFile.decode(text))
        }
    }

    @Test
    fun `a value the app would not store rejects the whole file`() {
        val settings = "learn_byzantine_music_settings"
        val eightModes = "eight_modes_base_shift_prefs"
        val analysis = "recording_analysis_settings"
        val badValues = listOf(
            Triple(settings, "app_font_step", entry("INT", 55)),
            Triple(settings, "app_font_step", entry("STRING", "80")),
            Triple(settings, "app_font_step", entry("INT", null)),
            Triple(settings, "app_language_code", entry("STRING", "fr")),
            Triple(settings, "app_theme_mode", entry("STRING", "sepia")),
            Triple(settings, "metronome_bpm", entry("INT", 200)),
            Triple(settings, "metronome_vibrate", entry("BOOLEAN", "yes")),
            Triple(settings, "learning_completed_step_ids", entry("STRING_SET", JSONArray(listOf("phthongs_names", "shop")))),
            Triple(settings, "favorite_topic_ids", entry("STRING_SET", JSONArray(listOf(" ")))),
            Triple(settings, "favorite_topic_ids", entry("STRING_SET", JSONArray(listOf("content://x")))),
            Triple(eightModes, "selected_mode_key", entry("STRING", "ninth")),
            Triple(eightModes, "selected_tone_timbre", entry("STRING", "LOUD")),
            Triple(eightModes, "mode_base_shift_moria_first", entry("INT", BaseShift.MAX_MORIA + 1)),
            Triple(settings, "global_base_shift_moria", entry("INT", BaseShift.MIN_MORIA - 1)),
            Triple("learn_byzantine_music_recordings", "recordings_output_format", entry("STRING", "ogg")),
            Triple(analysis, "hymn:first:01|expected", entry("STRING", "NI,XX")),
            Triple(analysis, "hymn:first:01|mode", entry("STRING", "ninth")),
            Triple(analysis, "hymn:first:01|start", entry("STRING", "XX")),
        )
        badValues.forEach { (store, key, value) ->
            // Next to a perfectly good entry, which must not be written either.
            val file = fileWith(Triple(store, key, value), Triple(settings, "metronome_foot_mode", entry("BOOLEAN", true)))
            assertEquals("$key = $value", Rejected(Reason.BAD_VALUE, key), LearningDataFile.decode(file))
        }
    }

    @Test
    fun `a key that stays on each device, or one this version does not know, rejects the whole file`() {
        val notImportable = listOf(
            Triple("learn_byzantine_music_notes", "notes_folder_tree_uri", entry("STRING", "content://x")),
            Triple("learn_byzantine_music_settings", "app_language_onboarding_completed", entry("BOOLEAN", true)),
            Triple("learn_byzantine_music_device", "notifications_permission_asked", entry("BOOLEAN", true)),
            Triple("learn_byzantine_music_settings", "a_setting_of_a_newer_build", entry("BOOLEAN", true)),
            Triple("recording_analysis_settings", "recording:content://x/1.flac|expected", entry("STRING", "NI")),
            Triple("recording_analysis_settings", "hymn:ninth:01|expected", entry("STRING", "NI")),
            Triple("eight_modes_base_shift_prefs", "mode_base_shift_moria_ninth", entry("INT", 1)),
            Triple("eight_modes_base_shift_prefs", "voice_range_offered", entry("BOOLEAN", true)),
            Triple("eight_modes_base_shift_prefs", "eight_modes_tour_shown", entry("BOOLEAN", true)),
        )
        notImportable.forEach { entry ->
            assertEquals(entry.second, Rejected(Reason.NOT_IMPORTABLE, entry.second), LearningDataFile.decode(fileWith(entry)))
        }
        assertEquals(
            Rejected(Reason.NOT_IMPORTABLE, "some_other_app_prefs"),
            LearningDataFile.decode(fileWith(Triple("some_other_app_prefs", "x", entry("INT", 1))))
        )
    }

    @Test
    fun `a refused file writes nothing`() {
        val commits = mutableListOf<AppPrefs.Store>()
        val writer = LearningDataFile.Writer { store, _ -> commits += store; true }
        val refused = listOf(
            fileWith(Triple("learn_byzantine_music_settings", "app_font_step", entry("INT", 55))),
            JSONObject(LearningDataFile.encode(everythingThatTravels, exportedAt, "1.17.0")).put("schemaVersion", 2).toString(),
        )

        refused.forEach { file ->
            val result = LearningDataFile.decode(file)
            // What the Settings screen does: only an accepted file ever reaches the writer.
            if (result is Accepted) LearningDataFile.write(result, writer)
        }

        assertEquals(emptyList<AppPrefs.Store>(), commits)
    }

    // ---- what the import writes -----------------------------------------------------------------------

    @Test
    fun `only the keys in the file are written, in one commit per preferences file`() {
        val file = fileWith(
            Triple("learn_byzantine_music_settings", "app_font_step", entry("INT", 80)),
            Triple("eight_modes_base_shift_prefs", "mode_base_shift_moria_first", entry("INT", 2)),
        )
        val commits = mutableListOf<Pair<AppPrefs.Store, Map<String, Any>>>()

        val written = LearningDataFile.write(accepted(file), LearningDataFile.Writer { store, values -> commits += store to values; true })

        assertTrue(written)
        assertEquals(2, commits.size)
        assertEquals(
            mapOf(SETTINGS to mapOf("app_font_step" to 80), EIGHT_MODES to mapOf("mode_base_shift_moria_first" to 2)),
            commits.toMap()
        )
    }

    @Test
    fun `a failed commit is reported, and every other file is still written`() {
        val attempted = mutableListOf<AppPrefs.Store>()
        val writer = LearningDataFile.Writer { store, _ -> attempted += store; store != SETTINGS }

        val written = LearningDataFile.write(accepted(LearningDataFile.encode(everythingThatTravels, exportedAt, "1.17.0")), writer)

        assertFalse(written)
        assertEquals(everythingThatTravels.keys, attempted.toSet())
    }

    @Test
    fun `a version 1 file keeps importing`() {
        val version1 = """
            {
              "schemaVersion": 1,
              "exportedAt": 1758800000000,
              "appVersion": "1.17.0",
              "stores": {
                "learn_byzantine_music_settings": {
                  "app_font_step": {"type": "INT", "value": 100},
                  "favorite_topic_ids": {"type": "STRING_SET", "value": ["first_mode"]},
                  "metronome_vibrate": {"type": "BOOLEAN", "value": false}
                },
                "eight_modes_base_shift_prefs": {
                  "mode_base_shift_moria_second": {"type": "INT", "value": -4}
                }
              }
            }
        """.trimIndent()

        assertEquals(
            mapOf(
                SETTINGS to mapOf("app_font_step" to 100, "favorite_topic_ids" to setOf("first_mode"), "metronome_vibrate" to false),
                EIGHT_MODES to mapOf("mode_base_shift_moria_second" to -4),
            ),
            accepted(version1).changes
        )
    }

    // ---- what the confirmation dialog lists -------------------------------------------------------------

    @Test
    fun `the dialog lists each kind of change once, with how many`() {
        val result = accepted(LearningDataFile.encode(everythingThatTravels, exportedAt, "1.17.0"))

        assertEquals(
            listOf(
                Line(Item.FONT_SIZE, 1),
                Line(Item.LANGUAGE, 1),
                Line(Item.THEME, 1),
                Line(Item.METRONOME, 4),
                Line(Item.FAVOURITES, 2),
                Line(Item.PROGRESS, 3),
                Line(Item.PRACTICE, 2),
                Line(Item.SELECTED_MODE, 1),
                Line(Item.TIMBRE, 1),
                Line(Item.ISON_BACKGROUND, 1),
                Line(Item.BASE_SHIFT, 3),
                Line(Item.GLOBAL_BASE_SHIFT, 1),
                Line(Item.RECORDING_FORMAT, 1),
                Line(Item.ANALYSIS, 2),
                Line(Item.TRAINER_EXERCISES, 2),
                Line(Item.TRAINER_LAST_MELODY, 1),
                Line(Item.LECTERN_PAGES, 2),
            ),
            LearningDataFile.summary(result)
        )
    }

    @Test
    fun `the suggested file name carries the app and the date`() {
        assertEquals(
            "LearnByzantineMusic-learning-data-2026-09-26.json",
            LearningDataFile.suggestedFileName(LocalDate.of(2026, 9, 26))
        )
    }
}
