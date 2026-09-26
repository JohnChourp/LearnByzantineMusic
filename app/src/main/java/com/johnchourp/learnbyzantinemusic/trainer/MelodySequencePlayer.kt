package com.johnchourp.learnbyzantinemusic.trainer

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
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
 *
 * [playLoop] is «Ψάλλε μαζί» (ClickUp `869f5x2cv`): the same scheduler, looping round after round
 * of [MelodyPlaybackPlanner.planRound] until [stop]. Every note and every metronome click of every
 * round is posted at its planned time from **one** origin, never after the previous one, so the loop
 * cannot drift however long it runs, and the clicks cannot drift from the notes: there is no second
 * scheduler and no `delay()` per note. Each round is posted while the one before it starts, a whole
 * round ahead of time.
 */
class MelodySequencePlayer(
    private val timbre: ToneTimbre = ToneTimbre.SOFT,
    voiceFactory: () -> PhthongTonePlayer = { PhthongTonePlayer() }
) {
    interface Listener {
        fun onNoteStarted(event: PlannedNoteEvent)
        fun onFinished(completed: Boolean)
    }

    /** «Ψάλλε μαζί»: told on the main thread when each note of each round starts, sounding or not. */
    fun interface LoopListener {
        fun onNoteStarted(round: PlannedRound, event: PlannedNoteEvent)
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

    /** The guide's own level in [playLoop], 0 … 1; each round's gain is applied on top of it. */
    @Volatile
    private var guideVolume = 1f

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

    /**
     * Loops the melody until [stop]: round `k` is [roundAt]`(k)`, played at its planned times from one
     * origin. Each note sounds at the guide's level ([setGuideVolume]) times the round's
     * [PlannedRound.guideGain]; a note at level 0 is not sounded at all, but still reported, so the
     * line stays lit while the guide is silent. [onTick] runs on the playback thread at every
     * [PlannedTick], and must not block. A round whose melody ends before the round does (a rest to
     * the next whole χρόνος) stops its last note at the note's own end.
     */
    fun playLoop(roundAt: (Int) -> PlannedRound, listener: LoopListener, onTick: (downbeat: Boolean) -> Unit) {
        stop()
        val first = roundAt(0)
        if (first.notes.isEmpty()) return
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

        val loop = Loop(workerHandlerLocal, SystemClock.uptimeMillis(), token, roundAt, listener, onTick)
        loop.schedule(first)
    }

    /** The guide's level in [playLoop], 0 … 1, from the next note on. */
    fun setGuideVolume(volume: Float) {
        guideVolume = volume.coerceIn(0f, 1f)
    }

    /** One [playLoop] session: everything is posted on [handler] at [origin] plus a planned time. */
    private inner class Loop(
        private val handler: Handler,
        private val origin: Long,
        private val token: Long,
        private val roundAt: (Int) -> PlannedRound,
        private val listener: LoopListener,
        private val onTick: (Boolean) -> Unit,
    ) {
        private val isCurrent: Boolean get() = playing && token == sessionToken

        /** Posts [round]'s clicks and notes, and — at its start — the posting of the next round. */
        fun schedule(round: PlannedRound) {
            round.ticks.forEach { tick ->
                handler.postAtTime({ if (isCurrent) onTick(tick.downbeat) }, origin + tick.startMillis)
            }
            round.notes.forEach { event ->
                handler.postAtTime({ startNote(round, event) }, origin + event.startMillis)
            }
            if (round.melodyEndMillis < round.endMillis) {
                val last = voiceFor(round, round.notes.last())
                handler.postAtTime({ if (isCurrent) fadeHandler?.post { last.stop() } }, origin + round.melodyEndMillis)
            }
            handler.postAtTime({ if (isCurrent) schedule(roundAt(round.index + 1)) }, origin + round.startMillis)
        }

        private fun startNote(round: PlannedRound, event: PlannedNoteEvent) {
            if (!isCurrent) return
            val current = voiceFor(round, event)
            val previous = voices.first { it !== current }
            val level = guideVolume * round.guideGain
            if (level > 0f) {
                current.start(event.frequencyHz, timbre, level)
                if (!isCurrent) {
                    current.stop()
                    return
                }
            }
            fadeHandler?.post { previous.stop() }
            mainHandler.post { if (isCurrent) listener.onNoteStarted(round, event) }
        }

        /**
         * The voice of [event] in [round]. Counted over the whole loop, not within a round, so the
         * last note of a round and the first of the next never share a voice — not even when the
         * melody has an odd number of notes.
         */
        private fun voiceFor(round: PlannedRound, event: PlannedNoteEvent): PhthongTonePlayer {
            val number = round.index.toLong() * round.notes.size + event.index
            return voices[(number % voices.size).toInt()]
        }
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
