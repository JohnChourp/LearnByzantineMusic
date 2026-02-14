package com.johnchourp.learnbyzantinemusic

import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView

class SettingsActivity : BaseActivity() {
    private lateinit var fontSizeSeekBar: SeekBar
    private lateinit var fontSizeValueTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_settings)

        fontSizeSeekBar = findViewById(R.id.font_size_seek_bar)
        fontSizeValueTextView = findViewById(R.id.font_size_value_text_view)

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
    }

    private fun updateValueLabel(step: Int) {
        fontSizeValueTextView.text = getString(R.string.settings_font_size_value, step)
    }
}
