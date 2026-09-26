package com.johnchourp.learnbyzantinemusic.music

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * An exact length of time in χρόνοι (beats), for the time rules of [ByzantineRhythmMapper]
 * (ClickUp `869f5x29r`, H5).
 *
 * The time signs cut a χρόνος into halves (γοργόν), thirds (δίγοργον), quarters (τρίγοργον) and
 * three-quarters (παρεστιγμένο γοργόν). A `Float` holds ½, ¼ and ¾ exactly but not ⅓, so three
 * thirds added up in floating point are not always one χρόνος, and a melody's total drifts away
 * from the sum of its notes. A length is therefore a whole number of **ticks**, [TICKS_PER_BEAT]
 * to the χρόνος — 12 is the smallest number that ½, ⅓ and ¼ all divide — and every sum, share and
 * comparison is integer arithmetic. Only the edges convert: to milliseconds for playback
 * (`MelodyTempo`), to text for display.
 */
@JvmInline
value class Beats(val ticks: Int) : Comparable<Beats> {

    operator fun plus(other: Beats): Beats = Beats(ticks + other.ticks)

    operator fun minus(other: Beats): Beats = Beats(ticks - other.ticks)

    operator fun times(times: Int): Beats = Beats(ticks * times)

    override fun compareTo(other: Beats): Int = ticks.compareTo(other.ticks)

    /**
     * Milliseconds this length lasts at [beatsPerMinute] χρόνοι per minute, rounded once from the exact
     * length. A player places every note at the exact offset from its start converted this way, so
     * rounding never adds up from note to note.
     */
    fun millisAt(beatsPerMinute: Int): Long =
        (ticks.coerceAtLeast(0) * (MILLIS_PER_MINUTE / beatsPerMinute.coerceAtLeast(1)) / TICKS_PER_BEAT).roundToLong()

    /** «2», «1/3», «3/2» — for test failures and logs; screens print lengths their own way. */
    override fun toString(): String {
        val divisor = greatestCommonDivisor(ticks, TICKS_PER_BEAT)
        val numerator = ticks / divisor
        val denominator = TICKS_PER_BEAT / divisor
        return if (denominator == 1) "$numerator" else "$numerator/$denominator"
    }

    companion object {
        /** Ticks in one χρόνος: the smallest count that ½, ⅓ and ¼ of a χρόνος all divide. */
        const val TICKS_PER_BEAT = 12

        val ZERO = Beats(0)
        val ONE = Beats(TICKS_PER_BEAT)

        private const val MILLIS_PER_MINUTE = 60_000.0

        /** [count] whole χρόνοι. */
        fun whole(count: Int): Beats = Beats(count * TICKS_PER_BEAT)

        /** [numerator]/[denominator] of a χρόνος; the denominator must divide [TICKS_PER_BEAT]. */
        fun of(numerator: Int, denominator: Int): Beats {
            require(denominator > 0 && TICKS_PER_BEAT % denominator == 0) {
                "1/$denominator of a χρόνος is not a whole number of ticks"
            }
            return Beats(numerator * (TICKS_PER_BEAT / denominator))
        }

        /**
         * The length nearest to [beats] χρόνοι. Exact for what the Melody Trainer lets you write —
         * whole and half χρόνοι — which is the only place a length still arrives as a number.
         */
        fun nearest(beats: Float): Beats = Beats((beats * TICKS_PER_BEAT).roundToInt())

        private tailrec fun greatestCommonDivisor(a: Int, b: Int): Int =
            if (b == 0) maxOf(1, abs(a)) else greatestCommonDivisor(b, a % b)
    }
}
