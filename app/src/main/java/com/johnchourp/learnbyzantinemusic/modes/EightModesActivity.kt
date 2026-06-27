package com.johnchourp.learnbyzantinemusic.modes

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.compose.setContent
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.modes.ui.EIGHT_MODES
import com.johnchourp.learnbyzantinemusic.modes.ui.EightModesScreen
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme

/**
 * Host for the redesigned «Κλίμακες των 8 Ήχων» screen. Owns the SharedPreferences (per-mode base
 * shift, selected mode, timbre) and the [PhthongTonePlayer] lifecycle; everything visual lives in
 * the Compose [EightModesScreen]. Pure scale/theory/audio logic is reused unchanged.
 */
class EightModesActivity : BaseActivity() {

    private val tonePlayer: PhthongTonePlayer by lazy { PhthongTonePlayer() }
    private lateinit var prefs: SharedPreferences
    private var activeTimbre: ToneTimbre = ToneTimbre.CLEAN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(BASE_SHIFT_PREFS_NAME, MODE_PRIVATE)
        activeTimbre = loadSavedTimbre()

        setContent {
            LbmTheme {
                EightModesScreen(
                    initialModeIndex = loadSavedModeIndex(),
                    initialTimbre = activeTimbre,
                    initialBaseShifts = loadSavedBaseShifts(),
                    onSelectMode = ::persistSelectedMode,
                    onSelectTimbre = { timbre ->
                        activeTimbre = timbre
                        persistTimbre(timbre)
                        tonePlayer.stop()
                    },
                    onBaseShiftChange = ::persistBaseShift,
                    onTonePress = { frequencyHz -> tonePlayer.start(frequencyHz, activeTimbre) },
                    onToneRelease = { tonePlayer.stop() },
                    onOpenMenu = { EightModesNavigation.showMenu(this, selectedTopicKey = null) },
                    onBack = ::finish,
                )
            }
        }
    }

    private fun loadSavedBaseShifts(): Map<Int, Int> =
        EIGHT_MODES.indices.associateWith { index ->
            prefs.getInt(baseShiftPrefKey(EIGHT_MODES[index].theoryKey), BASE_SHIFT_DEFAULT_MORIA)
                .coerceIn(BASE_SHIFT_MORIA_MIN, BASE_SHIFT_MORIA_MAX)
        }

    private fun loadSavedModeIndex(): Int {
        val savedKey = prefs.getString(SELECTED_MODE_KEY_PREF_KEY, null)
        return EIGHT_MODES.indexOfFirst { it.theoryKey == savedKey }.takeIf { it >= 0 } ?: 0
    }

    private fun loadSavedTimbre(): ToneTimbre {
        val stored = prefs.getString(TONE_TIMBRE_PREF_KEY, ToneTimbre.CLEAN.name)
        return ToneTimbre.entries.firstOrNull { it.name == stored } ?: ToneTimbre.CLEAN
    }

    private fun persistSelectedMode(modeIndex: Int) {
        val key = EIGHT_MODES.getOrNull(modeIndex)?.theoryKey ?: return
        prefs.edit().putString(SELECTED_MODE_KEY_PREF_KEY, key).apply()
    }

    private fun persistTimbre(timbre: ToneTimbre) {
        prefs.edit().putString(TONE_TIMBRE_PREF_KEY, timbre.name).apply()
    }

    private fun persistBaseShift(modeIndex: Int, shiftMoria: Int) {
        val key = EIGHT_MODES.getOrNull(modeIndex)?.theoryKey ?: return
        val bounded = shiftMoria.coerceIn(BASE_SHIFT_MORIA_MIN, BASE_SHIFT_MORIA_MAX)
        prefs.edit().putInt(baseShiftPrefKey(key), bounded).apply()
    }

    private fun baseShiftPrefKey(modeKey: String): String = "$BASE_SHIFT_PREF_KEY_PREFIX$modeKey"

    override fun onStop() {
        tonePlayer.stop()
        super.onStop()
    }

    override fun onDestroy() {
        tonePlayer.release()
        super.onDestroy()
    }

    private companion object {
        const val BASE_SHIFT_MORIA_MIN = -12
        const val BASE_SHIFT_MORIA_MAX = 12
        const val BASE_SHIFT_DEFAULT_MORIA = 0
        const val BASE_SHIFT_PREFS_NAME = "eight_modes_base_shift_prefs"
        const val BASE_SHIFT_PREF_KEY_PREFIX = "mode_base_shift_moria_"
        const val TONE_TIMBRE_PREF_KEY = "selected_tone_timbre"
        const val SELECTED_MODE_KEY_PREF_KEY = "selected_mode_key"
    }
}
