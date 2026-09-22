package com.johnchourp.learnbyzantinemusic.lessons.ui

import android.content.Context
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs

/**
 * Remembers the metronome tempo, in the same pattern as `AppFontScale` (ClickUp `869f4tpg0`).
 *
 * Clamping happens on read as well as on write: a value stored by an older build with a wider range
 * must not be able to drive the slider out of bounds.
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
}
