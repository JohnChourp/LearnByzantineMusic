package com.johnchourp.learnbyzantinemusic.prefs

import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs.Export.HYMN_CONTEXTS_ONLY
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs.Export.NO
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs.Export.YES
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What travels in the «Δεδομένα μάθησης» file, key by key (ClickUp `869f5x25w`).
 *
 * The registry does not compile a key without an [AppPrefs.Export] decision. This pins WHICH
 * decision, so adding a key, or changing where one goes, is a deliberate edit here, next to its
 * reason.
 */
class ExportPolicyTest {

    private val decided = mapOf(
        // Choices of the learner, which mean the same on any phone.
        "app_font_step" to YES,
        "app_language_code" to YES,
        "app_theme_mode" to YES,
        "metronome_bpm" to YES,
        "metronome_vibrate" to YES,
        "metronome_silent" to YES,
        "metronome_foot_mode" to YES,
        "favorite_topic_ids" to YES,
        "learning_completed_step_ids" to YES,
        "selected_mode_key" to YES,
        "selected_tone_timbre" to YES,
        "mode_base_shift_moria_<modeKey>" to YES,
        // The voice's global shift from «Βρες τη φωνή σου» (ClickUp 869f5x2dd): the singer's, on any phone.
        "global_base_shift_moria" to YES,
        "ison_in_background" to YES,
        "recordings_output_format" to YES,
        // The practice history behind the streak (ClickUp 869f5x2dy): the learner's own, like the path.
        "practice_log" to YES,
        // The learner's own work: the Melody Trainer's saved exercises and its last melody (F6).
        "trainer_exercises" to YES,
        "trainer_last_melody" to YES,
        // The lectern's page → ήχος maps: named by each PDF's SHA-256, never a URI (ClickUp 869f5x2e7).
        "lectern_pages_<sha256>" to YES,
        // Per hymn only: a recording's analysis settings are keyed by that recording's URI.
        "<analysisContext>|expected" to HYMN_CONTEXTS_ONLY,
        "<analysisContext>|mode" to HYMN_CONTEXTS_ONLY,
        "<analysisContext>|start" to HYMN_CONTEXTS_ONLY,
        // Folder and recording URIs: the permission to use them never leaves the device that granted it.
        "recordings_folder_tree_uri" to NO,
        "notes_folder_tree_uri" to NO,
        "owned_recordings" to NO,
        // The lectern's library: PDF URIs, each a READ grant of this phone.
        "lectern_library" to NO,
        // This device's own bookkeeping.
        "notes_last_sync_epoch_ms" to NO,
        "notes_last_sync_error" to NO,
        // Once per install: the first-launch language wizard, and Android's notifications prompt.
        "app_language_onboarding_completed" to NO,
        "notifications_permission_asked" to NO,
        // Once per phone, like those: 8 Ήχοι offering «Βρες τη φωνή σου», and its tour (ClickUp 869f5x2dd).
        "voice_range_offered" to NO,
        "eight_modes_tour_shown" to NO,
        // The daily reminder: switching it on is where Android asks for notifications, on that phone.
        "practice_reminder_enabled" to NO,
        "practice_reminder_minute_of_day" to NO,
    )

    @Test
    fun everyRegistryKeyHasTheDecidedPolicy() {
        assertEquals(decided.toSortedMap(), AppPrefs.all.associate { it.name to it.export }.toSortedMap())
    }
}
