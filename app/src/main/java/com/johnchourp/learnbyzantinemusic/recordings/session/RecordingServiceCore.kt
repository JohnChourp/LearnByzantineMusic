package com.johnchourp.learnbyzantinemusic.recordings.session

/**
 * Runs one step of the foreground machinery — starting `RecordingService`, promoting it, updating
 * its notification — so that no failure of it can reach the recording: whatever it throws
 * (`ForegroundServiceStartNotAllowedException`, `SecurityException`, an OEM's own) is logged and
 * reported as `false`, and the recording carries on as it would without the service.
 */
object ForegroundGuard {
    fun attempt(what: String, log: (String, Throwable) -> Unit, step: () -> Unit): Boolean =
        try {
            step()
            true
        } catch (failure: Throwable) {
            runCatching { log("could not $what — the recording goes on without it", failure) }
            false
        }
}

/**
 * What `RecordingService` decides, without Android, so it can be tested (ClickUp `869f5x273`).
 *
 * The service exists to keep a recording alive with the screen off; it must never be able to end
 * one badly. So it reads the session, forwards the notification's buttons through
 * [RecordingControls] — which has no discard — and when putting itself in the foreground fails,
 * for any reason, it logs, steps aside, and the recording runs on exactly as it would without it:
 * on its screen, which stays on (`RecordingServiceFallbackTest`).
 *
 * The service lives as long as the recording is active, saving included — it holds the process in
 * the foreground until the file is safe — and leaves by itself once the session is idle. Swiping the
 * app away from recents ends the recording the way «Στάση» does: stopped and saved.
 */
class RecordingServiceCore(
    private val session: RecordingControls,
    /** `startForeground` with the notification for this state. May throw. */
    private val promote: (RecordingSessionState) -> Unit,
    /** Re-posts the notification for this state. May throw. */
    private val refresh: (RecordingSessionState) -> Unit,
    /** `stopForeground` and `stopSelf`: the service leaves. The recording is not touched. */
    private val leave: () -> Unit,
    private val log: (String, Throwable) -> Unit,
) {
    var inForeground = false
        private set

    /**
     * One `onStartCommand`: the tapped button, if any, then the foreground. False when the service
     * steps aside — nothing to keep alive, or the platform refused the foreground.
     */
    fun onStartCommand(intentAction: String?): Boolean {
        RecordingNotificationAction.fromIntentAction(intentAction)?.applyTo(session)
        val state = session.state.value
        if (!state.isActive) {
            stepAside()
            return false
        }
        if (!inForeground) {
            inForeground = ForegroundGuard.attempt("put the recording in the foreground", log) { promote(state) }
            if (!inForeground) {
                stepAside()
                return false
            }
        }
        return true
    }

    /** Every state change of the session while the service runs. */
    fun onState(state: RecordingSessionState) {
        if (!state.isActive) {
            stepAside()
            return
        }
        if (inForeground) {
            ForegroundGuard.attempt("update the recording notification", log) { refresh(state) }
        }
    }

    /** The app was swiped away from recents while recording: stop and save — never discard. */
    fun onTaskRemoved() {
        session.stop()
    }

    private fun stepAside() {
        inForeground = false
        ForegroundGuard.attempt("stop the recording service", log) { leave() }
    }
}
