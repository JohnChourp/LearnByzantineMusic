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
import com.johnchourp.learnbyzantinemusic.calendar.LiturgicalToneCycle
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * «Αναστασιματάριο» page: the eight modes → services → hymns, with how many recordings the user
 * keeps for each hymn. Opens on the mode of the current week (LiturgicalToneCycle); tapping a
 * hymn opens [HymnActivity].
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
    }
}

data class AnastasimatarionUiState(
    val catalog: HymnCatalog? = null,
    val selectedModeKey: String = AnastasimatarionLabels.MODE_ORDER.first(),
    val weekModeKey: String? = null,
    val counts: Map<String, Int> = emptyMap(),
    val hasRecordingsFolder: Boolean = false,
    val loadFailed: Boolean = false,
)

class AnastasimatarionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HymnRecordingsRepository(application)
    private val _uiState = MutableStateFlow(AnastasimatarionUiState())
    val uiState: StateFlow<AnastasimatarionUiState> = _uiState.asStateFlow()

    init {
        val weekModeKey = runCatching {
            AnastasimatarionLabels.MODE_ORDER[LiturgicalToneCycle().resolveTone(LocalDate.now()).toneIndex]
        }.getOrNull()
        _uiState.update { state ->
            state.copy(selectedModeKey = weekModeKey ?: state.selectedModeKey, weekModeKey = weekModeKey)
        }
        viewModelScope.launch {
            val catalog = runCatching {
                withContext(Dispatchers.IO) { AnastasimatarionCatalogLoader.load(getApplication()) }
            }.getOrNull()
            _uiState.update { it.copy(catalog = catalog, loadFailed = catalog == null) }
            refreshCounts()
        }
    }

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
