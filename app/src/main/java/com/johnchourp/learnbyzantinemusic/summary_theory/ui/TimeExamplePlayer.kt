package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import com.johnchourp.learnbyzantinemusic.lessons.ui.MetronomeClicker
import com.johnchourp.learnbyzantinemusic.modes.PhthongTonePlayer
import com.johnchourp.learnbyzantinemusic.modes.ToneTimbre
import com.johnchourp.learnbyzantinemusic.music.RhythmTimeline

/**
 * Plays one [RhythmTimeline] — the notes, rests and clicks of a «Χαρακτήρες Χρόνου» example — and
 * reports the sounding note and the counted χρόνος for the highlight (ClickUp `869f5x25n`, F4).
 *
 * Every cue is posted at the **same origin** plus its own offset, on one worker thread, so the notes,
 * the clicks and the highlight fire from one timeline and nothing waits on anything else; there is no
 * `delay()` per note to add up. The note voices work as in the Melody Trainer's `MelodySequencePlayer`:
 * two [PhthongTonePlayer]s in turn, each note a fresh stream with its own attack — so the repeated ίσον
 * of an example is heard as separate notes — while the previous one fades out on a second thread,
 * off the schedule's path. The click is the metronome's own [MetronomeClicker].
 *
 * A session token, checked before every cue and callback, makes [stop] final: nothing from a stopped
 * run can sound or light up after it. [play] stops the example before it, so only one plays at a time.
 * [play], [stop] and [release] are called on the main thread, which alone changes the session and the
 * threads; callbacks arrive there too.
 */
class TimeExamplePlayer(
    private val timbre: ToneTimbre = ToneTimbre.SOFT,
    voiceFactory: () -> PhthongTonePlayer = { PhthongTonePlayer() },
) {
    interface Listener {
        /** Note [note] of the example starts — sounding, or a rest. */
        fun onNote(note: Int)

        /** χρόνος [beat] is counted, with its click. */
        fun onBeat(beat: Int)

        /** The example played to its end. */
        fun onFinished()
    }

    private val voices: List<PhthongTonePlayer> = listOf(voiceFactory(), voiceFactory())
    private val clicker = MetronomeClicker()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var session = 0L

    private var workerThread: HandlerThread? = null
    private var workerHandler: Handler? = null
    private var fadeThread: HandlerThread? = null
    private var fadeHandler: Handler? = null

    fun play(cues: List<RhythmTimeline.Cue>, listener: Listener) {
        stop()
        if (cues.isEmpty()) return
        val token = session
        val worker = HandlerThread("TimeExamplePlayback").apply { start() }
        val fade = HandlerThread("TimeExampleFade").apply { start() }
        val schedule = Handler(worker.looper)
        val fading = Handler(fade.looper)
        workerThread = worker
        workerHandler = schedule
        fadeThread = fade
        fadeHandler = fading

        fun onMain(action: () -> Unit) = mainHandler.post { if (token == session) action() }

        // One origin for every cue: each fires at origin + its own offset, never "after the last one".
        val origin = SystemClock.uptimeMillis()
        var sounded = 0
        for (cue in cues) {
            val action: () -> Unit = when (cue) {
                is RhythmTimeline.Sound -> {
                    val voice = voices[sounded % voices.size]
                    val previous = voices[(sounded + 1) % voices.size]
                    sounded++
                    {
                        voice.start(cue.frequencyHz, timbre)
                        if (token != session) {
                            voice.stop()
                        } else {
                            fading.post { previous.stop() }
                            onMain { listener.onNote(cue.note) }
                        }
                    }
                }
                is RhythmTimeline.Silence -> {
                    {
                        fading.post { voices.forEach { it.stop() } }
                        onMain { listener.onNote(cue.note) }
                    }
                }
                is RhythmTimeline.Click -> {
                    {
                        clicker.click(cue.accented)
                        onMain { listener.onBeat(cue.beat) }
                    }
                }
                is RhythmTimeline.End -> {
                    {
                        // The last note fades here, off the main thread; the threads are the main
                        // thread's to tear down, like everything else about a session.
                        voices.forEach { it.stop() }
                        onMain {
                            teardownThreads()
                            listener.onFinished()
                        }
                    }
                }
            }
            schedule.postAtTime({ if (token == session) action() }, origin + cue.atMillis)
        }
    }

    /** Stops at once: no note, click or callback of the run can come after it. Idempotent. */
    fun stop() {
        session++
        workerHandler?.removeCallbacksAndMessages(null)
        fadeHandler?.removeCallbacksAndMessages(null)
        teardownThreads()
        voices.forEach { it.stop() }
    }

    /** Stops and frees the audio. The player cannot be used afterwards. */
    fun release() {
        stop()
        voices.forEach { it.release() }
        clicker.release()
    }

    private fun teardownThreads() {
        workerThread?.quitSafely()
        workerThread = null
        workerHandler = null
        fadeThread?.quitSafely()
        fadeThread = null
        fadeHandler = null
    }
}
