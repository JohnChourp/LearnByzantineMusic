package com.johnchourp.learnbyzantinemusic.eartrainer

import android.content.Context
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs

/**
 * «Μάντεψε τον ήχο» in the device's preferences — `AppPrefs.Store.EAR_TRAINER`: the scores as
 * [EarTrainerScoresCodec] JSON, and the chosen level (ClickUp `869f4tqb9`). Local only.
 */
class EarTrainerStore(context: Context) {
    private val prefs = AppPrefs.open(context.applicationContext, AppPrefs.Store.EAR_TRAINER)

    fun scores(): EarTrainerScores =
        EarTrainerScoresCodec.decode(prefs.getString(AppPrefs.EarTrainerScoresJson.name, null))

    /** One more answer to a question in [asked], and the scores after it. */
    fun record(asked: Mode, correct: Boolean): EarTrainerScores {
        val updated = scores().record(asked, correct)
        prefs.edit().putString(AppPrefs.EarTrainerScoresJson.name, EarTrainerScoresCodec.encode(updated)).apply()
        return updated
    }

    /** The level last chosen; a missing or unknown value reads as [EarTrainerLevel.DEFAULT]. */
    fun level(): EarTrainerLevel =
        EarTrainerLevel.fromKey(prefs.getString(AppPrefs.EarTrainerChosenLevel.name, null)) ?: EarTrainerLevel.DEFAULT

    fun saveLevel(level: EarTrainerLevel) {
        prefs.edit().putString(AppPrefs.EarTrainerChosenLevel.name, level.key).apply()
    }
}
