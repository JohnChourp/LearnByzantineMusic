package com.johnchourp.learnbyzantinemusic.modes

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R
import kotlin.math.pow

class EightModesActivity : BaseActivity() {
    private lateinit var modeSelector: Spinner
    private lateinit var selectedModeType: TextView
    private lateinit var selectedModeApichimaText: TextView
    private lateinit var selectedModeApichimaAlternatives: TextView
    private lateinit var selectedModeApichimaFormsHint: TextView
    private lateinit var selectedModeApichimaPhthongs: TextView
    private lateinit var selectedModeApichimaSyllables: TextView
    private lateinit var selectedModeApichimaAlternativePhthongs: TextView
    private lateinit var selectedModeApichimaAlternativeSyllables: TextView
    private lateinit var selectedModeApichimaSign: ImageView
    private lateinit var selectedModeApichimaSignName: TextView
    private lateinit var selectedModeDetails: TextView
    private lateinit var ascendingDiagramView: ScaleDiagramView
    private lateinit var touchHintText: TextView
    private lateinit var baseShiftValueText: TextView
    private lateinit var baseShiftSeekBar: SeekBar
    private lateinit var baseShiftResetButton: Button
    private lateinit var baseShiftPrefs: SharedPreferences
    private val tonePlayer: PhthongTonePlayer by lazy { PhthongTonePlayer() }
    private val modeBaseShiftMoria: MutableMap<Int, Int> = mutableMapOf()
    private var frequenciesTopToBottom: List<Double> = emptyList()
    private var currentModePosition: Int = 0
    private var currentAscendingIntervalsExtended: List<Int> = emptyList()
    private var isSyncingBaseShiftControls: Boolean = false

    private val modes: List<ModeDefinition> by lazy {
        listOf(
            ModeDefinition(
                nameRes = R.string.mode_first,
                typeRes = R.string.mode_type_diatonic,
                apichimaRes = R.string.mode_apichima_first,
                apichimaAlternativeRes = null,
                apichimaPhthongsRes = R.string.mode_apichima_phthongs_first,
                apichimaSyllablesRes = R.string.mode_apichima_syllables_first,
                apichimaAlternativePhthongsRes = null,
                apichimaAlternativeSyllablesRes = null,
                apichimaSignRes = R.drawable.diatonic_intermediates_testimonial_pa,
                apichimaSignNameRes = R.string.phthong_pa,
                detailsRes = R.string.mode_details_first,
                colorCategory = ModeColorCategory.DIATONIC,
                ascendingIntervals = listOf(12, 10, 8, 12, 12, 10, 8)
            ),
            ModeDefinition(
                nameRes = R.string.mode_second,
                typeRes = R.string.mode_type_hard_chromatic,
                apichimaRes = R.string.mode_apichima_second,
                apichimaAlternativeRes = null,
                apichimaPhthongsRes = R.string.mode_apichima_phthongs_second,
                apichimaSyllablesRes = R.string.mode_apichima_syllables_second,
                apichimaAlternativePhthongsRes = null,
                apichimaAlternativeSyllablesRes = null,
                apichimaSignRes = R.drawable.diatonic_intermediates_testimonial_di,
                apichimaSignNameRes = R.string.phthong_di,
                detailsRes = R.string.mode_details_second,
                colorCategory = ModeColorCategory.HARD_CHROMATIC,
                ascendingIntervals = listOf(6, 20, 4, 12, 6, 20, 4)
            ),
            ModeDefinition(
                nameRes = R.string.mode_third,
                typeRes = R.string.mode_type_diatonic,
                apichimaRes = R.string.mode_apichima_third,
                apichimaAlternativeRes = null,
                apichimaPhthongsRes = R.string.mode_apichima_phthongs_third,
                apichimaSyllablesRes = R.string.mode_apichima_syllables_third,
                apichimaAlternativePhthongsRes = null,
                apichimaAlternativeSyllablesRes = null,
                apichimaSignRes = R.drawable.diatonic_intermediates_testimonial_ga,
                apichimaSignNameRes = R.string.phthong_ga,
                detailsRes = R.string.mode_details_third,
                colorCategory = ModeColorCategory.DIATONIC,
                ascendingIntervals = listOf(8, 12, 12, 10, 8, 12, 10)
            ),
            ModeDefinition(
                nameRes = R.string.mode_fourth,
                typeRes = R.string.mode_type_diatonic,
                apichimaRes = R.string.mode_apichima_fourth,
                apichimaAlternativeRes = null,
                apichimaPhthongsRes = R.string.mode_apichima_phthongs_fourth,
                apichimaSyllablesRes = R.string.mode_apichima_syllables_fourth,
                apichimaAlternativePhthongsRes = null,
                apichimaAlternativeSyllablesRes = null,
                apichimaSignRes = R.drawable.diatonic_intermediates_testimonial_di,
                apichimaSignNameRes = R.string.phthong_di,
                detailsRes = R.string.mode_details_fourth,
                colorCategory = ModeColorCategory.DIATONIC,
                ascendingIntervals = listOf(10, 8, 12, 12, 10, 8, 12)
            ),
            ModeDefinition(
                nameRes = R.string.mode_plagal_first,
                typeRes = R.string.mode_type_diatonic,
                apichimaRes = R.string.mode_apichima_plagal_first,
                apichimaAlternativeRes = null,
                apichimaPhthongsRes = R.string.mode_apichima_phthongs_plagal_first,
                apichimaSyllablesRes = R.string.mode_apichima_syllables_plagal_first,
                apichimaAlternativePhthongsRes = null,
                apichimaAlternativeSyllablesRes = null,
                apichimaSignRes = R.drawable.diatonic_intermediates_testimonial_ke,
                apichimaSignNameRes = R.string.phthong_ke,
                detailsRes = R.string.mode_details_plagal_first,
                colorCategory = ModeColorCategory.DIATONIC,
                ascendingIntervals = listOf(10, 8, 12, 12, 10, 8, 12)
            ),
            ModeDefinition(
                nameRes = R.string.mode_plagal_second,
                typeRes = R.string.mode_type_soft_chromatic,
                apichimaRes = R.string.mode_apichima_plagal_second,
                apichimaAlternativeRes = R.string.mode_apichima_alternative_plagal_second,
                apichimaPhthongsRes = R.string.mode_apichima_phthongs_plagal_second,
                apichimaSyllablesRes = R.string.mode_apichima_syllables_plagal_second,
                apichimaAlternativePhthongsRes = R.string.mode_apichima_alternative_phthongs_plagal_second,
                apichimaAlternativeSyllablesRes = R.string.mode_apichima_alternative_syllables_plagal_second,
                apichimaSignRes = R.drawable.diatonic_intermediates_testimonial_ni,
                apichimaSignNameRes = R.string.phthong_ni,
                detailsRes = R.string.mode_details_plagal_second,
                colorCategory = ModeColorCategory.SOFT_CHROMATIC,
                ascendingIntervals = listOf(8, 14, 8, 12, 8, 14, 8)
            ),
            ModeDefinition(
                nameRes = R.string.mode_varys,
                typeRes = R.string.mode_type_enharmonic,
                apichimaRes = R.string.mode_apichima_varys,
                apichimaAlternativeRes = null,
                apichimaPhthongsRes = R.string.mode_apichima_phthongs_varys,
                apichimaSyllablesRes = R.string.mode_apichima_syllables_varys,
                apichimaAlternativePhthongsRes = null,
                apichimaAlternativeSyllablesRes = null,
                apichimaSignRes = R.drawable.diatonic_filamentous_testimonial_zo,
                apichimaSignNameRes = R.string.phthong_zo,
                detailsRes = R.string.mode_details_varys,
                colorCategory = ModeColorCategory.ENHARMONIC,
                ascendingIntervals = listOf(12, 12, 6, 12, 12, 6, 12)
            ),
            ModeDefinition(
                nameRes = R.string.mode_plagal_fourth,
                typeRes = R.string.mode_type_diatonic,
                apichimaRes = R.string.mode_apichima_plagal_fourth,
                apichimaAlternativeRes = null,
                apichimaPhthongsRes = R.string.mode_apichima_phthongs_plagal_fourth,
                apichimaSyllablesRes = R.string.mode_apichima_syllables_plagal_fourth,
                apichimaAlternativePhthongsRes = null,
                apichimaAlternativeSyllablesRes = null,
                apichimaSignRes = R.drawable.diatonic_filamentous_testimonial_ni,
                apichimaSignNameRes = R.string.phthong_ni,
                detailsRes = R.string.mode_details_plagal_fourth,
                colorCategory = ModeColorCategory.DIATONIC,
                ascendingIntervals = listOf(12, 10, 8, 12, 12, 10, 8)
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_eight_modes)

        modeSelector = findViewById(R.id.mode_selector)
        selectedModeType = findViewById(R.id.selected_mode_type)
        selectedModeApichimaText = findViewById(R.id.selected_mode_apichima_text)
        selectedModeApichimaAlternatives = findViewById(R.id.selected_mode_apichima_alternatives)
        selectedModeApichimaFormsHint = findViewById(R.id.selected_mode_apichima_forms_hint)
        selectedModeApichimaPhthongs = findViewById(R.id.selected_mode_apichima_phthongs)
        selectedModeApichimaSyllables = findViewById(R.id.selected_mode_apichima_syllables)
        selectedModeApichimaAlternativePhthongs =
            findViewById(R.id.selected_mode_apichima_alternative_phthongs)
        selectedModeApichimaAlternativeSyllables =
            findViewById(R.id.selected_mode_apichima_alternative_syllables)
        selectedModeApichimaSign = findViewById(R.id.selected_mode_apichima_sign)
        selectedModeApichimaSignName = findViewById(R.id.selected_mode_apichima_sign_name)
        selectedModeDetails = findViewById(R.id.selected_mode_details)
        ascendingDiagramView = findViewById(R.id.ascending_diagram_view)
        touchHintText = findViewById(R.id.touch_hint_text)
        baseShiftValueText = findViewById(R.id.base_shift_value)
        baseShiftSeekBar = findViewById(R.id.base_shift_seekbar)
        baseShiftResetButton = findViewById(R.id.base_shift_reset_button)
        baseShiftPrefs = getSharedPreferences(BASE_SHIFT_PREFS_NAME, MODE_PRIVATE)

        loadSavedModeBaseShifts()
        setupBaseShiftControls()
        setupSelector()
        setupPhthongTouchPlayback()
        touchHintText.text = getString(R.string.eight_modes_touch_hint)
    }

    private fun loadSavedModeBaseShifts() {
        modeBaseShiftMoria.clear()
        for (modeIndex in modes.indices) {
            val savedShift = baseShiftPrefs
                .getInt(baseShiftPrefKey(modeIndex), BASE_SHIFT_DEFAULT_MORIA)
                .coerceIn(BASE_SHIFT_MORIA_MIN, BASE_SHIFT_MORIA_MAX)
            modeBaseShiftMoria[modeIndex] = savedShift
        }
    }

    private fun setupBaseShiftControls() {
        baseShiftSeekBar.max = BASE_SHIFT_MORIA_MAX - BASE_SHIFT_MORIA_MIN
        baseShiftSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (isSyncingBaseShiftControls) {
                    return
                }
                val shiftMoria = progressToShiftMoria(progress)
                applyBaseShiftToCurrentMode(shiftMoria, persist = true)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        baseShiftResetButton.setOnClickListener {
            syncBaseShiftControls(BASE_SHIFT_DEFAULT_MORIA)
            applyBaseShiftToCurrentMode(BASE_SHIFT_DEFAULT_MORIA, persist = true)
        }
    }

    private fun setupSelector() {
        val adapter = object :
            ArrayAdapter<ModeDefinition>(this, android.R.layout.simple_spinner_item, modes) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                return bindAndColorModeText(view, position)
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                return bindAndColorModeText(view, position)
            }

            private fun bindAndColorModeText(view: View, position: Int): View {
                val textView = view.findViewById<TextView>(android.R.id.text1)
                val mode = getItem(position)
                textView.text = mode?.let { getString(it.nameRes) }.orEmpty()
                textView.setTextColor(getModeSelectorColor(mode?.colorCategory ?: ModeColorCategory.OTHER))
                return view
            }
        }.apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        modeSelector.adapter = adapter

        modeSelector.setSelection(0, false)
        renderMode(0)
        modeSelector.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                renderMode(position)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                renderMode(0)
            }
        }
    }

    private fun getModeSelectorColor(category: ModeColorCategory): Int {
        val colorRes = when (category) {
            ModeColorCategory.DIATONIC -> R.color.black
            ModeColorCategory.HARD_CHROMATIC -> R.color.mode_hard_chromatic_blue
            ModeColorCategory.SOFT_CHROMATIC -> R.color.mode_soft_chromatic_purple
            ModeColorCategory.ENHARMONIC -> R.color.mode_enharmonic_orange
            ModeColorCategory.OTHER -> R.color.black
        }
        return ContextCompat.getColor(this, colorRes)
    }

    private fun renderMode(position: Int) {
        val safePosition = if (position in modes.indices) position else 0
        currentModePosition = safePosition
        val mode = modes[safePosition]
        val ascendingIntervalsExtended = mode.ascendingIntervals.repeatTimes(SCALE_OCTAVES)
        currentAscendingIntervalsExtended = ascendingIntervalsExtended
        val ascendingPhthongsExtended = buildTripleOctaveAscendingPhthongs()
        selectedModeType.text = getString(mode.typeRes)
        selectedModeApichimaText.text =
            getString(R.string.mode_apichima_label, getString(mode.apichimaRes))
        val alternativeRes = mode.apichimaAlternativeRes
        if (alternativeRes != null) {
            selectedModeApichimaAlternatives.visibility = View.VISIBLE
            selectedModeApichimaAlternatives.text =
                getString(R.string.mode_apichima_alternatives_label, getString(alternativeRes))
        } else {
            selectedModeApichimaAlternatives.visibility = View.GONE
        }
        selectedModeApichimaPhthongs.text =
            getString(R.string.mode_apichima_phthongs_label, getString(mode.apichimaPhthongsRes))
        selectedModeApichimaSyllables.text =
            getString(R.string.mode_apichima_syllables_label, getString(mode.apichimaSyllablesRes))

        val alternativePhthongsRes = mode.apichimaAlternativePhthongsRes
        val alternativeSyllablesRes = mode.apichimaAlternativeSyllablesRes
        if (alternativeRes != null && alternativePhthongsRes != null && alternativeSyllablesRes != null) {
            selectedModeApichimaAlternativePhthongs.visibility = View.VISIBLE
            selectedModeApichimaAlternativeSyllables.visibility = View.VISIBLE
            selectedModeApichimaAlternativePhthongs.text = getString(
                R.string.mode_apichima_alternative_phthongs_label,
                getString(alternativePhthongsRes)
            )
            selectedModeApichimaAlternativeSyllables.text = getString(
                R.string.mode_apichima_alternative_syllables_label,
                getString(alternativeSyllablesRes)
            )
        } else {
            selectedModeApichimaAlternativePhthongs.visibility = View.GONE
            selectedModeApichimaAlternativeSyllables.visibility = View.GONE
        }
        selectedModeApichimaFormsHint.text = getString(R.string.mode_apichima_forms_hint)
        selectedModeApichimaSign.setImageResource(mode.apichimaSignRes)
        selectedModeApichimaSignName.text =
            getString(R.string.mode_apichima_sign_name, getString(mode.apichimaSignNameRes))
        selectedModeDetails.text = getString(mode.detailsRes)
        tonePlayer.stop()
        ascendingDiagramView.clearTouchState()

        ascendingDiagramView.setDiagramData(
            phthongsTopToBottom = ascendingPhthongsExtended.reversed(),
            intervalsTopToBottom = ascendingIntervalsExtended.reversed()
        )
        val savedShift = modeBaseShiftMoria[safePosition] ?: BASE_SHIFT_DEFAULT_MORIA
        syncBaseShiftControls(savedShift)
        applyBaseShiftToCurrentMode(savedShift, persist = false)
    }

    private fun setupPhthongTouchPlayback() {
        ascendingDiagramView.setOnPhthongTouchListener { event ->
            when (event.action) {
                PhthongTouchAction.DOWN,
                PhthongTouchAction.MOVE -> playFrequencyForIndex(event.indexTopToBottom)

                PhthongTouchAction.UP,
                PhthongTouchAction.CANCEL,
                PhthongTouchAction.EXIT -> tonePlayer.stop()
            }
        }
    }

    private fun playFrequencyForIndex(indexTopToBottom: Int) {
        val frequencyHz = frequenciesTopToBottom.getOrNull(indexTopToBottom) ?: return
        tonePlayer.start(frequencyHz)
    }

    private fun syncBaseShiftControls(shiftMoria: Int) {
        isSyncingBaseShiftControls = true
        baseShiftSeekBar.progress = shiftMoriaToProgress(shiftMoria)
        isSyncingBaseShiftControls = false
        updateBaseShiftValueText(shiftMoria)
    }

    private fun applyBaseShiftToCurrentMode(shiftMoria: Int, persist: Boolean) {
        val boundedShiftMoria = shiftMoria.coerceIn(BASE_SHIFT_MORIA_MIN, BASE_SHIFT_MORIA_MAX)
        modeBaseShiftMoria[currentModePosition] = boundedShiftMoria
        if (persist) {
            persistModeBaseShift(currentModePosition, boundedShiftMoria)
        }
        updateBaseShiftValueText(boundedShiftMoria)
        frequenciesTopToBottom = calculateFrequenciesTopToBottom(
            ascendingIntervals = currentAscendingIntervalsExtended,
            baseShiftMoria = boundedShiftMoria
        )
    }

    private fun updateBaseShiftValueText(shiftMoria: Int) {
        baseShiftValueText.text = if (shiftMoria == BASE_SHIFT_DEFAULT_MORIA) {
            getString(R.string.eight_modes_base_shift_default_value)
        } else {
            getString(R.string.eight_modes_base_shift_value_template, shiftMoria)
        }
    }

    private fun persistModeBaseShift(modeIndex: Int, shiftMoria: Int) {
        baseShiftPrefs.edit()
            .putInt(baseShiftPrefKey(modeIndex), shiftMoria)
            .apply()
    }

    private fun baseShiftPrefKey(modeIndex: Int): String = "$BASE_SHIFT_PREF_KEY_PREFIX$modeIndex"

    private fun shiftMoriaToProgress(shiftMoria: Int): Int =
        shiftMoria.coerceIn(BASE_SHIFT_MORIA_MIN, BASE_SHIFT_MORIA_MAX) - BASE_SHIFT_MORIA_MIN

    private fun progressToShiftMoria(progress: Int): Int =
        (progress + BASE_SHIFT_MORIA_MIN).coerceIn(BASE_SHIFT_MORIA_MIN, BASE_SHIFT_MORIA_MAX)

    private fun calculateFrequenciesTopToBottom(
        ascendingIntervals: List<Int>,
        baseShiftMoria: Int
    ): List<Double> {
        val cumulativeMoriaBottomToTop = mutableListOf(0)
        var currentMoria = 0
        for (interval in ascendingIntervals) {
            currentMoria += interval
            cumulativeMoriaBottomToTop.add(currentMoria)
        }
        val transposeFactor = 2.0.pow(baseShiftMoria / MORIA_PER_OCTAVE)
        return cumulativeMoriaBottomToTop
            .map { moria ->
                val moriaFromBaseNi = moria - MORIA_PER_OCTAVE
                (BASE_NI_FREQUENCY_HZ * 2.0.pow(moriaFromBaseNi / MORIA_PER_OCTAVE)) * transposeFactor
            }
            .reversed()
    }

    private fun buildTripleOctaveAscendingPhthongs(): List<String> {
        val lowOctave = PHTHONGS_WITHOUT_NI.map { "$it," }
        val middleOctave = PHTHONGS_WITHOUT_NI
        val highOctave = PHTHONGS_WITHOUT_NI.map { "$it΄" }
        return lowOctave + middleOctave + highOctave + listOf("Νη΄΄")
    }

    override fun onStop() {
        tonePlayer.stop()
        super.onStop()
    }

    override fun onDestroy() {
        tonePlayer.release()
        super.onDestroy()
    }

    private data class ModeDefinition(
        val nameRes: Int,
        val typeRes: Int,
        val apichimaRes: Int,
        val apichimaAlternativeRes: Int?,
        val apichimaPhthongsRes: Int,
        val apichimaSyllablesRes: Int,
        val apichimaAlternativePhthongsRes: Int?,
        val apichimaAlternativeSyllablesRes: Int?,
        val apichimaSignRes: Int,
        val apichimaSignNameRes: Int,
        val detailsRes: Int,
        val colorCategory: ModeColorCategory,
        val ascendingIntervals: List<Int>
    )

    private enum class ModeColorCategory {
        DIATONIC,
        HARD_CHROMATIC,
        SOFT_CHROMATIC,
        ENHARMONIC,
        OTHER
    }

    private companion object {
        const val BASE_NI_FREQUENCY_HZ = 220.0
        const val MORIA_PER_OCTAVE = 72.0
        const val SCALE_OCTAVES = 3
        const val BASE_SHIFT_MORIA_MIN = -12
        const val BASE_SHIFT_MORIA_MAX = 12
        const val BASE_SHIFT_DEFAULT_MORIA = 0
        const val BASE_SHIFT_PREFS_NAME = "eight_modes_base_shift_prefs"
        const val BASE_SHIFT_PREF_KEY_PREFIX = "mode_base_shift_moria_"
        val PHTHONGS_WITHOUT_NI = listOf("Νη", "Πα", "Βου", "Γα", "Δι", "Κε", "Ζω")
    }
}

private fun List<Int>.repeatTimes(times: Int): List<Int> {
    if (times <= 1 || isEmpty()) {
        return this
    }
    return buildList(size * times) {
        repeat(times) { addAll(this@repeatTimes) }
    }
}
