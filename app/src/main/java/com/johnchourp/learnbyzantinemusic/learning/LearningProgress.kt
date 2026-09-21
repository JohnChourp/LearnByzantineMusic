package com.johnchourp.learnbyzantinemusic.learning

import android.content.Context

/**
 * Local, per-device record of which [LearningPath] steps have been opened.
 *
 * Stored in the app's existing settings preferences file so no new storage surface appears.
 * There is no account and no backend: progress never leaves the device, and uninstalling
 * the app clears it.
 */
object LearningProgress {
    private const val PREFS_NAME = "learn_byzantine_music_settings"
    private const val PREF_COMPLETED_STEPS_KEY = "learning_completed_step_ids"

    fun completedSteps(context: Context): Set<String> =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(PREF_COMPLETED_STEPS_KEY, emptySet())
            ?.toSet()
            ?: emptySet()

    /** Records [stepId] as done. Non-path ids are ignored so unrelated tiles cannot pollute the store. */
    fun markCompleted(context: Context, stepId: String) {
        if (!LearningPath.isStep(stepId)) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // getStringSet may hand back the live instance, so copy before mutating.
        val updated = completedSteps(context) + stepId
        prefs.edit().putStringSet(PREF_COMPLETED_STEPS_KEY, updated).apply()
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(PREF_COMPLETED_STEPS_KEY).apply()
    }
}
