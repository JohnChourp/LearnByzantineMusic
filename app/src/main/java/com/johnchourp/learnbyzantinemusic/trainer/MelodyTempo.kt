package com.johnchourp.learnbyzantinemusic.trainer

import kotlin.math.roundToLong

/**
 * Playback tempo expressed in χρόνοι-per-minute (beats per minute). One χρόνος lasts
 * `60000 / bpm` milliseconds, so a higher bpm makes the timer count faster — the speed
 * control the trainer page exposes.
 */
data class MelodyTempo(val beatsPerMinute: Int) {

    val millisPerBeat: Double get() = MILLIS_PER_MINUTE / beatsPerMinute.coerceAtLeast(1)

    /** Milliseconds a span of [beats] χρόνοι lasts at this tempo. */
    fun beatsToMillis(beats: Float): Long = (beats.coerceAtLeast(0f) * millisPerBeat).roundToLong()

    companion object {
        const val MIN_BPM = 30
        const val MAX_BPM = 240
        const val DEFAULT_BPM = 80
        const val MILLIS_PER_MINUTE = 60_000.0

        fun clampBpm(bpm: Int): Int = bpm.coerceIn(MIN_BPM, MAX_BPM)

        fun of(bpm: Int): MelodyTempo = MelodyTempo(clampBpm(bpm))
    }
}
