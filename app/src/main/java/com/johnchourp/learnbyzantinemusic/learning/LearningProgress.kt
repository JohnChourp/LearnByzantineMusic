package com.johnchourp.learnbyzantinemusic.learning

import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
import android.content.Context

/**
 * Local, per-device record of which [LearningPath] steps have been opened.
 *
 * Stored in the app's existing settings preferences file so no new storage surface appears.
 * There is no account and no backend: progress never leaves the device, and uninstalling
 * the app clears it.
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
