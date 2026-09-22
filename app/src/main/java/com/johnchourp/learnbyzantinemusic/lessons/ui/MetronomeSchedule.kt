package com.johnchourp.learnbyzantinemusic.lessons.ui

import kotlin.math.roundToLong

/**
 * Beat timing for the «Δίσημος / Τρίσημος / Τετράσημος» metronome (ClickUp `869f4tpg0`).
 *
 * ## Why this exists rather than a `delay(interval)` loop
 *
 * The obvious implementation — `while (playing) { delay(interval); beat++ }` — **accumulates drift**.
 * Each iteration rounds the interval to whole milliseconds and then waits *at least* that long, so
 * every beat inherits the error of every beat before it. At 110 bpm the interval is 545.4545…ms; a
 * loop delaying 545 ms loses ~0.45 ms per beat, which is about **50 ms every minute** — and that is
 * before scheduler jitter, which only ever adds.
 *
 * The audible click and the visual pulse would then also drift apart from each other, which is the
 * one thing a metronome must never do: a learner who sees the flash and hears the click at different
 * moments cannot use either.
 *
 * So every beat is placed at an **absolute** offset from the moment playback started. Rounding error
 * stays bounded at half a millisecond per beat and never compounds, and a late wake-up is corrected
 * by the next beat instead of being carried forward.
 *
 * Pure Kotlin, no Android, so the drift property is provable in a unit test.
 */
object MetronomeSchedule {

    /** Tempo range the page offers, in χρόνοι per minute. */
    const val MIN_BPM = 40
    const val MAX_BPM = 160
    const val DEFAULT_BPM = 80

    private const val MILLIS_PER_MINUTE = 60_000.0

    fun clampBpm(bpm: Int): Int = bpm.coerceIn(MIN_BPM, MAX_BPM)

    /** Exact milliseconds between beats. Deliberately fractional — see the class header. */
    fun intervalMillis(bpm: Int): Double = MILLIS_PER_MINUTE / clampBpm(bpm)

    /**
     * Absolute time of beat [index] (0-based), measured from [startMillis]. Computed from the index,
     * never by adding an interval to the previous beat, which is what stops error compounding.
     */
    fun beatTimeMillis(startMillis: Long, bpm: Int, index: Int): Long =
        startMillis + (index.coerceAtLeast(0) * intervalMillis(bpm)).roundToLong()

    /**
     * How long to wait from [nowMillis] until beat [index]. Never negative: a beat already due fires
     * immediately rather than the caller sleeping backwards.
     */
    fun delayUntilMillis(startMillis: Long, bpm: Int, index: Int, nowMillis: Long): Long =
        (beatTimeMillis(startMillis, bpm, index) - nowMillis).coerceAtLeast(0L)

    /**
     * 1-based beat inside a grouping of [beats] χρόνοι, for a 0-based absolute beat index. Derived
     * from the index rather than incremented, so a dropped frame cannot leave the highlight on the
     * wrong beat forever.
     */
    fun beatInGrouping(index: Int, beats: Int): Int {
        if (beats <= 0) return 1
        return (index.coerceAtLeast(0) % beats) + 1
    }

    /** True when [index] is the downbeat, which the click accents. */
    fun isDownbeat(index: Int, beats: Int): Boolean = beatInGrouping(index, beats) == 1
}
