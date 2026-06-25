package com.johnchourp.learnbyzantinemusic.trainer

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.modes.PhthongTonePlayer
import kotlin.math.roundToInt

/**
 * Practice page where the user writes a sequence of phthongi (Νη … Ζω), sets how long
 * each one lasts in χρόνοι, picks a tempo, and either:
 *  - Mode 1: plays the melody back at that tempo, or
 *  - Mode 2 (voice check): sings the melody while the microphone greens each phthong said
 *    correctly and advances through the sequence.
 * The timing rules (γοργόν, κλάσμα) are documented at the top and applied through
 * [MelodySequence] / the shared ByzantineRhythmMapper.
 */
class MelodyTrainerActivity : BaseActivity() {

    private val notes = mutableListOf<TrainerNote>()
    private var currentOctaveShift = 0
    private var bpm = MelodyTempo.DEFAULT_BPM

    private var isPlaybackActive = false
    private var isVoiceActive = false
    private var suppressVoiceSwitchCallback = false

    private val tonePlayer = PhthongTonePlayer()
    private val player by lazy { MelodySequencePlayer(tonePlayer) }
    private val pitchEngine by lazy { TrainerPitchEngine(onPitch = ::onPitchDetected) }
    private var evaluator: PitchGreeningEvaluator? = null
    private val matchedIndices = mutableSetOf<Int>()
    private var correctCount = 0

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
    private lateinit var voiceCheckSwitch: CheckBox
    private lateinit var voiceStatusText: TextView

    private val noteRowViews = mutableListOf<View>()
    private var highlightedRow = -1

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceSession()
        } else {
            setVoiceSwitchChecked(false)
            voiceStatusText.text = getString(R.string.melody_trainer_mic_permission_required)
        }
    }

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
        voiceCheckSwitch = findViewById(R.id.voice_check_switch)
        voiceStatusText = findViewById(R.id.voice_status_text)

        buildAddNoteButtons()
        setupOctaveControls()
        setupTempoControl()
        setupTransport()
        setupVoiceCheck()

        renderOctave()
        renderTempo()
        renderNotes()
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
        stopButton.setOnClickListener { player.stop() }
        clearButton.setOnClickListener {
            if (isBusy) return@setOnClickListener
            notes.clear()
            matchedIndices.clear()
            renderNotes()
        }
    }

    private fun setupVoiceCheck() {
        voiceCheckSwitch.setOnCheckedChangeListener { _, checked ->
            if (suppressVoiceSwitchCallback) return@setOnCheckedChangeListener
            if (checked) requestVoiceSession() else stopVoiceSession(clearGreens = true)
        }
    }

    // region editing

    private val isBusy: Boolean get() = isPlaybackActive || isVoiceActive

    private fun addNote(phthong: TrainerPhthong) {
        if (isBusy) return
        notes.add(TrainerNote(phthong = phthong, octaveShift = currentOctaveShift))
        renderNotes()
    }

    private fun changeDuration(index: Int, delta: Float) {
        if (isBusy) return
        val note = notes.getOrNull(index) ?: return
        if (note.hasGorgo) return
        val updated = (note.baseDurationBeats + delta).coerceIn(MIN_DURATION, MAX_DURATION)
        notes[index] = note.copy(baseDurationBeats = updated)
        renderNotes()
    }

    private fun toggleGorgo(index: Int) {
        if (isBusy) return
        val note = notes.getOrNull(index) ?: return
        notes[index] = note.withGorgo(!note.hasGorgo)
        renderNotes()
    }

    private fun removeNote(index: Int) {
        if (isBusy) return
        if (index !in notes.indices) return
        notes.removeAt(index)
        matchedIndices.clear()
        renderNotes()
    }

    // endregion

    // region Mode 1: playback

    private fun startPlayback() {
        if (isBusy || notes.isEmpty()) return
        val sequence = MelodySequence(notes.toList())
        val plan = MelodyPlaybackPlanner.plan(sequence, MelodyTempo.of(bpm))
        if (plan.isEmpty()) return
        isPlaybackActive = true
        renderNotes()
        player.play(plan, playerListener)
    }

    private fun onPlaybackStopped() {
        isPlaybackActive = false
        clearHighlight()
        renderNotes()
    }

    // endregion

    // region Mode 2: voice check

    private fun requestVoiceSession() {
        if (isPlaybackActive) {
            setVoiceSwitchChecked(false)
            return
        }
        if (notes.isEmpty()) {
            setVoiceSwitchChecked(false)
            voiceStatusText.text = getString(R.string.melody_trainer_voice_need_notes)
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            startVoiceSession()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceSession() {
        if (notes.isEmpty()) {
            setVoiceSwitchChecked(false)
            return
        }
        matchedIndices.clear()
        correctCount = 0
        evaluator = PitchGreeningEvaluator(notes.map { it.phthong })
        isVoiceActive = true
        renderNotes()
        if (!pitchEngine.start()) {
            isVoiceActive = false
            evaluator = null
            setVoiceSwitchChecked(false)
            voiceStatusText.text = getString(R.string.melody_trainer_mic_unavailable)
            renderNotes()
            return
        }
        updateVoiceStatus()
    }

    private fun onPitchDetected(match: PitchMatch?) {
        val activeEvaluator = evaluator ?: return
        if (!isVoiceActive) return
        val result = activeEvaluator.onFrame(match)
        if (result != null && result.matched) {
            matchedIndices.add(result.targetIndex)
            correctCount++
            colorRow(result.targetIndex, MATCHED_COLOR)
        }
        if (activeEvaluator.isComplete) {
            finishVoiceSession()
        } else {
            updateVoiceStatus()
        }
    }

    private fun finishVoiceSession() {
        pitchEngine.stop()
        isVoiceActive = false
        setVoiceSwitchChecked(false)
        voiceStatusText.text = getString(
            R.string.melody_trainer_voice_done,
            correctCount,
            notes.size
        )
        applyControlState()
    }

    private fun stopVoiceSession(clearGreens: Boolean) {
        val wasActive = isVoiceActive
        pitchEngine.stop()
        isVoiceActive = false
        evaluator = null
        if (clearGreens) {
            matchedIndices.clear()
        }
        if (wasActive) {
            voiceStatusText.text = getString(R.string.melody_trainer_voice_hint)
        }
        renderNotes()
    }

    private fun updateVoiceStatus() {
        val target = evaluator?.currentTarget() ?: return
        voiceStatusText.text = getString(R.string.melody_trainer_voice_listening, target.displayName)
    }

    private fun setVoiceSwitchChecked(checked: Boolean) {
        suppressVoiceSwitchCallback = true
        voiceCheckSwitch.isChecked = checked
        suppressVoiceSwitchCallback = false
    }

    // endregion

    // region rendering

    private fun renderOctave() {
        octaveValueText.text = getString(R.string.melody_trainer_octave_label, octaveLabel(currentOctaveShift))
        octaveDownButton.isEnabled = !isBusy && currentOctaveShift > MIN_OCTAVE_SHIFT
        octaveUpButton.isEnabled = !isBusy && currentOctaveShift < MAX_OCTAVE_SHIFT
    }

    private fun renderTempo() {
        tempoValueText.text = getString(R.string.melody_trainer_tempo_value, bpm)
    }

    private fun renderNotes() {
        noteListContainer.removeAllViews()
        noteRowViews.clear()
        highlightedRow = -1

        notes.forEachIndexed { index, note ->
            val row = createNoteRow(index, note)
            noteRowViews.add(row)
            noteListContainer.addView(row)
        }

        emptyHintText.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
        val total = MelodySequence(notes.toList()).totalBeats()
        totalBeatsText.text = getString(R.string.melody_trainer_total_beats, formatBeats(total))
        applyControlState()
    }

    private fun createNoteRow(index: Int, note: TrainerNote): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(6), dp(4), dp(6))
            if (matchedIndices.contains(index)) {
                setBackgroundColor(MATCHED_COLOR)
            }
        }

        val label = TextView(this).apply {
            text = "${index + 1}. ${phthongDisplay(note)}"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
        }
        row.addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val durationEditable = !note.hasGorgo && !isBusy

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
                text = getString(R.string.melody_trainer_note_duration, effectiveDurationLabel(note))
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
                text = getString(R.string.melody_trainer_gorgo)
                isAllCaps = false
                minWidth = dp(64)
                minimumWidth = dp(64)
                alpha = if (note.hasGorgo) 1f else 0.45f
                isEnabled = !isBusy
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
                isEnabled = !isBusy
                setOnClickListener { removeNote(index) }
            },
            wrapContent()
        )

        return row
    }

    private fun highlightRow(index: Int) {
        clearHighlight()
        noteRowViews.getOrNull(index)?.setBackgroundColor(HIGHLIGHT_COLOR)
        highlightedRow = index
    }

    private fun clearHighlight() {
        val restore = if (matchedIndices.contains(highlightedRow)) MATCHED_COLOR else Color.TRANSPARENT
        noteRowViews.getOrNull(highlightedRow)?.setBackgroundColor(restore)
        highlightedRow = -1
    }

    private fun colorRow(index: Int, color: Int) {
        noteRowViews.getOrNull(index)?.setBackgroundColor(color)
    }

    private fun applyControlState() {
        stopButton.isEnabled = isPlaybackActive
        playButton.isEnabled = !isBusy && notes.isNotEmpty()
        clearButton.isEnabled = !isBusy && notes.isNotEmpty()
        tempoSeek.isEnabled = !isBusy
        voiceCheckSwitch.isEnabled = !isPlaybackActive
        for (button in addNoteButtonsRow.children) {
            button.isEnabled = !isBusy
        }
        octaveDownButton.isEnabled = !isBusy && currentOctaveShift > MIN_OCTAVE_SHIFT
        octaveUpButton.isEnabled = !isBusy && currentOctaveShift < MAX_OCTAVE_SHIFT
    }

    // endregion

    private fun phthongDisplay(note: TrainerNote): String {
        val suffix = when {
            note.octaveShift > 0 -> "΄".repeat(note.octaveShift)
            note.octaveShift < 0 -> ",".repeat(-note.octaveShift)
            else -> ""
        }
        return note.phthong.displayName + suffix
    }

    private fun effectiveDurationLabel(note: TrainerNote): String =
        if (note.hasGorgo) formatBeats(GORGO_BEATS) else formatBeats(note.baseDurationBeats)

    private fun octaveLabel(shift: Int): String = if (shift > 0) "+$shift" else shift.toString()

    private fun formatBeats(beats: Float): String {
        val halves = (beats * 2).roundToInt()
        val whole = halves / 2
        return if (halves % 2 == 0) whole.toString() else "$whole,5"
    }

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
        if (isVoiceActive) {
            setVoiceSwitchChecked(false)
            stopVoiceSession(clearGreens = false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.stop()
        tonePlayer.release()
        pitchEngine.stop()
    }

    private companion object {
        const val MIN_OCTAVE_SHIFT = -1
        const val MAX_OCTAVE_SHIFT = 1
        const val DURATION_STEP = 0.5f
        const val MIN_DURATION = 0.5f
        const val MAX_DURATION = 4.0f
        const val GORGO_BEATS = 0.5f
        const val HIGHLIGHT_COLOR = 0x33FFC107 // translucent amber: note currently playing
        const val MATCHED_COLOR = 0x6600C853 // green: phthong sung correctly
    }
}
