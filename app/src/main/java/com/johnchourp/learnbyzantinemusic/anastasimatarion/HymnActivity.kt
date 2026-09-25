package com.johnchourp.learnbyzantinemusic.anastasimatarion

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.anastasimatarion.ui.HymnScreen
import com.johnchourp.learnbyzantinemusic.recordings.RecordingExternalOpener
import com.johnchourp.learnbyzantinemusic.recordings.RecordingFormatOption
import com.johnchourp.learnbyzantinemusic.recordings.RecordingsActivity
import com.johnchourp.learnbyzantinemusic.recordings.analysis.AnalysisSettingsStore
import com.johnchourp.learnbyzantinemusic.recordings.analysis.RecordingAnalysisActivity
import com.johnchourp.learnbyzantinemusic.recordings.player.InAppPlayerViewModel
import com.johnchourp.learnbyzantinemusic.recordings.player.PlayerItem
import com.johnchourp.learnbyzantinemusic.recordings.player.cardActions
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * One hymn of the Anastasimatarion: record it, and list or open the recordings kept for it.
 *
 * **In:** `EXTRA_MODE_KEY` + `EXTRA_HYMN_CODE`, resolved against [AnastasimatarionCatalog]. An
 * unknown pair is not a crash — the screen says the hymn was not found and offers nothing else.
 *
 * **Stores:** nothing of its own. Two things are keyed by the hymn's two-digit [Hymn.code], for
 * good:
 * - its recordings, in the user's chosen recordings folder under the per-hymn subfolder
 *   [HymnFolders] names (`Αναστασιματάριο/<mode>/<code> <incipit>/`), so they survive a reinstall
 *   and stay visible in «Διαχείριση»;
 * - its analysis settings, which [analyzeRecording] keys as `hymn:<mode>:<code>`
 *   ([AnalysisSettingsStore.hymnKey]), shared by every recording of the hymn.
 *
 * There is no database row for a hymn, so a code given to a different hymn would re-attach both,
 * silently. Codes are locked in `scripts/anastasimatarion-codes.lock.json` — see the catalog's
 * header in `AnastasimatarionCatalog.kt` (ClickUp `869f5x2a9`).
 *
 * **Needs:** a recordings folder to have been chosen already. Without one, recording is disabled
 * and the screen points the user at the «Ηχογραφήσεις» page rather than failing at save time.
 *
 * **Listening** happens here, in the in-app player ([InAppPlayerViewModel], ClickUp `869f5x268`):
 * loop a passage, slow it down without moving the pitch, shift it in μόρια to one's own voice. Another
 * app stays one tap away on the player card. The player is released in `onStop`.
 */
class HymnActivity : BaseActivity() {
    private val viewModel: HymnViewModel by viewModels()
    private val player: InAppPlayerViewModel by viewModels()
    private val recordingOpener by lazy { RecordingExternalOpener(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val modeKey = intent.getStringExtra(EXTRA_MODE_KEY)
        val hymnCode = intent.getStringExtra(EXTRA_HYMN_CODE)
        if (modeKey == null || hymnCode == null) {
            finish()
            return
        }
        viewModel.load(modeKey, hymnCode)

        setContent {
            LbmTheme(palette = currentPalette()) {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val playerState by player.state.collectAsStateWithLifecycle()
                HymnScreen(
                    uiState = uiState,
                    player = playerState,
                    playerActions = remember { player.cardActions(this@HymnActivity, ::openRecordingExternally) },
                    onBack = ::finish,
                    onRecord = ::startHymnRecording,
                    onOpenRecording = ::openRecording,
                    onAnalyzeRecording = ::analyzeRecording,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onStop() {
        super.onStop()
        // No sound in the background: released here, prepared again on the next play.
        player.onScreenStopped()
    }

    private fun startHymnRecording() {
        val ref = viewModel.uiState.value.ref ?: return
        val label = getString(
            R.string.anastasimatarion_recording_target_template,
            getString(AnastasimatarionLabels.modeName(ref.modeKey)),
            ref.hymn.incipit,
        )
        startActivity(
            Intent(this, RecordingsActivity::class.java)
                .putStringArrayListExtra(
                    RecordingsActivity.EXTRA_TARGET_FOLDER_PATH,
                    ArrayList(HymnFolders.pathSegments(ref.modeKey, ref.hymn)),
                )
                .putExtra(RecordingsActivity.EXTRA_TARGET_FOLDER_MATCH_PREFIX, HymnFolders.hymnFolderPrefix(ref.hymn))
                .putExtra(RecordingsActivity.EXTRA_TARGET_LABEL, label),
        )
    }

    /** The hymn's mode preselects the scale; the expected melody is shared by all its recordings. */
    private fun analyzeRecording(recording: HymnRecording) {
        val ref = viewModel.uiState.value.ref ?: return
        startActivity(
            RecordingAnalysisActivity.intent(
                context = this,
                uri = recording.uri,
                name = recording.name,
                contextKey = AnalysisSettingsStore.hymnKey(ref.modeKey, ref.hymn.code),
                modeKey = ref.modeKey,
            ),
        )
    }

    private fun openRecording(recording: HymnRecording) {
        player.open(PlayerItem(recording.uri, recording.name, recording.mimeType))
    }

    /** «Άνοιγμα σε άλλη εφαρμογή» on the player card — the only way out when the device cannot play the file. */
    private fun openRecordingExternally() {
        val recording = player.current ?: return
        val mimeType = recording.mimeType
            ?.takeIf { it.isNotBlank() && it != "application/octet-stream" }
            ?: RecordingFormatOption.resolveMimeTypeByFileName(recording.name)
            ?: "audio/*"
        lifecycleScope.launch {
            recordingOpener.openRecordingWithChooser(
                sourceUri = recording.uri,
                fileName = recording.name,
                mimeType = mimeType,
                chooserTitle = getString(R.string.recordings_open_recording_with),
                onFailure = {
                    Toast.makeText(this@HymnActivity, R.string.recordings_open_recording_no_app, Toast.LENGTH_SHORT).show()
                },
            )
        }
    }

    companion object {
        private const val EXTRA_MODE_KEY = "com.johnchourp.learnbyzantinemusic.anastasimatarion.EXTRA_MODE_KEY"
        private const val EXTRA_HYMN_CODE = "com.johnchourp.learnbyzantinemusic.anastasimatarion.EXTRA_HYMN_CODE"

        fun intent(context: Context, modeKey: String, hymnCode: String): Intent =
            Intent(context, HymnActivity::class.java)
                .putExtra(EXTRA_MODE_KEY, modeKey)
                .putExtra(EXTRA_HYMN_CODE, hymnCode)
    }
}

data class HymnUiState(
    val ref: HymnRef? = null,
    val recordings: List<HymnRecording> = emptyList(),
    val hasRecordingsFolder: Boolean = false,
    val loading: Boolean = true,
)

class HymnViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HymnRecordingsRepository(application)
    private val _uiState = MutableStateFlow(HymnUiState())
    val uiState: StateFlow<HymnUiState> = _uiState.asStateFlow()

    fun load(modeKey: String, hymnCode: String) {
        if (_uiState.value.ref != null) return
        viewModelScope.launch {
            val ref = runCatching {
                withContext(Dispatchers.IO) { AnastasimatarionCatalogLoader.load(getApplication()) }
            }.getOrNull()?.mode(modeKey)?.find(hymnCode)
            _uiState.update { it.copy(ref = ref, loading = ref != null) }
            refresh()
        }
    }

    fun refresh() {
        val ref = _uiState.value.ref ?: return
        viewModelScope.launch {
            val hasFolder = withContext(Dispatchers.IO) { repository.rootFolder() != null }
            val recordings = if (hasFolder) {
                runCatching { repository.recordingsFor(ref.modeKey, ref.hymn) }.getOrDefault(emptyList())
            } else {
                emptyList()
            }
            _uiState.update { it.copy(recordings = recordings, hasRecordingsFolder = hasFolder, loading = false) }
        }
    }
}
