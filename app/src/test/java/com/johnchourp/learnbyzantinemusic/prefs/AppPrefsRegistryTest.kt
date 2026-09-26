package com.johnchourp.learnbyzantinemusic.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the registry of ClickUp `869f4tpwz`.
 *
 * The stored names are already on users' devices. Renaming one does not migrate anything — it makes
 * the old value unreachable and silently resets that setting on upgrade. So the names are asserted
 * literally here: a rename has to be a deliberate edit to this test, next to the migration it needs.
 */
class AppPrefsRegistryTest {

    @Test
    fun everyStoredKeyNameIsFrozen() {
        assertEquals(
            listOf(
                "<analysisContext>|expected",
                "<analysisContext>|mode",
                "<analysisContext>|start",
                "app_font_step",
                "app_language_code",
                "app_language_onboarding_completed",
                "app_theme_mode",
                "eight_modes_tour_shown",
                "favorite_topic_ids",
                "global_base_shift_moria",
                "ison_in_background",
                "learning_completed_step_ids",
                "lectern_library",
                "lectern_pages_<sha256>",
                "metronome_bpm",
                "metronome_foot_mode",
                "metronome_silent",
                "metronome_vibrate",
                "mode_base_shift_moria_<modeKey>",
                "notes_folder_tree_uri",
                "notes_last_sync_epoch_ms",
                "notes_last_sync_error",
                "notifications_permission_asked",
                "owned_recordings",
                "practice_log",
                "practice_reminder_enabled",
                "practice_reminder_minute_of_day",
                "recordings_folder_tree_uri",
                "recordings_output_format",
                "selected_mode_key",
                "selected_tone_timbre",
                "trainer_exercises",
                "trainer_last_melody",
                "voice_range_offered",
            ),
            AppPrefs.all.map { it.name }.sorted()
        )
    }

    @Test
    fun everyPreferencesFileNameIsFrozen() {
        assertEquals(
            listOf(
                "eight_modes_base_shift_prefs",
                "learn_byzantine_music_lectern_library",
                "learn_byzantine_music_lectern_pages",
                "learn_byzantine_music_notes",
                "learn_byzantine_music_owned_recordings",
                "learn_byzantine_music_practice",
                "learn_byzantine_music_recordings",
                "learn_byzantine_music_settings",
                "learn_byzantine_music_trainer",
                "recording_analysis_settings",
            ),
            AppPrefs.Store.entries.map { it.fileName }.sorted()
        )
    }

    @Test
    fun noTwoKeysCollideInsideTheSameStore() {
        AppPrefs.Store.entries.forEach { store ->
            val names = AppPrefs.all.filter { it.store == store }.map { it.name }
            assertEquals("duplicate key name in $store", names.distinct(), names)
        }
    }

    @Test
    fun everyKeyDocumentsItsDefaultAndItsTwoSides() {
        AppPrefs.all.forEach { key ->
            assertTrue("${key.name} has no default", key.default.isNotBlank())
            assertTrue("${key.name} has no writer", key.writtenBy.isNotBlank())
            assertTrue("${key.name} has no reader", key.readBy.isNotBlank())
        }
    }

    @Test
    fun theBaseShiftFamilyBuildsItsNameFromTheRegisteredPrefix() {
        assertEquals("mode_base_shift_moria_first_mode", AppPrefs.baseShiftKeyName("first_mode"))
        assertTrue(AppPrefs.BaseShiftMoria.name.startsWith(AppPrefs.BASE_SHIFT_KEY_PREFIX))
    }

    @Test
    fun theAnalysisFamiliesBuildTheirNamesFromTheRegisteredSuffixes() {
        // The stored names are frozen the same way the others are: these keys are already written
        // on devices that ran the analysis screen, so a suffix change silently loses the setting.
        assertEquals("hymn:first_mode:02|expected", AppPrefs.analysisExpectedKeyName("hymn:first_mode:02"))
        assertEquals("recording:content://x|mode", AppPrefs.analysisModeKeyName("recording:content://x"))
        assertEquals("recording:content://x|start", AppPrefs.analysisStartKeyName("recording:content://x"))
        assertTrue(AppPrefs.AnalysisExpectedMelody.name.endsWith(AppPrefs.ANALYSIS_EXPECTED_SUFFIX))
        assertTrue(AppPrefs.AnalysisModeKey.name.endsWith(AppPrefs.ANALYSIS_MODE_SUFFIX))
        assertTrue(AppPrefs.AnalysisStartPhthong.name.endsWith(AppPrefs.ANALYSIS_START_SUFFIX))
    }
}
