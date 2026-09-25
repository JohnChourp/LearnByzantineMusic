package com.johnchourp.learnbyzantinemusic.modes

import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.trainer.TrainerPitchEngine
import com.johnchourp.learnbyzantinemusic.modes.ui.EIGHT_MODES
import com.johnchourp.learnbyzantinemusic.modes.ui.EightModesScreen
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme

/**
 * Host for the redesigned «Κλίμακες των 8 Ήχων» screen. Owns the SharedPreferences (per-mode base
 * shift, selected mode, timbre) and the [PhthongTonePlayer] lifecycle; everything visual lives in
 * the Compose [EightModesScreen]. Pure scale/theory/audio logic is reused unchanged.
 *
 * It also owns the **microphone** for the live pitch mirror (ClickUp `869f4tqad`): the RECORD_AUDIO
 * permission, the [TrainerPitchEngine] and its lifecycle. The screen never touches any of that —
 * it says whether it wants to listen, and reads back a frequency.
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

    /**
     * The live pitch mirror's capture. Separate from the drone's AudioTrack in every way: this one
     * reads, and it is torn down on onStop rather than silenced, because a capture thread left
     * running in the background holds the microphone away from every other app.
     */
    private val pitchEngine: TrainerPitchEngine by lazy {
        TrainerPitchEngine(
            onPitch = { match, _ ->
                // Hand the screen the RAW frequency, not this match: the match is resolved against
                // the fixed diatonic table with octaves folded away, and the mirror reads against
                // the mode's own ladder, which has neither property.
                heardFrequencyHz = match?.frequencyHz?.takeIf { it > 0.0 }
            },
            onCaptureError = {
                listenRequested = false
                stopListening()
            },
        )
    }

    /** What the screen asked for, so onStart can resume a mirror that onStop tore down. */
    private var listenRequested = false
    private var heardFrequencyHz: Double? by mutableStateOf(null)
    private var micDenied: Boolean by mutableStateOf(false)

    private val micPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            micDenied = !granted
            if (granted) startListening() else listenRequested = false
        }
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
                        // A restart, not a retune: a glide keeps the timbre it started with.
                        droneFrequencyHz?.let { dronePlayer.start(it, activeTimbre) }
                    },
                    onBaseShiftChange = ::persistBaseShift,
                    onTonePress = { frequencyHz -> tonePlayer.start(frequencyHz, activeTimbre) },
                    onToneRelease = { tonePlayer.stop() },
                    onDroneChange = ::setDroneFrequency,
                    onListenChange = ::setListening,
                    heardFrequencyHz = heardFrequencyHz,
                    micDenied = micDenied,
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
     * Starts, moves or stops the ison. A sounding drone is **moved** — to another φθόγγος, by a base
     * shift, or to a new ήχος — with a glide inside the same stream, so it neither clicks nor gaps
     * (ClickUp `869f5x251`). Only a silent drone is started afresh, and it is always the one player,
     * so a move can never layer a second voice.
     */
    private fun setDroneFrequency(frequencyHz: Double?) {
        droneFrequencyHz = frequencyHz
        when {
            frequencyHz == null -> dronePlayer.stop()
            dronePlayer.retune(frequencyHz) -> Unit
            else -> dronePlayer.start(frequencyHz, activeTimbre)
        }
    }

    /**
     * The screen wants to listen, or to stop. Asking for the permission happens here and only here;
     * a refusal is remembered so the card can explain itself instead of silently doing nothing.
     */
    private fun setListening(wanted: Boolean) {
        listenRequested = wanted
        if (!wanted) {
            stopListening()
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            micDenied = false
            startListening()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startListening() {
        if (pitchEngine.isRunning) return
        if (!pitchEngine.start()) {
            // The mic exists but could not be opened — another app holds it, or the device refused.
            listenRequested = false
            micDenied = true
        }
    }

    private fun stopListening() {
        pitchEngine.stop()
        heardFrequencyHz = null
    }

    override fun onStart() {
        super.onStart()
        // onStop silenced the drone without forgetting it; bring it back when the screen returns.
        droneFrequencyHz?.let { dronePlayer.start(it, activeTimbre) }
        if (listenRequested) setListening(true)
    }

    override fun onStop() {
        tonePlayer.stop()
        // Silence the drone in the background - a held AudioTrack would keep sounding over other
        // apps - but keep droneFrequencyHz so onStart can restore it.
        dronePlayer.stop()
        // The microphone is RELEASED, not paused: holding it in the background would deny it to
        // every other app. listenRequested remembers that the user wanted it, so onStart resumes.
        stopListening()
        super.onStop()
    }

    override fun onDestroy() {
        tonePlayer.release()
        dronePlayer.release()
        pitchEngine.stop()
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
