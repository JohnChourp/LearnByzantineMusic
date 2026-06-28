package com.johnchourp.learnbyzantinemusic

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.johnchourp.learnbyzantinemusic.settings.ui.LanguagePrompt
import com.johnchourp.learnbyzantinemusic.settings.ui.SettingsScreen
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme

/**
 * App settings page. State holder for the redesigned Compose [SettingsScreen]: it owns the
 * applied font step and language, persists them, and performs the two side effects the screen
 * cannot — recreating the Activity so a new font scale takes effect, and restarting the app when
 * the language changes. The previous XML layout (`layout_settings.xml`) was replaced by the
 * Compose screen; the persistence helpers ([AppFontScale] / [AppLanguage]) are unchanged.
 */
class SettingsActivity : BaseActivity() {
    private var appliedFontStep by mutableIntStateOf(AppFontScale.defaultStep)
    private var languageCode by mutableStateOf(AppLanguage.languageGreek)
    private var languagePrompt by mutableStateOf<LanguagePrompt?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appliedFontStep = AppFontScale.getSavedStep(this)
        languageCode = AppLanguage.getSavedLanguageCode(this)

        setContent {
            LbmTheme {
                SettingsScreen(
                    appliedFontStep = appliedFontStep,
                    currentLanguageCode = languageCode,
                    languagePrompt = languagePrompt,
                    onApplyFontStep = ::applyFontStep,
                    onLanguageSelected = ::requestLanguageChange,
                    onConfirmLanguage = ::confirmLanguageChange,
                    onDismissLanguagePrompt = { languagePrompt = null },
                    onBack = ::finish,
                )
            }
        }
    }

    /** Persist the chosen font size and recreate so the new fontScale is applied app-wide. */
    private fun applyFontStep(step: Int) {
        val normalized = AppFontScale.normalizeStep(step)
        if (normalized == AppFontScale.getSavedStep(this)) {
            return
        }
        AppFontScale.saveStep(this, normalized)
        recreate()
    }

    /**
     * Show the confirmation dialog for switching to [targetLanguageCode]. The prompt is resolved
     * in the *target* language (so picking «English» shows it in English, and vice-versa),
     * matching the previous behaviour. Selecting the already-current language is a no-op.
     */
    private fun requestLanguageChange(targetLanguageCode: String) {
        val currentLanguageCode = AppLanguage.getSavedLanguageCode(this)
        if (targetLanguageCode == currentLanguageCode) {
            return
        }

        val languageName = AppLanguage.getNativeLanguageName(targetLanguageCode)
        languagePrompt = LanguagePrompt(
            targetCode = targetLanguageCode,
            title = AppLanguage.getLocalizedString(
                context = this,
                languageCode = targetLanguageCode,
                stringRes = R.string.language_change_confirm_title,
            ),
            message = AppLanguage.getLocalizedString(
                context = this,
                languageCode = targetLanguageCode,
                stringRes = R.string.language_change_confirm_message,
                languageName,
            ),
            confirmLabel = AppLanguage.getLocalizedString(
                context = this,
                languageCode = targetLanguageCode,
                stringRes = R.string.language_change_confirm_accept,
            ),
            dismissLabel = AppLanguage.getLocalizedString(
                context = this,
                languageCode = targetLanguageCode,
                stringRes = R.string.language_change_confirm_cancel,
            ),
        )
    }

    private fun confirmLanguageChange() {
        val target = languagePrompt?.targetCode ?: return
        languagePrompt = null
        AppLanguage.saveLanguageCode(this, target)
        AppLanguage.setLanguageOnboardingCompleted(this, true)
        restartToMainActivity()
    }

    private fun restartToMainActivity() {
        val restartIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(restartIntent)
        finish()
    }
}
