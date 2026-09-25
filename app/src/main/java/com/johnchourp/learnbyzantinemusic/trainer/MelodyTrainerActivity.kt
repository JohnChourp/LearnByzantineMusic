package com.johnchourp.learnbyzantinemusic.trainer

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
import com.johnchourp.learnbyzantinemusic.trainer.ui.MelodyTrainerScreen
import com.johnchourp.learnbyzantinemusic.trainer.ui.MelodyTrainerUiState
import com.johnchourp.learnbyzantinemusic.trainer.ui.PracticeModeUi
import com.johnchourp.learnbyzantinemusic.trainer.ui.TrainerNoteUi
import com.johnchourp.learnbyzantinemusic.trainer.ui.TrainerScaleUi
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme
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
 *
 * This Activity is the state holder / "brain": it owns the melody, the audio player, the mic
 * pitch engine, the evaluators and all timing, and it hosts the redesigned Compose
 * [MelodyTrainerScreen] via [setContent]. After any change it rebuilds an immutable
 * [MelodyTrainerUiState] that the screen renders. The timing rules (γοργόν, κλάσμα) are
 * applied through [MelodySequence] / the shared ByzantineRhythmMapper.
 *
 * Every pitch — what playback sounds and what the voice is judged against — comes from one
 * [TrainerScale] (ClickUp `869f5x24v`): «Διατονικός», the default and the Trainer as it always was,
 * or a ήχος on the same ladder the 8 Ήχοι page uses, with that ήχος's own «Μεταφορά βάσης», read
 * from and written to the key the 8 Ήχοι page keeps for it.
 */
class MelodyTrainerActivity : BaseActivity() {

    private val notes = mutableListOf<TrainerNote>()
    private var currentOctaveShift = 0
    private var bpm = MelodyTempo.DEFAULT_BPM

    /** Where every pitch comes from. [TrainerScale.DIATONIC] reproduces the Trainer before F2. */
    private var scale = TrainerScale.DIATONIC

    /** The 8 Ήχοι store: the per-mode «Μεταφορά βάσης» lives there, shared with that page. */
    private val modePrefs: SharedPreferences by lazy { AppPrefs.open(this, AppPrefs.Store.EIGHT_MODES) }

    private var isPlaybackActive = false
    private var isVoiceActive = false
    private var isRhythmActive = false
    private var rhythmRequirePitch = false // false = time only (Mode 2), true = phthong + time (Mode 3)

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

    /** Note currently playing (Mode 1) or expected (Mode 3); -1 when none. Drives the amber glow. */
    private var activeIndex = -1

    // Already-resolved status sentences for the three practice-mode cards.
    private var voiceStatus = ""
    private var rhythmStatus = ""
    private var comboStatus = ""

    private var pendingMicGrant: (() -> Unit)? = null
    private var pendingMicDeny: (() -> Unit)? = null

    private val phthongLabels = PhthongName.entries.map { it.displayName }

    private var uiState by mutableStateOf(MelodyTrainerUiState())

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

        voiceStatus = getString(R.string.melody_trainer_voice_hint)
        rhythmStatus = getString(R.string.melody_trainer_rhythm_hint)
        comboStatus = getString(R.string.melody_trainer_combo_hint)
        rebuildState()

        setContent {
            LbmTheme(palette = currentPalette()) {
                MelodyTrainerScreen(
                    state = uiState,
                    phthongLabels = phthongLabels,
                    onBack = ::finish,
                    onAddPhthong = { index -> PhthongName.entries.getOrNull(index)?.let(::addNote) },
                    onOctaveDown = ::octaveDown,
                    onOctaveUp = ::octaveUp,
                    onDecrementDuration = { index -> changeDuration(index, -DURATION_STEP) },
                    onIncrementDuration = { index -> changeDuration(index, DURATION_STEP) },
                    onToggleGorgo = ::toggleGorgo,
                    onRemoveNote = ::removeNote,
                    onSelectScale = ::selectScale,
                    onBaseShiftChange = ::changeBaseShift,
                    onTempoChange = ::changeTempo,
                    onPlay = ::startPlayback,
                    onStop = ::stopPlayback,
                    onClear = ::clearSequence,
                    onToggleVoice = { checked ->
                        if (checked) requestVoiceSession() else stopVoiceSession(clearGreens = true)
                    },
                    onToggleRhythm = { checked ->
                        if (checked) requestRhythmSession(requirePitch = false) else stopRhythmSession(clearGreens = true)
                    },
                    onToggleCombo = { checked ->
                        if (checked) requestRhythmSession(requirePitch = true) else stopRhythmSession(clearGreens = true)
                    },
                )
            }
        }
    }

    // region editing

    private val isBusy: Boolean get() = isPlaybackActive || isVoiceActive || isRhythmActive

    private fun addNote(phthong: PhthongName) {
        if (isBusy) return
        notes.add(TrainerNote(phthong = phthong, octaveShift = currentOctaveShift))
        rebuildState()
    }

    private fun octaveDown() {
        if (isBusy) return
        currentOctaveShift = (currentOctaveShift - 1).coerceAtLeast(MIN_OCTAVE_SHIFT)
        rebuildState()
    }

    private fun octaveUp() {
        if (isBusy) return
        currentOctaveShift = (currentOctaveShift + 1).coerceAtMost(MAX_OCTAVE_SHIFT)
        rebuildState()
    }

    private fun changeTempo(newBpm: Int) {
        if (isBusy) return
        bpm = MelodyTempo.clampBpm(newBpm)
        rebuildState()
    }

    /**
     * Puts the Trainer on [mode]'s ladder at the «Μεταφορά βάσης» the 8 Ήχοι page has saved for it,
     * or back on «Διατονικός» (null), which is always at shift 0. Clamped on read, like that page.
     */
    private fun selectScale(mode: Mode?) {
        if (isBusy) return
        val shift = if (mode == null) {
            BaseShift.DEFAULT_MORIA
        } else {
            BaseShift.clamp(modePrefs.getInt(AppPrefs.baseShiftKeyName(mode.key), BaseShift.DEFAULT_MORIA))
        }
        scale = TrainerScale(mode, shift)
        rebuildState()
    }

    /** Moves the chosen ήχος's base and saves it under that ήχος's own key, shared with 8 Ήχοι. */
    private fun changeBaseShift(moria: Int) {
        if (isBusy) return
        val mode = scale.mode ?: return
        val bounded = BaseShift.clamp(moria)
        if (bounded == scale.baseShiftMoria) return
        scale = scale.copy(baseShiftMoria = bounded)
        modePrefs.edit().putInt(AppPrefs.baseShiftKeyName(mode.key), bounded).apply()
        rebuildState()
    }

    private fun changeDuration(index: Int, delta: Float) {
        if (isBusy) return
        val note = notes.getOrNull(index) ?: return
        if (note.hasGorgo) return
        val updated = (note.baseDurationBeats + delta).coerceIn(MIN_DURATION, MAX_DURATION)
        notes[index] = note.copy(baseDurationBeats = updated)
        rebuildState()
    }

    private fun toggleGorgo(index: Int) {
        if (isBusy) return
        if (index == 0) return // γοργόν needs a previous note to shorten
        val note = notes.getOrNull(index) ?: return
        notes[index] = note.withGorgo(!note.hasGorgo)
        rebuildState()
    }

    private fun removeNote(index: Int) {
        if (isBusy) return
        if (index !in notes.indices) return
        notes.removeAt(index)
        normalizeLeadingGorgo()
        matchedIndices.clear()
        rebuildState()
    }

    private fun clearSequence() {
        if (isBusy) return
        notes.clear()
        matchedIndices.clear()
        rebuildState()
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
        val plan = MelodyPlaybackPlanner.plan(sequence, MelodyTempo.of(bpm), scale::frequencyHz)
        if (plan.isEmpty()) return
        isPlaybackActive = true
        rebuildState()
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
        rebuildState()
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

    /**
     * The engine matches against the fixed diatonic table; its [PitchMatch.frequencyHz] is the pitch
     * as sung, so it is read again on the Trainer's own scale before any evaluator sees it. Silence
     * stays null.
     */
    private fun onPitchDetected(match: PitchMatch?, capturedAtMillis: Long) {
        val onScale = match?.let { scale.match(it.frequencyHz) }
        when {
            isVoiceActive -> handleVoiceFrame(onScale)
            isRhythmActive -> handleRhythmFrame(onScale, capturedAtMillis)
        }
    }

    // endregion

    // region Mode 2: voice check

    private fun requestVoiceSession() {
        if (isPlaybackActive || isRhythmActive) {
            rebuildState()
            return
        }
        if (notes.isEmpty()) {
            voiceStatus = getString(R.string.melody_trainer_voice_need_notes)
            rebuildState()
            return
        }
        ensureMicThen(onGranted = ::startVoiceSession, onDenied = ::onVoiceMicDenied)
    }

    private fun onVoiceMicDenied() {
        voiceStatus = getString(R.string.melody_trainer_mic_permission_required)
        rebuildState()
    }

    private fun startVoiceSession() {
        if (notes.isEmpty()) {
            rebuildState()
            return
        }
        matchedIndices.clear()
        voiceCorrectCount = 0
        voiceEvaluator = PitchGreeningEvaluator(notes.map { it.phthong })
        isVoiceActive = true
        rebuildState()
        if (!pitchEngine.start()) {
            isVoiceActive = false
            voiceEvaluator = null
            voiceStatus = getString(R.string.melody_trainer_mic_unavailable)
            rebuildState()
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
        }
        if (evaluator.isComplete) {
            finishVoiceSession()
        } else {
            updateVoiceStatus()
        }
    }

    private fun onCaptureError() {
        if (isVoiceActive) {
            stopVoiceSession(clearGreens = false)
            voiceStatus = getString(R.string.melody_trainer_mic_unavailable)
            rebuildState()
        }
        if (isRhythmActive) {
            stopRhythmSession(clearGreens = false)
            setActiveRhythmStatus(getString(R.string.melody_trainer_mic_unavailable))
            rebuildState()
        }
    }

    private fun finishVoiceSession() {
        pitchEngine.stop()
        isVoiceActive = false
        voiceStatus = getString(
            R.string.melody_trainer_voice_done,
            voiceCorrectCount,
            notes.size
        )
        rebuildState()
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
            voiceStatus = getString(R.string.melody_trainer_voice_hint)
        }
        rebuildState()
    }

    private fun updateVoiceStatus() {
        val target = voiceEvaluator?.currentTarget() ?: return
        voiceStatus = getString(R.string.melody_trainer_voice_listening, target.displayName)
        rebuildState()
    }

    // endregion

    // region Mode 3: rhythm timing

    private fun requestRhythmSession(requirePitch: Boolean) {
        if (isPlaybackActive || isVoiceActive || (isRhythmActive && rhythmRequirePitch != requirePitch)) {
            rebuildState()
            return
        }
        rhythmRequirePitch = requirePitch
        if (notes.isEmpty()) {
            setActiveRhythmStatus(getString(R.string.melody_trainer_voice_need_notes))
            rebuildState()
            return
        }
        ensureMicThen(onGranted = ::startRhythmCountdown, onDenied = ::onRhythmMicDenied)
    }

    private fun onRhythmMicDenied() {
        setActiveRhythmStatus(getString(R.string.melody_trainer_mic_permission_required))
        rebuildState()
    }

    private fun startRhythmCountdown() {
        if (notes.isEmpty()) {
            rebuildState()
            return
        }
        matchedIndices.clear()
        rhythmCorrectCount = 0
        rhythmStartMillis = -1L
        isRhythmActive = true
        rebuildState()
        countdownRemaining = RhythmTimingEvaluator.COUNTDOWN_SECONDS
        runCountdownTick()
    }

    private fun runCountdownTick() {
        if (!isRhythmActive) return
        if (countdownRemaining > 0) {
            setActiveRhythmStatus(getString(R.string.melody_trainer_rhythm_countdown, countdownRemaining))
            rebuildState()
            countdownRemaining--
            uiHandler.postDelayed({ runCountdownTick() }, COUNTDOWN_TICK_MILLIS)
        } else {
            setActiveRhythmStatus(getString(R.string.melody_trainer_rhythm_go))
            rebuildState()
            startRhythmClock()
        }
    }

    private fun startRhythmClock() {
        rhythmPlan = MelodyPlaybackPlanner.plan(
            MelodySequence(notes.toList()),
            MelodyTempo.of(bpm),
            scale::frequencyHz,
        )
        if (rhythmPlan.isEmpty()) {
            stopRhythmSession(clearGreens = true)
            return
        }
        rhythmEvaluator = RhythmTimingEvaluator(rhythmPlan, requirePitch = rhythmRequirePitch)
        rhythmTotalMillis = MelodyPlaybackPlanner.totalDurationMillis(rhythmPlan)
        if (!pitchEngine.start()) {
            // stopRhythmSession resets the status to the hint, so stop first and set the failure
            // message last, otherwise the user never sees why it failed.
            stopRhythmSession(clearGreens = false)
            setActiveRhythmStatus(getString(R.string.melody_trainer_mic_unavailable))
            rebuildState()
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

        // The evaluator scores time-only vs phthong+time itself (per requirePitch). We always
        // hand it the in-tune phthong; in time-only mode it is ignored, in combined mode the
        // engine segments by phthong and matches it against the assigned note — so an early
        // pitch change and a still-held final note are handled by the timing engine itself.
        val inTunePhthong = ComboPitchGate.inTunePhthong(match)
        val verdict = evaluator.onFrame(elapsed, voicedNow = match != null, phthong = inTunePhthong)
        if (verdict != null && verdict.matched) {
            matchedIndices.add(verdict.noteIndex)
            rhythmCorrectCount++
        }

        val active = evaluator.activeNoteIndex(elapsed)
        if (active in notes.indices && !matchedIndices.contains(active)) {
            activeIndex = active
            setActiveRhythmStatus(
                getString(R.string.melody_trainer_rhythm_running, notes[active].pitch.label)
            )
        } else {
            activeIndex = -1
        }
        rebuildState()

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
        activeIndex = -1
        val doneRes =
            if (rhythmRequirePitch) R.string.melody_trainer_combo_done else R.string.melody_trainer_rhythm_done
        setActiveRhythmStatus(getString(doneRes, rhythmCorrectCount, notes.size))
        rebuildState()
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
        activeIndex = -1
        if (wasActive) {
            val hintRes =
                if (rhythmRequirePitch) R.string.melody_trainer_combo_hint else R.string.melody_trainer_rhythm_hint
            setActiveRhythmStatus(getString(hintRes))
        }
        rebuildState()
    }

    /** Writes a status sentence to whichever rhythm-mode card (time-only vs phthong+time) is current. */
    private fun setActiveRhythmStatus(text: String) {
        if (rhythmRequirePitch) comboStatus = text else rhythmStatus = text
    }

    // endregion

    // region rendering

    private fun highlightRow(index: Int) {
        if (index == activeIndex) return
        activeIndex = index
        rebuildState()
    }

    private fun clearHighlight() {
        if (activeIndex < 0) return
        activeIndex = -1
        rebuildState()
    }

    /** Recomputes the immutable UI state the Compose screen renders. Always called on the main thread. */
    private fun rebuildState() {
        val effective = MelodySequence(notes.toList()).effectiveDurationsBeats()
        val noteUis = notes.mapIndexed { index, note ->
            val beats = effective.getOrElse(index) { note.baseDurationBeats }
            TrainerNoteUi(
                index = index,
                phthongLabel = note.pitch.label,
                beatsLabel = formatBeats(beats),
                hasGorgo = note.hasGorgo,
                editable = !isBusy,
                matched = matchedIndices.contains(index),
                active = index == activeIndex,
            )
        }
        val nowPlaying = if (isPlaybackActive && activeIndex in notes.indices) {
            notes[activeIndex].pitch.label
        } else {
            null
        }
        val total = notes.size
        uiState = MelodyTrainerUiState(
            notes = noteUis,
            totalBeatsLabel = formatBeats(effective.sum()),
            octaveLabel = octaveLabel(currentOctaveShift),
            octaveDownEnabled = !isBusy && currentOctaveShift > MIN_OCTAVE_SHIFT,
            octaveUpEnabled = !isBusy && currentOctaveShift < MAX_OCTAVE_SHIFT,
            bpm = bpm,
            tempoEnabled = !isBusy,
            playEnabled = !isBusy && notes.isNotEmpty(),
            stopEnabled = isPlaybackActive,
            clearEnabled = !isBusy && notes.isNotEmpty(),
            addEnabled = !isBusy,
            nowPlayingLabel = nowPlaying,
            scale = TrainerScaleUi(
                mode = scale.mode,
                baseShiftMoria = scale.baseShiftMoria,
                enabled = !isBusy,
            ),
            voice = PracticeModeUi(
                checked = isVoiceActive,
                enabled = !isPlaybackActive && !isRhythmActive,
                status = voiceStatus,
                progress = if (isVoiceActive) correctProgress(voiceCorrectCount, total) else null,
            ),
            rhythm = PracticeModeUi(
                checked = isRhythmActive && !rhythmRequirePitch,
                enabled = !isPlaybackActive && !isVoiceActive && !(isRhythmActive && rhythmRequirePitch),
                status = rhythmStatus,
                progress = if (isRhythmActive && !rhythmRequirePitch) correctProgress(rhythmCorrectCount, total) else null,
            ),
            combo = PracticeModeUi(
                checked = isRhythmActive && rhythmRequirePitch,
                enabled = !isPlaybackActive && !isVoiceActive && !(isRhythmActive && !rhythmRequirePitch),
                status = comboStatus,
                progress = if (isRhythmActive && rhythmRequirePitch) correctProgress(rhythmCorrectCount, total) else null,
            ),
        )
    }

    // endregion

    private fun octaveLabel(shift: Int): String = if (shift > 0) "+$shift" else shift.toString()

    private fun correctProgress(correct: Int, total: Int): String =
        getString(R.string.melody_trainer_correct_progress, correct, total)

    private fun formatBeats(beats: Float): String {
        val halves = (beats * 2).roundToInt()
        val whole = halves / 2
        if (halves % 2 == 0) return whole.toString()
        val separator = DecimalFormatSymbols.getInstance(currentLocale()).decimalSeparator
        return "$whole${separator}5"
    }

    private fun currentLocale(): Locale = resources.configuration.locales.get(0)

    override fun onPause() {
        super.onPause()
        if (isPlaybackActive) {
            player.stop()
            onPlaybackStopped()
        }
        if (isVoiceActive) {
            stopVoiceSession(clearGreens = false)
        }
        if (isRhythmActive) {
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
        /** Notes are written one octave either side of the middle — the range [TrainerScale]'s ladder covers. */
        const val MIN_OCTAVE_SHIFT = TrainerScale.MIN_NOTE_OCTAVE
        const val MAX_OCTAVE_SHIFT = TrainerScale.MAX_NOTE_OCTAVE
        const val DURATION_STEP = 0.5f
        const val MIN_DURATION = 0.5f
        const val MAX_DURATION = 4.0f
        const val COUNTDOWN_TICK_MILLIS = 1_000L
        const val RHYTHM_END_GRACE_MILLIS = 1_500L
    }
}
