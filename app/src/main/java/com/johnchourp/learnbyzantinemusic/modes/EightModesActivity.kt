package com.johnchourp.learnbyzantinemusic.modes

import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R
import kotlin.math.abs
import kotlin.math.pow

class EightModesActivity : BaseActivity() {
    private lateinit var modeSelector: Spinner
    private lateinit var timbreSelector: Spinner
    private lateinit var selectedModeTheorySectionsContainer: LinearLayout
    private lateinit var modeSelectorCard: View
    private lateinit var timbreSelectorCard: View
    private lateinit var selectedModeSummaryCard: View
    private lateinit var baseShiftCard: View
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
    private var currentReferenceMoriaFromBottom: Int = 0
    private var isSyncingBaseShiftControls: Boolean = false
    private var selectedToneTimbre: ToneTimbre = ToneTimbre.CLEAN
    private val timbreOptions: List<ToneTimbre> = listOf(
        ToneTimbre.CLEAN,
        ToneTimbre.SOFT,
        ToneTimbre.CRYSTAL
    )

    private val modes: List<ModeDefinition> by lazy {
        listOf(
            ModeDefinition(
                nameRes = R.string.mode_first,
                selectorNameRes = R.string.mode_first,
                selectorGenusRes = R.string.mode_genus_diatonic,
                apichimaRes = R.string.mode_apichima_first,
                apichimaAlternativeRes = null,
                apichimaSyllablesRes = R.string.mode_apichima_syllables_first,
                apichimaAlternativeSyllablesRes = null,
                theoryKey = ModeTheoryCatalog.keyForPosition(0),
                colorCategory = ModeColorCategory.DIATONIC,
                scale = EightModeScaleDefinitions.DIATONIC
            ),
            ModeDefinition(
                nameRes = R.string.mode_fourth,
                selectorNameRes = R.string.mode_fourth,
                selectorGenusRes = R.string.mode_genus_diatonic,
                apichimaRes = R.string.mode_apichima_fourth,
                apichimaAlternativeRes = R.string.mode_apichima_alternative_fourth,
                apichimaSyllablesRes = R.string.mode_apichima_syllables_fourth,
                apichimaAlternativeSyllablesRes = R.string.mode_apichima_alternative_syllables_fourth,
                theoryKey = ModeTheoryCatalog.keyForPosition(1),
                colorCategory = ModeColorCategory.DIATONIC,
                scale = EightModeScaleDefinitions.DIATONIC
            ),
            ModeDefinition(
                nameRes = R.string.mode_plagal_first,
                selectorNameRes = R.string.mode_selector_plagal_first,
                selectorGenusRes = R.string.mode_genus_diatonic,
                apichimaRes = R.string.mode_apichima_plagal_first,
                apichimaAlternativeRes = null,
                apichimaSyllablesRes = R.string.mode_apichima_syllables_plagal_first,
                apichimaAlternativeSyllablesRes = null,
                theoryKey = ModeTheoryCatalog.keyForPosition(2),
                colorCategory = ModeColorCategory.DIATONIC,
                scale = EightModeScaleDefinitions.DIATONIC
            ),
            ModeDefinition(
                nameRes = R.string.mode_plagal_fourth,
                selectorNameRes = R.string.mode_selector_plagal_fourth,
                selectorGenusRes = R.string.mode_genus_diatonic,
                apichimaRes = R.string.mode_apichima_plagal_fourth,
                apichimaAlternativeRes = null,
                apichimaSyllablesRes = R.string.mode_apichima_syllables_plagal_fourth,
                apichimaAlternativeSyllablesRes = null,
                theoryKey = ModeTheoryCatalog.keyForPosition(3),
                colorCategory = ModeColorCategory.DIATONIC,
                scale = EightModeScaleDefinitions.DIATONIC
            ),
            ModeDefinition(
                nameRes = R.string.mode_third,
                selectorNameRes = R.string.mode_third,
                selectorGenusRes = R.string.mode_genus_enharmonic,
                apichimaRes = R.string.mode_apichima_third,
                apichimaAlternativeRes = null,
                apichimaSyllablesRes = R.string.mode_apichima_syllables_third,
                apichimaAlternativeSyllablesRes = null,
                theoryKey = ModeTheoryCatalog.keyForPosition(4),
                colorCategory = ModeColorCategory.ENHARMONIC,
                scale = EightModeScaleDefinitions.ENHARMONIC
            ),
            ModeDefinition(
                nameRes = R.string.mode_varys,
                selectorNameRes = R.string.mode_varys,
                selectorGenusRes = R.string.mode_genus_enharmonic,
                apichimaRes = R.string.mode_apichima_varys,
                apichimaAlternativeRes = null,
                apichimaSyllablesRes = R.string.mode_apichima_syllables_varys,
                apichimaAlternativeSyllablesRes = null,
                theoryKey = ModeTheoryCatalog.keyForPosition(5),
                colorCategory = ModeColorCategory.ENHARMONIC,
                scale = EightModeScaleDefinitions.ENHARMONIC
            ),
            ModeDefinition(
                nameRes = R.string.mode_second,
                selectorNameRes = R.string.mode_second,
                selectorGenusRes = R.string.mode_genus_chromatic_second,
                apichimaRes = R.string.mode_apichima_second,
                apichimaAlternativeRes = null,
                apichimaSyllablesRes = R.string.mode_apichima_syllables_second,
                apichimaAlternativeSyllablesRes = null,
                theoryKey = ModeTheoryCatalog.keyForPosition(6),
                colorCategory = ModeColorCategory.SOFT_CHROMATIC,
                scale = EightModeScaleDefinitions.SOFT_CHROMATIC
            ),
            ModeDefinition(
                nameRes = R.string.mode_plagal_second,
                selectorNameRes = R.string.mode_selector_plagal_second,
                selectorGenusRes = R.string.mode_genus_chromatic_plagal_second,
                apichimaRes = R.string.mode_apichima_plagal_second,
                apichimaAlternativeRes = null,
                apichimaSyllablesRes = R.string.mode_apichima_syllables_plagal_second,
                apichimaAlternativeSyllablesRes = null,
                theoryKey = ModeTheoryCatalog.keyForPosition(7),
                colorCategory = ModeColorCategory.HARD_CHROMATIC,
                scale = EightModeScaleDefinitions.HARD_CHROMATIC
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_eight_modes)

        modeSelector = findViewById(R.id.mode_selector)
        timbreSelector = findViewById(R.id.timbre_selector)
        modeSelectorCard = findViewById(R.id.mode_selector_card)
        timbreSelectorCard = findViewById(R.id.timbre_selector_card)
        selectedModeSummaryCard = findViewById(R.id.selected_mode_summary_card)
        selectedModeTheorySectionsContainer = findViewById(
            R.id.selected_mode_theory_sections_container
        )
        ascendingDiagramView = findViewById(R.id.ascending_diagram_view)
        touchHintText = findViewById(R.id.touch_hint_text)
        baseShiftCard = findViewById(R.id.base_shift_card)
        baseShiftValueText = findViewById(R.id.base_shift_value)
        baseShiftSeekBar = findViewById(R.id.base_shift_seekbar)
        baseShiftResetButton = findViewById(R.id.base_shift_reset_button)
        baseShiftPrefs = getSharedPreferences(BASE_SHIFT_PREFS_NAME, MODE_PRIVATE)

        loadSavedModeBaseShifts()
        loadSavedToneTimbre()
        setupBaseShiftControls()
        setupSelector()
        setupTimbreSelector()
        setupPhthongTouchPlayback()
        touchHintText.text = getString(R.string.eight_modes_touch_hint)
        runEntranceAnimations()
    }

    private fun loadSavedModeBaseShifts() {
        modeBaseShiftMoria.clear()
        for (modeIndex in modes.indices) {
            val savedShift = baseShiftPrefs
                .getInt(baseShiftPrefKey(modes[modeIndex].theoryKey), BASE_SHIFT_DEFAULT_MORIA)
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
            ArrayAdapter<ModeDefinition>(
                this,
                R.layout.list_item_mode_selector,
                android.R.id.text1,
                modes
            ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                return bindAndColorModeText(view, position).also {
                    forwardSelectorRowClicks(it, modeSelector)
                }
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                return bindAndColorModeText(view, position)
            }

            private fun bindAndColorModeText(view: View, position: Int): View {
                resetSelectorScroll(view)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                val mode = getItem(position)
                textView.text = mode?.let { formatModeSelectorLabel(it) }.orEmpty()
                textView.setTextColor(getModeSelectorColor(mode?.colorCategory ?: ModeColorCategory.OTHER))
                return view
            }
        }.apply {
            setDropDownViewResource(R.layout.list_item_mode_selector_dropdown)
        }
        modeSelector.adapter = adapter

        val initialModePosition = loadSavedModePosition()
        modeSelector.setSelection(initialModePosition, false)
        renderMode(initialModePosition, animate = false)
        modeSelector.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                renderMode(position, animate = true)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                renderMode(0, animate = true)
            }
        }
    }

    private fun loadSavedModePosition(): Int {
        val savedKey = baseShiftPrefs.getString(SELECTED_MODE_KEY_PREF_KEY, null)
        return modes.indexOfFirst { it.theoryKey == savedKey }.takeIf { it >= 0 } ?: 0
    }

    private fun persistSelectedModePosition(position: Int) {
        val selectedKey = modes.getOrNull(position)?.theoryKey ?: return
        baseShiftPrefs.edit()
            .putString(SELECTED_MODE_KEY_PREF_KEY, selectedKey)
            .apply()
    }

    private fun setupTimbreSelector() {
        val adapter = object :
            ArrayAdapter<ToneTimbre>(
                this,
                R.layout.list_item_mode_selector,
                android.R.id.text1,
                timbreOptions
            ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                return bindTimbreText(view, position).also {
                    forwardSelectorRowClicks(it, timbreSelector)
                }
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                return bindTimbreText(view, position)
            }

            private fun bindTimbreText(view: View, position: Int): View {
                resetSelectorScroll(view)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                val timbre = getItem(position)
                textView.text = timbre?.let { getString(getToneTimbreLabelRes(it)) }.orEmpty()
                textView.setTextColor(ContextCompat.getColor(this@EightModesActivity, R.color.black))
                return view
            }
        }.apply {
            setDropDownViewResource(R.layout.list_item_mode_selector_dropdown)
        }
        timbreSelector.adapter = adapter

        val selectedIndex = timbreOptions.indexOf(selectedToneTimbre).coerceAtLeast(0)
        timbreSelector.setSelection(selectedIndex, false)
        timbreSelector.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selected = timbreOptions.getOrNull(position) ?: ToneTimbre.CLEAN
                if (selected == selectedToneTimbre) {
                    return
                }
                selectedToneTimbre = selected
                persistToneTimbre(selected)
                tonePlayer.stop()
                animateTimbreSelection()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
    }

    private fun getToneTimbreLabelRes(timbre: ToneTimbre): Int = when (timbre) {
        ToneTimbre.CLEAN -> R.string.eight_modes_timbre_clean
        ToneTimbre.SOFT -> R.string.eight_modes_timbre_soft
        ToneTimbre.CRYSTAL -> R.string.eight_modes_timbre_crystal
    }

    private fun loadSavedToneTimbre() {
        val storedValue = baseShiftPrefs.getString(TONE_TIMBRE_PREF_KEY, ToneTimbre.CLEAN.name)
        val resolved = ToneTimbre.entries.firstOrNull { it.name == storedValue } ?: ToneTimbre.CLEAN
        selectedToneTimbre = if (resolved in timbreOptions) resolved else ToneTimbre.CLEAN
    }

    private fun persistToneTimbre(timbre: ToneTimbre) {
        baseShiftPrefs.edit()
            .putString(TONE_TIMBRE_PREF_KEY, timbre.name)
            .apply()
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

    private fun formatModeSelectorLabel(mode: ModeDefinition): String =
        getString(
            R.string.eight_modes_mode_option_template,
            getString(mode.selectorNameRes),
            getString(mode.selectorGenusRes)
        )

    private fun resetSelectorScroll(view: View) {
        val scrollView: HorizontalScrollView? = view.findViewById(R.id.selector_text_scroll)
        scrollView?.scrollTo(0, 0)
    }

    private fun forwardSelectorRowClicks(view: View, spinner: Spinner) {
        val openSpinner = View.OnClickListener { spinner.performClick() }
        view.setOnClickListener(openSpinner)
        val scrollView: HorizontalScrollView? = view.findViewById(R.id.selector_text_scroll)
        scrollView?.setOnTouchListener(createOpenSpinnerOnTapListener(spinner))
        val arrowView: View? = view.findViewById(R.id.selector_dropdown_arrow)
        arrowView?.setOnClickListener(openSpinner)
    }

    private fun createOpenSpinnerOnTapListener(spinner: Spinner): View.OnTouchListener {
        val touchSlop = ViewConfiguration.get(spinner.context).scaledTouchSlop
        var downRawX = 0f
        var downRawY = 0f
        return View.OnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    false
                }

                MotionEvent.ACTION_UP -> {
                    val isTap = abs(event.rawX - downRawX) <= touchSlop &&
                        abs(event.rawY - downRawY) <= touchSlop
                    if (isTap) {
                        spinner.performClick()
                    }
                    isTap
                }

                else -> false
            }
        }
    }

    private fun renderMode(position: Int, animate: Boolean = true) {
        val safePosition = if (position in modes.indices) position else 0
        currentModePosition = safePosition
        persistSelectedModePosition(safePosition)
        val mode = modes[safePosition]
        val scale = mode.scale
        val ascendingIntervalsExtended = scale.repeatedIntervals(SCALE_OCTAVES)
        currentAscendingIntervalsExtended = ascendingIntervalsExtended
        currentReferenceMoriaFromBottom =
            scale.referenceMoriaFromBottom(BASE_REFERENCE_PHTHONG, SCALE_OCTAVES)
        val ascendingPhthongsExtended = scale.ascendingPhthongs(SCALE_OCTAVES)
        renderSelectedModeTheory(mode)
        tonePlayer.stop()
        ascendingDiagramView.clearTouchState()

        ascendingDiagramView.setDiagramData(
            phthongsTopToBottom = ascendingPhthongsExtended.reversed(),
            intervalsTopToBottom = ascendingIntervalsExtended.reversed()
        )
        val savedShift = modeBaseShiftMoria[safePosition] ?: BASE_SHIFT_DEFAULT_MORIA
        syncBaseShiftControls(savedShift)
        applyBaseShiftToCurrentMode(savedShift, persist = false)
        if (animate) {
            animateModeSelection()
        }
    }

    private fun renderSelectedModeTheory(mode: ModeDefinition) {
        val theory = ModeTheoryCatalog.byKey(mode.theoryKey)
        selectedModeTheorySectionsContainer.removeAllViews()
        selectedModeTheorySectionsContainer.addView(
            createTextTheorySection(
                number = 1,
                titleRes = R.string.mode_theory_section_apichima,
                bodyText = formatApichima(mode)
            )
        )
        selectedModeTheorySectionsContainer.addView(
            createTextTheorySection(
                number = 2,
                titleRes = R.string.mode_theory_section_syllables_phthongs,
                bodyText = formatApichimaSyllables(mode)
            )
        )
        theory.styleRows.forEachIndexed { index, styleRow ->
            selectedModeTheorySectionsContainer.addView(
                createStyleTheorySection(
                    number = index + 3,
                    row = styleRow
                )
            )
        }
        selectedModeTheorySectionsContainer.addView(
            createTextTheorySection(
                number = 6,
                titleRes = R.string.mode_theory_section_modulations,
                bodyText = getString(theory.modulationsRes)
            )
        )
        selectedModeTheorySectionsContainer.addView(
            createTextTheorySection(
                number = 7,
                titleRes = R.string.mode_theory_section_attractions,
                bodyText = getString(theory.attractionsRes)
            )
        )
        selectedModeTheorySectionsContainer.addView(
            createTextTheorySection(
                number = 8,
                titleRes = R.string.mode_theory_section_phthores,
                bodyText = getString(theory.phthoresRes)
            )
        )
    }

    private fun formatApichima(mode: ModeDefinition): String =
        listOfNotNull(
            getString(mode.apichimaRes),
            mode.apichimaAlternativeRes?.let { getString(it) }
        ).joinToString(" / ")

    private fun formatApichimaSyllables(mode: ModeDefinition): String {
        val primarySyllables = getString(mode.apichimaSyllablesRes)
        val alternativeApichimaRes = mode.apichimaAlternativeRes
        val alternativeSyllablesRes = mode.apichimaAlternativeSyllablesRes
        return if (alternativeApichimaRes != null && alternativeSyllablesRes != null) {
            getString(
                R.string.mode_theory_primary_alternative_template,
                getString(mode.apichimaRes),
                primarySyllables,
                getString(alternativeApichimaRes),
                getString(alternativeSyllablesRes)
            )
        } else {
            primarySyllables
        }
    }

    private fun createTextTheorySection(number: Int, titleRes: Int, bodyText: String): View =
        createSectionContainer().apply {
            addView(createSectionTitle(number, titleRes))
            addView(createSectionBody(bodyText))
        }

    private fun createStyleTheorySection(number: Int, row: ModeTheoryStyleRow): View =
        createSectionContainer().apply {
            addView(createSectionTitle(number, row.styleNameRes))
            addView(createTheoryTable(row))
        }

    private fun createTheoryTable(row: ModeTheoryStyleRow): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
            addView(createTheoryTableRow(R.string.mode_theory_field_system, row.systemRes))
            addView(
                createTheoryTableRow(
                    R.string.mode_theory_field_dominant_phthongs,
                    row.dominantsCadencesRes
                )
            )
            addView(createTheoryTableRow(R.string.mode_theory_field_cadences, row.dominantsCadencesRes))
            addView(
                createTheoryTableRow(
                    R.string.mode_theory_field_final_cadences,
                    row.dominantsCadencesRes
                )
            )
        }

    private fun createTheoryTableRow(labelRes: Int, valueRes: Int): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            isBaselineAligned = false
            setPadding(0, dp(5), 0, dp(5))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            addView(createTheoryTableCell(getString(labelRes), isLabel = true))
            addView(createTheoryTableCell(getString(valueRes), isLabel = false))
        }

    private fun createTheoryTableCell(textValue: String, isLabel: Boolean): TextView =
        TextView(this).apply {
            if (isLabel) {
                text = textValue
            } else {
                TheoryTopicLinks.setLinkedText(this@EightModesActivity, this, textValue)
            }
            setTextColor(
                ContextCompat.getColor(
                    this@EightModesActivity,
                    if (isLabel) {
                        R.color.first_mode_theory_text_secondary
                    } else {
                        R.color.first_mode_theory_text_primary
                    }
                )
            )
            textSize = if (isLabel) 13f else 14f
            if (isLabel) {
                typeface = Typeface.DEFAULT_BOLD
            }
            setLineSpacing(dp(2).toFloat(), 1.0f)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                if (isLabel) TABLE_LABEL_WEIGHT else TABLE_VALUE_WEIGHT
            ).apply {
                marginEnd = if (isLabel) dp(8) else 0
            }
        }

    private fun createSectionContainer(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        }

    private fun createSectionTitle(number: Int, titleRes: Int): TextView =
        TextView(this).apply {
            text = getString(R.string.mode_theory_numbered_section_template, number, getString(titleRes))
            setTextColor(ContextCompat.getColor(this@EightModesActivity, R.color.first_mode_theory_accent))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        }

    private fun createSectionBody(textValue: String): TextView =
        TextView(this).apply {
            TheoryTopicLinks.setLinkedText(this@EightModesActivity, this, textValue)
            setTextColor(ContextCompat.getColor(this@EightModesActivity, R.color.first_mode_theory_text_primary))
            textSize = 15f
            setLineSpacing(dp(2).toFloat(), 1.0f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(5)
            }
        }

    private fun runEntranceAnimations() {
        val entranceViews = listOf(
            modeSelectorCard,
            timbreSelectorCard,
            selectedModeSummaryCard,
            baseShiftCard,
            touchHintText,
            ascendingDiagramView
        )
        entranceViews.forEachIndexed { index, view ->
            view.animate().cancel()
            view.alpha = 0f
            view.translationY = ENTRANCE_TRANSLATION_Y
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(ENTRANCE_ANIMATION_DURATION_MS)
                .setStartDelay(index.toLong() * ENTRANCE_ANIMATION_STAGGER_MS)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun animateModeSelection() {
        pulseView(modeSelectorCard)
        animateContentRefresh(selectedModeSummaryCard, delayMs = 0L)
        animateContentRefresh(baseShiftCard, delayMs = CONTENT_REFRESH_STAGGER_MS)
        animateContentRefresh(ascendingDiagramView, delayMs = CONTENT_REFRESH_STAGGER_MS * 2)
    }

    private fun animateTimbreSelection() {
        pulseView(timbreSelectorCard)
    }

    private fun animateContentRefresh(view: View, delayMs: Long) {
        view.animate().cancel()
        view.alpha = CONTENT_REFRESH_START_ALPHA
        view.translationY = CONTENT_REFRESH_TRANSLATION_Y
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(CONTENT_REFRESH_DURATION_MS)
            .setStartDelay(delayMs)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun pulseView(view: View) {
        view.animate().cancel()
        view.alpha = 1f
        view.translationY = 0f
        view.scaleX = SELECTOR_PULSE_START_SCALE
        view.scaleY = SELECTOR_PULSE_START_SCALE
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(SELECTOR_PULSE_DURATION_MS)
            .setInterpolator(OvershootInterpolator(SELECTOR_PULSE_TENSION))
            .start()
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
        tonePlayer.start(frequencyHz, selectedToneTimbre)
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
            referenceMoriaFromBottom = currentReferenceMoriaFromBottom,
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
        val modeKey = modes.getOrNull(modeIndex)?.theoryKey ?: return
        baseShiftPrefs.edit()
            .putInt(baseShiftPrefKey(modeKey), shiftMoria)
            .apply()
    }

    private fun baseShiftPrefKey(modeKey: String): String = "$BASE_SHIFT_PREF_KEY_PREFIX$modeKey"

    private fun shiftMoriaToProgress(shiftMoria: Int): Int =
        shiftMoria.coerceIn(BASE_SHIFT_MORIA_MIN, BASE_SHIFT_MORIA_MAX) - BASE_SHIFT_MORIA_MIN

    private fun progressToShiftMoria(progress: Int): Int =
        (progress + BASE_SHIFT_MORIA_MIN).coerceIn(BASE_SHIFT_MORIA_MIN, BASE_SHIFT_MORIA_MAX)

    private fun calculateFrequenciesTopToBottom(
        ascendingIntervals: List<Int>,
        referenceMoriaFromBottom: Int,
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
                val moriaFromBaseNi = moria - referenceMoriaFromBottom
                (BASE_NI_FREQUENCY_HZ * 2.0.pow(moriaFromBaseNi / MORIA_PER_OCTAVE)) * transposeFactor
            }
            .reversed()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

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
        val selectorNameRes: Int,
        val selectorGenusRes: Int,
        val apichimaRes: Int,
        val apichimaAlternativeRes: Int?,
        val apichimaSyllablesRes: Int,
        val apichimaAlternativeSyllablesRes: Int?,
        val theoryKey: String,
        val colorCategory: ModeColorCategory,
        val scale: ModeScaleDefinition
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
        const val BASE_REFERENCE_PHTHONG = "Νη"
        const val MORIA_PER_OCTAVE = 72.0
        const val SCALE_OCTAVES = 3
        const val BASE_SHIFT_MORIA_MIN = -12
        const val BASE_SHIFT_MORIA_MAX = 12
        const val BASE_SHIFT_DEFAULT_MORIA = 0
        const val BASE_SHIFT_PREFS_NAME = "eight_modes_base_shift_prefs"
        const val BASE_SHIFT_PREF_KEY_PREFIX = "mode_base_shift_moria_"
        const val TONE_TIMBRE_PREF_KEY = "selected_tone_timbre"
        const val SELECTED_MODE_KEY_PREF_KEY = "selected_mode_key"
        const val ENTRANCE_ANIMATION_DURATION_MS = 320L
        const val ENTRANCE_ANIMATION_STAGGER_MS = 55L
        const val ENTRANCE_TRANSLATION_Y = 28f
        const val CONTENT_REFRESH_DURATION_MS = 240L
        const val CONTENT_REFRESH_STAGGER_MS = 45L
        const val CONTENT_REFRESH_TRANSLATION_Y = 14f
        const val CONTENT_REFRESH_START_ALPHA = 0.68f
        const val SELECTOR_PULSE_DURATION_MS = 220L
        const val SELECTOR_PULSE_START_SCALE = 0.985f
        const val SELECTOR_PULSE_TENSION = 1.5f
        const val TABLE_LABEL_WEIGHT = 0.43f
        const val TABLE_VALUE_WEIGHT = 0.57f
    }
}
