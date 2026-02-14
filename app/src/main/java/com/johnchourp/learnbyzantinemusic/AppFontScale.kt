package com.johnchourp.learnbyzantinemusic

import android.content.Context
import android.content.res.Configuration
import kotlin.math.abs

object AppFontScale {
    private const val PREFS_NAME = "learn_byzantine_music_settings"
    private const val PREF_FONT_STEP_KEY = "app_font_step"

    private val allowedSteps = intArrayOf(20, 40, 60, 80, 100)
    const val defaultStep = 60

    fun normalizeStep(rawStep: Int): Int =
        allowedSteps.minByOrNull { abs(it - rawStep) } ?: defaultStep

    fun getSavedStep(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getInt(PREF_FONT_STEP_KEY, defaultStep)
        return normalizeStep(raw)
    }

    fun saveStep(context: Context, step: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(PREF_FONT_STEP_KEY, normalizeStep(step)).apply()
    }

    fun stepToFontScale(step: Int): Float =
        when (normalizeStep(step)) {
            20 -> 0.80f
            40 -> 0.90f
            60 -> 1.00f
            80 -> 1.10f
            100 -> 1.20f
            else -> 1.00f
        }

    fun stepToSeekBarIndex(step: Int): Int {
        val normalized = normalizeStep(step)
        return allowedSteps.indexOf(normalized).coerceAtLeast(0)
    }

    fun seekBarIndexToStep(index: Int): Int {
        val safeIndex = index.coerceIn(0, allowedSteps.lastIndex)
        return allowedSteps[safeIndex]
    }

    fun wrapContextWithFontScale(baseContext: Context?): Context? {
        if (baseContext == null) {
            return null
        }

        val step = getSavedStep(baseContext)
        val configuration = Configuration(baseContext.resources.configuration)
        configuration.fontScale = stepToFontScale(step)
        return baseContext.createConfigurationContext(configuration)
    }
}
