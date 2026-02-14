package com.johnchourp.learnbyzantinemusic.modes

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.johnchourp.learnbyzantinemusic.R

class EightModesActivity : ComponentActivity() {
    private lateinit var modeSelector: Spinner
    private lateinit var selectedModeTitle: TextView
    private lateinit var selectedModeType: TextView
    private lateinit var ascendingScaleText: TextView
    private lateinit var descendingScaleText: TextView
    private lateinit var ascendingDiagramView: ScaleDiagramView
    private lateinit var descendingDiagramView: ScaleDiagramView

    private val ascendingPhthongs = listOf("Νη", "Πα", "Βου", "Γα", "Δι", "Κε", "Ζω", "Νη΄")
    private val descendingPhthongs = listOf("Νη΄", "Ζω", "Κε", "Δι", "Γα", "Βου", "Πα", "Νη")

    private val modes: List<ModeDefinition> by lazy {
        listOf(
            ModeDefinition(
                nameRes = R.string.mode_first,
                typeRes = R.string.mode_type_diatonic,
                ascendingIntervals = listOf(12, 10, 8, 12, 12, 10, 8),
                descendingIntervals = listOf(8, 10, 12, 12, 8, 10, 12)
            ),
            ModeDefinition(
                nameRes = R.string.mode_second,
                typeRes = R.string.mode_type_hard_chromatic,
                ascendingIntervals = listOf(6, 20, 4, 12, 6, 20, 4),
                descendingIntervals = listOf(4, 20, 6, 12, 4, 20, 6)
            ),
            ModeDefinition(
                nameRes = R.string.mode_third,
                typeRes = R.string.mode_type_diatonic,
                ascendingIntervals = listOf(8, 12, 12, 10, 8, 12, 10),
                descendingIntervals = listOf(10, 12, 8, 10, 12, 12, 8)
            ),
            ModeDefinition(
                nameRes = R.string.mode_fourth,
                typeRes = R.string.mode_type_diatonic,
                ascendingIntervals = listOf(10, 8, 12, 12, 10, 8, 12),
                descendingIntervals = listOf(12, 8, 10, 12, 12, 8, 10)
            ),
            ModeDefinition(
                nameRes = R.string.mode_plagal_first,
                typeRes = R.string.mode_type_diatonic,
                ascendingIntervals = listOf(10, 8, 12, 12, 10, 8, 12),
                descendingIntervals = listOf(12, 8, 10, 12, 12, 8, 10)
            ),
            ModeDefinition(
                nameRes = R.string.mode_plagal_second,
                typeRes = R.string.mode_type_soft_chromatic,
                ascendingIntervals = listOf(8, 14, 8, 12, 8, 14, 8),
                descendingIntervals = listOf(8, 14, 8, 12, 8, 14, 8)
            ),
            ModeDefinition(
                nameRes = R.string.mode_varys,
                typeRes = R.string.mode_type_enharmonic,
                ascendingIntervals = listOf(12, 12, 6, 12, 12, 6, 12),
                descendingIntervals = listOf(12, 6, 12, 12, 6, 12, 12)
            ),
            ModeDefinition(
                nameRes = R.string.mode_plagal_fourth,
                typeRes = R.string.mode_type_diatonic,
                ascendingIntervals = listOf(12, 10, 8, 12, 12, 10, 8),
                descendingIntervals = listOf(8, 10, 12, 12, 8, 10, 12)
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_eight_modes)

        modeSelector = findViewById(R.id.mode_selector)
        selectedModeTitle = findViewById(R.id.selected_mode_title)
        selectedModeType = findViewById(R.id.selected_mode_type)
        ascendingScaleText = findViewById(R.id.ascending_scale_text)
        descendingScaleText = findViewById(R.id.descending_scale_text)
        ascendingDiagramView = findViewById(R.id.ascending_diagram_view)
        descendingDiagramView = findViewById(R.id.descending_diagram_view)

        setupSelector()
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
        selectedModeTitle.text = getString(R.string.mode_selected_label, getString(mode.nameRes))
        selectedModeType.text = getString(mode.typeRes)
        ascendingScaleText.text = getString(
            R.string.mode_scale_notes,
            ascendingPhthongs.joinToString(" - ")
        )
        descendingScaleText.text = getString(
            R.string.mode_scale_notes,
            descendingPhthongs.joinToString(" - ")
        )

        ascendingDiagramView.setDiagramData(
            phthongsTopToBottom = ascendingPhthongs.reversed(),
            intervalsTopToBottom = mode.ascendingIntervals.reversed()
        )
        descendingDiagramView.setDiagramData(
            phthongsTopToBottom = descendingPhthongs,
            intervalsTopToBottom = mode.descendingIntervals
        )
    }

    private data class ModeDefinition(
        val nameRes: Int,
        val typeRes: Int,
        val ascendingIntervals: List<Int>,
        val descendingIntervals: List<Int>
    )
}
