package com.johnchourp.learnbyzantinemusic.trainer

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.johnchourp.learnbyzantinemusic.modes.PhthongTonePlayer
import com.johnchourp.learnbyzantinemusic.modes.ToneTimbre

/**
 * Plays a [MelodyPlaybackPlanner] schedule by driving the shared [PhthongTonePlayer].
 * Each note start is scheduled at its **absolute** planned time (and the final stop at the
 * planned total), so the tone player's fade/start latency does not accumulate into tempo
 * drift across the melody. Scheduling runs on a dedicated background thread so that
 * latency never blocks the UI; note-start and completion callbacks are delivered on the
 * main thread for safe view updates.
 */
class MelodySequencePlayer(
    private val tonePlayer: PhthongTonePlayer,
    private val timbre: ToneTimbre = ToneTimbre.SOFT
) {
    interface Listener {
        fun onNoteStarted(event: PlannedNoteEvent)
        fun onFinished(completed: Boolean)
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var workerThread: HandlerThread? = null

    @Volatile
    private var workerHandler: Handler? = null

    @Volatile
    private var playing = false

    val isPlaying: Boolean get() = playing

    fun play(plan: List<PlannedNoteEvent>, listener: Listener?) {
        stop()
        if (plan.isEmpty()) {
            listener?.onFinished(true)
            return
        }
        playing = true

        val thread = HandlerThread("MelodyTrainerPlayback").apply { start() }
        val handler = Handler(thread.looper)
        workerThread = thread
        workerHandler = handler

        for (event in plan) {
            handler.postDelayed({
                if (!playing) return@postDelayed
                tonePlayer.start(event.frequencyHz, timbre)
                mainHandler.post { if (playing) listener?.onNoteStarted(event) }
            }, event.startMillis)
        }

        val totalMillis = MelodyPlaybackPlanner.totalDurationMillis(plan)
        handler.postDelayed({ finish(listener) }, totalMillis)
    }

    private fun finish(listener: Listener?) {
        if (!playing) return
        playing = false
        tonePlayer.stop()
        teardownWorker()
        mainHandler.post { listener?.onFinished(true) }
    }

    /** Stops playback immediately. Safe to call from the main thread and idempotent. */
    fun stop() {
        val wasPlaying = playing
        playing = false
        workerHandler?.removeCallbacksAndMessages(null)
        teardownWorker()
        if (wasPlaying) {
            tonePlayer.stop()
        }
    }

    private fun teardownWorker() {
        workerThread?.quit()
        workerThread = null
        workerHandler = null
    }
}
