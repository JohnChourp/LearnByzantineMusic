package com.johnchourp.learnbyzantinemusic

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.johnchourp.learnbyzantinemusic.learning.LearningPath
import com.johnchourp.learnbyzantinemusic.learning.LearningProgress
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile
import com.johnchourp.learnbyzantinemusic.settings.LearningDataPrefs
import com.johnchourp.learnbyzantinemusic.settings.OpenLearningDataFile
import com.johnchourp.learnbyzantinemusic.settings.SaveLearningDataFile
import com.johnchourp.learnbyzantinemusic.settings.ui.LanguagePrompt
import com.johnchourp.learnbyzantinemusic.settings.ui.LearningDataPrompt
import com.johnchourp.learnbyzantinemusic.settings.ui.SettingsScreen
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme
import com.johnchourp.learnbyzantinemusic.ui.theme.AppThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date

/**
 * App settings page. State holder for the redesigned Compose [SettingsScreen]: it owns the
 * applied font step and language, persists them, and performs the two side effects the screen
 * cannot — recreating the Activity so a new font scale takes effect, and restarting the app when
 * the language changes. The previous XML layout (`layout_settings.xml`) was replaced by the
 * Compose screen; the persistence helpers ([AppFontScale] / [AppLanguage]) are unchanged.
 *
 * **«Δεδομένα μάθησης»** (ClickUp `869f5x25w`): export and import of one file with the favourites,
 * the «Από το μηδέν» progress and the settings, through the system pickers, and «Μηδενισμός
 * προόδου». The file's rules are in [LearningDataFile]: an import is checked whole, confirmed with
 * a list of what will change, written only for the keys it carries, and followed by a restart so
 * language and theme apply.
 */
class SettingsActivity : BaseActivity() {
    private var appliedFontStep by mutableIntStateOf(AppFontScale.defaultStep)
    private var languageCode by mutableStateOf(AppLanguage.languageGreek)
    private var languagePrompt by mutableStateOf<LanguagePrompt?>(null)
    private var learningDataPrompt by mutableStateOf<LearningDataPrompt?>(null)
    private var onLearningDataConfirmed: (() -> Unit)? = null

    private val saveLearningDataFile = registerForActivityResult(SaveLearningDataFile()) { uri ->
        if (uri != null) exportLearningData(uri)
    }

    private val openLearningDataFile = registerForActivityResult(OpenLearningDataFile()) { uri ->
        if (uri != null) readLearningData(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appliedFontStep = AppFontScale.getSavedStep(this)
        languageCode = AppLanguage.getSavedLanguageCode(this)

        setContent {
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
                    learningDataPrompt = learningDataPrompt,
                    onExportLearningData = {
                        saveLearningDataFile.launch(
                            SaveLearningDataFile.Request(
                                fileName = LearningDataFile.suggestedFileName(LocalDate.now()),
                                folder = LearningDataPrefs.startFolder(this),
                            )
                        )
                    },
                    onImportLearningData = { openLearningDataFile.launch(LearningDataPrefs.startFolder(this)) },
                    onResetProgress = ::askToResetProgress,
                    onConfirmLearningData = ::confirmLearningDataPrompt,
                    onDismissLearningData = ::dismissLearningDataPrompt,
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

    // ---- «Δεδομένα μάθησης» ----------------------------------------------------------------------

    private fun exportLearningData(uri: Uri) {
        val file = LearningDataFile.encode(
            snapshots = LearningDataPrefs.snapshot(this),
            exportedAtEpochMs = System.currentTimeMillis(),
            appVersion = BuildConfig.VERSION_NAME,
        )
        lifecycleScope.launch {
            val bytes = file.toByteArray(Charsets.UTF_8)
            val written = withContext(Dispatchers.IO) {
                // Some providers refuse "wt"; the picker created a new, empty document, so "w" is as good.
                runCatching { writeTo(uri, "wt", bytes) }.recoverCatching { writeTo(uri, "w", bytes) }.getOrDefault(false)
            }
            Toast.makeText(
                this@SettingsActivity,
                if (written) R.string.learning_data_export_done else R.string.learning_data_export_failed,
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun writeTo(uri: Uri, mode: String, bytes: ByteArray): Boolean =
        contentResolver.openOutputStream(uri, mode)?.use { it.write(bytes); true } ?: false

    private fun readLearningData(uri: Uri) {
        lifecycleScope.launch {
            val text = withContext(Dispatchers.IO) { runCatching { readAtMost(uri) }.getOrNull() }
            if (text == null) {
                showLearningDataNotice(getString(R.string.learning_data_import_read_failed))
                return@launch
            }
            when (val result = LearningDataFile.decode(text)) {
                is LearningDataFile.Result.Rejected -> showLearningDataNotice(rejectionMessage(result))
                is LearningDataFile.Result.Accepted ->
                    if (result.changes.isEmpty()) {
                        showLearningDataNotice(getString(R.string.learning_data_import_empty))
                    } else {
                        askToImport(result)
                    }
            }
        }
    }

    /** The file's text, cut one character past the limit so [LearningDataFile.decode] refuses a huge one. */
    private fun readAtMost(uri: Uri): String? =
        contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
            val buffer = CharArray(LearningDataFile.MAX_CHARS + 1)
            var length = 0
            while (length < buffer.size) {
                val read = reader.read(buffer, length, buffer.size - length)
                if (read < 0) break
                length += read
            }
            String(buffer, 0, length)
        }

    private fun askToImport(accepted: LearningDataFile.Result.Accepted) {
        val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(accepted.exportedAtEpochMs))
        val lines = LearningDataFile.summary(accepted).joinToString("\n") { "• " + describe(it) }
        showLearningDataPrompt(
            LearningDataPrompt(
                title = getString(R.string.learning_data_import_title),
                message = getString(R.string.learning_data_import_intro, accepted.appVersion, date) +
                    "\n\n" + lines + "\n\n" + getString(R.string.learning_data_import_outro),
                confirmLabel = getString(R.string.settings_learning_data_import),
                dismissLabel = getString(R.string.learning_data_cancel),
            )
        ) {
            lifecycleScope.launch {
                val written = withContext(Dispatchers.IO) {
                    LearningDataFile.write(accepted, LearningDataPrefs.writer(this@SettingsActivity))
                }
                if (written) {
                    restartToMainActivity()
                } else {
                    showLearningDataNotice(
                        getString(R.string.learning_data_import_write_failed),
                        title = getString(R.string.learning_data_import_unfinished_title),
                    )
                }
            }
        }
    }

    private fun describe(line: LearningDataFile.Line): String = when (line.item) {
        LearningDataFile.Item.FONT_SIZE -> getString(R.string.learning_data_item_font_size)
        LearningDataFile.Item.LANGUAGE -> getString(R.string.learning_data_item_language)
        LearningDataFile.Item.THEME -> getString(R.string.learning_data_item_theme)
        LearningDataFile.Item.METRONOME -> getString(R.string.learning_data_item_metronome)
        LearningDataFile.Item.FAVOURITES -> getString(R.string.learning_data_item_favourites, line.count)
        LearningDataFile.Item.PROGRESS -> getString(R.string.learning_data_item_progress, line.count, LearningPath.size)
        LearningDataFile.Item.SELECTED_MODE -> getString(R.string.learning_data_item_selected_mode)
        LearningDataFile.Item.TIMBRE -> getString(R.string.learning_data_item_timbre)
        LearningDataFile.Item.ISON_BACKGROUND -> getString(R.string.learning_data_item_ison_background)
        LearningDataFile.Item.BASE_SHIFT -> getString(R.string.learning_data_item_base_shift, line.count)
        LearningDataFile.Item.RECORDING_FORMAT -> getString(R.string.learning_data_item_recording_format)
        LearningDataFile.Item.ANALYSIS -> getString(R.string.learning_data_item_analysis, line.count)
        LearningDataFile.Item.TRAINER_EXERCISES -> getString(R.string.learning_data_item_trainer_exercises, line.count)
        LearningDataFile.Item.TRAINER_LAST_MELODY -> getString(R.string.learning_data_item_trainer_last_melody)
    }

    private fun rejectionMessage(rejected: LearningDataFile.Result.Rejected): String = when (rejected.reason) {
        LearningDataFile.Reason.NOT_A_LEARNING_DATA_FILE -> getString(R.string.learning_data_import_not_a_file)
        LearningDataFile.Reason.NEWER_VERSION -> getString(R.string.learning_data_import_newer)
        LearningDataFile.Reason.NOT_IMPORTABLE ->
            getString(R.string.learning_data_import_not_importable, rejected.detail.orEmpty())
        LearningDataFile.Reason.BAD_VALUE -> getString(R.string.learning_data_import_bad_value, rejected.detail.orEmpty())
    }

    private fun askToResetProgress() {
        showLearningDataPrompt(
            LearningDataPrompt(
                title = getString(R.string.learning_data_reset_title),
                message = getString(R.string.learning_data_reset_message),
                confirmLabel = getString(R.string.learning_data_reset_confirm),
                dismissLabel = getString(R.string.learning_data_cancel),
            )
        ) {
            LearningProgress.reset(this)
            Toast.makeText(this, R.string.learning_data_reset_done, Toast.LENGTH_SHORT).show()
        }
    }

    /** A notice with one button: by default, that the import did not happen, and why. */
    private fun showLearningDataNotice(
        message: String,
        title: String = getString(R.string.learning_data_import_failed_title),
    ) {
        showLearningDataPrompt(
            LearningDataPrompt(
                title = title,
                message = message,
                confirmLabel = getString(R.string.learning_data_ok),
                dismissLabel = null,
            )
        ) {}
    }

    private fun showLearningDataPrompt(prompt: LearningDataPrompt, onConfirm: () -> Unit) {
        onLearningDataConfirmed = onConfirm
        learningDataPrompt = prompt
    }

    private fun confirmLearningDataPrompt() {
        val action = onLearningDataConfirmed
        dismissLearningDataPrompt()
        action?.invoke()
    }

    private fun dismissLearningDataPrompt() {
        learningDataPrompt = null
        onLearningDataConfirmed = null
    }

    private fun restartToMainActivity() {
        val restartIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(restartIntent)
        finish()
    }
}
