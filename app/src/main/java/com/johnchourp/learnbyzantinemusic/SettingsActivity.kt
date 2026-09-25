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
import com.johnchourp.learnbyzantinemusic.ui.theme.AppThemeMode
import androidx.compose.runtime.collectAsState
import com.johnchourp.learnbyzantinemusic.modes.IsonPlaybackService
import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
import com.johnchourp.learnbyzantinemusic.voice.GlobalShift
import com.johnchourp.learnbyzantinemusic.voice.VoiceMicrophone

/**
 * App settings page. State holder for the redesigned Compose [SettingsScreen]: it owns the
 * applied font step and language, persists them, and performs the two side effects the screen
 * cannot — recreating the Activity so a new font scale takes effect, and restarting the app when
 * the language changes. The previous XML layout (`layout_settings.xml`) was replaced by the
 * Compose screen; the persistence helpers ([AppFontScale] / [AppLanguage]) are unchanged.
 *
 * **«Φωνή»** (ClickUp `869f5x2dd`): «Βρες τη φωνή σου» is always here, and this page owns the
 * microphone it listens with ([VoiceMicrophone]) — asked for only on «Ξεκίνα», released on onStop.
 * An accepted suggestion becomes the voice's global shift ([GlobalShift]); «Μηδενισμός» sets it
 * back to 0. A background ison is stopped when the test starts listening, so it measures only the
 * singer. Taken from here, the test counts as offered, and the 8 Ήχοι page does not offer it again.
 */
class SettingsActivity : BaseActivity() {
    private var appliedFontStep by mutableIntStateOf(AppFontScale.defaultStep)
    private var languageCode by mutableStateOf(AppLanguage.languageGreek)
    private var languagePrompt by mutableStateOf<LanguagePrompt?>(null)
    private var globalShiftMoria by mutableIntStateOf(BaseShift.DEFAULT_MORIA)
    private var voiceTestOpen by mutableStateOf(false)

    /** Built with the activity, which lets it register its permission request (see its KDoc). */
    private val microphone = VoiceMicrophone(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appliedFontStep = AppFontScale.getSavedStep(this)
        languageCode = AppLanguage.getSavedLanguageCode(this)
        globalShiftMoria = GlobalShift.load(this)

        setContent {
            val backgroundIson by IsonPlaybackService.playing.collectAsState()
            LbmTheme(palette = currentPalette()) {
                SettingsScreen(
                    appliedFontStep = appliedFontStep,
                    currentLanguageCode = languageCode,
                    languagePrompt = languagePrompt,
                    onApplyFontStep = ::applyFontStep,
                    currentThemeMode = AppThemeMode.saved(this),
                    onThemeModeSelected = ::applyThemeMode,
                    onLanguageSelected = ::requestLanguageChange,
                    onConfirmLanguage = ::confirmLanguageChange,
                    onDismissLanguagePrompt = { languagePrompt = null },
                    onBack = ::finish,
                    globalShiftMoria = globalShiftMoria,
                    onFindVoice = { voiceTestOpen = true },
                    onResetVoice = { applyGlobalShift(BaseShift.DEFAULT_MORIA) },
                    voiceTestOpen = voiceTestOpen,
                    heardFrequencyHz = microphone.heardFrequencyHz,
                    micDenied = microphone.denied,
                    isonWillStop = backgroundIson != null,
                    onVoiceListen = ::listenForVoice,
                    onApplyGlobalShift = ::applyGlobalShift,
                    onCloseVoiceTest = ::closeVoiceTest,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        microphone.onStart()
    }

    override fun onStop() {
        // Released, not paused: a capture left running would hold the microphone from every other app.
        microphone.onStop()
        super.onStop()
    }

    /** The test listens, or stops; a background ison would be measured with the singer, so it stops. */
    private fun listenForVoice(on: Boolean) {
        if (on) IsonPlaybackService.stop(this)
        microphone.listen(on)
    }

    private fun applyGlobalShift(moria: Int) {
        GlobalShift.save(this, moria)
        globalShiftMoria = BaseShift.clamp(moria)
    }

    /** However it ended, the test was seen: the 8 Ήχοι page need not offer it again. */
    private fun closeVoiceTest() {
        voiceTestOpen = false
        AppPrefs.open(this, AppPrefs.Store.EIGHT_MODES).edit()
            .putBoolean(AppPrefs.VoiceRangeOffered.name, true)
            .apply()
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
    /**
     * Saves the theme and restarts this screen so it takes effect immediately.
     *
     * The restart is not laziness: the night flag is resolved in `BaseActivity.attachBaseContext`,
     * before any resource is inflated, and an Activity that is already attached cannot be
     * re-attached. A language change restarts for exactly the same reason.
     */
    private fun applyThemeMode(mode: AppThemeMode) {
        if (mode == AppThemeMode.saved(this)) return
        AppThemeMode.save(this, mode)
        recreate()
    }

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
