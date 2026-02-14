package com.johnchourp.learnbyzantinemusic.modes

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R
import kotlin.math.pow

class EightModesActivity : BaseActivity() {
    private lateinit var modeSelector: Spinner
    private lateinit var selectedModeType: TextView
    private lateinit var ascendingDiagramView: ScaleDiagramView
    private lateinit var touchHintText: TextView
    private val tonePlayer: PhthongTonePlayer by lazy { PhthongTonePlayer() }
    private var frequenciesTopToBottom: List<Double> = emptyList()

    private val ascendingPhthongs = listOf("Νη", "Πα", "Βου", "Γα", "Δι", "Κε", "Ζω", "Νη΄")

    private val modes: List<ModeDefinition> by lazy {
        listOf(
            ModeDefinition(
                nameRes = R.string.mode_first,
                typeRes = R.string.mode_type_diatonic,
                ascendingIntervals = listOf(12, 10, 8, 12, 12, 10, 8)
            ),
            ModeDefinition(
                nameRes = R.string.mode_second,
                typeRes = R.string.mode_type_hard_chromatic,
                ascendingIntervals = listOf(6, 20, 4, 12, 6, 20, 4)
            ),
            ModeDefinition(
                nameRes = R.string.mode_third,
                typeRes = R.string.mode_type_diatonic,
                ascendingIntervals = listOf(8, 12, 12, 10, 8, 12, 10)
            ),
            ModeDefinition(
                nameRes = R.string.mode_fourth,
                typeRes = R.string.mode_type_diatonic,
                ascendingIntervals = listOf(10, 8, 12, 12, 10, 8, 12)
            ),
            ModeDefinition(
                nameRes = R.string.mode_plagal_first,
                typeRes = R.string.mode_type_diatonic,
                ascendingIntervals = listOf(10, 8, 12, 12, 10, 8, 12)
            ),
            ModeDefinition(
                nameRes = R.string.mode_plagal_second,
                typeRes = R.string.mode_type_soft_chromatic,
                ascendingIntervals = listOf(8, 14, 8, 12, 8, 14, 8)
            ),
            ModeDefinition(
                nameRes = R.string.mode_varys,
                typeRes = R.string.mode_type_enharmonic,
                ascendingIntervals = listOf(12, 12, 6, 12, 12, 6, 12)
            ),
            ModeDefinition(
                nameRes = R.string.mode_plagal_fourth,
                typeRes = R.string.mode_type_diatonic,
                ascendingIntervals = listOf(12, 10, 8, 12, 12, 10, 8)
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_eight_modes)

        modeSelector = findViewById(R.id.mode_selector)
        selectedModeType = findViewById(R.id.selected_mode_type)
        ascendingDiagramView = findViewById(R.id.ascending_diagram_view)
        touchHintText = findViewById(R.id.touch_hint_text)

        setupSelector()
        setupPhthongTouchPlayback()
        touchHintText.text = getString(R.string.eight_modes_touch_hint)
    }

    private fun setupSelector() {
        val modeNames = modes.map { getString(it.nameRes) }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modeNames).apply {
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

    private fun renderMode(position: Int) {
        val mode = modes.getOrElse(position) { modes.first() }
        selectedModeType.text = getString(mode.typeRes)
        frequenciesTopToBottom = calculateFrequenciesTopToBottom(mode.ascendingIntervals)
        tonePlayer.stop()
        ascendingDiagramView.clearTouchState()

        ascendingDiagramView.setDiagramData(
            phthongsTopToBottom = ascendingPhthongs.reversed(),
            intervalsTopToBottom = mode.ascendingIntervals.reversed()
        )
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

    private fun calculateFrequenciesTopToBottom(ascendingIntervals: List<Int>): List<Double> {
        val cumulativeMoriaBottomToTop = mutableListOf(0)
        var currentMoria = 0
        for (interval in ascendingIntervals) {
            currentMoria += interval
            cumulativeMoriaBottomToTop.add(currentMoria)
        }
        return cumulativeMoriaBottomToTop
            .map { moria -> BASE_NI_FREQUENCY_HZ * 2.0.pow(moria / MORIA_PER_OCTAVE) }
            .reversed()
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
        val ascendingIntervals: List<Int>
    )

    private companion object {
        const val BASE_NI_FREQUENCY_HZ = 220.0
        const val MORIA_PER_OCTAVE = 72.0
    }
}
