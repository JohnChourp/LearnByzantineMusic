package com.johnchourp.learnbyzantinemusic.prefs

import android.content.Context
import android.content.SharedPreferences

/**
 * The registry of every `SharedPreferences` file and key the app owns.
 *
 * ## Why this exists
 *
 * Key names used to live as string literals next to the code that read them, spread over six owners
 * in five different packages. A typo in one of those literals does **not** break the build: it
 * silently writes to a key nobody reads, so the setting appears to save and is gone on the next
 * launch. Collecting them here makes the compiler the thing that agrees on the name, and gives each
 * key one place that records its type, its default, its allowed values and who touches it
 * (ClickUp `869f4tpwz`).
 *
 * ## The rule
 *
 * Nothing outside this file writes a preference key or a preference file name as a literal. Call
 * sites take the name from a [Key] here. `AppPrefsRegistryTest` pins the names, and
 * `NoUnregisteredPrefKeyTest` fails the build when a literal reappears elsewhere.
 *
 * ## Compatibility
 *
 * **The stored names below are frozen.** They are already on users' devices; renaming one does not
 * migrate anything, it makes the old value unreachable and silently resets that setting for everyone
 * who upgrades. Adding a key is fine. Renaming one needs a migration, and the test will stop you
 * until you have written it.
 */
object AppPrefs {

    /** A `SharedPreferences` file. */
    enum class Store(val fileName: String) {
        /** Global app settings: font scale, language, learning-path progress. */
        SETTINGS("learn_byzantine_music_settings"),

        /** The recordings page: SAF folder grant and chosen output format. */
        RECORDINGS("learn_byzantine_music_recordings"),

        /** The notes page: SAF backup folder grant and last-sync bookkeeping. */
        NOTES("learn_byzantine_music_notes"),

        /** History of recordings this app itself produced, for the "recent 10" list. */
        OWNED_RECORDINGS("learn_byzantine_music_owned_recordings"),

        /** The 8 Ήχοι page: per-mode base shift, last selected mode, chosen timbre. */
        EIGHT_MODES("eight_modes_base_shift_prefs"),
    }

    /** What a key holds, so a reader cannot ask for the wrong accessor. */
    enum class Type { INT, LONG, BOOLEAN, STRING, STRING_SET }

    /**
     * One registered key.
     *
     * @param name the stored name. **Frozen** — see the class header.
     * @param store which preferences file it lives in.
     * @param type the accessor that may read it.
     * @param default what a reader gets before anything was written, as documentation.
     * @param allowed the accepted values, or null when the key is free-form.
     * @param writtenBy the code that sets it.
     * @param readBy the code that consumes it.
     */
    data class Key(
        val name: String,
        val store: Store,
        val type: Type,
        val default: String,
        val allowed: String? = null,
        val writtenBy: String,
        val readBy: String,
    )

    // ---- SETTINGS -----------------------------------------------------------------------------

    val FontStep = Key(
        name = "app_font_step",
        store = Store.SETTINGS,
        type = Type.INT,
        default = "60",
        allowed = "20, 40, 60, 80, 100 — anything else is normalised to the nearest step",
        writtenBy = "SettingsActivity via AppFontScale.saveStep",
        readBy = "BaseActivity on every screen, via AppFontScale.wrapContextWithFontScale",
    )

    val LanguageCode = Key(
        name = "app_language_code",
        store = Store.SETTINGS,
        type = Type.STRING,
        default = "el",
        allowed = "el, en — anything else falls back to el",
        writtenBy = "the first-launch wizard and SettingsActivity, via AppLanguage.saveLanguageCode",
        readBy = "BaseActivity on every screen, via AppLanguage.wrapContextWithLocale",
    )

    val LanguageOnboardingCompleted = Key(
        name = "app_language_onboarding_completed",
        store = Store.SETTINGS,
        type = Type.BOOLEAN,
        default = "false",
        writtenBy = "MainActivity once the first-launch language wizard is confirmed",
        readBy = "MainActivity, to decide whether to show that wizard",
    )

    val LearningCompletedStepIds = Key(
        name = "learning_completed_step_ids",
        store = Store.SETTINGS,
        type = Type.STRING_SET,
        default = "empty set",
        allowed = "tile ids that LearningPath.isStep accepts; others are ignored on write",
        writtenBy = "MainActivity when a path step is opened, via LearningProgress.markCompleted",
        readBy = "the home LearningPathCard, via LearningProgress.completedSteps",
    )

    val FavoriteTopicIds = Key(
        name = "favorite_topic_ids",
        store = Store.SETTINGS,
        type = Type.STRING_SET,
        default = "empty set",
        allowed = "TheoryTopicCatalog keys; unknown ids are dropped on read, not on write, so a " +
            "topic that is temporarily absent does not lose its star",
        writtenBy = "the star on a theory page, via TheoryTopicFavorites.toggle",
        readBy = "the «8 Ήχοι» pages menu, which lists favourites first",
    )

    // ---- RECORDINGS ---------------------------------------------------------------------------

    val RecordingsFolderTreeUri = Key(
        name = "recordings_folder_tree_uri",
        store = Store.RECORDINGS,
        type = Type.STRING,
        default = "absent — the page then asks for a folder",
        allowed = "a SAF tree URI the app holds a persisted read/write grant for",
        writtenBy = "RecordingsActivity after the folder picker returns",
        readBy = "the recordings recorder, indexer and manager",
    )

    val RecordingsOutputFormat = Key(
        name = "recordings_output_format",
        store = Store.RECORDINGS,
        type = Type.STRING,
        default = "FLAC",
        allowed = "a RecordingFormatOption name; unknown values fall back to FLAC",
        writtenBy = "the format selector on the recordings page",
        readBy = "the recorder, to pick the container and the transcode step",
    )

    // ---- NOTES --------------------------------------------------------------------------------

    val NotesFolderTreeUri = Key(
        name = "notes_folder_tree_uri",
        store = Store.NOTES,
        type = Type.STRING,
        default = "absent — notes then have no backup target",
        allowed = "a SAF tree URI the app holds a persisted read/write grant for",
        writtenBy = "NotesActivity after the mandatory first-run folder pick",
        readBy = "the notes backup/sync path",
    )

    val NotesLastSyncEpochMs = Key(
        name = "notes_last_sync_epoch_ms",
        store = Store.NOTES,
        type = Type.LONG,
        default = "-1, read as \"never\"",
        writtenBy = "the notes sync, on a successful snapshot write",
        readBy = "the notes screen status line",
    )

    val NotesLastSyncError = Key(
        name = "notes_last_sync_error",
        store = Store.NOTES,
        type = Type.STRING,
        default = "absent — no error pending",
        writtenBy = "the notes sync, on a failed snapshot write",
        readBy = "the notes screen status line and the manual resync action",
    )

    // ---- OWNED RECORDINGS ---------------------------------------------------------------------

    val OwnedRecordings = Key(
        name = "owned_recordings",
        store = Store.OWNED_RECORDINGS,
        type = Type.STRING,
        default = "absent — the recent list is then empty",
        allowed = "a JSON array, newest first, capped at 300 entries",
        writtenBy = "the recorder, after a recording is saved",
        readBy = "the recordings page's \"last 10\" list",
    )

    // ---- 8 ΗΧΟΙ -------------------------------------------------------------------------------

    val SelectedModeKey = Key(
        name = "selected_mode_key",
        store = Store.EIGHT_MODES,
        type = Type.STRING,
        default = "absent — the page opens on the first mode",
        allowed = "a theoryKey of EIGHT_MODES",
        writtenBy = "the mode selector on the 8 Ήχοι page",
        readBy = "the same page on next open",
    )

    val SelectedToneTimbre = Key(
        name = "selected_tone_timbre",
        store = Store.EIGHT_MODES,
        type = Type.STRING,
        default = "CLEAN",
        allowed = "a ToneTimbre name; unknown values fall back to CLEAN",
        writtenBy = "the timbre selector on the 8 Ήχοι page",
        readBy = "PhthongTonePlayer, for touch playback and the ison drone",
    )

    /**
     * The per-mode base shift is a **family** of keys, one per mode: the stored name is this prefix
     * followed by the mode's `theoryKey`. Use [baseShiftKeyName]; never build the name inline.
     */
    const val BASE_SHIFT_KEY_PREFIX = "mode_base_shift_moria_"

    val BaseShiftMoria = Key(
        name = "$BASE_SHIFT_KEY_PREFIX<modeKey>",
        store = Store.EIGHT_MODES,
        type = Type.INT,
        default = "0",
        allowed = "-12..+12 μόρια; values outside are clamped on both read and write",
        writtenBy = "the «Μεταφορά βάσης» slider, per mode",
        readBy = "the scale diagram, touch playback and the ison drone of that mode",
    )

    /** Stored name of the base-shift key for [modeKey]. */
    fun baseShiftKeyName(modeKey: String): String = BASE_SHIFT_KEY_PREFIX + modeKey

    /** Every registered key. A new key must appear here, or `AppPrefsRegistryTest` fails. */
    val all: List<Key> = listOf(
        FontStep,
        LanguageCode,
        LanguageOnboardingCompleted,
        LearningCompletedStepIds,
        FavoriteTopicIds,
        RecordingsFolderTreeUri,
        RecordingsOutputFormat,
        NotesFolderTreeUri,
        NotesLastSyncEpochMs,
        NotesLastSyncError,
        OwnedRecordings,
        SelectedModeKey,
        SelectedToneTimbre,
        BaseShiftMoria,
    )

    /** Opens [store]. The only place the app names a preferences file. */
    fun open(context: Context, store: Store): SharedPreferences =
        context.getSharedPreferences(store.fileName, Context.MODE_PRIVATE)
}
