package com.johnchourp.learnbyzantinemusic.recordings.player

import com.johnchourp.learnbyzantinemusic.recordings.session.RecordingSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** What [PlayerController] needs from a media player: `MediaPlayer` in the app, a fake in tests. */
interface AudioPlayer {
    /** Starts preparing; the listener then hears [Listener.onPrepared] or [Listener.onError]. */
    fun prepare(listener: Listener)

    fun start()

    fun pause()

    fun seekTo(positionMs: Int)

    /**
     * Speed and pitch factor. **On Android's `MediaPlayer` a non-zero speed also starts playback**,
     * even from pause — so the controller calls this only when it means to play. False when the
     * device cannot apply them to this file.
     */
    fun setTuning(speed: Float, pitchRatio: Float): Boolean

    val positionMs: Int

    fun release()

    interface Listener {
        fun onPrepared(durationMs: Int)

        fun onError()

        fun onCompletion()
    }
}

enum class PlayerPhase { EMPTY, LOADING, PAUSED, PLAYING, FAILED }

data class PlayerState(
    val phase: PlayerPhase = PlayerPhase.EMPTY,
    val title: String? = null,
    val durationMs: Int = 0,
    val positionMs: Int = 0,
    val tuning: PlaybackTuning = PlaybackTuning(),
    val loopStartMs: Int? = null,
    val loopEndMs: Int? = null,
    /** A recording is in progress: nothing plays — it would go into the microphone. */
    val blocked: Boolean = false,
    /** False once the device refused speed or pitch for this file: it still plays, unchanged. */
    val tuningAvailable: Boolean = true,
    /** How many recordings were opened so far — a screen scrolls its card into view when it changes. */
    val openCount: Int = 0,
) {
    val loop: LoopRegion?
        get() {
            val start = loopStartMs ?: return null
            val end = loopEndMs ?: return null
            return LoopRegion.of(start, end, durationMs)
        }

    val controlsEnabled: Boolean
        get() = !blocked && (phase == PlayerPhase.PAUSED || phase == PlayerPhase.PLAYING)
}

/** The in-app player waits while a recording is recording, paused or being saved. */
fun playbackBlockedBy(session: RecordingSessionState): Boolean = session.isActive

/**
 * The in-app player's behaviour, without Android (ClickUp `869f5x268`): open a recording, play and
 * pause, seek, the A-B loop, speed and shift — every decision here, so `MediaPlayerAudio` stays a thin
 * adapter and the rules are tested on the JVM.
 *
 * **Two platform traps it is built around.**
 * - `MediaPlayer.setPlaybackParams` with any non-zero speed *starts* a paused player. So speed and
 *   shift are applied only while playing, or as playback starts; changed while paused, they wait
 *   (`PlayerParamsWhilePausedTest`).
 * - The screen releases the player in `onStop` — no sound in the background. [suspend] keeps the
 *   recording, position, loop and tuning, and the next [play] prepares it again where it was.
 *
 * Everything runs on the main thread, as the player's callbacks do. [S] identifies a recording: an
 * Android `Uri` in the app, a string in the tests.
 */
class PlayerController<S>(private val playerFor: (S) -> AudioPlayer) {
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var source: S? = null
    private var player: AudioPlayer? = null
    private var playWhenReady = false

    /** What the current player was last told; null = its own defaults, 1× and unshifted. */
    private var appliedTuning: PlaybackTuning? = null

    /** Selects [source] and starts it. A new recording starts at 0, with no loop; speed and shift carry over. */
    fun open(source: S, title: String) {
        releasePlayer()
        this.source = source
        _state.update { PlayerState(title = title, tuning = it.tuning, blocked = it.blocked, openCount = it.openCount + 1) }
        load(playAfter = true)
    }

    fun play() {
        val state = _state.value
        if (state.blocked) return
        when (state.phase) {
            PlayerPhase.LOADING -> playWhenReady = true
            PlayerPhase.PAUSED -> {
                val current = player
                // Released in onStop: prepare it again, and it resumes where it was.
                if (current == null) load(playAfter = true) else startPlaying(current)
            }
            PlayerPhase.EMPTY, PlayerPhase.PLAYING, PlayerPhase.FAILED -> Unit
        }
    }

    fun pause() {
        playWhenReady = false
        val current = player ?: return
        if (_state.value.phase != PlayerPhase.PLAYING) return
        runCatching { current.pause() }
        _state.update { it.copy(phase = PlayerPhase.PAUSED, positionMs = current.positionMs) }
    }

    fun toggle() {
        if (_state.value.phase == PlayerPhase.PLAYING) pause() else play()
    }

    fun seekTo(positionMs: Int) {
        val state = _state.value
        if (state.phase != PlayerPhase.PAUSED && state.phase != PlayerPhase.PLAYING) return
        val target = positionMs.coerceIn(0, state.durationMs)
        player?.let { runCatching { it.seekTo(target) } }
        _state.update { it.copy(positionMs = target) }
    }

    fun setSpeed(speed: Float) = retune(_state.value.tuning.withSpeed(speed))

    fun setShift(moria: Int) = retune(_state.value.tuning.withShift(moria))

    fun resetTuning() = retune(PlaybackTuning())

    /** A at the current position. A mark B no longer fits after is dropped. */
    fun markLoopStart(): LoopMarkResult {
        val state = _state.value
        if (!hasMedia(state)) return LoopMarkResult.UNAVAILABLE
        val start = currentPositionMs().coerceIn(0, state.durationMs)
        val end = state.loopEndMs?.takeIf { it > start && LoopRegion.of(start, it, state.durationMs) != null }
        _state.update { it.copy(loopStartMs = start, loopEndMs = end) }
        return LoopMarkResult.SET
    }

    /** B at the current position: after A, and far enough from it. */
    fun markLoopEnd(): LoopMarkResult {
        val state = _state.value
        if (!hasMedia(state)) return LoopMarkResult.UNAVAILABLE
        val start = state.loopStartMs ?: return LoopMarkResult.NEEDS_START
        val end = currentPositionMs().coerceIn(0, state.durationMs)
        if (end <= start) return LoopMarkResult.BEFORE_START
        if (LoopRegion.of(start, end, state.durationMs) == null) return LoopMarkResult.TOO_SHORT
        _state.update { it.copy(loopEndMs = end) }
        return LoopMarkResult.SET
    }

    fun clearLoop() {
        _state.update { it.copy(loopStartMs = null, loopEndMs = null) }
    }

    /** Called many times a second while playing: follows the position, and goes back to A at B. */
    fun tick() {
        val current = player ?: return
        if (_state.value.phase != PlayerPhase.PLAYING) return
        val position = current.positionMs
        val backTo = _state.value.loop?.wrap(position)
        if (backTo != null) {
            runCatching { current.seekTo(backTo) }
            _state.update { it.copy(positionMs = backTo) }
        } else {
            _state.update { it.copy(positionMs = position) }
        }
    }

    /** While a recording is in progress nothing plays; what was playing pauses. */
    fun setBlocked(blocked: Boolean) {
        if (blocked) pause()
        _state.update { it.copy(blocked = blocked) }
    }

    /** `onStop`: the player is released — no sound in the background — and everything else is kept. */
    fun suspend() {
        val current = player ?: return
        val state = _state.value
        val position = if (state.phase == PlayerPhase.PLAYING) current.positionMs else state.positionMs
        playWhenReady = false
        releasePlayer()
        _state.update {
            when (it.phase) {
                PlayerPhase.LOADING, PlayerPhase.PAUSED, PlayerPhase.PLAYING -> it.copy(phase = PlayerPhase.PAUSED, positionMs = position)
                PlayerPhase.EMPTY, PlayerPhase.FAILED -> it
            }
        }
    }

    /** Forgets the recording. Speed and shift stay for the next one. */
    fun close() {
        releasePlayer()
        source = null
        playWhenReady = false
        _state.update { PlayerState(tuning = it.tuning, blocked = it.blocked, openCount = it.openCount) }
    }

    private fun retune(tuning: PlaybackTuning) {
        _state.update { it.copy(tuning = tuning) }
        // Only while playing: on MediaPlayer, a speed applied to a paused player starts it. A paused
        // player takes the new tuning as it next starts.
        val current = player ?: return
        if (_state.value.phase == PlayerPhase.PLAYING) applyTuning(current)
    }

    private fun load(playAfter: Boolean) {
        val source = source ?: return
        playWhenReady = playAfter
        appliedTuning = null
        _state.update { it.copy(phase = PlayerPhase.LOADING, tuningAvailable = true) }
        val created = runCatching { playerFor(source) }.getOrNull()
        if (created == null) {
            fail()
            return
        }
        player = created
        runCatching { created.prepare(listenerFor(created)) }.onFailure { if (player === created) fail() }
    }

    private fun startPlaying(current: AudioPlayer) {
        if (_state.value.blocked) return
        // Tuning first: on MediaPlayer that alone starts playback; start() then only makes sure.
        applyTuning(current)
        if (runCatching { current.start() }.isFailure) {
            fail()
            return
        }
        _state.update { it.copy(phase = PlayerPhase.PLAYING) }
    }

    private fun applyTuning(current: AudioPlayer) {
        val tuning = _state.value.tuning
        // A fresh player already plays 1×, unshifted: telling it so could only fail on a device
        // without speed control, for a change nobody asked for.
        if (tuning == (appliedTuning ?: PlaybackTuning())) return
        val applied = runCatching { current.setTuning(tuning.speed, tuning.pitchRatio) }.getOrDefault(false)
        if (applied) {
            appliedTuning = tuning
        } else {
            _state.update { it.copy(tuningAvailable = false) }
        }
    }

    private fun listenerFor(owner: AudioPlayer) = object : AudioPlayer.Listener {
        override fun onPrepared(durationMs: Int) {
            if (player !== owner) return
            val duration = durationMs.coerceAtLeast(0)
            val resumeAt = _state.value.positionMs.coerceIn(0, duration)
            if (resumeAt > 0) runCatching { owner.seekTo(resumeAt) }
            _state.update { it.copy(phase = PlayerPhase.PAUSED, durationMs = duration, positionMs = resumeAt) }
            val play = playWhenReady
            playWhenReady = false
            if (play) startPlaying(owner)
        }

        override fun onError() {
            if (player === owner) fail()
        }

        override fun onCompletion() {
            if (player !== owner) return
            val loop = _state.value.loop
            val restartAt = loop?.startMs ?: 0
            runCatching { owner.seekTo(restartAt) }
            _state.update { it.copy(phase = PlayerPhase.PAUSED, positionMs = restartAt) }
            // With a loop the passage goes round again; without one the recording waits at its start.
            if (loop != null) startPlaying(owner)
        }
    }

    private fun fail() {
        releasePlayer()
        playWhenReady = false
        _state.update { it.copy(phase = PlayerPhase.FAILED) }
    }

    private fun releasePlayer() {
        val current = player ?: return
        player = null
        appliedTuning = null
        runCatching { current.release() }
    }

    private fun hasMedia(state: PlayerState): Boolean =
        (state.phase == PlayerPhase.PAUSED || state.phase == PlayerPhase.PLAYING) && state.durationMs > 0

    private fun currentPositionMs(): Int {
        val current = player
        return if (current != null && _state.value.phase == PlayerPhase.PLAYING) current.positionMs else _state.value.positionMs
    }
}
