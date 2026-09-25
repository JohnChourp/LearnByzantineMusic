package com.johnchourp.learnbyzantinemusic.recordings.analysis

import android.content.Context
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs

/**
 * Remembers, per analysis context, the expected melody the user typed and the mode / starting
 * phthong they chose. The context is a hymn (`hymn:<mode>:<code>`, shared by all its
 * recordings) or a single recording (`recording:<uri>`).
 *
 * The file and every key name come from [AppPrefs]: nothing here names a preference as a literal,
 * so a typo cannot silently write to a key nobody reads. The φθόγγοι are written and read by
 * [StoredPhthongs], whose format is frozen because it is already on users' devices.
 */
class AnalysisSettingsStore(context: Context) {
    private val prefs = AppPrefs.open(context.applicationContext, AppPrefs.Store.RECORDING_ANALYSIS)

    fun expected(contextKey: String): List<PhthongName> =
        StoredPhthongs.decodeList(prefs.getString(AppPrefs.analysisExpectedKeyName(contextKey), null))

    fun saveExpected(contextKey: String, phthongs: List<PhthongName>) {
        prefs.edit()
            .putString(AppPrefs.analysisExpectedKeyName(contextKey), StoredPhthongs.encodeList(phthongs))
            .apply()
    }

    fun modeKey(contextKey: String): String? = prefs.getString(AppPrefs.analysisModeKeyName(contextKey), null)

    fun startPhthong(contextKey: String): PhthongName? =
        StoredPhthongs.decode(prefs.getString(AppPrefs.analysisStartKeyName(contextKey), null))

    fun saveScale(contextKey: String, modeKey: String, start: PhthongName) {
        prefs.edit()
            .putString(AppPrefs.analysisModeKeyName(contextKey), modeKey)
            .putString(AppPrefs.analysisStartKeyName(contextKey), StoredPhthongs.encode(start))
            .apply()
    }

    companion object {
        /**
         * The mode an analysis opens on: the one stored for its context, else the one it was opened
         * with (a hymn's mode), else Α΄ — today's order (ClickUp `869f5x299`).
         *
         * A stored value that names no mode — a corrupted or hand-edited preference — is skipped, never
         * thrown on: the analysis must still open. What changed is only what happens next: such a key
         * used to reach the scale lookup and turn silently into the diatonic scale from Νη.
         */
        fun resolveMode(stored: String?, requested: String?): Mode =
            Mode.fromKey(stored) ?: Mode.fromKey(requested) ?: Mode.FIRST

        fun hymnKey(modeKey: String, hymnCode: String) = "hymn:$modeKey:$hymnCode"

        fun recordingKey(uri: String) = "recording:$uri"
    }
}
