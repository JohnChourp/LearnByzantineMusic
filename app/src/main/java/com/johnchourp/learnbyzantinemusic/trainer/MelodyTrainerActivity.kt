package com.johnchourp.learnbyzantinemusic.trainer

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.children
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Practice page where the user writes a sequence of phthongi (Νη … Ζω), sets how long
 * each one lasts in χρόνοι, picks a tempo, and plays the melody back (Mode 1). The timing
 * rules (γοργόν, κλάσμα) are documented at the top and applied through [MelodySequence] /
 * the shared ByzantineRhythmMapper.
 */
class MelodyTrainerActivity : BaseActivity() {

    private val notes = mutableListOf<TrainerNote>()
    private var currentOctaveShift = 0
    private var bpm = MelodyTempo.DEFAULT_BPM
    private var isPlaybackActive = false

    private val player = MelodySequencePlayer()

    private lateinit var noteListContainer: LinearLayout
    private lateinit var emptyHintText: TextView
    private lateinit var totalBeatsText: TextView
    private lateinit var octaveValueText: TextView
    private lateinit var tempoValueText: TextView
    private lateinit var playButton: Button
    private lateinit var stopButton: Button
    private lateinit var clearButton: Button
    private lateinit var octaveDownButton: Button
    private lateinit var octaveUpButton: Button
    private lateinit var tempoSeek: SeekBar
    private lateinit var addNoteButtonsRow: LinearLayout

    private val noteRowViews = mutableListOf<View>()
    private var highlightedRow = -1

    private val playerListener = object : MelodySequencePlayer.Listener {
        override fun onNoteStarted(event: PlannedNoteEvent) = highlightRow(event.index)
        override fun onFinished(completed: Boolean) = onPlaybackStopped()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_melody_trainer)

        noteListContainer = findViewById(R.id.note_list_container)
        emptyHintText = findViewById(R.id.empty_hint_text)
        totalBeatsText = findViewById(R.id.total_beats_text)
        octaveValueText = findViewById(R.id.octave_value_text)
        tempoValueText = findViewById(R.id.tempo_value_text)
        playButton = findViewById(R.id.play_btn)
        stopButton = findViewById(R.id.stop_btn)
        clearButton = findViewById(R.id.clear_btn)
        octaveDownButton = findViewById(R.id.octave_down_btn)
        octaveUpButton = findViewById(R.id.octave_up_btn)
        tempoSeek = findViewById(R.id.tempo_seek)
        addNoteButtonsRow = findViewById(R.id.add_note_buttons_row)

        buildAddNoteButtons()
        setupOctaveControls()
        setupTempoControl()
        setupTransport()

        renderOctave()
        renderTempo()
        renderNotes()
        setGlobalControlsForPlayback(false)
    }

    private fun buildAddNoteButtons() {
        for (phthong in TrainerPhthong.ascending) {
            val button = Button(this).apply {
                text = phthong.displayName
                minWidth = dp(48)
                minimumWidth = dp(48)
                setOnClickListener { addNote(phthong) }
            }
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(4) }
            addNoteButtonsRow.addView(button, params)
        }
    }

    private fun setupOctaveControls() {
        octaveDownButton.setOnClickListener {
            currentOctaveShift = (currentOctaveShift - 1).coerceAtLeast(MIN_OCTAVE_SHIFT)
            renderOctave()
        }
        octaveUpButton.setOnClickListener {
            currentOctaveShift = (currentOctaveShift + 1).coerceAtMost(MAX_OCTAVE_SHIFT)
            renderOctave()
        }
    }

    private fun setupTempoControl() {
        tempoSeek.max = MelodyTempo.MAX_BPM - MelodyTempo.MIN_BPM
        tempoSeek.progress = bpm - MelodyTempo.MIN_BPM
        tempoSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                bpm = MelodyTempo.clampBpm(progress + MelodyTempo.MIN_BPM)
                renderTempo()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    private fun setupTransport() {
        playButton.setOnClickListener { startPlayback() }
        stopButton.setOnClickListener { stopPlayback() }
        clearButton.setOnClickListener {
            if (isPlaybackActive) return@setOnClickListener
            notes.clear()
            renderNotes()
        }
    }

    private fun addNote(phthong: TrainerPhthong) {
        if (isPlaybackActive) return
        notes.add(TrainerNote(phthong = phthong, octaveShift = currentOctaveShift))
        renderNotes()
    }

    private fun startPlayback() {
        if (isPlaybackActive || notes.isEmpty()) return
        val sequence = MelodySequence(notes.toList())
        val plan = MelodyPlaybackPlanner.plan(sequence, MelodyTempo.of(bpm))
        if (plan.isEmpty()) return
        isPlaybackActive = true
        setGlobalControlsForPlayback(true)
        renderNotes()
        player.play(plan, playerListener)
    }

    private fun stopPlayback() {
        if (!isPlaybackActive) return
        player.stop()
        onPlaybackStopped()
    }

    private fun onPlaybackStopped() {
        isPlaybackActive = false
        clearHighlight()
        setGlobalControlsForPlayback(false)
        renderNotes()
    }

    // region rendering

    private fun renderOctave() {
        octaveValueText.text = getString(R.string.melody_trainer_octave_label, octaveLabel(currentOctaveShift))
        octaveDownButton.isEnabled = !isPlaybackActive && currentOctaveShift > MIN_OCTAVE_SHIFT
        octaveUpButton.isEnabled = !isPlaybackActive && currentOctaveShift < MAX_OCTAVE_SHIFT
    }

    private fun renderTempo() {
        tempoValueText.text = getString(R.string.melody_trainer_tempo_value, bpm)
    }

    private fun renderNotes() {
        noteListContainer.removeAllViews()
        noteRowViews.clear()
        highlightedRow = -1

        val effectiveDurations = MelodySequence(notes.toList()).effectiveDurationsBeats()
        notes.forEachIndexed { index, note ->
            val effectiveBeats = effectiveDurations.getOrElse(index) { note.baseDurationBeats }
            val row = createNoteRow(index, note, effectiveBeats)
            noteRowViews.add(row)
            noteListContainer.addView(row)
        }

        emptyHintText.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
        totalBeatsText.text = getString(R.string.melody_trainer_total_beats, formatBeats(effectiveDurations.sum()))
        playButton.isEnabled = notes.isNotEmpty() && !isPlaybackActive
        clearButton.isEnabled = notes.isNotEmpty() && !isPlaybackActive
    }

    private fun createNoteRow(index: Int, note: TrainerNote, effectiveBeats: Float): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(6), dp(4), dp(6))
        }

        val label = TextView(this).apply {
            text = "${index + 1}. ${phthongDisplay(note)}"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
        }
        row.addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val durationEditable = !note.hasGorgo && !isPlaybackActive

        row.addView(
            Button(this).apply {
                text = "−"
                minWidth = dp(44)
                minimumWidth = dp(44)
                contentDescription = getString(R.string.melody_trainer_duration_decrease)
                isEnabled = durationEditable
                setOnClickListener { changeDuration(index, -DURATION_STEP) }
            },
            wrapContent()
        )

        row.addView(
            TextView(this).apply {
                text = getString(R.string.melody_trainer_note_duration, formatBeats(effectiveBeats))
                gravity = Gravity.CENTER
                minWidth = dp(56)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            },
            wrapContent()
        )

        row.addView(
            Button(this).apply {
                text = "+"
                minWidth = dp(44)
                minimumWidth = dp(44)
                contentDescription = getString(R.string.melody_trainer_duration_increase)
                isEnabled = durationEditable
                setOnClickListener { changeDuration(index, DURATION_STEP) }
            },
            wrapContent()
        )

        row.addView(
            Button(this).apply {
                // γοργόν shortens the *previous* note, so it is invalid on the first row.
                val gorgoAllowed = index > 0
                text = getString(R.string.melody_trainer_gorgo)
                isAllCaps = false
                minWidth = dp(64)
                minimumWidth = dp(64)
                alpha = if (note.hasGorgo) 1f else 0.45f
                isEnabled = !isPlaybackActive && gorgoAllowed
                setOnClickListener { toggleGorgo(index) }
            },
            wrapContent()
        )

        row.addView(
            Button(this).apply {
                text = "✕"
                minWidth = dp(44)
                minimumWidth = dp(44)
                contentDescription = getString(R.string.melody_trainer_note_remove)
                isEnabled = !isPlaybackActive
                setOnClickListener { removeNote(index) }
            },
            wrapContent()
        )

        return row
    }

    // endregion

    private fun changeDuration(index: Int, delta: Float) {
        if (isPlaybackActive) return
        val note = notes.getOrNull(index) ?: return
        if (note.hasGorgo) return
        val updated = (note.baseDurationBeats + delta).coerceIn(MIN_DURATION, MAX_DURATION)
        notes[index] = note.copy(baseDurationBeats = updated)
        renderNotes()
    }

    private fun toggleGorgo(index: Int) {
        if (isPlaybackActive) return
        if (index == 0) return // γοργόν needs a previous note to shorten
        val note = notes.getOrNull(index) ?: return
        notes[index] = note.withGorgo(!note.hasGorgo)
        renderNotes()
    }

    private fun removeNote(index: Int) {
        if (isPlaybackActive) return
        if (index !in notes.indices) return
        notes.removeAt(index)
        normalizeLeadingGorgo()
        renderNotes()
    }

    /** γοργόν is invalid on the first note, so strip it if a deletion shifted one to index 0. */
    private fun normalizeLeadingGorgo() {
        val first = notes.firstOrNull() ?: return
        if (first.hasGorgo) notes[0] = first.withGorgo(false)
    }

    private fun highlightRow(index: Int) {
        clearHighlight()
        noteRowViews.getOrNull(index)?.setBackgroundColor(HIGHLIGHT_COLOR)
        highlightedRow = index
    }

    private fun clearHighlight() {
        noteRowViews.getOrNull(highlightedRow)?.setBackgroundColor(Color.TRANSPARENT)
        highlightedRow = -1
    }

    private fun setGlobalControlsForPlayback(playing: Boolean) {
        stopButton.isEnabled = playing
        playButton.isEnabled = !playing && notes.isNotEmpty()
        clearButton.isEnabled = !playing && notes.isNotEmpty()
        tempoSeek.isEnabled = !playing
        for (button in addNoteButtonsRow.children) {
            button.isEnabled = !playing
        }
        octaveDownButton.isEnabled = !playing && currentOctaveShift > MIN_OCTAVE_SHIFT
        octaveUpButton.isEnabled = !playing && currentOctaveShift < MAX_OCTAVE_SHIFT
    }

    private fun phthongDisplay(note: TrainerNote): String {
        val suffix = when {
            note.octaveShift > 0 -> "΄".repeat(note.octaveShift)
            note.octaveShift < 0 -> ",".repeat(-note.octaveShift)
            else -> ""
        }
        return note.phthong.displayName + suffix
    }

    private fun octaveLabel(shift: Int): String = if (shift > 0) "+$shift" else shift.toString()

    private fun formatBeats(beats: Float): String {
        val halves = (beats * 2).roundToInt()
        val whole = halves / 2
        if (halves % 2 == 0) return whole.toString()
        val separator = DecimalFormatSymbols.getInstance(currentLocale()).decimalSeparator
        return "$whole${separator}5"
    }

    private fun currentLocale(): Locale = resources.configuration.locales.get(0)

    private fun wrapContent(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    override fun onPause() {
        super.onPause()
        if (isPlaybackActive) {
            player.stop()
            onPlaybackStopped()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

    private companion object {
        const val MIN_OCTAVE_SHIFT = -1
        const val MAX_OCTAVE_SHIFT = 1
        const val DURATION_STEP = 0.5f
        const val MIN_DURATION = 0.5f
        const val MAX_DURATION = 4.0f
        const val HIGHLIGHT_COLOR = 0x3300C853 // translucent green
    }
}
