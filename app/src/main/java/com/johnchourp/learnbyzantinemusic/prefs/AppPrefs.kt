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
 *
 * ## What travels
 *
 * Every key also says whether it goes into the «Δεδομένα μάθησης» file that the learner takes to
 * another phone ([Export], ClickUp `869f5x25w`). The field has no default, so a new key does not
 * compile until somebody has decided. Folder URIs never travel: the folder permission stays on the
 * device that granted it.
 */
object AppPrefs {

    /** A `SharedPreferences` file. */
    enum class Store(val fileName: String) {
        /** Global app settings: font scale, language, learning-path progress, the notifications prompt. */
        SETTINGS("learn_byzantine_music_settings"),

        /** The recordings page: SAF folder grant and chosen output format. */
        RECORDINGS("learn_byzantine_music_recordings"),

        /** The notes page: SAF backup folder grant and last-sync bookkeeping. */
        NOTES("learn_byzantine_music_notes"),

        /** History of recordings this app itself produced, for the "recent 10" list. */
        OWNED_RECORDINGS("learn_byzantine_music_owned_recordings"),

        /** The 8 Ήχοι page: per-mode base shift, last selected mode, chosen timbre. */
        EIGHT_MODES("eight_modes_base_shift_prefs"),

        /** «Ανάλυση φθόγγων»: what the user expects to have chanted, per hymn or per recording. */
        RECORDING_ANALYSIS("recording_analysis_settings"),

        /**
         * «Πεντάλεπτο της ημέρας»: the practice history behind the streak, and the reminder. Its own
         * file, so resetting the learning path's progress never touches the streak.
         */
        PRACTICE("learn_byzantine_music_practice"),
    }

    /** What a key holds, so a reader cannot ask for the wrong accessor. */
    enum class Type { INT, LONG, BOOLEAN, STRING, STRING_SET }

    /** Whether a key goes into the «Δεδομένα μάθησης» file (see the class header). */
    enum class Export {
        /** Exported and imported: a choice of the learner that means the same on any phone. */
        YES,

        /** Stays on this device: a folder or recording URI, this device's bookkeeping, a one-off prompt. */
        NO,

        /**
         * An analysis family: hymn contexts (`hymn:<mode>:<code>`) travel; recording contexts
         * (`recording:<uri>`) stay, because the key itself holds a URI.
         */
        HYMN_CONTEXTS_ONLY,
    }

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
     * @param export whether it travels in the «Δεδομένα μάθησης» file. No default: every key decides.
     */
    data class Key(
        val name: String,
        val store: Store,
        val type: Type,
        val default: String,
        val allowed: String? = null,
        val writtenBy: String,
        val readBy: String,
        val export: Export,
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
        export = Export.YES,
    )

    val LanguageCode = Key(
        name = "app_language_code",
        store = Store.SETTINGS,
        type = Type.STRING,
        default = "el",
        allowed = "el, en — anything else falls back to el",
        writtenBy = "the first-launch wizard and SettingsActivity, via AppLanguage.saveLanguageCode",
        readBy = "BaseActivity on every screen, via AppLanguage.wrapContextWithLocale",
        export = Export.YES,
    )

    val LanguageOnboardingCompleted = Key(
        name = "app_language_onboarding_completed",
        store = Store.SETTINGS,
        type = Type.BOOLEAN,
        default = "false",
        writtenBy = "MainActivity once the first-launch language wizard is confirmed",
        readBy = "MainActivity, to decide whether to show that wizard",
        export = Export.NO,
    )

    val LearningCompletedStepIds = Key(
        name = "learning_completed_step_ids",
        store = Store.SETTINGS,
        type = Type.STRING_SET,
        default = "empty set",
        allowed = "tile ids that LearningPath.isStep accepts; others are ignored on write",
        writtenBy = "MainActivity when a path step is opened, via LearningProgress.markCompleted; " +
            "removed by «Μηδενισμός προόδου» in Ρυθμίσεις, via LearningProgress.reset",
        readBy = "the home LearningPathCard, via LearningProgress.completedSteps",
        export = Export.YES,
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
        export = Export.YES,
    )

    val MetronomeBpm = Key(
        name = "metronome_bpm",
        store = Store.SETTINGS,
        type = Type.INT,
        default = "80",
        allowed = "40..160 χρόνοι per minute; values outside are clamped on read and on write",
        writtenBy = "the tempo slider on the «Δίσημος/Τρίσημος/Τετράσημος» page",
        readBy = "the same page, which reopens at the tempo the learner was practising at",
        export = Export.YES,
    )

    val MetronomeVibrate = Key(
        name = "metronome_vibrate",
        store = Store.SETTINGS,
        type = Type.BOOLEAN,
        default = "true — the metronome vibrated on every beat before the switch existed",
        allowed = "true, false; on a device without a vibrator it is ignored and its switch is hidden",
        writtenBy = "the «Δόνηση» switch of the metronome on the «Δίσημος/Τρίσημος/Τετράσημος» page",
        readBy = "the same metronome, via MetronomePrefs.savedOptions",
        export = Export.YES,
    )

    val MetronomeSilent = Key(
        name = "metronome_silent",
        store = Store.SETTINGS,
        type = Type.BOOLEAN,
        default = "false",
        allowed = "true, false; applies only while vibration is on and available, so the metronome " +
            "can never end up neither sounding nor vibrating",
        writtenBy = "the «Σιωπηλά» switch of the same metronome",
        readBy = "the same metronome, via MetronomePrefs.savedOptions",
        export = Export.YES,
    )

    val MetronomeFootMode = Key(
        name = "metronome_foot_mode",
        store = Store.SETTINGS,
        type = Type.BOOLEAN,
        default = "false",
        allowed = "true, false; true marks only the θέσεις",
        writtenBy = "the «Πόδι» switch of the same metronome",
        readBy = "the same metronome, via MetronomePrefs.savedOptions",
        export = Export.YES,
    )

    val ThemeMode = Key(
        name = "app_theme_mode",
        store = Store.SETTINGS,
        type = Type.STRING,
        default = "system",
        allowed = "system, light, dark, high_contrast — an unknown value falls back to system",
        writtenBy = "the theme selector in Ρυθμίσεις",
        readBy = "BaseActivity, which applies it before any screen inflates",
        export = Export.YES,
    )

    val NotificationsPermissionAsked = Key(
        name = "notifications_permission_asked",
        store = Store.SETTINGS,
        type = Type.BOOLEAN,
        default = "false",
        writtenBy = "AppNotifications, just before the Android 13+ notifications prompt is first shown",
        readBy = "AppNotifications, so that prompt is shown at most once per install",
        export = Export.NO,
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
        export = Export.NO,
    )

    val RecordingsOutputFormat = Key(
        name = "recordings_output_format",
        store = Store.RECORDINGS,
        type = Type.STRING,
        default = "FLAC",
        allowed = "a RecordingFormatOption name; unknown values fall back to FLAC",
        writtenBy = "the format selector on the recordings page",
        readBy = "the recorder, to pick the container and the transcode step",
        export = Export.YES,
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
        export = Export.NO,
    )

    val NotesLastSyncEpochMs = Key(
        name = "notes_last_sync_epoch_ms",
        store = Store.NOTES,
        type = Type.LONG,
        default = "-1, read as \"never\"",
        writtenBy = "the notes sync, on a successful snapshot write",
        readBy = "the notes screen status line",
        export = Export.NO,
    )

    val NotesLastSyncError = Key(
        name = "notes_last_sync_error",
        store = Store.NOTES,
        type = Type.STRING,
        default = "absent — no error pending",
        writtenBy = "the notes sync, on a failed snapshot write",
        readBy = "the notes screen status line and the manual resync action",
        export = Export.NO,
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
        export = Export.NO,
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
        export = Export.YES,
    )

    val SelectedToneTimbre = Key(
        name = "selected_tone_timbre",
        store = Store.EIGHT_MODES,
        type = Type.STRING,
        default = "CLEAN",
        allowed = "a ToneTimbre name; unknown values fall back to CLEAN",
        writtenBy = "the timbre selector on the 8 Ήχοι page",
        readBy = "PhthongTonePlayer, for touch playback and the ison drone",
        export = Export.YES,
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
        export = Export.YES,
    )

    /** Stored name of the base-shift key for [modeKey]. */
    fun baseShiftKeyName(modeKey: String): String = BASE_SHIFT_KEY_PREFIX + modeKey

    val IsonInBackground = Key(
        name = "ison_in_background",
        store = Store.EIGHT_MODES,
        type = Type.BOOLEAN,
        default = "false — the ison stops when you leave the 8 Ήχοι page",
        writtenBy = "the «Συνέχισε στο παρασκήνιο» switch of the ison card",
        readBy = "EightModesActivity, to decide whether the page or IsonPlaybackService plays the ison",
        export = Export.YES,
    )

    /**
     * The analysis settings are three **families** of keys, one set per analysis context. The
     * context is a hymn (`hymn:<modeKey>:<code>`, shared by every recording of it) or a single
     * recording (`recording:<uri>`); the stored name is that context followed by the suffix below.
     * Use the helpers; never build the name inline.
     */
    const val ANALYSIS_EXPECTED_SUFFIX = "|expected"
    const val ANALYSIS_MODE_SUFFIX = "|mode"
    const val ANALYSIS_START_SUFFIX = "|start"

    val AnalysisExpectedMelody = Key(
        name = "<analysisContext>$ANALYSIS_EXPECTED_SUFFIX",
        store = Store.RECORDING_ANALYSIS,
        type = Type.STRING,
        default = "empty — no expected melody has been typed for this context",
        allowed = "comma-separated PhthongName constant names (NI … ZO, frozen: see StoredPhthongs); " +
            "unknown names are dropped on read",
        writtenBy = "the «αναμενόμενη μελωδία» field on the analysis screen",
        readBy = "SequenceAligner, to score what was chanted against what was expected",
        export = Export.HYMN_CONTEXTS_ONLY,
    )

    val AnalysisModeKey = Key(
        name = "<analysisContext>$ANALYSIS_MODE_SUFFIX",
        store = Store.RECORDING_ANALYSIS,
        type = Type.STRING,
        default = "unset — the screen falls back to its own default mode",
        allowed = "a mode theoryKey",
        writtenBy = "the mode picker on the analysis screen",
        readBy = "ModeScalePositions, to place the phthongs of that mode",
        export = Export.HYMN_CONTEXTS_ONLY,
    )

    val AnalysisStartPhthong = Key(
        name = "<analysisContext>$ANALYSIS_START_SUFFIX",
        store = Store.RECORDING_ANALYSIS,
        type = Type.STRING,
        default = "unset — calibration falls back to the first steady note",
        allowed = "a PhthongName constant name (NI … ZO, frozen: see StoredPhthongs)",
        writtenBy = "the starting-phthong picker on the analysis screen",
        readBy = "the analysis, to calibrate the singer's voice from a declared phthong",
        export = Export.HYMN_CONTEXTS_ONLY,
    )

    /** Stored name of the expected-melody key for [analysisContext]. */
    fun analysisExpectedKeyName(analysisContext: String): String = analysisContext + ANALYSIS_EXPECTED_SUFFIX

    /** Stored name of the mode key for [analysisContext]. */
    fun analysisModeKeyName(analysisContext: String): String = analysisContext + ANALYSIS_MODE_SUFFIX

    /** Stored name of the starting-phthong key for [analysisContext]. */
    fun analysisStartKeyName(analysisContext: String): String = analysisContext + ANALYSIS_START_SUFFIX

    // ---- PRACTICE -----------------------------------------------------------------------------

    val PracticeLogJson = Key(
        name = "practice_log",
        store = Store.PRACTICE,
        type = Type.STRING,
        default = "absent — no practice yet, a streak of 0",
        allowed = "PracticeLogCodec JSON, schemaVersion 1: completed sessions and minutes per day; " +
            "anything unreadable reads as an empty history",
        writtenBy = "«Πεντάλεπτο της ημέρας» when a session is completed, via PracticeLogStore",
        readBy = "the home card and «Ιστορικό εξάσκησης» (streak, weekly chart), and the reminder",
    )

    val PracticeReminderEnabled = Key(
        name = "practice_reminder_enabled",
        store = Store.PRACTICE,
        type = Type.BOOLEAN,
        default = "false — the reminder is opt-in",
        writtenBy = "the reminder switch in «Ιστορικό εξάσκησης», via PracticeReminders",
        readBy = "PracticeReminders, which schedules the work, and PracticeReminderWorker",
    )

    val PracticeReminderMinuteOfDay = Key(
        name = "practice_reminder_minute_of_day",
        store = Store.PRACTICE,
        type = Type.INT,
        default = "1140 (19:00)",
        allowed = "0..1439 minutes after local midnight; anything else falls back to the default",
        writtenBy = "the reminder time in «Ιστορικό εξάσκησης», via PracticeReminders",
        readBy = "PracticeReminders, to schedule the next reminder",
    )

    /** Every registered key. A new key must appear here, or `AppPrefsRegistryTest` fails. */
    val all: List<Key> = listOf(
        FontStep,
        LanguageCode,
        LanguageOnboardingCompleted,
        LearningCompletedStepIds,
        FavoriteTopicIds,
        MetronomeBpm,
        MetronomeVibrate,
        MetronomeSilent,
        MetronomeFootMode,
        ThemeMode,
        NotificationsPermissionAsked,
        RecordingsFolderTreeUri,
        RecordingsOutputFormat,
        NotesFolderTreeUri,
        NotesLastSyncEpochMs,
        NotesLastSyncError,
        OwnedRecordings,
        SelectedModeKey,
        SelectedToneTimbre,
        BaseShiftMoria,
        IsonInBackground,
        AnalysisExpectedMelody,
        AnalysisModeKey,
        AnalysisStartPhthong,
        PracticeLogJson,
        PracticeReminderEnabled,
        PracticeReminderMinuteOfDay,
    )

    /** Opens [store]. The only place the app names a preferences file. */
    fun open(context: Context, store: Store): SharedPreferences =
        context.getSharedPreferences(store.fileName, Context.MODE_PRIVATE)
}
