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
                "app_font_step",
                "app_language_code",
                "app_language_onboarding_completed",
                "learning_completed_step_ids",
                "mode_base_shift_moria_<modeKey>",
                "notes_folder_tree_uri",
                "notes_last_sync_epoch_ms",
                "notes_last_sync_error",
                "owned_recordings",
                "recordings_folder_tree_uri",
                "recordings_output_format",
                "selected_mode_key",
                "selected_tone_timbre",
            ),
            AppPrefs.all.map { it.name }.sorted()
        )
    }

    @Test
    fun everyPreferencesFileNameIsFrozen() {
        assertEquals(
            listOf(
                "eight_modes_base_shift_prefs",
                "learn_byzantine_music_notes",
                "learn_byzantine_music_owned_recordings",
                "learn_byzantine_music_recordings",
                "learn_byzantine_music_settings",
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
}
