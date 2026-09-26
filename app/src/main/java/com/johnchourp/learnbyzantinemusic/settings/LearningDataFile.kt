package com.johnchourp.learnbyzantinemusic.settings

import com.johnchourp.learnbyzantinemusic.AppFontScale
import com.johnchourp.learnbyzantinemusic.AppLanguage
import com.johnchourp.learnbyzantinemusic.lectern.LecternFileKey
import com.johnchourp.learnbyzantinemusic.lectern.LecternPagesCodec
import com.johnchourp.learnbyzantinemusic.learning.LearningPath
import com.johnchourp.learnbyzantinemusic.lessons.ui.MetronomeSchedule
import com.johnchourp.learnbyzantinemusic.modes.ToneTimbre
import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
import com.johnchourp.learnbyzantinemusic.recordings.RecordingFormatOption
import com.johnchourp.learnbyzantinemusic.recordings.analysis.StoredPhthongs
import com.johnchourp.learnbyzantinemusic.ui.theme.AppThemeMode
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate

/**
 * The «Δεδομένα μάθησης» file: the favourites, the «Από το μηδέν» progress and the settings, in one
 * file the learner saves and opens through the system pickers to take them to another phone
 * (ClickUp `869f5x25w`). Pure — no Android — so every rule here runs in plain JVM tests; the Settings
 * screen reads and writes the preferences around it ([LearningDataPrefs]).
 *
 * **Format, version 1 — readable forever.**
 * `{"schemaVersion": 1, "exportedAt": <epoch ms>, "appVersion": "1.17.0",
 * "stores": {"<preferences file>": {"<stored key>": {"type": "INT", "value": 80}}}}`,
 * the types being [AppPrefs.Type] names. A newer format raises the version and keeps reading this one.
 *
 * **What travels** is decided per key in the registry ([AppPrefs.Key.export]). Folder and recording
 * URIs never do; as a last guard, no key or value containing `://` is ever exported or imported.
 *
 * **One rule for both directions: [normalized].** For every key it gives the value the app itself
 * would use when it reads that key — through the app's own rules: [AppFontScale.normalizeStep],
 * [MetronomeSchedule.clampBpm], the shared «Μεταφορά βάσης» range [BaseShift.clamp] (so the file
 * follows it when it widens), [LearningPath.isStep], [Mode.fromKey], [StoredPhthongs], the lectern's
 * [LecternPagesCodec] and the enums. The
 * export writes normalised values; the import accepts a value only when it is already in that form.
 * So whatever the app exports, it can import back.
 *
 * **Import is all or nothing.** [decode] checks the whole file — version, stores, keys, types,
 * values — before anything can be written; one bad entry rejects it with a [Result.Rejected]. Only an
 * accepted file can be written ([write]), and then only the keys it carries: one commit per
 * preferences file. What was not in the file stays as it is.
 */
object LearningDataFile {

    const val SCHEMA_VERSION = 1
    const val MIME_TYPE = "application/json"

    /** The largest file the import reads, in characters. A real one is a few kilobytes. */
    const val MAX_CHARS = 1_000_000

    fun suggestedFileName(date: LocalDate): String = "LearnByzantineMusic-learning-data-$date.json"

    /** The outcome of reading a file. */
    sealed interface Result {
        /** A valid file: the keys it will write, per preferences file, with typed values. */
        data class Accepted(
            val changes: Map<AppPrefs.Store, Map<String, Any>>,
            val exportedAtEpochMs: Long,
            val appVersion: String,
        ) : Result

        /** Refused: nothing may be written. [detail] names the store or key at fault, when there is one. */
        data class Rejected(val reason: Reason, val detail: String? = null) : Result
    }

    enum class Reason {
        /** Not JSON, not this kind of file, or damaged. */
        NOT_A_LEARNING_DATA_FILE,

        /** Written by a newer app, in a format this one cannot read. */
        NEWER_VERSION,

        /** Carries a store or key this version does not import — unknown, or one that stays on each device. */
        NOT_IMPORTABLE,

        /** A value of the wrong type, or one the app would not store. */
        BAD_VALUE,
    }

    // ---- export ---------------------------------------------------------------------------------

    /**
     * The file for [snapshots] (each preferences file as `SharedPreferences.getAll()` returns it):
     * only the keys that travel, normalised, sorted so the same data gives the same file.
     */
    fun encode(snapshots: Map<AppPrefs.Store, Map<String, Any?>>, exportedAtEpochMs: Long, appVersion: String): String {
        val stores = JSONObject()
        AppPrefs.Store.entries.forEach { store ->
            val entries = JSONObject()
            snapshots[store].orEmpty().toSortedMap().forEach { (name, stored) ->
                val key = exportedKey(store, name) ?: return@forEach
                val value = stored?.let { normalized(key, it) } ?: return@forEach
                if (carriesUri(name, value)) return@forEach
                entries.put(name, JSONObject().put("type", key.type.name).put("value", toJson(value)))
            }
            if (entries.length() > 0) {
                stores.put(store.fileName, entries)
            }
        }
        return JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("exportedAt", exportedAtEpochMs)
            .put("appVersion", appVersion)
            .put("stores", stores)
            .toString(2)
    }

    // ---- import ---------------------------------------------------------------------------------

    fun decode(json: String): Result {
        if (json.length > MAX_CHARS) return Result.Rejected(Reason.NOT_A_LEARNING_DATA_FILE)
        val root = try {
            JSONObject(json)
        } catch (_: JSONException) {
            return Result.Rejected(Reason.NOT_A_LEARNING_DATA_FILE)
        }
        val version = root.opt("schemaVersion") as? Int ?: return Result.Rejected(Reason.NOT_A_LEARNING_DATA_FILE)
        if (version > SCHEMA_VERSION) return Result.Rejected(Reason.NEWER_VERSION)
        // One branch per version ever written: a new format adds a branch and keeps this one.
        return when (version) {
            1 -> decodeVersion1(root)
            else -> Result.Rejected(Reason.NOT_A_LEARNING_DATA_FILE)
        }
    }

    private fun decodeVersion1(root: JSONObject): Result {
        val exportedAt = (root.opt("exportedAt") as? Number)?.toLong()?.takeIf { it > 0 }
        val appVersion = root.opt("appVersion") as? String
        val stores = root.optJSONObject("stores")
        if (exportedAt == null || appVersion.isNullOrBlank() || stores == null) {
            return Result.Rejected(Reason.NOT_A_LEARNING_DATA_FILE)
        }
        val changes = LinkedHashMap<AppPrefs.Store, Map<String, Any>>()
        for (fileName in stores.keys()) {
            val store = AppPrefs.Store.entries.firstOrNull { it.fileName == fileName }
                ?: return Result.Rejected(Reason.NOT_IMPORTABLE, fileName)
            val entries = stores.optJSONObject(fileName) ?: return Result.Rejected(Reason.NOT_A_LEARNING_DATA_FILE, fileName)
            val values = LinkedHashMap<String, Any>()
            for (name in entries.keys()) {
                val key = exportedKey(store, name) ?: return Result.Rejected(Reason.NOT_IMPORTABLE, name)
                val entry = entries.optJSONObject(name) ?: return Result.Rejected(Reason.BAD_VALUE, name)
                val value = fromJson(entry, key.type) ?: return Result.Rejected(Reason.BAD_VALUE, name)
                if (normalized(key, value) != value || carriesUri(name, value)) {
                    return Result.Rejected(Reason.BAD_VALUE, name)
                }
                values[name] = value
            }
            if (values.isNotEmpty()) {
                changes[store] = values
            }
        }
        return Result.Accepted(changes, exportedAt, appVersion)
    }

    /** Writes one preferences file — all of [values] in one commit — and says whether the commit succeeded. */
    fun interface Writer {
        fun write(store: AppPrefs.Store, values: Map<String, Any>): Boolean
    }

    /**
     * Writes the keys [accepted] carries and nothing else, one commit per preferences file. Every file
     * is attempted even if one fails; true only when all of them were written.
     */
    fun write(accepted: Result.Accepted, writer: Writer): Boolean =
        accepted.changes.map { (store, values) -> writer.write(store, values) }.all { it }

    // ---- what the confirmation dialog lists ------------------------------------------------------

    /** The kinds of change an import can make, in the order the dialog lists them. */
    enum class Item { FONT_SIZE, LANGUAGE, THEME, METRONOME, FAVOURITES, PROGRESS, SELECTED_MODE, TIMBRE, ISON_BACKGROUND, BASE_SHIFT, RECORDING_FORMAT, ANALYSIS, LECTERN_PAGES }

    /** One line of the dialog; [count] is how many pages, steps, modes, hymns or PDFs, where that matters. */
    data class Line(val item: Item, val count: Int)

    fun summary(accepted: Result.Accepted): List<Line> {
        val things = LinkedHashMap<Item, MutableSet<String>>()
        accepted.changes.forEach { (store, values) ->
            values.forEach { (name, value) ->
                val key = exportedKey(store, name) ?: return@forEach
                val item = itemOf(key) ?: return@forEach
                val bucket = things.getOrPut(item) { mutableSetOf() }
                when (item) {
                    Item.FAVOURITES, Item.PROGRESS -> (value as Set<*>).forEach { bucket += it.toString() }
                    Item.BASE_SHIFT -> bucket += name.removePrefix(AppPrefs.BASE_SHIFT_KEY_PREFIX)
                    Item.ANALYSIS -> bucket += analysisContext(name)
                    Item.LECTERN_PAGES -> bucket += name.removePrefix(AppPrefs.LECTERN_PAGES_KEY_PREFIX)
                    else -> bucket += name
                }
            }
        }
        return Item.entries.filter { it in things }.map { Line(it, things.getValue(it).size) }
    }

    /** Every key that travels has a line; `LearningDataFileTest` fails on one that does not. */
    internal fun itemOf(key: AppPrefs.Key): Item? = when (key) {
        AppPrefs.FontStep -> Item.FONT_SIZE
        AppPrefs.LanguageCode -> Item.LANGUAGE
        AppPrefs.ThemeMode -> Item.THEME
        AppPrefs.MetronomeBpm, AppPrefs.MetronomeVibrate, AppPrefs.MetronomeSilent, AppPrefs.MetronomeFootMode -> Item.METRONOME
        AppPrefs.FavoriteTopicIds -> Item.FAVOURITES
        AppPrefs.LearningCompletedStepIds -> Item.PROGRESS
        AppPrefs.SelectedModeKey -> Item.SELECTED_MODE
        AppPrefs.SelectedToneTimbre -> Item.TIMBRE
        AppPrefs.IsonInBackground -> Item.ISON_BACKGROUND
        AppPrefs.BaseShiftMoria -> Item.BASE_SHIFT
        AppPrefs.RecordingsOutputFormat -> Item.RECORDING_FORMAT
        AppPrefs.AnalysisExpectedMelody, AppPrefs.AnalysisModeKey, AppPrefs.AnalysisStartPhthong -> Item.ANALYSIS
        AppPrefs.LecternPageModes -> Item.LECTERN_PAGES
        else -> null
    }

    // ---- which stored names travel, and in what form ---------------------------------------------

    /** The registry key behind [storedName] in [store] when the file may carry it; null otherwise. */
    fun exportedKey(store: AppPrefs.Store, storedName: String): AppPrefs.Key? {
        val key = AppPrefs.all.firstOrNull { it.store == store && isNameOf(it, storedName) } ?: return null
        return when (key.export) {
            AppPrefs.Export.YES -> key
            AppPrefs.Export.NO -> null
            AppPrefs.Export.HYMN_CONTEXTS_ONLY -> key.takeIf { isHymnContext(analysisContext(storedName)) }
        }
    }

    /** Whether [storedName] is [key]'s own name, or a member of its family. */
    private fun isNameOf(key: AppPrefs.Key, storedName: String): Boolean = when (key) {
        AppPrefs.BaseShiftMoria -> storedName.startsWith(AppPrefs.BASE_SHIFT_KEY_PREFIX) &&
            Mode.fromKey(storedName.removePrefix(AppPrefs.BASE_SHIFT_KEY_PREFIX)) != null
        AppPrefs.AnalysisExpectedMelody -> storedName.endsWith(AppPrefs.ANALYSIS_EXPECTED_SUFFIX)
        AppPrefs.AnalysisModeKey -> storedName.endsWith(AppPrefs.ANALYSIS_MODE_SUFFIX)
        AppPrefs.AnalysisStartPhthong -> storedName.endsWith(AppPrefs.ANALYSIS_START_SUFFIX)
        // Named by the PDF's SHA-256 (LecternFileKey), so the name itself can carry nothing else.
        AppPrefs.LecternPageModes -> storedName.startsWith(AppPrefs.LECTERN_PAGES_KEY_PREFIX) &&
            LecternFileKey.isSha256Hex(storedName.removePrefix(AppPrefs.LECTERN_PAGES_KEY_PREFIX))
        else -> storedName == key.name
    }

    /** `hymn:<mode>:<code>`, as `AnalysisSettingsStore.hymnKey` builds it; codes are digits (locked by H7). */
    private val HYMN_CONTEXT = Regex("""hymn:([a-z_]+):(\d+)""")

    private fun analysisContext(storedName: String): String = storedName.substringBeforeLast('|')

    private fun isHymnContext(context: String): Boolean =
        HYMN_CONTEXT.matchEntire(context)?.let { Mode.fromKey(it.groupValues[1]) != null } ?: false

    /**
     * The value the app itself uses when it reads [value] under [key], or null when it cannot use it
     * (the key then falls back to its default, so nothing is exported for it).
     */
    internal fun normalized(key: AppPrefs.Key, value: Any): Any? = when (key) {
        AppPrefs.FontStep -> (value as? Int)?.let(AppFontScale::normalizeStep)
        AppPrefs.LanguageCode -> (value as? String)?.let(AppLanguage::normalizeLanguageCode)
        AppPrefs.ThemeMode -> (value as? String)?.let { AppThemeMode.fromStored(it).storedValue }
        AppPrefs.MetronomeBpm -> (value as? Int)?.let(MetronomeSchedule::clampBpm)
        AppPrefs.MetronomeVibrate, AppPrefs.MetronomeSilent, AppPrefs.MetronomeFootMode,
        AppPrefs.IsonInBackground -> value as? Boolean
        // Ids the path no longer has count for nothing (LearningPath.completedCount ignores them).
        AppPrefs.LearningCompletedStepIds -> stringSet(value)?.filter(LearningPath::isStep)?.toSet()
        // The registry keeps unknown topic ids on purpose: a star survives a topic briefly missing.
        AppPrefs.FavoriteTopicIds -> stringSet(value)?.filter { it.isNotBlank() }?.toSet()
        AppPrefs.SelectedModeKey, AppPrefs.AnalysisModeKey -> Mode.fromKey(value as? String)?.key
        AppPrefs.SelectedToneTimbre -> ToneTimbre.entries.firstOrNull { it.name == value }?.name
        AppPrefs.BaseShiftMoria -> (value as? Int)?.let(BaseShift::clamp)
        AppPrefs.RecordingsOutputFormat -> (value as? String)?.let { RecordingFormatOption.fromStoredValue(it).name }
        AppPrefs.AnalysisExpectedMelody -> (value as? String)?.let { StoredPhthongs.encodeList(StoredPhthongs.decodeList(it)) }
        AppPrefs.AnalysisStartPhthong -> StoredPhthongs.decode(value as? String)?.let(StoredPhthongs::encode)
        // The canonical text of a readable, non-empty map; its shifts follow BaseShift like the 8 Ήχοι's.
        AppPrefs.LecternPageModes -> (value as? String)?.let(LecternPagesCodec::normalized)
        // A key the file may not carry has no value in it.
        else -> null
    }

    private fun stringSet(value: Any): Set<String>? =
        (value as? Set<*>)?.takeIf { set -> set.all { it is String } }?.map { it as String }?.toSet()

    private fun carriesUri(name: String, value: Any): Boolean {
        val strings = when (value) {
            is String -> listOf(value)
            is Set<*> -> value.map { it.toString() }
            else -> emptyList()
        }
        return (strings + name).any { "://" in it }
    }

    private fun toJson(value: Any): Any = when (value) {
        is Set<*> -> JSONArray(value.map { it.toString() }.sorted())
        else -> value
    }

    /** The typed value of [entry] when its declared type is [type] and its value is of that type. */
    private fun fromJson(entry: JSONObject, type: AppPrefs.Type): Any? {
        if (entry.opt("type") != type.name) return null
        val raw = entry.opt("value")
        return when (type) {
            AppPrefs.Type.INT -> (raw as? Int) ?: (raw as? Long)?.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt()
            AppPrefs.Type.LONG -> (raw as? Long) ?: (raw as? Int)?.toLong()
            AppPrefs.Type.BOOLEAN -> raw as? Boolean
            AppPrefs.Type.STRING -> raw as? String
            AppPrefs.Type.STRING_SET -> (raw as? JSONArray)?.let { array ->
                val items = (0 until array.length()).map { array.opt(it) }
                items.takeIf { list -> list.all { it is String } }?.map { it as String }?.toSet()
            }
        }
    }
}
