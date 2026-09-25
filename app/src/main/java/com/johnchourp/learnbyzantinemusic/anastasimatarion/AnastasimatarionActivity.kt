package com.johnchourp.learnbyzantinemusic.anastasimatarion

import android.app.Application
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.anastasimatarion.ui.AnastasimatarionScreen
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * «Αναστασιματάριο» page: the eight modes → services → hymns, with how many recordings the user
 * keeps for each hymn. Opens on the current mode ([AnastasimatarionWeekMode]: the tone of the week,
 * the day's own tone in Bright Week, from Saturday noon the tone of tonight's vespers, and the first
 * mode in the weeks that have no tone); tapping a hymn opens [HymnActivity].
 */
class AnastasimatarionActivity : BaseActivity() {
    private val viewModel: AnastasimatarionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LbmTheme(palette = currentPalette()) {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                AnastasimatarionScreen(
                    uiState = uiState,
                    onBack = ::finish,
                    onSelectMode = viewModel::selectMode,
                    onOpenHymn = { modeKey, hymn -> startActivity(HymnActivity.intent(this, modeKey, hymn.code)) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Back from a hymn page or from recording: the counts may have changed.
        viewModel.refreshCounts()
        // And the clock may have passed the vespers hour; the badge follows it, the selection stays.
        viewModel.refreshWeekMode()
    }
}

data class AnastasimatarionUiState(
    val catalog: HymnCatalog? = null,
    val selectedModeKey: String = AnastasimatarionLabels.MODE_ORDER.first(),
    /** The current mode and the badge that names it; null in a week with no tone (no badge). */
    val weekMode: AnastasimatarionWeekMode.CurrentMode? = null,
    val counts: Map<String, Int> = emptyMap(),
    val hasRecordingsFolder: Boolean = false,
    val loadFailed: Boolean = false,
)

class AnastasimatarionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HymnRecordingsRepository(application)
    private val _uiState = MutableStateFlow(AnastasimatarionUiState())
    val uiState: StateFlow<AnastasimatarionUiState> = _uiState.asStateFlow()

    init {
        val weekMode = currentWeekMode()
        _uiState.update { state ->
            state.copy(selectedModeKey = weekMode?.modeKey ?: state.selectedModeKey, weekMode = weekMode)
        }
        viewModelScope.launch {
            val catalog = runCatching {
                withContext(Dispatchers.IO) { AnastasimatarionCatalogLoader.load(getApplication()) }
            }.getOrNull()
            _uiState.update { it.copy(catalog = catalog, loadFailed = catalog == null) }
            refreshCounts()
        }
    }

    /** Re-reads the badge only: the mode the user is looking at is theirs, not the clock's. */
    fun refreshWeekMode() {
        val weekMode = currentWeekMode()
        _uiState.update { it.copy(weekMode = weekMode) }
    }

    private fun currentWeekMode(): AnastasimatarionWeekMode.CurrentMode? =
        runCatching { AnastasimatarionWeekMode.currentMode(LocalDateTime.now()) }.getOrNull()

    fun selectMode(modeKey: String) {
        if (modeKey == _uiState.value.selectedModeKey) return
        _uiState.update { it.copy(selectedModeKey = modeKey, counts = emptyMap()) }
        refreshCounts()
    }

    fun refreshCounts() {
        val state = _uiState.value
        val mode = state.catalog?.mode(state.selectedModeKey) ?: return
        viewModelScope.launch {
            val hasFolder = withContext(Dispatchers.IO) { repository.rootFolder() != null }
            val counts = if (hasFolder) runCatching { repository.countsForMode(mode) }.getOrDefault(emptyMap()) else emptyMap()
            _uiState.update { current ->
                if (current.selectedModeKey == mode.key) current.copy(counts = counts, hasRecordingsFolder = hasFolder) else current
            }
        }
    }
}
