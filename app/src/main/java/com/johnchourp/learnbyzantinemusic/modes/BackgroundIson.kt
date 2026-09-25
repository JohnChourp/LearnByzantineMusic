package com.johnchourp.learnbyzantinemusic.modes

import com.johnchourp.learnbyzantinemusic.music.Phthong
import kotlin.math.sign

/**
 * The ison the background service holds, as plain state and decisions (ClickUp `869f5x2dq`).
 *
 * `IsonPlaybackService` is a thin Android shell around this: it keeps the notification and the
 * AudioTrack, and does what the returned [Sound] says. Everything that decides — which pitch, moving
 * a φθόγγος up or down from the notification, when to stop by itself — lives here, where a test can
 * reach it without a device.
 *
 * - **The pitch** is [IsonDrone.held]: the page's own lookup on the page's own ladder.
 * - **φθόγγος −/+** walks «Ίσον σε…»'s choices ([IsonDrone.choices]) by pitch, and stops at the ends.
 * - **It stops by itself** [MAX_PLAY_MILLIS] after it started sounding, so an ison left on in a pocket
 *   does not play all night; moving it does not restart the clock, stopping and playing again does.
 *   Unplugged headphones stop it too ([onBecomingNoisy]): a drone that suddenly sounds out of the
 *   loudspeaker, in church, is the one thing it must never do.
 */
class BackgroundIson(private val maxPlayMillis: Long = MAX_PLAY_MILLIS) {

    /** What the player must do after a change. */
    enum class Sound {
        /** Start a fresh tone: from silence, or because the timbre changed — a glide cannot. */
        START,

        /** Move the sounding tone to the new pitch, inside the same stream. */
        RETUNE,

        /** Fall silent. */
        STOP,

        /** Nothing audible changes. */
        NONE,
    }

    /** What is sounding, or null when silent. */
    var request: IsonDrone.Request? = null
        private set

    var timbre: ToneTimbre = ToneTimbre.CLEAN
        private set

    private var startedAtMillis = 0L

    /** The pitch [request] sounds: exactly the page's, because it is the page's lookup. */
    val frequencyHz: Double? get() = request?.let { IsonDrone.held(it)?.frequencyHz }

    /** The φθόγγος [request] holds, for the notification to name. */
    val held: Phthong? get() = request?.let { IsonDrone.held(it)?.phthong }

    /**
     * Sounds [request] in [timbre]. Coming out of silence starts the clock for [isTimeUp]; a request
     * that only moves a sounding ison does not.
     */
    fun play(request: IsonDrone.Request, timbre: ToneTimbre, nowMillis: Long): Sound {
        val before = this.request
        val timbreBefore = this.timbre
        this.request = request
        this.timbre = timbre
        return when {
            before == null -> {
                startedAtMillis = nowMillis
                Sound.START
            }
            timbre != timbreBefore -> Sound.START
            request == before -> Sound.NONE
            else -> Sound.RETUNE
        }
    }

    fun stop(): Sound {
        if (request == null) return Sound.NONE
        request = null
        return Sound.STOP
    }

    /**
     * One φθόγγος up ([direction] > 0) or down (< 0) through «Ίσον σε…»'s choices, ordered by pitch.
     * At the top or the bottom it stays where it is. The base is asked for as «no choice», exactly as
     * the page asks for it, so returning to it is the page's base and not a copy of it.
     */
    fun move(direction: Int): Sound {
        val current = request ?: return Sound.NONE
        val ladder = ModeLadders.ladder(current.mode, current.baseShiftMoria)
        val choices = IsonDrone.choices(current.mode, ladder) ?: return Sound.NONE
        val byPitch = choices.all.sortedBy { ladder.stepFor(it)?.moriaFromNi }
        val from = byPitch.indexOf(current.choice ?: choices.base).takeIf { it >= 0 }
            ?: byPitch.indexOf(choices.base)
        val to = byPitch.getOrNull(from + direction.sign) ?: return Sound.NONE
        if (to == byPitch[from]) return Sound.NONE
        request = current.copy(choice = to.takeIf { it != choices.base })
        return Sound.RETUNE
    }

    /** True once the ison has been sounding for the whole allowance. */
    fun isTimeUp(nowMillis: Long): Boolean =
        request != null && nowMillis - startedAtMillis >= maxPlayMillis

    /** How long until [isTimeUp], or null when silent — for scheduling the one check that stops it. */
    fun millisUntilTimeUp(nowMillis: Long): Long? =
        request?.let { (startedAtMillis + maxPlayMillis - nowMillis).coerceAtLeast(0L) }

    /** The headphones came out. */
    fun onBecomingNoisy(): Sound = stop()

    companion object {
        /** An hour: longer than any practice session, short enough not to play all night. */
        const val MAX_PLAY_MILLIS = 60L * 60L * 1000L
    }
}
