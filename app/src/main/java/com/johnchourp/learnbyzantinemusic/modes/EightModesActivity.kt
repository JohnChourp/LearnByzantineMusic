package com.johnchourp.learnbyzantinemusic.modes

import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.music.Phthong
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.notifications.AppNotifications
import com.johnchourp.learnbyzantinemusic.trainer.TrainerPitchEngine
import com.johnchourp.learnbyzantinemusic.modes.ui.EIGHT_MODES
import com.johnchourp.learnbyzantinemusic.modes.ui.EightModesScreen
import com.johnchourp.learnbyzantinemusic.modes.ui.eightModesIndexOf
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme

/**
 * Host for the redesigned «Κλίμακες των 8 Ήχων» screen. Owns the SharedPreferences (per-mode base
 * shift, selected mode, timbre) and the [PhthongTonePlayer] lifecycle; everything visual lives in
 * the Compose [EightModesScreen]. Pure scale/theory/audio logic is reused unchanged.
 *
 * It also owns the **microphone** for the live pitch mirror (ClickUp `869f4tqad`): the RECORD_AUDIO
 * permission, the [TrainerPitchEngine] and its lifecycle. The screen never touches any of that —
 * it says whether it wants to listen, and reads back a frequency.
 *
 * **Opening on a given mode is one-shot** (decision of ClickUp `869f5x24r`). [intent] with a mode key
 * — the home card's «Άνοιξε στους 8 Ήχους» — shows that mode for this opening only. It is NOT written
 * to the saved «last selected mode»: that is saved only when the user taps a mode here, so following
 * the card never changes where the page opens next time from its own tile. The mode on screen is kept
 * in the instance state, so a recreation (a theme change) shows it again instead of the saved one.
 *
 * **The launcher shortcut «Ίσο»** (ClickUp `869f5x2dq`) opens this page with [EXTRA_START_ISON]: the
 * last mode and its «Μεταφορά βάσης», with the ison already sounding. It only ever plays — it never
 * records — and it is honoured on a fresh start only, so a recreation after the user switched the
 * ison off does not switch it back on.
 *
 * **Who plays the ison.** With «Συνέχισε στο παρασκήνιο» off — the default — this page plays it and
 * silences it in onStop, as it always has. With it on, [IsonPlaybackService] plays it from the first
 * moment and this page only sends it requests, so leaving the page hands nothing over and nothing
 * clicks. A page opened over a background ison shows that ison's mode and φθόγγος. If the system
 * refuses the service, this page plays the ison itself.
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

    /** The mode on screen: kept across a recreation, and saved to prefs only by a tap. */
    private var shownModeKey: String? = null

    /** «Συνέχισε στο παρασκήνιο»: the background service plays the ison, not this page. */
    private var inBackground: Boolean by mutableStateOf(false)

    /** What the screen last asked the ison to hold; null when it is off. */
    private var isonRequest: IsonDrone.Request? = null

    /** The answer does not matter: without the permission the ison still plays, only unannounced. */
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPrefs.open(this, AppPrefs.Store.EIGHT_MODES)
        activeTimbre = loadSavedTimbre()
        inBackground = prefs.getBoolean(IN_BACKGROUND_PREF_KEY, false)
        val sounding = if (inBackground) IsonPlaybackService.playing.value else null
        // What was on screen before a recreation; else the mode this opening asked for; else the
        // mode the background ison is playing; else the last one the user picked here.
        shownModeKey = savedInstanceState?.getString(STATE_SHOWN_MODE_KEY)
            ?: intent.getStringExtra(EXTRA_MODE_KEY)
            ?: sounding?.mode?.key
            ?: prefs.getString(SELECTED_MODE_KEY_PREF_KEY, null)
        val startIson = savedInstanceState == null && intent.getBooleanExtra(EXTRA_START_ISON, false)
        // «Αναπαραγωγή» of a stopped background ison names the φθόγγος it had moved to.
        val startChoice = if (startIson) intent.readIsonChoice() else null

        // The system would not let the service play: this page plays the ison instead.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                IsonPlaybackService.refused.collect { refused ->
                    if (inBackground && refused == isonRequest) playLocally(refused)
                }
            }
        }

        setContent {
            val backgroundIson by IsonPlaybackService.playing.collectAsState()
            LbmTheme(palette = currentPalette()) {
                EightModesScreen(
                    initialModeIndex = eightModesIndexOf(shownModeKey) ?: 0,
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
                        if (inBackground) isonRequest?.let { playInBackground(it) }
                    },
                    onBaseShiftChange = ::persistBaseShift,
                    onTonePress = { frequencyHz -> tonePlayer.start(frequencyHz, activeTimbre) },
                    onToneRelease = { tonePlayer.stop() },
                    onIsonChange = ::setIson,
                    onListenChange = ::setListening,
                    heardFrequencyHz = heardFrequencyHz,
                    micDenied = micDenied,
                    onOpenMenu = { EightModesNavigation.showMenu(this, selectedTopicKey = null) },
                    onBack = ::finish,
                    initialDroneOn = startIson || sounding != null,
                    initialIsonChoice = sounding?.takeIf { it.mode.key == shownModeKey }?.choice ?: startChoice,
                    inBackground = inBackground,
                    onInBackgroundChange = ::switchInBackground,
                    backgroundIson = backgroundIson,
                )
            }
        }
    }

    private fun loadSavedBaseShifts(): Map<Int, Int> =
        EIGHT_MODES.indices.associateWith { index ->
            prefs.getInt(baseShiftPrefKey(EIGHT_MODES[index].theoryKey), BASE_SHIFT_DEFAULT_MORIA)
                .coerceIn(BASE_SHIFT_MORIA_MIN, BASE_SHIFT_MORIA_MAX)
        }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        shownModeKey?.let { outState.putString(STATE_SHOWN_MODE_KEY, it) }
    }

    private fun loadSavedTimbre(): ToneTimbre {
        val stored = prefs.getString(TONE_TIMBRE_PREF_KEY, ToneTimbre.CLEAN.name)
        return ToneTimbre.entries.firstOrNull { it.name == stored } ?: ToneTimbre.CLEAN
    }

    private fun persistSelectedMode(modeIndex: Int) {
        val key = EIGHT_MODES.getOrNull(modeIndex)?.theoryKey ?: return
        shownModeKey = key
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
     * The screen's ison, to whoever plays it (ClickUp `869f5x2dq`): this page, or — with «Συνέχισε
     * στο παρασκήνιο» — the background service, which then owns it from the first moment.
     */
    private fun setIson(request: IsonDrone.Request?) {
        isonRequest = request
        if (!inBackground) {
            playLocally(request)
            return
        }
        setDroneFrequency(null)
        if (request == null) IsonPlaybackService.stop(this) else playInBackground(request)
    }

    private fun playInBackground(request: IsonDrone.Request) {
        // Refused at once: today's behaviour, played by this page.
        if (!IsonPlaybackService.play(this, request, activeTimbre)) playLocally(request)
    }

    /** The page's own drone, at the pitch of the one lookup ([IsonDrone.held]). */
    private fun playLocally(request: IsonDrone.Request?) {
        setDroneFrequency(request?.let { IsonDrone.held(it)?.frequencyHz })
    }

    /** Turns «Συνέχισε στο παρασκήνιο» on or off, handing a sounding ison to its new owner. */
    private fun switchInBackground(on: Boolean) {
        if (on == inBackground) return
        inBackground = on
        prefs.edit().putBoolean(IN_BACKGROUND_PREF_KEY, on).apply()
        val request = isonRequest
        if (on) {
            askForNotifications()
            if (request != null) {
                setDroneFrequency(null)
                playInBackground(request)
            }
        } else {
            IsonPlaybackService.forget(this)
            playLocally(request)
        }
    }

    /**
     * Android 13+, when the option is turned on: ask to show the ison's notification — once per
     * install, shared with the recording's ([AppNotifications.shouldAskPermission]). Marked as asked
     * before the prompt, so it is never asked twice. A «no» only hides the notification.
     */
    private fun askForNotifications() {
        if (!AppNotifications.shouldAskPermission(this)) return
        AppNotifications.markPermissionAsked(this)
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

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
        // Silence this page's drone in the background - a held AudioTrack would keep sounding over
        // other apps - but keep droneFrequencyHz so onStart can restore it. A background ison is the
        // service's, not this page's, and plays on.
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

    companion object {
        private const val BASE_SHIFT_MORIA_MIN = -12
        private const val BASE_SHIFT_MORIA_MAX = 12
        private const val BASE_SHIFT_DEFAULT_MORIA = 0
        private const val BASE_SHIFT_PREF_KEY_PREFIX = AppPrefs.BASE_SHIFT_KEY_PREFIX
        private val TONE_TIMBRE_PREF_KEY = AppPrefs.SelectedToneTimbre.name
        private val SELECTED_MODE_KEY_PREF_KEY = AppPrefs.SelectedModeKey.name
        private val IN_BACKGROUND_PREF_KEY = AppPrefs.IsonInBackground.name
        private const val EXTRA_MODE_KEY = "com.johnchourp.learnbyzantinemusic.modes.EXTRA_MODE_KEY"
        private const val STATE_SHOWN_MODE_KEY = "shown_mode_key"

        /**
         * Set by the launcher shortcut «Ίσο» in `res/xml/shortcuts.xml`, whose `<extra>` must spell
         * this exactly; `IsonShortcutTest` holds the two together.
         */
        internal const val EXTRA_START_ISON = "com.johnchourp.learnbyzantinemusic.modes.EXTRA_START_ISON"

        /** The φθόγγος [startIsonIntent] asks for: its name and octave. */
        private const val EXTRA_ISON_CHOICE = "com.johnchourp.learnbyzantinemusic.modes.EXTRA_ISON_CHOICE"
        private const val EXTRA_ISON_CHOICE_OCTAVE = "com.johnchourp.learnbyzantinemusic.modes.EXTRA_ISON_CHOICE_OCTAVE"

        /** Opens the page on [modeKey] for this opening only; see the class KDoc. */
        fun intent(context: Context, modeKey: String): Intent =
            Intent(context, EightModesActivity::class.java).putExtra(EXTRA_MODE_KEY, modeKey)

        /**
         * Opens the page on [request]'s mode with that ison already sounding, on its φθόγγος — the
         * «Αναπαραγωγή» of a stopped background ison, which only a visible page may start again.
         */
        fun startIsonIntent(context: Context, request: IsonDrone.Request): Intent {
            val intent = intent(context, request.mode.key).putExtra(EXTRA_START_ISON, true)
            request.choice?.let { choice ->
                intent.putExtra(EXTRA_ISON_CHOICE, choice.name.name)
                intent.putExtra(EXTRA_ISON_CHOICE_OCTAVE, choice.octave)
            }
            return intent
        }

        private fun Intent.readIsonChoice(): Phthong? =
            getStringExtra(EXTRA_ISON_CHOICE)
                ?.let { stored -> PhthongName.entries.firstOrNull { it.name == stored } }
                ?.let { name -> Phthong(name, getIntExtra(EXTRA_ISON_CHOICE_OCTAVE, 0)) }
    }
}
