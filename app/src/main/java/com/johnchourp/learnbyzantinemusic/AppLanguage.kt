package com.johnchourp.learnbyzantinemusic

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import java.util.Locale

object AppLanguage {
    const val languageGreek = "el"
    const val languageEnglish = "en"

    private const val prefsName = "learn_byzantine_music_settings"
    private const val prefsLanguageCodeKey = "app_language_code"
    private const val prefsLanguageOnboardingCompletedKey = "app_language_onboarding_completed"

    private val supportedLanguages = setOf(languageGreek, languageEnglish)

    fun normalizeLanguageCode(rawCode: String?): String =
        if (rawCode in supportedLanguages) rawCode!! else languageGreek

    fun getSavedLanguageCode(context: Context): String {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val raw = prefs.getString(prefsLanguageCodeKey, languageGreek)
        return normalizeLanguageCode(raw)
    }

    fun saveLanguageCode(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit().putString(prefsLanguageCodeKey, normalizeLanguageCode(languageCode)).apply()
    }

    fun isLanguageOnboardingCompleted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        return prefs.getBoolean(prefsLanguageOnboardingCompletedKey, false)
    }

    fun setLanguageOnboardingCompleted(context: Context, completed: Boolean) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(prefsLanguageOnboardingCompletedKey, completed).apply()
    }

    fun getNativeLanguageName(languageCode: String): String =
        when (normalizeLanguageCode(languageCode)) {
            languageEnglish -> "English"
            else -> "Ελληνικά"
        }

    fun wrapContextWithLocale(baseContext: Context?, languageCode: String? = null): Context? {
        if (baseContext == null) {
            return null
        }

        val safeLanguageCode = normalizeLanguageCode(languageCode ?: getSavedLanguageCode(baseContext))
        val locale = Locale(safeLanguageCode)
        Locale.setDefault(locale)

        val configuration = Configuration(baseContext.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return baseContext.createConfigurationContext(configuration)
    }

    fun getLocalizedString(
        context: Context,
        languageCode: String,
        @StringRes stringRes: Int,
        vararg formatArgs: Any,
    ): String {
        val localizedContext = wrapContextWithLocale(context, languageCode) ?: context
        return if (formatArgs.isEmpty()) {
            localizedContext.getString(stringRes)
        } else {
            localizedContext.getString(stringRes, *formatArgs)
        }
    }
}
