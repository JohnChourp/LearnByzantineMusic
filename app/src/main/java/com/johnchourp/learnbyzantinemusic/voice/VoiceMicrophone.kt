package com.johnchourp.learnbyzantinemusic.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.johnchourp.learnbyzantinemusic.trainer.TrainerPitchEngine

/**
 * The microphone for «Βρες τη φωνή σου» on a screen that has none of its own — Settings (ClickUp
 * `869f5x2dd`, J4). The 8 Ήχοι page keeps its own, which its «Πού είμαι» shares with the test.
 *
 * The same contract as that page's: the RECORD_AUDIO permission is asked only when the test starts
 * listening («Ξεκίνα»), never before; a refusal is remembered so the dialog can say so; and the
 * capture is **released**, not paused, whenever the screen stops — a capture left running in the
 * background holds the microphone away from every other app — and resumed when it returns, if the
 * test still wants it.
 *
 * Create it while the activity is being constructed (a property initializer): it registers the
 * permission request, which an activity allows only before it is started.
 */
class VoiceMicrophone(private val activity: ComponentActivity) {

    /** The raw pitch last heard, in Hz, or null when nothing usable is coming in. */
    var heardFrequencyHz: Double? by mutableStateOf(null)
        private set

    /** True once the microphone was refused, or could not be opened. */
    var denied: Boolean by mutableStateOf(false)
        private set

    /** What the test asked for, so [onStart] can resume a capture that [onStop] released. */
    private var wanted = false

    private val engine: TrainerPitchEngine by lazy {
        TrainerPitchEngine(
            onPitch = { match, _ -> heardFrequencyHz = match?.frequencyHz?.takeIf { it > 0.0 } },
            onCaptureError = {
                wanted = false
                stopCapture()
            },
        )
    }

    private val permission =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            denied = !granted
            if (granted) startCapture() else wanted = false
        }

    /** The test wants to listen, or to stop. The permission is asked here and only here. */
    fun listen(on: Boolean) {
        wanted = on
        if (!on) {
            stopCapture()
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            denied = false
            startCapture()
        } else {
            permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun onStart() {
        if (wanted) listen(true)
    }

    fun onStop() {
        stopCapture()
    }

    private fun startCapture() {
        if (engine.isRunning) return
        if (!engine.start()) {
            // The microphone exists but could not be opened — another app holds it, or the device refused.
            wanted = false
            denied = true
        }
    }

    private fun stopCapture() {
        engine.stop()
        heardFrequencyHz = null
    }
}
