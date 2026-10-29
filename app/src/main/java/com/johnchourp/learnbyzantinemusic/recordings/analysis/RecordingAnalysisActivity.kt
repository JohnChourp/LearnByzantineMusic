package com.johnchourp.learnbyzantinemusic.recordings.analysis

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.recordings.analysis.ui.RecordingAnalysisScreen
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
import com.johnchourp.learnbyzantinemusic.trainer.TrainerExerciseStore
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * «Ανάλυση φθόγγων» for one saved recording: which phthongs were sung, in which order, how in
 * tune, a pitch-over-time diagram, and — against a melody the user types, or takes from an exercise
 * of «Οι ασκήσεις μου» ([ExerciseTemplates]) — how correct it was.
 *
 * Flow: [RecordingDecoder] (Android decoders → mono WAV) → [PitchTrackAnalyzer] (YIN, once per
 * recording) → [PhthongSegmenter] on the chosen mode's scale, calibrated on the starting
 * phthong → [SequenceAligner] against the expected melody. Changing the mode or the starting
 * phthong only reruns the last two steps; changing the expected melody only the last one.
 *
 * Opened from a recording's menu on «Ηχογραφήσεις» and from a hymn's recordings on the
 * «Αναστασιματάριο»; a hymn shares its expected melody and scale across all its recordings.
 */
class RecordingAnalysisActivity : BaseActivity() {
    private val viewModel: RecordingAnalysisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent.getStringExtra(EXTRA_URI)?.let(Uri::parse)
        val name = intent.getStringExtra(EXTRA_NAME).orEmpty()
        val contextKey = intent.getStringExtra(EXTRA_CONTEXT_KEY)
        if (uri == null || contextKey == null) {
            finish()
            return
        }
        viewModel.start(uri, name, contextKey, intent.getStringExtra(EXTRA_MODE_KEY))

        setContent {
            LbmTheme(palette = currentPalette()) {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                RecordingAnalysisScreen(
                    uiState = uiState,
                    onBack = ::finish,
                    onSelectMode = viewModel::selectMode,
                    onSelectStart = viewModel::selectStart,
                    onAddExpected = viewModel::addExpected,
                    onRemoveLastExpected = viewModel::removeLastExpected,
                    onClearExpected = viewModel::clearExpected,
                    onOpenExercises = viewModel::openExercisePicker,
                    onUseExercise = viewModel::useExercise,
                    onCloseExercises = viewModel::closeExercisePicker,
                    onRetry = viewModel::retry,
                )
            }
        }
    }

    companion object {
        private const val EXTRA_URI = "com.johnchourp.learnbyzantinemusic.recordings.analysis.EXTRA_URI"
        private const val EXTRA_NAME = "com.johnchourp.learnbyzantinemusic.recordings.analysis.EXTRA_NAME"
        private const val EXTRA_CONTEXT_KEY = "com.johnchourp.learnbyzantinemusic.recordings.analysis.EXTRA_CONTEXT_KEY"
        private const val EXTRA_MODE_KEY = "com.johnchourp.learnbyzantinemusic.recordings.analysis.EXTRA_MODE_KEY"

        /** [contextKey] from [AnalysisSettingsStore]; [modeKey] preselects the scale (e.g. a hymn's mode). */
        fun intent(context: Context, uri: Uri, name: String, contextKey: String, modeKey: String? = null): Intent =
            Intent(context, RecordingAnalysisActivity::class.java)
                .putExtra(EXTRA_URI, uri.toString())
                .putExtra(EXTRA_NAME, name)
                .putExtra(EXTRA_CONTEXT_KEY, contextKey)
                .putExtra(EXTRA_MODE_KEY, modeKey)
    }
}

enum class AnalysisStatus { DECODING, ANALYZING, READY, NO_PITCH, FAILED }

data class RecordingAnalysisUiState(
    val recordingName: String = "",
    val status: AnalysisStatus = AnalysisStatus.DECODING,
    val progress: Float = 0f,
    val mode: Mode = Mode.FIRST,
    val startPhthong: PhthongName = PhthongName.PA,
    val track: PitchTrack? = null,
    val niHz: Double? = null,
    val notes: List<SungNote> = emptyList(),
    val expected: List<PhthongName> = emptyList(),
    val comparison: MelodyComparison? = null,
    /** «Από τις ασκήσεις μου» while it is open, else null. */
    val exercisePicker: ExercisePicker? = null,
) {
    val positions: IntArray get() = ModeScalePositions.forMode(mode)

    /**
     * This state with [expected] as the melody, compared with the notes it has now. The melody and its
     * comparison change together: the notes do not depend on the melody and the alignment is cheap, so
     * the card never shows a result for a melody it no longer has.
     */
    fun withExpected(expected: List<PhthongName>): RecordingAnalysisUiState =
        copy(expected = expected, comparison = MelodyComparison.of(expected, notes))

    /** This state with the notes a recompute found, compared with the melody as it is now. */
    fun withNotes(niHz: Double?, notes: List<SungNote>): RecordingAnalysisUiState = copy(
        niHz = niHz,
        notes = notes,
        comparison = MelodyComparison.of(expected, notes),
        status = if (notes.isEmpty()) AnalysisStatus.NO_PITCH else AnalysisStatus.READY,
    )
}

class RecordingAnalysisViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AnalysisSettingsStore(application)
    private val exercises = TrainerExerciseStore(TrainerExerciseStore.forPrefs(AppPrefs.open(application, AppPrefs.Store.TRAINER)))
    private val _uiState = MutableStateFlow(RecordingAnalysisUiState())
    val uiState: StateFlow<RecordingAnalysisUiState> = _uiState.asStateFlow()

    private var uri: Uri? = null
    private var contextKey: String = ""
    private var recomputeJob: Job? = null

    fun start(uri: Uri, name: String, contextKey: String, initialModeKey: String?) {
        if (this.uri != null) return
        this.uri = uri
        this.contextKey = contextKey
        val mode = AnalysisSettingsStore.resolveMode(stored = store.modeKey(contextKey), requested = initialModeKey)
        _uiState.update {
            it.copy(
                recordingName = name,
                mode = mode,
                startPhthong = store.startPhthong(contextKey) ?: ModeScalePositions.defaultStartPhthong(mode),
                expected = store.expected(contextKey),
            )
        }
        analyze()
    }

    fun retry() = analyze()

    private fun analyze() {
        val source = uri ?: return
        val name = _uiState.value.recordingName
        _uiState.update { it.copy(status = AnalysisStatus.DECODING, progress = 0f) }
        viewModelScope.launch {
            val track = runCatching {
                withContext(Dispatchers.IO) {
                    val wav = RecordingDecoder.decodeToWav(getApplication(), source, name)
                    try {
                        _uiState.update { it.copy(status = AnalysisStatus.ANALYZING) }
                        wav.inputStream().buffered().use { input ->
                            PitchTrackAnalyzer.analyze(WavPcmReader.open(input)) { progress ->
                                _uiState.update { it.copy(progress = progress) }
                            }
                        }
                    } finally {
                        wav.delete()
                    }
                }
            }.getOrNull()
            if (track == null) {
                _uiState.update { it.copy(status = AnalysisStatus.FAILED) }
                return@launch
            }
            _uiState.update { it.copy(track = track) }
            recompute()
        }
    }

    fun selectMode(mode: Mode) {
        if (mode == _uiState.value.mode) return
        _uiState.update { it.copy(mode = mode, startPhthong = ModeScalePositions.defaultStartPhthong(mode)) }
        store.saveScale(contextKey, mode.key, _uiState.value.startPhthong)
        recompute()
    }

    fun selectStart(phthong: PhthongName) {
        _uiState.update { it.copy(startPhthong = phthong) }
        store.saveScale(contextKey, _uiState.value.mode.key, phthong)
        recompute()
    }

    fun addExpected(phthong: PhthongName) = updateExpected(_uiState.value.expected + phthong)

    fun removeLastExpected() = updateExpected(_uiState.value.expected.dropLast(1))

    fun clearExpected() = updateExpected(emptyList())

    /** Opens «Από τις ασκήσεις μου», read afresh: the Trainer may have changed them since this screen opened. */
    fun openExercisePicker() {
        _uiState.update { it.copy(exercisePicker = ExerciseTemplates.picker(exercises.loadExercises())) }
    }

    fun closeExercisePicker() {
        _uiState.update { it.copy(exercisePicker = null) }
    }

    /** The φθόγγοι of [choice] become the expected melody; the exercise itself is left as it is. */
    fun useExercise(choice: ExerciseChoice) {
        closeExercisePicker()
        updateExpected(choice.phthongs)
    }

    private fun updateExpected(expected: List<PhthongName>) {
        _uiState.update { it.withExpected(expected) }
        store.saveExpected(contextKey, expected)
    }

    /** Notes on the current scale, then the comparison with the expected melody. */
    private fun recompute() {
        val state = _uiState.value
        val track = state.track ?: return
        recomputeJob?.cancel()
        recomputeJob = viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                val positions = ModeScalePositions.forMode(state.mode)
                val niHz = PhthongSegmenter.calibrate(track, positions, state.startPhthong)
                niHz to niHz?.let { PhthongSegmenter.segment(track, it, positions) }.orEmpty()
            }
            // Compared with the melody as it is now, which may have changed while the notes were found.
            _uiState.update { it.withNotes(result.first, result.second) }
        }
    }
}
