package com.johnchourp.learnbyzantinemusic.trainer

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.johnchourp.learnbyzantinemusic.modes.PhthongTonePlayer
import com.johnchourp.learnbyzantinemusic.modes.ToneTimbre

/**
 * Plays a [MelodyPlaybackPlanner] schedule with accurate timing.
 *
 * Each note start is scheduled at its **absolute** planned time so latency never
 * accumulates into tempo drift. To keep the scheduler unblocked, two [PhthongTonePlayer]
 * voices are used in ping-pong: the next note is started on the idle voice (a fast,
 * non-blocking call) while the previous voice is faded out on a separate thread. This
 * avoids the previous note's fade/join delay being consumed inside the scheduler path,
 * which would otherwise make short notes at high tempo fall behind the beat.
 *
 * A monotonically increasing session token, checked before and after the (brief) blocking
 * `start()` call, prevents a note from beginning after [stop] has already cancelled
 * playback. Note-start and completion callbacks are delivered on the main thread.
 */
class MelodySequencePlayer(
    private val timbre: ToneTimbre = ToneTimbre.SOFT,
    voiceFactory: () -> PhthongTonePlayer = { PhthongTonePlayer() }
) {
    interface Listener {
        fun onNoteStarted(event: PlannedNoteEvent)
        fun onFinished(completed: Boolean)
    }

    private val voices: List<PhthongTonePlayer> = listOf(voiceFactory(), voiceFactory())
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var workerThread: HandlerThread? = null

    @Volatile
    private var workerHandler: Handler? = null

    @Volatile
    private var fadeThread: HandlerThread? = null

    @Volatile
    private var fadeHandler: Handler? = null

    @Volatile
    private var playing = false

    @Volatile
    private var sessionToken = 0L

    val isPlaying: Boolean get() = playing

    fun play(plan: List<PlannedNoteEvent>, listener: Listener?) {
        stop()
        if (plan.isEmpty()) {
            listener?.onFinished(true)
            return
        }
        val token = sessionToken + 1
        sessionToken = token
        playing = true

        val worker = HandlerThread("MelodyTrainerPlayback").apply { start() }
        val fade = HandlerThread("MelodyTrainerFade").apply { start() }
        val workerHandlerLocal = Handler(worker.looper)
        workerThread = worker
        workerHandler = workerHandlerLocal
        fadeThread = fade
        fadeHandler = Handler(fade.looper)

        for (event in plan) {
            workerHandlerLocal.postDelayed({
                if (!playing || token != sessionToken) return@postDelayed
                val current = voices[event.index % voices.size]
                val previous = voices[(event.index + 1) % voices.size]
                current.start(event.frequencyHz, timbre)
                if (!playing || token != sessionToken) {
                    current.stop()
                    return@postDelayed
                }
                fadeHandler?.post { previous.stop() }
                mainHandler.post { if (playing && token == sessionToken) listener?.onNoteStarted(event) }
            }, event.startMillis)
        }

        val totalMillis = MelodyPlaybackPlanner.totalDurationMillis(plan)
        workerHandlerLocal.postDelayed({ finish(listener, token) }, totalMillis)
    }

    private fun finish(listener: Listener?, token: Long) {
        // Bail if a newer session (a fresh play()/stop()) has superseded this one — otherwise
        // an old finish could tear down the new session's worker/fade threads.
        if (token != sessionToken) return
        playing = false
        voices.forEach { it.stop() }
        // A stop()+play() can slip in during the blocking voices.stop() above; re-check before
        // touching the shared thread fields so we never quit the new session's handlers.
        if (token != sessionToken) return
        teardownThreads()
        mainHandler.post { listener?.onFinished(true) }
    }

    /** Stops playback immediately. Safe to call from the main thread and idempotent. */
    fun stop() {
        val wasPlaying = playing
        playing = false
        sessionToken++
        workerHandler?.removeCallbacksAndMessages(null)
        fadeHandler?.removeCallbacksAndMessages(null)
        teardownThreads()
        if (wasPlaying) {
            voices.forEach { it.stop() }
        }
    }

    /** Releases the underlying audio resources. The player cannot be reused afterwards. */
    fun release() {
        stop()
        voices.forEach { it.release() }
    }

    private fun teardownThreads() {
        workerThread?.quit()
        workerThread = null
        workerHandler = null
        fadeThread?.quit()
        fadeThread = null
        fadeHandler = null
    }
}
