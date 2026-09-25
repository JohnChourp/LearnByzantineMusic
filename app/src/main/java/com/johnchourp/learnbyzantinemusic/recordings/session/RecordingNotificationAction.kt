package com.johnchourp.learnbyzantinemusic.recordings.session

import com.johnchourp.learnbyzantinemusic.recordings.RecordingStateUi
import kotlinx.coroutines.flow.StateFlow

/**
 * What a control outside the «Ηχογραφήσεις» screen — the notification, the foreground service — may
 * do to the recording. There is deliberately no discard here: only the screen's own «Απόρριψη &
 * έξοδος» can throw a recording away (ClickUp `869f5x273`).
 */
interface RecordingControls {
    val state: StateFlow<RecordingSessionState>

    fun pause()

    fun resume()

    /** Stops **and saves**. */
    fun stop(): Boolean
}

/**
 * The buttons of the «Ηχογράφηση…» notification and what each does to the recording.
 *
 * «Στάση» stops and saves, exactly like the screen's «Σταμάτημα»: a tap in the notification shade,
 * often made without looking, must never cost a recording (`RecordingNotificationActionTest`).
 * The intent actions are what the notification's buttons send to `RecordingService`.
 */
enum class RecordingNotificationAction(val intentAction: String) {
    PAUSE("com.johnchourp.learnbyzantinemusic.recordings.action.PAUSE"),
    RESUME("com.johnchourp.learnbyzantinemusic.recordings.action.RESUME"),
    STOP("com.johnchourp.learnbyzantinemusic.recordings.action.STOP");

    fun applyTo(controls: RecordingControls) {
        when (this) {
            PAUSE -> controls.pause()
            RESUME -> controls.resume()
            STOP -> controls.stop()
        }
    }

    companion object {
        fun fromIntentAction(action: String?): RecordingNotificationAction? =
            entries.firstOrNull { it.intentAction == action }

        /** The buttons for [phase]: pause or resume, then stop. None while saving — there is nothing to control. */
        fun buttonsFor(phase: RecordingStateUi): List<RecordingNotificationAction> = when (phase) {
            RecordingStateUi.RECORDING -> listOf(PAUSE, STOP)
            RecordingStateUi.PAUSED -> listOf(RESUME, STOP)
            RecordingStateUi.SAVING, RecordingStateUi.IDLE, RecordingStateUi.ERROR -> emptyList()
        }
    }
}
