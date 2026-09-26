package com.johnchourp.learnbyzantinemusic.voice

import android.content.Context
import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs

/**
 * The voice's global «Μεταφορά βάσης» (ClickUp `869f5x2dd`, J4): one value, stored under
 * [AppPrefs.GlobalBaseShift], that every ladder adds to its mode's own shift through
 * [BaseShift.combined]. Written only when «Βρες τη φωνή σου» is accepted, or reset from Settings;
 * clamped on read and on write, like the per-mode values. 0 means every ladder exactly as before.
 */
object GlobalShift {

    fun load(context: Context): Int =
        BaseShift.clamp(prefs(context).getInt(AppPrefs.GlobalBaseShift.name, BaseShift.DEFAULT_MORIA))

    fun save(context: Context, moria: Int) {
        prefs(context).edit().putInt(AppPrefs.GlobalBaseShift.name, BaseShift.clamp(moria)).apply()
    }

    private fun prefs(context: Context) = AppPrefs.open(context, AppPrefs.Store.SETTINGS)
}
