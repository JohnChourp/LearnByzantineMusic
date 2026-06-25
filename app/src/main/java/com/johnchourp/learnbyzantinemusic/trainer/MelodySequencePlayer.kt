package com.johnchourp.learnbyzantinemusic.trainer

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.johnchourp.learnbyzantinemusic.modes.PhthongTonePlayer
import com.johnchourp.learnbyzantinemusic.modes.ToneTimbre

/**
 * Plays a [MelodyPlaybackPlanner] schedule by driving the shared [PhthongTonePlayer] from
 * note to note. Scheduling runs on a dedicated background thread so the tone player's
 * fade-out/join never blocks the UI; note-start and completion callbacks are delivered on
 * the main thread for safe view updates. A short gap between notes keeps repeated phthongi
 * audibly distinct.
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
    private var workerThread: HandlerThread? = null
    private var workerHandler: Handler? = null

    private var plan: List<PlannedNoteEvent> = emptyList()
    private var listener: Listener? = null
    private var cursor = 0

    @Volatile
    private var playing = false

    val isPlaying: Boolean get() = playing

    fun play(plan: List<PlannedNoteEvent>, listener: Listener?) {
        stop()
        if (plan.isEmpty()) {
            listener?.onFinished(true)
            return
        }
        this.plan = plan
        this.listener = listener
        cursor = 0
        playing = true

        val thread = HandlerThread("MelodyTrainerPlayback").apply { start() }
        workerThread = thread
        workerHandler = Handler(thread.looper).also { it.post { step() } }
    }

    private fun step() {
        if (!playing) return
        if (cursor >= plan.size) {
            finish(completed = true)
            return
        }
        val event = plan[cursor]
        tonePlayer.start(event.frequencyHz, timbre)
        mainHandler.post { if (playing) listener?.onNoteStarted(event) }

        val gap = NOTE_GAP_MILLIS.coerceAtMost(event.durationMillis / 4)
        val audibleMillis = (event.durationMillis - gap).coerceAtLeast(MIN_AUDIBLE_MILLIS)
        val handler = workerHandler ?: return
        handler.postDelayed({
            if (!playing) return@postDelayed
            tonePlayer.stop()
            handler.postDelayed({
                if (!playing) return@postDelayed
                cursor++
                step()
            }, gap)
        }, audibleMillis)
    }

    private fun finish(completed: Boolean) {
        playing = false
        tonePlayer.stop()
        teardownWorker()
        mainHandler.post { listener?.onFinished(completed) }
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

    companion object {
        const val NOTE_GAP_MILLIS = 45L
        const val MIN_AUDIBLE_MILLIS = 90L
    }
}
