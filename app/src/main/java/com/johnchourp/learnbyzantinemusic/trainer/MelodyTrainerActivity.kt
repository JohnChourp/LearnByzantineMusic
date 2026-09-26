package com.johnchourp.learnbyzantinemusic.trainer

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.lessons.ui.MetronomeClicker
import com.johnchourp.learnbyzantinemusic.modes.PhthongTonePlayer
import com.johnchourp.learnbyzantinemusic.modes.ToneTimbre
import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.voice.GlobalShift
import com.johnchourp.learnbyzantinemusic.music.Beats
import com.johnchourp.learnbyzantinemusic.music.IntonationProfile
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
import com.johnchourp.learnbyzantinemusic.trainer.ui.ExerciseDialogUi
import com.johnchourp.learnbyzantinemusic.trainer.ui.ExerciseItemUi
import com.johnchourp.learnbyzantinemusic.trainer.ui.ExerciseNameError
import com.johnchourp.learnbyzantinemusic.trainer.ui.MelodyTrainerScreen
import com.johnchourp.learnbyzantinemusic.trainer.ui.MelodyTrainerUiState
import com.johnchourp.learnbyzantinemusic.trainer.ui.PracticeModeUi
import com.johnchourp.learnbyzantinemusic.trainer.ui.SingAlongSound
import com.johnchourp.learnbyzantinemusic.trainer.ui.SingAlongUi
import com.johnchourp.learnbyzantinemusic.trainer.ui.SyllableDialogUi
import com.johnchourp.learnbyzantinemusic.trainer.ui.TimingRuleNumbersUi
import com.johnchourp.learnbyzantinemusic.trainer.ui.TrainerExercisesUi
import com.johnchourp.learnbyzantinemusic.trainer.ui.TrainerNoteUi
import com.johnchourp.learnbyzantinemusic.trainer.ui.TRACE_RANGE_MORIA
import com.johnchourp.learnbyzantinemusic.trainer.ui.TrainerScaleUi
import com.johnchourp.learnbyzantinemusic.trainer.ui.WaitModeUi
import com.johnchourp.learnbyzantinemusic.trainer.ui.WaitResultUi
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
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
 * [MelodyTrainerUiState] that the screen renders. The time rules (γοργόν, κλάσμα …) are applied
 * through [MelodySequence], which also holds the Trainer's input rules — where a γοργόν may go,
 * which lengths the ± buttons write — so this class and the screen only ask it.
 *
 * Every pitch — what playback sounds and what the voice is judged against — comes from one
 * [TrainerScale] (ClickUp `869f5x24v`): «Διατονικός», the default and the Trainer as it always was,
 * or a ήχος on the same ladder the 8 Ήχοι page uses, with that ήχος's own «Μεταφορά βάσης», read
 * from and written to the key the 8 Ήχοι page keeps for it.
 *
 * The melody is no longer lost when the screen closes (ClickUp `869f5x261`): every edit writes it
 * as the last melody, onStop does too, and onCreate puts it back — after a close, a recreation or a
 * process death alike. «Οι ασκήσεις μου» keeps named melodies. Both live in [TrainerExerciseStore];
 * the formats, and what is done with a value that cannot be read, are [TrainerMelodyCodec]'s and
 * [ExerciseBook]'s.
 *
 * «Ψάλλε μαζί» (ClickUp `869f5x2cv`) loops the line with the current note lit, the guide fading
 * round by round while an ison and a metronome go on, until «Στάση» ([SingAlong]). Its notes and
 * clicks come from [MelodyPlaybackPlanner.planRound] and are played by the same [player]; the ison
 * has a [PhthongTonePlayer] of its own and the click a [MetronomeClicker], each with its own volume.
 * The screen stays on while it runs, since nothing else touches it for minutes. A note may carry a
 * syllable, saved with the melody, and the line can show the syllables instead of the φθόγγοι.
 *
 * «Παραλλαγή με αναμονή» (ClickUp `869f5x2cd`) waits on each note until the voice holds it: the
 * [WaitModeEvaluator] moves the same [PracticeCursor] only on a held note or an explicit skip, and
 * reads the voice on this [scale]'s ladder — mode, «Μεταφορά βάσης» and the voice's global shift. Its
 * tolerance narrows through the levels of `IntonationProfile` for as long as this screen stays open,
 * and each run ends with [WaitModeScore]'s stars. Its ison is off unless switched on.
 */
class MelodyTrainerActivity : BaseActivity() {

    private val notes = mutableListOf<TrainerNote>()
    private var currentOctaveShift = 0
    private var bpm = MelodyTempo.DEFAULT_BPM

    /** Where every pitch comes from. [TrainerScale.DIATONIC] reproduces the Trainer before F2. */
    private var scale = TrainerScale.DIATONIC

    /** The 8 Ήχοι store: the per-mode «Μεταφορά βάσης» lives there, shared with that page. */
    private val modePrefs: SharedPreferences by lazy { AppPrefs.open(this, AppPrefs.Store.EIGHT_MODES) }

    /** The last melody and «Οι ασκήσεις μου» (ClickUp `869f5x261`). */
    private val exerciseStore by lazy {
        TrainerExerciseStore(TrainerExerciseStore.forPrefs(AppPrefs.open(this, AppPrefs.Store.TRAINER)))
    }
    private var exercises = ExerciseBook.EMPTY
    private var exerciseDialog: ExerciseDialogUi? = null

    /** What the autosave last wrote (or restored), so an unchanged melody is not written again. */
    private var lastSavedMelody: TrainerMelody? = null

    private var isPlaybackActive = false
    private var isVoiceActive = false
    private var isRhythmActive = false

    // «Ψάλλε μαζί» (ClickUp 869f5x2cv)
    private var isSingAlongActive = false
    /** Where the loop is: moved by the timeline, one note at a time. Null when it is not running. */
    private var singAlongCursor: PracticeCursor? = null
    private var singAlongVolumes = SingAlong.Volumes.DEFAULT
    /** The line shows the syllables instead of the φθόγγοι. */
    private var lineShowsSyllables = false
    /** The note whose syllable is being typed, or null. */
    private var syllableDialogIndex: Int? = null
    private val isonPlayer = PhthongTonePlayer()

    // «Παραλλαγή με αναμονή» (ClickUp 869f5x2cd)
    private var isWaitActive = false
    private var waitEvaluator: WaitModeEvaluator? = null
    /** The last frame the waiting line took, for the arrow and the hold bar. */
    private var waitFrame: WaitFrame? = null
    private val voiceTrace = VoiceTrace()
    /** The narrowing level, from `IntonationProfile`: starts loose every time the Trainer opens. */
    private var waitLevel = 0
    private var waitIsonOn = false
    private var waitResult: WaitResultUi? = null
    private var waitStatus: String? = null
    private val metronome = MetronomeClicker(percentOf(SingAlong.Volumes.DEFAULT.metronome))
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
        scale = scale.copy(globalShiftMoria = GlobalShift.load(this))
        restoreSaved()
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
                    onDecrementDuration = { index -> changeDuration(index, -MelodySequence.LENGTH_STEP_BEATS) },
                    onIncrementDuration = { index -> changeDuration(index, MelodySequence.LENGTH_STEP_BEATS) },
                    onToggleGorgo = ::toggleGorgo,
                    onRemoveNote = ::removeNote,
                    onSelectScale = ::selectScale,
                    onBaseShiftChange = ::changeBaseShift,
                    onRequestSaveExercise = ::requestSaveExercise,
                    onSaveExercise = ::saveExercise,
                    onRequestOpenExercise = ::requestOpenExercise,
                    onOpenExercise = ::openExercise,
                    onRequestRenameExercise = ::requestRenameExercise,
                    onRenameExercise = ::renameExercise,
                    onRequestDeleteExercise = ::requestDeleteExercise,
                    onDeleteExercise = ::deleteExercise,
                    onDismissExerciseDialog = ::dismissExerciseDialog,
                    onTempoChange = ::changeTempo,
                    onPlay = ::startPlayback,
                    onStop = ::stopPlayback,
                    onClear = ::clearSequence,
                    onStartSingAlong = ::startSingAlong,
                    onStopSingAlong = ::stopSingAlong,
                    onShowSyllables = ::showSyllables,
                    onSingAlongVolumeChange = ::changeSingAlongVolume,
                    onRequestSyllable = ::requestSyllable,
                    onSaveSyllable = ::saveSyllable,
                    onDismissSyllable = ::dismissSyllable,
                    onStartWait = ::requestWaitMode,
                    onStopWait = { stopWaitMode(clearGreens = true) },
                    onSkipWait = ::skipWaitNote,
                    onWaitIsonChange = ::changeWaitIson,
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

    /**
     * «Βρες τη φωνή σου» may have changed the voice's global shift in Settings meanwhile: take it on
     * when this screen returns, unless something is sounding or listening (ClickUp `869f5x2dd`).
     */
    override fun onStart() {
        super.onStart()
        val global = GlobalShift.load(this)
        if (global != scale.globalShiftMoria && !isBusy) {
            scale = scale.copy(globalShiftMoria = global)
            rebuildState()
        }
    }

    // region editing

    private val isBusy: Boolean
        get() = isPlaybackActive || isVoiceActive || isRhythmActive || isSingAlongActive || isWaitActive

    private fun addNote(phthong: PhthongName) {
        if (isBusy) return
        if (!MelodySequence(notes.toList()).canAddNote()) return
        notes.add(TrainerNote(phthong = phthong, octaveShift = currentOctaveShift))
        autosave()
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
        autosave()
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
        scale = TrainerScale(mode, shift, scale.globalShiftMoria)
        autosave()
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
        autosave()
        rebuildState()
    }

    private fun changeDuration(index: Int, delta: Float) {
        if (isBusy) return
        if (!MelodySequence(notes.toList()).canChangeLength(index)) return
        val note = notes[index]
        val updated = (note.baseDurationBeats + delta)
            .coerceIn(MelodySequence.MIN_LENGTH_BEATS, MelodySequence.MAX_LENGTH_BEATS)
        notes[index] = note.copy(baseDurationBeats = updated)
        autosave()
        rebuildState()
    }

    private fun toggleGorgo(index: Int) {
        if (isBusy) return
        if (!MelodySequence(notes.toList()).canToggleGorgon(index)) return
        val note = notes[index]
        notes[index] = note.withGorgo(!note.hasGorgo)
        autosave()
        rebuildState()
    }

    private fun removeNote(index: Int) {
        if (isBusy) return
        if (index !in notes.indices) return
        notes.removeAt(index)
        // A deletion can move a γοργόν onto the first note, where the rules have no note for it to share with.
        val kept = MelodySequence(notes.toList()).normalised().notes
        notes.clear()
        notes.addAll(kept)
        matchedIndices.clear()
        autosave()
        rebuildState()
    }

    private fun clearSequence() {
        if (isBusy) return
        notes.clear()
        matchedIndices.clear()
        autosave()
        rebuildState()
    }

    // endregion

    // region saved melodies: the last one, and «Οι ασκήσεις μου» (ClickUp 869f5x261)

    private fun currentMelody(): TrainerMelody = TrainerMelody(notes.toList(), bpm, scale)

    /**
     * Puts back the last melody — after the screen was closed, or the process killed — and reads the
     * saved exercises. A melody that cannot be read is simply not restored: the Trainer opens empty.
     */
    private fun restoreSaved() {
        exercises = exerciseStore.loadExercises()
        exerciseStore.loadLastMelody()?.let(::showMelody)
        lastSavedMelody = currentMelody()
    }

    /** Writes the melody as the last one, when it changed since the last write. */
    private fun autosave() {
        val melody = currentMelody()
        if (melody == lastSavedMelody) return
        exerciseStore.saveLastMelody(melody)
        lastSavedMelody = melody
    }

    /** Replaces the melody on screen with [melody], at the «Μεταφορά βάσης» its ήχος has now. */
    private fun showMelody(melody: TrainerMelody) {
        val live = melody.withLiveShift(liveShiftOf(melody.scale.mode))
        notes.clear()
        notes.addAll(live.notes)
        bpm = live.bpm
        // A melody keeps its ήχος and «Μεταφορά βάσης»; the voice's global shift is the singer's, not
        // the melody's, so it stays as it is (ClickUp `869f5x2dd`).
        scale = live.scale.copy(globalShiftMoria = scale.globalShiftMoria)
        matchedIndices.clear()
    }

    /** The shift the shared key holds for [mode], or null when that ήχος has none stored yet. */
    private fun liveShiftOf(mode: Mode?): Int? {
        mode ?: return null
        val key = AppPrefs.baseShiftKeyName(mode.key)
        return if (modePrefs.contains(key)) modePrefs.getInt(key, BaseShift.DEFAULT_MORIA) else null
    }

    private fun requestSaveExercise() {
        if (isBusy || notes.isEmpty()) return
        exerciseDialog = ExerciseDialogUi.SaveAs()
        rebuildState()
    }

    private fun saveExercise(name: String) {
        if (isBusy) return
        applyExerciseChange(exercises.saveAs(name, currentMelody(), System.currentTimeMillis())) { error ->
            ExerciseDialogUi.SaveAs(error)
        }
    }

    /** Opening replaces the melody on screen, so a melody in progress is asked about first. */
    private fun requestOpenExercise(name: String) {
        if (isBusy) return
        if (notes.isEmpty()) {
            openExercise(name)
        } else {
            exerciseDialog = ExerciseDialogUi.ConfirmOpen(name)
            rebuildState()
        }
    }

    private fun openExercise(name: String) {
        if (isBusy) return
        exerciseDialog = null
        exercises.find(name)?.let { showMelody(it.melody) }
        autosave()
        rebuildState()
    }

    private fun requestRenameExercise(name: String) {
        if (isBusy) return
        exerciseDialog = ExerciseDialogUi.Rename(name)
        rebuildState()
    }

    private fun renameExercise(from: String, to: String) {
        if (isBusy) return
        applyExerciseChange(exercises.rename(from, to)) { error -> ExerciseDialogUi.Rename(from, error) }
    }

    private fun requestDeleteExercise(name: String) {
        if (isBusy) return
        exerciseDialog = ExerciseDialogUi.ConfirmDelete(name)
        rebuildState()
    }

    private fun deleteExercise(name: String) {
        if (isBusy) return
        applyExerciseChange(exercises.delete(name)) { null }
    }

    private fun dismissExerciseDialog() {
        exerciseDialog = null
        rebuildState()
    }

    /** Stores a done change and closes the dialog; a refused one keeps it open with the reason. */
    private fun applyExerciseChange(change: ExerciseChange, reopen: (ExerciseNameError) -> ExerciseDialogUi?) {
        exerciseDialog = when (change) {
            is ExerciseChange.Done -> {
                exercises = change.book
                exerciseStore.saveExercises(change.book)
                null
            }
            ExerciseChange.NotFound -> null
            ExerciseChange.NameBlank -> reopen(ExerciseNameError.BLANK)
            ExerciseChange.NameTooLong -> reopen(ExerciseNameError.TOO_LONG)
            ExerciseChange.NameTaken -> reopen(ExerciseNameError.TAKEN)
            ExerciseChange.Full -> reopen(ExerciseNameError.FULL)
            ExerciseChange.NewerFormat -> reopen(ExerciseNameError.NEWER_FORMAT)
        }
        rebuildState()
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

    // region «Ψάλλε μαζί» (ClickUp 869f5x2cv)

    /**
     * Starts the loop on a snapshot of the melody, tempo and scale — nothing can change while it runs.
     * The ison starts first, so the first note already sounds over it.
     */
    private fun startSingAlong() {
        if (isBusy || notes.isEmpty()) return
        val sequence = MelodySequence(notes.toList())
        val tempo = MelodyTempo.of(bpm)
        val loopScale = scale
        val frequencyOf: (TrainerNote) -> Double = loopScale::frequencyHz
        if (MelodyPlaybackPlanner.planRound(sequence, tempo, 0, frequencyOf).notes.isEmpty()) return
        isSingAlongActive = true
        singAlongCursor = PracticeCursor.start(notes.size)
        matchedIndices.clear()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        SingAlong.isonFrequencyHz(loopScale)?.let { isonPlayer.start(it, ToneTimbre.CLEAN, singAlongVolumes.ison) }
        metronome.setVolume(percentOf(singAlongVolumes.metronome))
        metronome.warmUp()
        player.setGuideVolume(singAlongVolumes.melody)
        player.playLoop(
            roundAt = { round -> MelodyPlaybackPlanner.planRound(sequence, tempo, round, frequencyOf) },
            listener = ::onSingAlongNote,
            onTick = metronome::click,
        )
        rebuildState()
    }

    /** The timeline moved: the cursor follows it, and the note it is on lights up. */
    private fun onSingAlongNote(round: PlannedRound, event: PlannedNoteEvent) {
        val cursor = singAlongCursor ?: return
        singAlongCursor = cursor.at(round.index, event.index)
        activeIndex = event.index
        rebuildState()
    }

    private fun stopSingAlong() {
        if (!isSingAlongActive) return
        player.stop()
        isonPlayer.stop()
        metronome.release()
        isSingAlongActive = false
        singAlongCursor = null
        activeIndex = -1
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        rebuildState()
    }

    /** A volume moved: it applies at once — to the next note for the guide, to the sounding ison and click. */
    private fun changeSingAlongVolume(sound: SingAlongSound, percent: Int) {
        val level = (percent / 100f).coerceIn(0f, 1f)
        singAlongVolumes = when (sound) {
            SingAlongSound.MELODY -> singAlongVolumes.copy(melody = level).also { player.setGuideVolume(level) }
            SingAlongSound.ISON -> singAlongVolumes.copy(ison = level).also { isonPlayer.setVolume(level) }
            SingAlongSound.METRONOME -> singAlongVolumes.copy(metronome = level).also { metronome.setVolume(percentOf(level)) }
        }
        rebuildState()
    }

    private fun showSyllables(show: Boolean) {
        lineShowsSyllables = show
        rebuildState()
    }

    private fun requestSyllable(index: Int) {
        if (isBusy || index !in notes.indices) return
        syllableDialogIndex = index
        rebuildState()
    }

    /** Stores what was typed, cleaned by the note itself; an empty text takes the syllable off. */
    private fun saveSyllable(index: Int, typed: String) {
        syllableDialogIndex = null
        if (!isBusy && index in notes.indices) {
            notes[index] = notes[index].withSyllable(typed)
            autosave()
        }
        rebuildState()
    }

    private fun dismissSyllable() {
        syllableDialogIndex = null
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
            // The wait mode reads the frequency on its own, octave-folded against the note it waits on.
            isWaitActive -> handleWaitFrame(match?.frequencyHz, capturedAtMillis)
        }
    }

    // endregion

    // region «Παραλλαγή με αναμονή» (ClickUp 869f5x2cd)

    private fun requestWaitMode() {
        if (isBusy) return
        if (notes.isEmpty()) {
            waitStatus = getString(R.string.melody_trainer_voice_need_notes)
            rebuildState()
            return
        }
        ensureMicThen(onGranted = ::startWaitMode, onDenied = {
            waitStatus = getString(R.string.melody_trainer_mic_permission_required)
            rebuildState()
        })
    }

    /** Every note is waited on at its rung of this scale's ladder; the Trainer's ladder holds them all. */
    private fun startWaitMode() {
        if (isBusy || notes.isEmpty()) return
        val ladder = scale.ladder
        val targets = notes.map { note ->
            requireNotNull(ladder.stepFor(note.pitch)) { "${note.pitch.label} is outside the Trainer's ladder" }
        }
        val evaluator = WaitModeEvaluator(targets, ladder, IntonationProfile.waitTolerance(waitLevel))
        matchedIndices.clear()
        voiceTrace.clear()
        waitFrame = null
        waitResult = null
        waitStatus = null
        waitEvaluator = evaluator
        isWaitActive = true
        activeIndex = evaluator.currentIndex ?: -1
        rebuildState()
        if (!pitchEngine.start()) {
            stopWaitMode(clearGreens = true)
            waitStatus = getString(R.string.melody_trainer_mic_unavailable)
            rebuildState()
            return
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (waitIsonOn) startWaitIson()
    }

    private fun handleWaitFrame(frequencyHz: Double?, capturedAtMillis: Long) {
        val evaluator = waitEvaluator ?: return
        val frame = evaluator.onFrame(frequencyHz, capturedAtMillis)
        waitFrame = frame
        voiceTrace.add(capturedAtMillis, frame.reading?.offsetMoria)
        if (frame.advanced) {
            frame.targetIndex?.let(matchedIndices::add)
            onWaitNoteChanged(evaluator)
        } else {
            rebuildState()
        }
    }

    /** «Παράλειψη»: only ever from a tap. The skipped note stays un-greened. */
    private fun skipWaitNote() {
        val evaluator = waitEvaluator ?: return
        if (evaluator.skip()) onWaitNoteChanged(evaluator)
    }

    private fun onWaitNoteChanged(evaluator: WaitModeEvaluator) {
        voiceTrace.clear()
        waitFrame = null
        if (evaluator.isComplete) {
            finishWaitMode(evaluator)
        } else {
            activeIndex = evaluator.currentIndex ?: -1
            rebuildState()
        }
    }

    /** The whole line is done: stars, and the next run's tolerance — narrower only after no skip. */
    private fun finishWaitMode(evaluator: WaitModeEvaluator) {
        val run = evaluator.result()
        val nextLevel = IntonationProfile.nextWaitLevel(waitLevel, withoutSkips = run.skips == 0)
        val next = formatMoria(IntonationProfile.waitTolerance(nextLevel))
        val summary = WaitModeScore.medianLockMillis(run.lockMillis)?.let { median ->
            getString(R.string.melody_trainer_wait_done, run.skips, formatSeconds(median))
        } ?: getString(R.string.melody_trainer_wait_done_all_skipped)
        val nextLine = when {
            run.skips > 0 -> getString(R.string.melody_trainer_wait_same_level, next)
            nextLevel > waitLevel -> getString(R.string.melody_trainer_wait_next_level, next)
            else -> getString(R.string.melody_trainer_wait_top_level, next)
        }
        waitResult = WaitResultUi(WaitModeScore.stars(run), summary, nextLine)
        waitLevel = nextLevel
        stopWaitMode(clearGreens = false)
    }

    private fun stopWaitMode(clearGreens: Boolean) {
        if (!isWaitActive) return
        pitchEngine.stop()
        isonPlayer.stop()
        isWaitActive = false
        waitEvaluator = null
        waitFrame = null
        voiceTrace.clear()
        if (clearGreens) matchedIndices.clear()
        activeIndex = -1
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        rebuildState()
    }

    /** Off unless the learner turns it on: from the loudspeaker the microphone may hear it (I4 is still to measure). */
    private fun changeWaitIson(on: Boolean) {
        waitIsonOn = on
        if (isWaitActive) {
            if (on) startWaitIson() else isonPlayer.stop()
        }
        rebuildState()
    }

    private fun startWaitIson() {
        SingAlong.isonFrequencyHz(scale)?.let { isonPlayer.start(it, ToneTimbre.CLEAN, singAlongVolumes.ison) }
    }

    private fun waitModeUi(): WaitModeUi {
        val evaluator = waitEvaluator
        val index = evaluator?.currentIndex
        val frame = waitFrame?.takeIf { it.targetIndex == index }
        val reading = frame?.reading
        val guidance = when {
            index == null -> null
            reading == null -> getString(R.string.melody_trainer_wait_silence)
            frame.onTarget -> getString(R.string.melody_trainer_wait_hold)
            reading.offsetMoria < 0 -> getString(R.string.melody_trainer_wait_higher, formatMoria(abs(reading.offsetMoria)))
            else -> getString(R.string.melody_trainer_wait_lower, formatMoria(abs(reading.offsetMoria)))
        }
        val tolerance = evaluator?.toleranceMoria ?: IntonationProfile.waitTolerance(waitLevel)
        val target = index?.let { scale.ladder.stepFor(notes[it].pitch) }
        return WaitModeUi(
            running = isWaitActive,
            startEnabled = !isBusy && notes.isNotEmpty(),
            targetLabel = index?.let { SingAlong.lineLabel(notes[it], lineShowsSyllables) },
            guidance = guidance,
            onTarget = frame?.onTarget == true,
            holdProgress = ((frame?.heldMillis ?: 0L).toFloat() / IntonationProfile.WAIT_HOLD_MS.toFloat()).coerceIn(0f, 1f),
            trace = if (isWaitActive) voiceTrace.points() else emptyList(),
            rungOffsets = target?.let { t ->
                scale.ladder.steps
                    .map { (it.moriaFromNi.value - t.moriaFromNi.value).toDouble() }
                    .filter { abs(it) <= TRACE_RANGE_MORIA }
            }.orEmpty(),
            toleranceMoria = tolerance,
            toleranceLabel = getString(R.string.melody_trainer_wait_tolerance, formatMoria(tolerance)),
            isonOn = waitIsonOn,
            result = waitResult,
            status = waitStatus,
        )
    }

    /** μόρια as the card prints them: whole when whole, else one decimal, in the screen's language. */
    private fun formatMoria(moria: Double): String =
        DecimalFormat("0.#", DecimalFormatSymbols.getInstance(currentLocale())).format(moria)

    private fun formatSeconds(millis: Long): String =
        DecimalFormat("0.0", DecimalFormatSymbols.getInstance(currentLocale())).format(millis / 1000.0)

    // endregion

    // region Mode 2: voice check

    private fun requestVoiceSession() {
        if (isPlaybackActive || isRhythmActive || isSingAlongActive || isWaitActive) {
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
        if (isWaitActive) {
            stopWaitMode(clearGreens = false)
            waitStatus = getString(R.string.melody_trainer_mic_unavailable)
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
        if (isPlaybackActive || isVoiceActive || isSingAlongActive || isWaitActive || (isRhythmActive && rhythmRequirePitch != requirePitch)) {
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
        val sequence = MelodySequence(notes.toList())
        val durations = sequence.durations()
        val noteUis = notes.mapIndexed { index, note ->
            TrainerNoteUi(
                index = index,
                phthongLabel = note.pitch.label,
                lineLabel = SingAlong.lineLabel(note, lineShowsSyllables),
                syllable = note.syllable,
                otherLabel = SingAlong.lineLabel(note, !lineShowsSyllables).takeIf { note.syllable != null },
                beatsLabel = formatBeats(durations[index]),
                hasGorgo = note.hasGorgo,
                editable = !isBusy,
                matched = matchedIndices.contains(index),
                active = index == activeIndex,
                lengthChangeable = sequence.canChangeLength(index),
                gorgoToggleable = sequence.canToggleGorgon(index),
            )
        }
        val nowPlaying = if ((isPlaybackActive || isSingAlongActive) && activeIndex in notes.indices) {
            SingAlong.lineLabel(notes[activeIndex], lineShowsSyllables)
        } else {
            null
        }
        val total = notes.size
        uiState = MelodyTrainerUiState(
            notes = noteUis,
            totalBeatsLabel = formatBeats(sequence.total()),
            octaveLabel = octaveLabel(currentOctaveShift),
            octaveDownEnabled = !isBusy && currentOctaveShift > MIN_OCTAVE_SHIFT,
            octaveUpEnabled = !isBusy && currentOctaveShift < MAX_OCTAVE_SHIFT,
            bpm = bpm,
            tempoEnabled = !isBusy,
            playEnabled = !isBusy && notes.isNotEmpty(),
            stopEnabled = isPlaybackActive,
            clearEnabled = !isBusy && notes.isNotEmpty(),
            addEnabled = !isBusy && sequence.canAddNote(),
            noteLimitReached = !sequence.canAddNote(),
            nowPlayingLabel = nowPlaying,
            scale = TrainerScaleUi(
                mode = scale.mode,
                baseShiftMoria = scale.baseShiftMoria,
                enabled = !isBusy,
                globalShiftMoria = scale.globalShiftMoria,
            ),
            exercises = TrainerExercisesUi(
                items = exercises.exercises.map { exercise ->
                    ExerciseItemUi(
                        name = exercise.name,
                        noteCount = exercise.melody.notes.size,
                        mode = exercise.melody.scale.mode,
                        bpm = exercise.melody.bpm,
                    )
                },
                saveEnabled = !isBusy && notes.isNotEmpty() && !exercises.isNewerFormat,
                enabled = !isBusy,
                newerFormat = exercises.isNewerFormat,
                dialog = exerciseDialog,
            ),
            singAlong = SingAlongUi(
                running = isSingAlongActive,
                startEnabled = !isBusy && notes.isNotEmpty(),
                round = singAlongCursor?.round?.plus(1),
                guidePercent = singAlongCursor?.let { SingAlong.guidePercent(it.round) },
                nowLabel = nowPlaying.takeIf { isSingAlongActive },
                showSyllables = lineShowsSyllables,
                hasSyllables = notes.any { it.syllable != null },
                melodyPercent = percentOf(singAlongVolumes.melody),
                isonPercent = percentOf(singAlongVolumes.ison),
                metronomePercent = percentOf(singAlongVolumes.metronome),
                isonLabel = SingAlong.isonPhthong(scale).label,
            ),
            waitMode = waitModeUi(),
            syllableDialog = syllableDialogIndex?.takeIf { it in notes.indices }?.let { index ->
                SyllableDialogUi(index, notes[index].pitch.label, notes[index].syllable.orEmpty())
            },
            voice = PracticeModeUi(
                checked = isVoiceActive,
                enabled = !isPlaybackActive && !isRhythmActive && !isSingAlongActive && !isWaitActive,
                status = voiceStatus,
                progress = if (isVoiceActive) correctProgress(voiceCorrectCount, total) else null,
            ),
            rhythm = PracticeModeUi(
                checked = isRhythmActive && !rhythmRequirePitch,
                enabled = !isPlaybackActive && !isVoiceActive && !isSingAlongActive && !isWaitActive && !(isRhythmActive && rhythmRequirePitch),
                status = rhythmStatus,
                progress = if (isRhythmActive && !rhythmRequirePitch) correctProgress(rhythmCorrectCount, total) else null,
            ),
            combo = PracticeModeUi(
                checked = isRhythmActive && rhythmRequirePitch,
                enabled = !isPlaybackActive && !isVoiceActive && !isSingAlongActive && !isWaitActive && !(isRhythmActive && !rhythmRequirePitch),
                status = comboStatus,
                progress = if (isRhythmActive && rhythmRequirePitch) correctProgress(rhythmCorrectCount, total) else null,
            ),
            ruleNumbers = TimingRuleNumbersUi(
                defaultLength = formatBeats(TimingRulesHelp.defaultLength),
                gorgonNote = formatBeats(TimingRulesHelp.gorgonNote),
                gorgonTakes = formatBeats(TimingRulesHelp.gorgonTakes),
                klasmaAdds = formatBeats(TimingRulesHelp.klasmaAdds),
            ),
        )
    }

    // endregion

    private fun octaveLabel(shift: Int): String = if (shift > 0) "+$shift" else shift.toString()

    /** A 0 … 1 level as the whole percentage the card shows and the click generator takes. */
    private fun percentOf(level: Float): Int = (level * 100).roundToInt()

    private fun correctProgress(correct: Int, total: Int): String =
        getString(R.string.melody_trainer_correct_progress, correct, total)

    private fun formatBeats(beats: Beats): String =
        BeatsLabel.of(beats, DecimalFormatSymbols.getInstance(currentLocale()).decimalSeparator)

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
        // Like every mode, «Ψάλλε μαζί» stops when the screen is left; only «Στάση» stops it otherwise.
        stopSingAlong()
        stopWaitMode(clearGreens = false)
    }

    /** The autosave's safety net: every edit already wrote the melody, so this is usually a no-op. */
    override fun onStop() {
        super.onStop()
        autosave()
    }

    override fun onDestroy() {
        super.onDestroy()
        uiHandler.removeCallbacksAndMessages(null)
        player.release()
        isonPlayer.release()
        metronome.release()
        pitchEngine.stop()
    }

    private companion object {
        /** Notes are written one octave either side of the middle — the range [TrainerScale]'s ladder covers. */
        const val MIN_OCTAVE_SHIFT = TrainerScale.MIN_NOTE_OCTAVE
        const val MAX_OCTAVE_SHIFT = TrainerScale.MAX_NOTE_OCTAVE
        const val COUNTDOWN_TICK_MILLIS = 1_000L
        const val RHYTHM_END_GRACE_MILLIS = 1_500L
    }
}
