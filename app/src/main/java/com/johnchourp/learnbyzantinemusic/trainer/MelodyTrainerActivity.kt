package com.johnchourp.learnbyzantinemusic.trainer

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Practice page where the user writes a sequence of phthongi (Νη … Ζω), sets how long
 * each one lasts in χρόνοι, picks a tempo, and then either:
 *  - Mode 1: plays the melody back at that tempo,
 *  - Mode 2 (voice check): sings while the mic greens each phthong said correctly, or
 *  - Mode 3 (timing exercise): after a 5-second countdown, sings each phthong on time and
 *    the notes said in the right χρόνο turn green.
 * The timing rules (γοργόν, κλάσμα) are documented at the top and applied through
 * [MelodySequence] / the shared ByzantineRhythmMapper.
 */
class MelodyTrainerActivity : BaseActivity() {

    private val notes = mutableListOf<TrainerNote>()
    private var currentOctaveShift = 0
    private var bpm = MelodyTempo.DEFAULT_BPM

    private var isPlaybackActive = false
    private var isVoiceActive = false
    private var isRhythmActive = false
    private var rhythmRequirePitch = false // false = time only (Mode 2), true = phthong + time (Mode 3)
    private var suppressVoiceSwitchCallback = false
    private var suppressRhythmSwitchCallback = false
    private var suppressComboSwitchCallback = false

    private val player = MelodySequencePlayer()
    private val pitchEngine by lazy {
        TrainerPitchEngine(onPitch = ::onPitchDetected, onCaptureError = ::onCaptureError)
    }
    private val uiHandler = Handler(Looper.getMainLooper())

    private var voiceEvaluator: PitchGreeningEvaluator? = null
    private var voiceCorrectCount = 0

    private var rhythmEvaluator: RhythmTimingEvaluator? = null
    private var rhythmPlan: List<PlannedNoteEvent> = emptyList()
    private var rhythmStartMillis = -1L
    private var rhythmTotalMillis = 0L
    private var rhythmCorrectCount = 0
    private var countdownRemaining = 0

    private val matchedIndices = mutableSetOf<Int>()

    private var pendingMicGrant: (() -> Unit)? = null
    private var pendingMicDeny: (() -> Unit)? = null

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
    private lateinit var rhythmCheckSwitch: CheckBox
    private lateinit var rhythmStatusText: TextView
    private lateinit var comboCheckSwitch: CheckBox
    private lateinit var comboStatusText: TextView

    private val noteRowViews = mutableListOf<View>()
    private var highlightedRow = -1

    private val rhythmEndRunnable = Runnable { finishRhythm() }

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val grant = pendingMicGrant
        val deny = pendingMicDeny
        pendingMicGrant = null
        pendingMicDeny = null
        if (granted) grant?.invoke() else deny?.invoke()
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
        rhythmCheckSwitch = findViewById(R.id.rhythm_check_switch)
        rhythmStatusText = findViewById(R.id.rhythm_status_text)
        comboCheckSwitch = findViewById(R.id.combo_check_switch)
        comboStatusText = findViewById(R.id.combo_status_text)

        buildAddNoteButtons()
        setupOctaveControls()
        setupTempoControl()
        setupTransport()
        setupVoiceCheck()
        setupRhythmCheck()
        setupComboCheck()

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
        stopButton.setOnClickListener { stopPlayback() }
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

    private fun setupRhythmCheck() {
        rhythmCheckSwitch.setOnCheckedChangeListener { _, checked ->
            if (suppressRhythmSwitchCallback) return@setOnCheckedChangeListener
            if (checked) requestRhythmSession(requirePitch = false) else stopRhythmSession(clearGreens = true)
        }
    }

    private fun setupComboCheck() {
        comboCheckSwitch.setOnCheckedChangeListener { _, checked ->
            if (suppressComboSwitchCallback) return@setOnCheckedChangeListener
            if (checked) requestRhythmSession(requirePitch = true) else stopRhythmSession(clearGreens = true)
        }
    }

    // region editing

    private val isBusy: Boolean get() = isPlaybackActive || isVoiceActive || isRhythmActive

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
        if (index == 0) return // γοργόν needs a previous note to shorten
        val note = notes.getOrNull(index) ?: return
        notes[index] = note.withGorgo(!note.hasGorgo)
        renderNotes()
    }

    private fun removeNote(index: Int) {
        if (isBusy) return
        if (index !in notes.indices) return
        notes.removeAt(index)
        normalizeLeadingGorgo()
        matchedIndices.clear()
        renderNotes()
    }

    /** γοργόν is invalid on the first note, so strip it if a deletion shifted one to index 0. */
    private fun normalizeLeadingGorgo() {
        val first = notes.firstOrNull() ?: return
        if (first.hasGorgo) notes[0] = first.withGorgo(false)
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

    private fun stopPlayback() {
        if (!isPlaybackActive) return
        player.stop()
        onPlaybackStopped()
    }

    private fun onPlaybackStopped() {
        isPlaybackActive = false
        clearHighlight()
        renderNotes()
    }

    // endregion

    // region microphone permission

    private fun ensureMicThen(onGranted: () -> Unit, onDenied: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            onGranted()
        } else {
            pendingMicGrant = onGranted
            pendingMicDeny = onDenied
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun onPitchDetected(match: PitchMatch?, capturedAtMillis: Long) {
        when {
            isVoiceActive -> handleVoiceFrame(match)
            isRhythmActive -> handleRhythmFrame(match, capturedAtMillis)
        }
    }

    // endregion

    // region Mode 2: voice check

    private fun requestVoiceSession() {
        if (isPlaybackActive || isRhythmActive) {
            setVoiceSwitchChecked(false)
            return
        }
        if (notes.isEmpty()) {
            setVoiceSwitchChecked(false)
            voiceStatusText.text = getString(R.string.melody_trainer_voice_need_notes)
            return
        }
        ensureMicThen(onGranted = ::startVoiceSession, onDenied = ::onVoiceMicDenied)
    }

    private fun onVoiceMicDenied() {
        setVoiceSwitchChecked(false)
        voiceStatusText.text = getString(R.string.melody_trainer_mic_permission_required)
    }

    private fun startVoiceSession() {
        if (notes.isEmpty()) {
            setVoiceSwitchChecked(false)
            return
        }
        matchedIndices.clear()
        voiceCorrectCount = 0
        voiceEvaluator = PitchGreeningEvaluator(notes.map { it.phthong })
        isVoiceActive = true
        renderNotes()
        if (!pitchEngine.start()) {
            isVoiceActive = false
            voiceEvaluator = null
            setVoiceSwitchChecked(false)
            voiceStatusText.text = getString(R.string.melody_trainer_mic_unavailable)
            renderNotes()
            return
        }
        updateVoiceStatus()
    }

    private fun handleVoiceFrame(match: PitchMatch?) {
        val evaluator = voiceEvaluator ?: return
        val result = evaluator.onFrame(match)
        if (result != null && result.matched) {
            matchedIndices.add(result.targetIndex)
            voiceCorrectCount++
            colorRow(result.targetIndex, MATCHED_COLOR)
        }
        if (evaluator.isComplete) {
            finishVoiceSession()
        } else {
            updateVoiceStatus()
        }
    }

    private fun onCaptureError() {
        if (isVoiceActive) {
            setVoiceSwitchChecked(false)
            stopVoiceSession(clearGreens = false)
            voiceStatusText.text = getString(R.string.melody_trainer_mic_unavailable)
        }
        if (isRhythmActive) {
            setActiveRhythmSwitchChecked(false)
            stopRhythmSession(clearGreens = false)
            activeRhythmStatus().text = getString(R.string.melody_trainer_mic_unavailable)
        }
    }

    private fun finishVoiceSession() {
        pitchEngine.stop()
        isVoiceActive = false
        setVoiceSwitchChecked(false)
        voiceStatusText.text = getString(
            R.string.melody_trainer_voice_done,
            voiceCorrectCount,
            notes.size
        )
        renderNotes()
    }

    private fun stopVoiceSession(clearGreens: Boolean) {
        val wasActive = isVoiceActive
        pitchEngine.stop()
        isVoiceActive = false
        voiceEvaluator = null
        if (clearGreens) {
            matchedIndices.clear()
        }
        if (wasActive) {
            voiceStatusText.text = getString(R.string.melody_trainer_voice_hint)
        }
        renderNotes()
    }

    private fun updateVoiceStatus() {
        val target = voiceEvaluator?.currentTarget() ?: return
        voiceStatusText.text = getString(R.string.melody_trainer_voice_listening, target.displayName)
    }

    private fun setVoiceSwitchChecked(checked: Boolean) {
        suppressVoiceSwitchCallback = true
        voiceCheckSwitch.isChecked = checked
        suppressVoiceSwitchCallback = false
    }

    // endregion

    // region Mode 3: rhythm timing

    private fun requestRhythmSession(requirePitch: Boolean) {
        if (isPlaybackActive || isVoiceActive || (isRhythmActive && rhythmRequirePitch != requirePitch)) {
            setRhythmSwitch(requirePitch, false)
            return
        }
        rhythmRequirePitch = requirePitch
        if (notes.isEmpty()) {
            setActiveRhythmSwitchChecked(false)
            activeRhythmStatus().text = getString(R.string.melody_trainer_voice_need_notes)
            return
        }
        ensureMicThen(onGranted = ::startRhythmCountdown, onDenied = ::onRhythmMicDenied)
    }

    private fun onRhythmMicDenied() {
        setActiveRhythmSwitchChecked(false)
        activeRhythmStatus().text = getString(R.string.melody_trainer_mic_permission_required)
    }

    private fun startRhythmCountdown() {
        if (notes.isEmpty()) {
            setActiveRhythmSwitchChecked(false)
            return
        }
        matchedIndices.clear()
        rhythmCorrectCount = 0
        rhythmStartMillis = -1L
        isRhythmActive = true
        renderNotes()
        countdownRemaining = RhythmTimingEvaluator.COUNTDOWN_SECONDS
        runCountdownTick()
    }

    private fun runCountdownTick() {
        if (!isRhythmActive) return
        if (countdownRemaining > 0) {
            activeRhythmStatus().text = getString(R.string.melody_trainer_rhythm_countdown, countdownRemaining)
            countdownRemaining--
            uiHandler.postDelayed({ runCountdownTick() }, COUNTDOWN_TICK_MILLIS)
        } else {
            activeRhythmStatus().text = getString(R.string.melody_trainer_rhythm_go)
            startRhythmClock()
        }
    }

    private fun startRhythmClock() {
        rhythmPlan = MelodyPlaybackPlanner.plan(MelodySequence(notes.toList()), MelodyTempo.of(bpm))
        if (rhythmPlan.isEmpty()) {
            stopRhythmSession(clearGreens = true)
            return
        }
        rhythmEvaluator = RhythmTimingEvaluator(rhythmPlan)
        rhythmTotalMillis = MelodyPlaybackPlanner.totalDurationMillis(rhythmPlan)
        if (!pitchEngine.start()) {
            // stopRhythmSession resets the status to the hint, so uncheck + stop first and
            // set the failure message last, otherwise the user never sees why it failed.
            setActiveRhythmSwitchChecked(false)
            stopRhythmSession(clearGreens = false)
            activeRhythmStatus().text = getString(R.string.melody_trainer_mic_unavailable)
            return
        }
        rhythmStartMillis = SystemClock.elapsedRealtime()
        uiHandler.postDelayed(rhythmEndRunnable, rhythmTotalMillis + RHYTHM_END_GRACE_MILLIS)
    }

    private fun handleRhythmFrame(match: PitchMatch?, capturedAtMillis: Long) {
        val evaluator = rhythmEvaluator ?: return
        if (rhythmStartMillis < 0L) return
        // Use the time the audio frame was captured (on the audio thread), not now, so
        // main-thread queueing/GC delay cannot shift an on-time onset past tolerance.
        val elapsed = capturedAtMillis - rhythmStartMillis

        // Time-only (Mode 2) scores voicing in the right window regardless of pitch. The
        // combined Mode 3 additionally gates voicing on singing the *expected* phthong, so a
        // note greens only when sung at the right pitch AND time.
        val voicedNow = if (!rhythmRequirePitch) {
            match != null
        } else {
            val expected = notes.getOrNull(evaluator.activeNoteIndex(elapsed))?.phthong
            ComboPitchGate.isCorrectPhthong(match, expected)
        }

        val verdict = evaluator.onFrame(elapsed, voicedNow)
        if (verdict != null && verdict.matched) {
            matchedIndices.add(verdict.noteIndex)
            rhythmCorrectCount++
            colorRow(verdict.noteIndex, MATCHED_COLOR)
        }

        val active = evaluator.activeNoteIndex(elapsed)
        if (active in notes.indices && !matchedIndices.contains(active)) {
            highlightRow(active)
            activeRhythmStatus().text = getString(
                R.string.melody_trainer_rhythm_running,
                phthongDisplay(notes[active])
            )
        } else {
            clearHighlight()
        }

        // Only end once the singer has actually gone silent (or the safety grace fires), so a
        // final note held well past its tolerance is judged on its real release instead of
        // being closed at the scheduled end and counted correct.
        if (elapsed >= rhythmTotalMillis && match == null) {
            finishRhythm()
        }
    }

    private fun finishRhythm() {
        if (!isRhythmActive) return
        uiHandler.removeCallbacks(rhythmEndRunnable)
        val elapsed = if (rhythmStartMillis >= 0L) SystemClock.elapsedRealtime() - rhythmStartMillis else 0L
        val verdict = rhythmEvaluator?.finish(elapsed)
        if (verdict != null && verdict.matched) {
            matchedIndices.add(verdict.noteIndex)
            rhythmCorrectCount++
        }
        pitchEngine.stop()
        isRhythmActive = false
        rhythmStartMillis = -1L
        rhythmEvaluator = null
        clearHighlight()
        val doneRes =
            if (rhythmRequirePitch) R.string.melody_trainer_combo_done else R.string.melody_trainer_rhythm_done
        setActiveRhythmSwitchChecked(false)
        activeRhythmStatus().text = getString(doneRes, rhythmCorrectCount, notes.size)
        renderNotes()
    }

    private fun stopRhythmSession(clearGreens: Boolean) {
        val wasActive = isRhythmActive
        uiHandler.removeCallbacksAndMessages(null)
        pitchEngine.stop()
        isRhythmActive = false
        rhythmStartMillis = -1L
        rhythmEvaluator = null
        if (clearGreens) {
            matchedIndices.clear()
        }
        clearHighlight()
        if (wasActive) {
            val hintRes =
                if (rhythmRequirePitch) R.string.melody_trainer_combo_hint else R.string.melody_trainer_rhythm_hint
            activeRhythmStatus().text = getString(hintRes)
        }
        renderNotes()
    }

    /** The status TextView for the currently-selected rhythm mode (time-only vs phthong+time). */
    private fun activeRhythmStatus(): TextView = if (rhythmRequirePitch) comboStatusText else rhythmStatusText

    private fun setActiveRhythmSwitchChecked(checked: Boolean) = setRhythmSwitch(rhythmRequirePitch, checked)

    private fun setRhythmSwitch(requirePitch: Boolean, checked: Boolean) {
        if (requirePitch) {
            suppressComboSwitchCallback = true
            comboCheckSwitch.isChecked = checked
            suppressComboSwitchCallback = false
        } else {
            suppressRhythmSwitchCallback = true
            rhythmCheckSwitch.isChecked = checked
            suppressRhythmSwitchCallback = false
        }
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

        val effectiveDurations = MelodySequence(notes.toList()).effectiveDurationsBeats()
        notes.forEachIndexed { index, note ->
            val effectiveBeats = effectiveDurations.getOrElse(index) { note.baseDurationBeats }
            val row = createNoteRow(index, note, effectiveBeats)
            noteRowViews.add(row)
            noteListContainer.addView(row)
        }

        emptyHintText.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
        totalBeatsText.text = getString(R.string.melody_trainer_total_beats, formatBeats(effectiveDurations.sum()))
        applyControlState()
    }

    private fun createNoteRow(index: Int, note: TrainerNote, effectiveBeats: Float): View {
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
                isEnabled = !isBusy && gorgoAllowed
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
        if (index == highlightedRow) return
        clearHighlight()
        noteRowViews.getOrNull(index)?.setBackgroundColor(HIGHLIGHT_COLOR)
        highlightedRow = index
    }

    private fun clearHighlight() {
        if (highlightedRow < 0) return
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
        // The three mode toggles are mutually exclusive: the active one stays enabled (to turn
        // off), the others are disabled while any mode runs.
        voiceCheckSwitch.isEnabled = !isPlaybackActive && !isRhythmActive
        rhythmCheckSwitch.isEnabled = !isPlaybackActive && !isVoiceActive && !(isRhythmActive && rhythmRequirePitch)
        comboCheckSwitch.isEnabled = !isPlaybackActive && !isVoiceActive && !(isRhythmActive && !rhythmRequirePitch)
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
        if (isVoiceActive) {
            setVoiceSwitchChecked(false)
            stopVoiceSession(clearGreens = false)
        }
        if (isRhythmActive) {
            setActiveRhythmSwitchChecked(false)
            stopRhythmSession(clearGreens = false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        uiHandler.removeCallbacksAndMessages(null)
        player.release()
        pitchEngine.stop()
    }

    private companion object {
        const val MIN_OCTAVE_SHIFT = -1
        const val MAX_OCTAVE_SHIFT = 1
        const val DURATION_STEP = 0.5f
        const val MIN_DURATION = 0.5f
        const val MAX_DURATION = 4.0f
        const val COUNTDOWN_TICK_MILLIS = 1_000L
        const val RHYTHM_END_GRACE_MILLIS = 1_500L
        const val HIGHLIGHT_COLOR = 0x33FFC107 // translucent amber: note currently active
        const val MATCHED_COLOR = 0x6600C853 // green: phthong sung / timed correctly
    }
}
