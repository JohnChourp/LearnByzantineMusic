package com.johnchourp.learnbyzantinemusic.recordings.player

/**
 * An A-B loop: the passage the in-app player repeats until the learner has it (ClickUp `869f5x268`).
 *
 * Kept for the screen's session only — never stored. A loop is valid when A comes before B, lasts at
 * least [MIN_LENGTH_MS] (so a double tap does not make a stutter), and lies within the recording;
 * marks outside it are clamped to it. Once playback reaches B it goes back to A ([wrap]).
 */
data class LoopRegion(val startMs: Int, val endMs: Int) {

    /** Where playback continues from [positionMs]: A once it has reached B, otherwise nowhere new. */
    fun wrap(positionMs: Int): Int? = if (positionMs >= endMs) startMs else null

    companion object {
        const val MIN_LENGTH_MS = 500

        /** The loop from A to B in a recording of [durationMs], or null when those marks make none. */
        fun of(startMs: Int, endMs: Int, durationMs: Int): LoopRegion? {
            val duration = durationMs.coerceAtLeast(0)
            val a = startMs.coerceIn(0, duration)
            val b = endMs.coerceIn(0, duration)
            return if (b - a >= MIN_LENGTH_MS) LoopRegion(a, b) else null
        }
    }
}

/** What marking a loop point did, for the message the screen shows. */
enum class LoopMarkResult {
    SET,

    /** B before any A. */
    NEEDS_START,

    /** B at or before A. */
    BEFORE_START,

    /** A and B closer than [LoopRegion.MIN_LENGTH_MS]. */
    TOO_SHORT,

    /** Nothing is loaded that could be looped. */
    UNAVAILABLE,
}
