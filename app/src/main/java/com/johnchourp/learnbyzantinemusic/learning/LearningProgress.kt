package com.johnchourp.learnbyzantinemusic.learning

import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
import android.content.Context

/**
 * Local, per-device record of which [LearningPath] steps have been opened — opening a step is what
 * counts it as done.
 *
 * Stored in the app's existing settings preferences file so no new storage surface appears.
 * There is no account and no backend. Progress leaves the device only in the two ways the user
 * controls: Android Auto Backup of the settings file (see the backup rules in `res/xml`), and the
 * «Δεδομένα μάθησης» file exported from Ρυθμίσεις (`settings/LearningDataFile`). [reset] is what
 * «Μηδενισμός προόδου» there calls.
 */
object LearningProgress {

    fun completedSteps(context: Context): Set<String> =
        AppPrefs.open(context, AppPrefs.Store.SETTINGS)
            .getStringSet(AppPrefs.LearningCompletedStepIds.name, emptySet())
            ?.toSet()
            ?: emptySet()

    /** Records [stepId] as done. Non-path ids are ignored so unrelated tiles cannot pollute the store. */
    fun markCompleted(context: Context, stepId: String) {
        if (!LearningPath.isStep(stepId)) return
        val prefs = AppPrefs.open(context, AppPrefs.Store.SETTINGS)
        // getStringSet may hand back the live instance, so copy before mutating.
        val updated = completedSteps(context) + stepId
        prefs.edit().putStringSet(AppPrefs.LearningCompletedStepIds.name, updated).apply()
    }

    fun reset(context: Context) {
        AppPrefs.open(context, AppPrefs.Store.SETTINGS)
            .edit().remove(AppPrefs.LearningCompletedStepIds.name).apply()
    }
}
