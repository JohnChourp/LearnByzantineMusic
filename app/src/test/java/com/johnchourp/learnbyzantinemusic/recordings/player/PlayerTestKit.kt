package com.johnchourp.learnbyzantinemusic.recordings.player

/**
 * A fake [AudioPlayer] that behaves like Android's `MediaPlayer` where it matters (ClickUp
 * `869f5x268`) — above all, **a non-zero speed starts playback, even from pause** — and a rig that
 * hands the controller a fresh one per prepare, keeping every player it made. Shared by the player
 * tests; holds no tests itself.
 */
class FakeAudioPlayer(
    private val durationMs: Int,
    private val failPrepare: Boolean,
    private val refuseTuning: Boolean,
    private val prepareAtOnce: Boolean,
) : AudioPlayer {
    private var listener: AudioPlayer.Listener? = null

    var isPlaying = false
        private set
    var released = false
        private set
    var position = 0
    val tunings = mutableListOf<Pair<Float, Float>>()
    val seeks = mutableListOf<Int>()
    val calls = mutableListOf<String>()

    override fun prepare(listener: AudioPlayer.Listener) {
        this.listener = listener
        calls += "prepare"
        if (prepareAtOnce) finishPreparing()
    }

    fun finishPreparing() {
        if (failPrepare) listener?.onError() else listener?.onPrepared(durationMs)
    }

    override fun start() {
        calls += "start"
        isPlaying = true
    }

    override fun pause() {
        calls += "pause"
        isPlaying = false
    }

    override fun seekTo(positionMs: Int) {
        seeks += positionMs
        position = positionMs
    }

    /** MediaPlayer's quirk, reproduced: any non-zero speed also starts playback. */
    override fun setTuning(speed: Float, pitchRatio: Float): Boolean {
        calls += "tune"
        if (refuseTuning) return false
        tunings += speed to pitchRatio
        if (speed != 0f) isPlaying = true
        return true
    }

    override val positionMs: Int get() = position

    override fun release() {
        released = true
        isPlaying = false
    }

    /** Playback reaches the end of the file. */
    fun complete() {
        isPlaying = false
        listener?.onCompletion()
    }
}

class PlayerRig(private val durationMs: Int = 60_000) {
    val players = mutableListOf<FakeAudioPlayer>()
    var failPrepare = false
    var refuseTuning = false
    var prepareAtOnce = true
    var factoryThrows = false

    val controller = PlayerController<String> {
        if (factoryThrows) throw IllegalStateException("no player for this file (fake)")
        FakeAudioPlayer(durationMs, failPrepare, refuseTuning, prepareAtOnce).also { players += it }
    }

    /** The player the controller is using now. */
    val player: FakeAudioPlayer get() = players.last()

    val state: PlayerState get() = controller.state.value

    /** Opens a recording, which starts playing, and moves playback to [positionMs]. */
    fun playingAt(positionMs: Int, name: String = "recording_20260926_101500.flac"): PlayerRig {
        controller.open(name, name)
        player.position = positionMs
        return this
    }
}
