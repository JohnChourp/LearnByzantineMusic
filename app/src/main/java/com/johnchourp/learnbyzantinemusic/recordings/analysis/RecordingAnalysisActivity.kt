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
import com.johnchourp.learnbyzantinemusic.recordings.analysis.ui.RecordingAnalysisScreen
import com.johnchourp.learnbyzantinemusic.trainer.TrainerPhthong
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
 * tune, a pitch-over-time diagram, and — against a melody the user types — how correct it was.
 *
 * Flow: [RecordingDecoder] (FFmpegKit → mono WAV) → [PitchTrackAnalyzer] (YIN, once per
 * recording) → [PhthongSegmenter] on the chosen mode's scale, calibrated on the starting
 * phthong → [SequenceAligner] against the expected melody. Changing the mode, the starting
 * phthong or the expected melody only reruns the last two steps.
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
            LbmTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                RecordingAnalysisScreen(
                    uiState = uiState,
                    onBack = ::finish,
                    onSelectMode = viewModel::selectMode,
                    onSelectStart = viewModel::selectStart,
                    onAddExpected = viewModel::addExpected,
                    onRemoveLastExpected = viewModel::removeLastExpected,
                    onClearExpected = viewModel::clearExpected,
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
    val modeKey: String = "first",
    val startPhthong: TrainerPhthong = TrainerPhthong.PA,
    val track: PitchTrack? = null,
    val niHz: Double? = null,
    val notes: List<SungNote> = emptyList(),
    val expected: List<TrainerPhthong> = emptyList(),
    val alignment: AlignmentResult? = null,
) {
    val positions: IntArray get() = ModeScalePositions.forMode(modeKey)
}

class RecordingAnalysisViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AnalysisSettingsStore(application)
    private val _uiState = MutableStateFlow(RecordingAnalysisUiState())
    val uiState: StateFlow<RecordingAnalysisUiState> = _uiState.asStateFlow()

    private var uri: Uri? = null
    private var contextKey: String = ""
    private var recomputeJob: Job? = null

    fun start(uri: Uri, name: String, contextKey: String, initialModeKey: String?) {
        if (this.uri != null) return
        this.uri = uri
        this.contextKey = contextKey
        val modeKey = store.modeKey(contextKey) ?: initialModeKey ?: "first"
        _uiState.update {
            it.copy(
                recordingName = name,
                modeKey = modeKey,
                startPhthong = store.startPhthong(contextKey) ?: ModeScalePositions.defaultStartPhthong(modeKey),
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

    fun selectMode(modeKey: String) {
        if (modeKey == _uiState.value.modeKey) return
        _uiState.update { it.copy(modeKey = modeKey, startPhthong = ModeScalePositions.defaultStartPhthong(modeKey)) }
        store.saveScale(contextKey, modeKey, _uiState.value.startPhthong)
        recompute()
    }

    fun selectStart(phthong: TrainerPhthong) {
        _uiState.update { it.copy(startPhthong = phthong) }
        store.saveScale(contextKey, _uiState.value.modeKey, phthong)
        recompute()
    }

    fun addExpected(phthong: TrainerPhthong) = updateExpected(_uiState.value.expected + phthong)

    fun removeLastExpected() = updateExpected(_uiState.value.expected.dropLast(1))

    fun clearExpected() = updateExpected(emptyList())

    private fun updateExpected(expected: List<TrainerPhthong>) {
        _uiState.update { it.copy(expected = expected) }
        store.saveExpected(contextKey, expected)
        recompute()
    }

    /** Notes on the current scale, then the comparison with the expected melody. */
    private fun recompute() {
        val state = _uiState.value
        val track = state.track ?: return
        recomputeJob?.cancel()
        recomputeJob = viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                val positions = ModeScalePositions.forMode(state.modeKey)
                val niHz = PhthongSegmenter.calibrate(track, positions, state.startPhthong)
                val notes = niHz?.let { PhthongSegmenter.segment(track, it, positions) }.orEmpty()
                val alignment = if (state.expected.isNotEmpty() && notes.isNotEmpty()) {
                    SequenceAligner.align(state.expected, notes.map { it.phthong })
                } else {
                    null
                }
                Triple(niHz, notes, alignment)
            }
            _uiState.update {
                it.copy(
                    niHz = result.first,
                    notes = result.second,
                    alignment = result.third,
                    status = if (result.second.isEmpty()) AnalysisStatus.NO_PITCH else AnalysisStatus.READY,
                )
            }
        }
    }
}
