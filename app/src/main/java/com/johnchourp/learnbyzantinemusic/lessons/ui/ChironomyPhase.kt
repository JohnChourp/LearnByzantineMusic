package com.johnchourp.learnbyzantinemusic.lessons.ui

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * The moving hand of the metronome — χειρονομία (ClickUp `869f5x2d7`).
 *
 * Time is learned with the body: the hand falls on the θέση and rises through the άρσεις. Here
 * the hand draws **one circle per grouping**: at its lowest point on every θέση, and passing the
 * άρσεις on the way round — so a δίσημος is down-up, and a τετράσημος goes round in four χρόνοι.
 *
 * The phase comes from the same [MetronomeSchedule] interval as the click, measured from the same
 * start ([MetronomeRun.handPhase]), so the hand reaches the bottom on the θέση itself, not near it.
 * Pure Kotlin, no Android, so that property is tested.
 */
object ChironomyPhase {

    /**
     * Where the hand is in its circle, `0 ≤ phase < 1`, [elapsedMillis] after the run started:
     * 0 on every θέση, `k / beats` on the k-th χρόνος after it.
     */
    fun of(elapsedMillis: Double, bpm: Int, beats: Int): Double {
        if (beats <= 0 || elapsedMillis <= 0.0) return 0.0
        val cycles = elapsedMillis / (MetronomeSchedule.intervalMillis(bpm) * beats)
        return cycles - floor(cycles)
    }

    /**
     * The hand's point on a circle of radius 1 around the origin, in screen orientation (y grows
     * DOWNWARDS): phase 0 is `(0, 1)`, the lowest point.
     */
    fun position(phase: Double): Pair<Double, Double> {
        val angle = 2 * PI * phase
        return sin(angle) to cos(angle)
    }
}
