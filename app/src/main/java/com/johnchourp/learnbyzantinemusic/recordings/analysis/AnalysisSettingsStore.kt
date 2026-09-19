package com.johnchourp.learnbyzantinemusic.recordings.analysis

import android.content.Context
import com.johnchourp.learnbyzantinemusic.trainer.TrainerPhthong

/**
 * Remembers, per analysis context, the expected melody the user typed and the mode / starting
 * phthong they chose. The context is a hymn (`hymn:<mode>:<code>`, shared by all its
 * recordings) or a single recording (`recording:<uri>`).
 */
class AnalysisSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun expected(contextKey: String): List<TrainerPhthong> =
        prefs.getString("$contextKey|expected", null)
            ?.split(',')
            ?.mapNotNull { name -> TrainerPhthong.values().firstOrNull { it.name == name } }
            .orEmpty()

    fun saveExpected(contextKey: String, phthongs: List<TrainerPhthong>) {
        prefs.edit().putString("$contextKey|expected", phthongs.joinToString(",") { it.name }).apply()
    }

    fun modeKey(contextKey: String): String? = prefs.getString("$contextKey|mode", null)

    fun startPhthong(contextKey: String): TrainerPhthong? =
        prefs.getString("$contextKey|start", null)?.let { name -> TrainerPhthong.values().firstOrNull { it.name == name } }

    fun saveScale(contextKey: String, modeKey: String, start: TrainerPhthong) {
        prefs.edit().putString("$contextKey|mode", modeKey).putString("$contextKey|start", start.name).apply()
    }

    companion object {
        private const val PREFS_NAME = "recording_analysis_settings"

        fun hymnKey(modeKey: String, hymnCode: String) = "hymn:$modeKey:$hymnCode"

        fun recordingKey(uri: String) = "recording:$uri"
    }
}
