package com.johnchourp.learnbyzantinemusic.modes

import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
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

    /**
     * The drone needs its OWN player: [PhthongTonePlayer] drives a single AudioTrack, so reusing
     * `tonePlayer` would make every touch on the diagram cut the ison off — the exact opposite of
     * what chanting over a drone requires. Two tracks at AMPLITUDE 0.18 each sum to 0.36 of full
     * scale, so they mix without clipping.
     */
    private val dronePlayer: PhthongTonePlayer by lazy { PhthongTonePlayer() }

    /** Kept so onStart can restore a drone that onStop silenced. */
    private var droneFrequencyHz: Double? = null
    private lateinit var prefs: SharedPreferences
    private var activeTimbre: ToneTimbre = ToneTimbre.CLEAN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPrefs.open(this, AppPrefs.Store.EIGHT_MODES)
        activeTimbre = loadSavedTimbre()

        setContent {
            LbmTheme(palette = currentPalette()) {
                EightModesScreen(
                    initialModeIndex = loadSavedModeIndex(),
                    initialTimbre = activeTimbre,
                    initialBaseShifts = loadSavedBaseShifts(),
                    onSelectMode = ::persistSelectedMode,
                    onSelectTimbre = { timbre ->
                        activeTimbre = timbre
                        persistTimbre(timbre)
                        tonePlayer.stop()
                        // Re-voice a sounding drone, otherwise it keeps the old timbre until toggled.
                        droneFrequencyHz?.let { setDroneFrequency(it) }
                    },
                    onBaseShiftChange = ::persistBaseShift,
                    onTonePress = { frequencyHz -> tonePlayer.start(frequencyHz, activeTimbre) },
                    onToneRelease = { tonePlayer.stop() },
                    onDroneChange = ::setDroneFrequency,
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

    private fun baseShiftPrefKey(modeKey: String): String = AppPrefs.baseShiftKeyName(modeKey)

    /**
     * Starts, retunes or stops the ison. Retuning is a stop-then-start on the same player, so a base
     * shift while the drone sounds moves it instead of layering a second voice.
     */
    private fun setDroneFrequency(frequencyHz: Double?) {
        droneFrequencyHz = frequencyHz
        dronePlayer.stop()
        if (frequencyHz != null) {
            dronePlayer.start(frequencyHz, activeTimbre)
        }
    }

    override fun onStart() {
        super.onStart()
        // onStop silenced the drone without forgetting it; bring it back when the screen returns.
        droneFrequencyHz?.let { dronePlayer.start(it, activeTimbre) }
    }

    override fun onStop() {
        tonePlayer.stop()
        // Silence the drone in the background - a held AudioTrack would keep sounding over other
        // apps - but keep droneFrequencyHz so onStart can restore it.
        dronePlayer.stop()
        super.onStop()
    }

    override fun onDestroy() {
        tonePlayer.release()
        dronePlayer.release()
        super.onDestroy()
    }

    private companion object {
        const val BASE_SHIFT_MORIA_MIN = -12
        const val BASE_SHIFT_MORIA_MAX = 12
        const val BASE_SHIFT_DEFAULT_MORIA = 0
        const val BASE_SHIFT_PREF_KEY_PREFIX = AppPrefs.BASE_SHIFT_KEY_PREFIX
        val TONE_TIMBRE_PREF_KEY = AppPrefs.SelectedToneTimbre.name
        val SELECTED_MODE_KEY_PREF_KEY = AppPrefs.SelectedModeKey.name
    }
}
