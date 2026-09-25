package com.johnchourp.learnbyzantinemusic.lessons.ui

import android.content.Context
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs

/**
 * Remembers the metronome tempo, in the same pattern as `AppFontScale` (ClickUp `869f4tpg0`), and
 * its three switches — «Δόνηση», «Σιωπηλά», «Πόδι» (ClickUp `869f5x2d7`).
 *
 * Clamping happens on read as well as on write: a value stored by an older build with a wider range
 * must not be able to drive the slider out of bounds. The switches are stored as chosen; what they
 * do on a device without a vibrator is decided by [MetronomeOptions.effectiveOn], not here.
 */
object MetronomePrefs {

    fun savedBpm(context: Context): Int = MetronomeSchedule.clampBpm(
        AppPrefs.open(context, AppPrefs.Store.SETTINGS)
            .getInt(AppPrefs.MetronomeBpm.name, MetronomeSchedule.DEFAULT_BPM)
    )

    fun saveBpm(context: Context, bpm: Int) {
        AppPrefs.open(context, AppPrefs.Store.SETTINGS)
            .edit()
            .putInt(AppPrefs.MetronomeBpm.name, MetronomeSchedule.clampBpm(bpm))
            .apply()
    }

    fun savedOptions(context: Context): MetronomeOptions {
        val prefs = AppPrefs.open(context, AppPrefs.Store.SETTINGS)
        val defaults = MetronomeOptions()
        return MetronomeOptions(
            vibrate = prefs.getBoolean(AppPrefs.MetronomeVibrate.name, defaults.vibrate),
            silent = prefs.getBoolean(AppPrefs.MetronomeSilent.name, defaults.silent),
            foot = prefs.getBoolean(AppPrefs.MetronomeFootMode.name, defaults.foot),
        )
    }

    fun saveOptions(context: Context, options: MetronomeOptions) {
        AppPrefs.open(context, AppPrefs.Store.SETTINGS)
            .edit()
            .putBoolean(AppPrefs.MetronomeVibrate.name, options.vibrate)
            .putBoolean(AppPrefs.MetronomeSilent.name, options.silent)
            .putBoolean(AppPrefs.MetronomeFootMode.name, options.foot)
            .apply()
    }
}
