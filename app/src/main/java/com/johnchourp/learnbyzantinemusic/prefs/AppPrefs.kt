package com.johnchourp.learnbyzantinemusic.prefs

import com.johnchourp.learnbyzantinemusic.music.BaseShift
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
 * compile until somebody has decided. Folder URIs never travel, nor the lectern's PDF URIs: the
 * permission to use them stays on the device that granted it.
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

        /** The Melody Trainer: the user's saved exercises and the last melody (ClickUp `869f5x261`). */
        TRAINER("learn_byzantine_music_trainer"),
        /** The digital lectern's library: the user's own PDFs, as this phone's grants to read them. */
        LECTERN_LIBRARY("learn_byzantine_music_lectern_library"),

        /** The digital lectern's page → ήχος maps, one per PDF, named by the file's SHA-256. */
        LECTERN_PAGES("learn_byzantine_music_lectern_pages"),
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

    val GlobalBaseShift = Key(
        name = "global_base_shift_moria",
        store = Store.SETTINGS,
        type = Type.INT,
        default = "0 — every ladder exactly as before it existed",
        allowed = "${BaseShift.MIN_MORIA}..+${BaseShift.MAX_MORIA} μόρια, clamped on read. Added to each mode's own " +
            "«Μεταφορά βάσης» wherever a ladder is built, and the sum clamped again (BaseShift.combined); " +
            "the modes' own values are never rewritten",
        writtenBy = "«Βρες τη φωνή σου», when its suggestion is accepted, and the reset of the «Φωνή» card in Settings",
        readBy = "the 8 Ήχοι page (diagram, απήχημα, ison, «Πού είμαι») — and through its requests " +
            "IsonPlaybackService — the Melody Trainer, «Διατονικός» included, and the lectern's ison bar",
        // The singer's voice, the same on any phone — like the per-mode shifts it is added to.
        export = Export.YES,
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
        readBy = "PhthongTonePlayer, for touch playback and the ison drone — the lectern's ison bar too",
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
        allowed = "${BaseShift.MIN_MORIA}..+${BaseShift.MAX_MORIA} μόρια (BaseShift.RANGE); values outside are clamped on " +
            "both read and write",
        writtenBy = "the «Μεταφορά βάσης» slider, per mode",
        readBy = "the scale diagram, touch playback and the ison drone of that mode; the lectern, as " +
            "the starting shift of that mode when it is newly set on a page (the page then keeps its own)",
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
        readBy = "EightModesActivity and the lectern's reader, to decide whether the screen or " +
            "IsonPlaybackService plays the ison",
        export = Export.YES,
    )

    val VoiceRangeOffered = Key(
        name = "voice_range_offered",
        store = Store.EIGHT_MODES,
        type = Type.BOOLEAN,
        default = "false",
        writtenBy = "the 8 Ήχοι page, once it has offered «Βρες τη φωνή σου» — taken or not — and Settings, " +
            "once the test has been opened from its «Φωνή» card",
        readBy = "the 8 Ήχοι page, so it offers the test by itself only the first time it opens",
        // A once-per-install prompt, like the notifications one: each phone offers it once.
        export = Export.NO,
    )

    val EightModesTourShown = Key(
        name = "eight_modes_tour_shown",
        store = Store.EIGHT_MODES,
        type = Type.BOOLEAN,
        default = "false",
        writtenBy = "the 8 Ήχοι page's four-step tour, when it is finished or skipped",
        readBy = "the same page, so the tour never shows by itself again",
        export = Export.NO,
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
        export = Export.YES,
    )

    /**
     * The reminder and its time stay on each phone ([Export.NO]): switching it on is where Android
     * 13+ asks for the notifications permission, and the schedule belongs to that phone. On a new
     * phone the learner switches it on there, and that phone asks.
     */
    val PracticeReminderEnabled = Key(
        name = "practice_reminder_enabled",
        store = Store.PRACTICE,
        type = Type.BOOLEAN,
        default = "false — the reminder is opt-in",
        writtenBy = "the reminder switch in «Ιστορικό εξάσκησης», via PracticeReminders",
        readBy = "PracticeReminders, which schedules the work, and PracticeReminderWorker",
        export = Export.NO,
    )

    val PracticeReminderMinuteOfDay = Key(
        name = "practice_reminder_minute_of_day",
        store = Store.PRACTICE,
        type = Type.INT,
        default = "1140 (19:00)",
        allowed = "0..1439 minutes after local midnight; anything else falls back to the default",
        writtenBy = "the reminder time in «Ιστορικό εξάσκησης», via PracticeReminders",
        readBy = "PracticeReminders, to schedule the next reminder",
        export = Export.NO,
    )

    // ---- TRAINER ------------------------------------------------------------------------------
    // Both values are JSON the Trainer's own codecs write and read; their formats, and what happens to
    // a value a newer app wrote, are documented in TrainerMelodyCodec and ExerciseBook. Both are the
    // learner's own work, so both travel in the «Δεδομένα μάθησης» file.

    val TrainerExercises = Key(
        name = "trainer_exercises",
        store = Store.TRAINER,
        type = Type.STRING,
        default = "absent — no exercise has been saved yet",
        allowed = "a JSON object {schemaVersion, exercises[]}, at most ExerciseBook.MAX_EXERCISES; " +
            "an entry that cannot be read is kept as it is, and the Trainer never writes over a newer schemaVersion",
        writtenBy = "«Αποθήκευση ως…», rename and delete in «Οι ασκήσεις μου» on the Melody Trainer",
        readBy = "the «Οι ασκήσεις μου» list, and «Άνοιγμα» of one exercise",
        export = Export.YES,
    )

    val TrainerLastMelody = Key(
        name = "trainer_last_melody",
        store = Store.TRAINER,
        type = Type.STRING,
        default = "absent — the Trainer opens with an empty melody",
        allowed = "a JSON melody {schemaVersion, bpm, mode?, baseShift?, notes[]}; one that cannot be read " +
            "is not restored",
        writtenBy = "the Melody Trainer's autosave, after every edit of the melody and in onStop",
        readBy = "MelodyTrainerActivity.onCreate, which puts the melody back after a close or a process death",
        export = Export.YES,
    )
    // ---- ΨΗΦΙΑΚΟ ΑΝΑΛΟΓΙΟ ---------------------------------------------------------------------

    val LecternLibraryEntries = Key(
        name = "lectern_library",
        store = Store.LECTERN_LIBRARY,
        type = Type.STRING,
        default = "absent — the library is empty",
        allowed = "a LecternLibrary JSON object of up to 100 PDFs: content URIs the app holds a persisted " +
            "READ grant for, each with its title and the page the reader left it on",
        writtenBy = "the lectern's library when a PDF is added, opened or removed, and the reader when it leaves a page",
        readBy = "the lectern's library list, and the reader for the page to reopen on",
        export = Export.NO,
    )

    /**
     * The lectern's page → ήχος map is a **family** of keys, one per PDF: the stored name is this prefix
     * followed by the SHA-256 of the file, 64 lowercase hex digits — never its URI, which means nothing
     * on another phone. Use [lecternPagesKeyName]; never build the name inline.
     */
    const val LECTERN_PAGES_KEY_PREFIX = "lectern_pages_"

    val LecternPageModes = Key(
        name = "$LECTERN_PAGES_KEY_PREFIX<sha256>",
        store = Store.LECTERN_PAGES,
        type = Type.STRING,
        default = "absent — no page of that PDF has a ήχος yet",
        allowed = "LecternPagesCodec JSON, schemaVersion 1: per page, a mode key, a «Μεταφορά βάσης» " +
            "clamped like the 8 Ήχοι's, and the ison φθόγγος (absent for the base)",
        writtenBy = "the lectern's ison bar, when a ήχος, a shift or a φθόγγος is set on a page or a page's own setting is removed",
        readBy = "the lectern's reader: a page sounds the most recent setting at or before it",
        export = Export.YES,
    )

    /** Stored name of the page → ήχος key for the PDF whose SHA-256 is [sha256Hex]. */
    fun lecternPagesKeyName(sha256Hex: String): String = LECTERN_PAGES_KEY_PREFIX + sha256Hex

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
        GlobalBaseShift,
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
        VoiceRangeOffered,
        EightModesTourShown,
        AnalysisExpectedMelody,
        AnalysisModeKey,
        AnalysisStartPhthong,
        PracticeLogJson,
        PracticeReminderEnabled,
        PracticeReminderMinuteOfDay,
        TrainerExercises,
        TrainerLastMelody,
        LecternLibraryEntries,
        LecternPageModes,
    )

    /** Opens [store]. The only place the app names a preferences file. */
    fun open(context: Context, store: Store): SharedPreferences =
        context.getSharedPreferences(store.fileName, Context.MODE_PRIVATE)
}
