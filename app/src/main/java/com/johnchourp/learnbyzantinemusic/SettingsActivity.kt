package com.johnchourp.learnbyzantinemusic

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView

class SettingsActivity : BaseActivity() {
    private lateinit var fontSizeSeekBar: SeekBar
    private lateinit var fontSizeValueTextView: TextView
    private lateinit var languageCurrentTextView: TextView
    private lateinit var languageGreekButton: Button
    private lateinit var languageEnglishButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_settings)

        fontSizeSeekBar = findViewById(R.id.font_size_seek_bar)
        fontSizeValueTextView = findViewById(R.id.font_size_value_text_view)
        languageCurrentTextView = findViewById(R.id.settings_language_current_text_view)
        languageGreekButton = findViewById(R.id.settings_language_greek_button)
        languageEnglishButton = findViewById(R.id.settings_language_english_button)

        fontSizeSeekBar.max = 4

        val savedStep = AppFontScale.getSavedStep(this)
        val initialIndex = AppFontScale.stepToSeekBarIndex(savedStep)
        fontSizeSeekBar.progress = initialIndex
        updateValueLabel(savedStep)

        fontSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val selectedStep = AppFontScale.seekBarIndexToStep(progress)
                updateValueLabel(selectedStep)
                if (!fromUser) {
                    return
                }

                val currentSavedStep = AppFontScale.getSavedStep(this@SettingsActivity)
                if (selectedStep != currentSavedStep) {
                    AppFontScale.saveStep(this@SettingsActivity, selectedStep)
                    recreate()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        languageGreekButton.setOnClickListener {
            promptLanguageChange(AppLanguage.languageGreek)
        }
        languageEnglishButton.setOnClickListener {
            promptLanguageChange(AppLanguage.languageEnglish)
        }

        updateLanguageSection()
    }

    private fun updateValueLabel(step: Int) {
        fontSizeValueTextView.text = getString(R.string.settings_font_size_value, step)
    }

    private fun promptLanguageChange(targetLanguageCode: String) {
        val currentLanguageCode = AppLanguage.getSavedLanguageCode(this)
        if (targetLanguageCode == currentLanguageCode) {
            return
        }

        val languageName = AppLanguage.getNativeLanguageName(targetLanguageCode)
        val title = AppLanguage.getLocalizedString(
            context = this,
            languageCode = targetLanguageCode,
            stringRes = R.string.language_change_confirm_title,
        )
        val message = AppLanguage.getLocalizedString(
            this,
            targetLanguageCode,
            R.string.language_change_confirm_message,
            languageName,
        )
        val acceptLabel = AppLanguage.getLocalizedString(
            context = this,
            languageCode = targetLanguageCode,
            stringRes = R.string.language_change_confirm_accept,
        )
        val cancelLabel = AppLanguage.getLocalizedString(
            context = this,
            languageCode = targetLanguageCode,
            stringRes = R.string.language_change_confirm_cancel,
        )

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(acceptLabel) { _, _ ->
                AppLanguage.saveLanguageCode(this, targetLanguageCode)
                AppLanguage.setLanguageOnboardingCompleted(this, true)
                restartToMainActivity()
            }
            .setNegativeButton(cancelLabel, null)
            .show()
    }

    private fun updateLanguageSection() {
        val currentLanguageCode = AppLanguage.getSavedLanguageCode(this)
        languageCurrentTextView.text = getString(
            R.string.settings_language_current_value,
            AppLanguage.getNativeLanguageName(currentLanguageCode),
        )

        val isGreekSelected = currentLanguageCode == AppLanguage.languageGreek
        languageGreekButton.isEnabled = !isGreekSelected
        languageEnglishButton.isEnabled = isGreekSelected
        languageGreekButton.alpha = if (isGreekSelected) 0.6f else 1f
        languageEnglishButton.alpha = if (isGreekSelected) 1f else 0.6f
    }

    private fun restartToMainActivity() {
        val restartIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(restartIntent)
        finish()
    }
}
