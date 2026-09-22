package com.johnchourp.learnbyzantinemusic

import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import java.util.Locale

object AppLanguage {
    const val languageGreek = "el"
    const val languageEnglish = "en"


    private val supportedLanguages = setOf(languageGreek, languageEnglish)

    fun normalizeLanguageCode(rawCode: String?): String =
        if (rawCode in supportedLanguages) rawCode!! else languageGreek

    fun getSavedLanguageCode(context: Context): String {
        val prefs = AppPrefs.open(context, AppPrefs.Store.SETTINGS)
        val raw = prefs.getString(AppPrefs.LanguageCode.name, languageGreek)
        return normalizeLanguageCode(raw)
    }

    fun saveLanguageCode(context: Context, languageCode: String) {
        val prefs = AppPrefs.open(context, AppPrefs.Store.SETTINGS)
        prefs.edit().putString(AppPrefs.LanguageCode.name, normalizeLanguageCode(languageCode)).apply()
    }

    fun isLanguageOnboardingCompleted(context: Context): Boolean {
        val prefs = AppPrefs.open(context, AppPrefs.Store.SETTINGS)
        return prefs.getBoolean(AppPrefs.LanguageOnboardingCompleted.name, false)
    }

    fun setLanguageOnboardingCompleted(context: Context, completed: Boolean) {
        val prefs = AppPrefs.open(context, AppPrefs.Store.SETTINGS)
        prefs.edit().putBoolean(AppPrefs.LanguageOnboardingCompleted.name, completed).apply()
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
