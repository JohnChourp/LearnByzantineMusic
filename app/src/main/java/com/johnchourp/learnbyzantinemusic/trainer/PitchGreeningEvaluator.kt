package com.johnchourp.learnbyzantinemusic.trainer

import kotlin.math.abs

/** Verdict for one target note once the singer has produced a stable phthong for it. */
data class GreeningResult(
    val targetIndex: Int,
    val matched: Boolean,
    val sungPhthong: TrainerPhthong
)

/**
 * Streaming evaluator for the voice-following ("πρασίνισμα") mode. It is fed one detected
 * [PitchMatch] per analysis frame (null = silence/no pitch) and segments the stream into
 * sung notes: a phthong held for [minStableFrames] consecutive frames commits the current
 * target. The target turns green only when the sung phthong equals the expected one within
 * [toleranceMoria]; either way the evaluator advances to the next target, matching the
 * "right or wrong, move to the next" behaviour from the feature request.
 *
 * Pure logic with no Android dependency, so it is fully unit-testable.
 */
class PitchGreeningEvaluator(
    private val targets: List<TrainerPhthong>,
    val toleranceMoria: Double = DEFAULT_TOLERANCE_MORIA,
    private val minStableFrames: Int = DEFAULT_MIN_STABLE_FRAMES
) {
    private var index = 0
    private var candidate: TrainerPhthong? = null
    private var stableFrames = 0
    private var segmentCommitted = false

    val currentTargetIndex: Int get() = index
    val isComplete: Boolean get() = index >= targets.size

    /** Expected phthong the singer should produce next, or null once complete. */
    fun currentTarget(): TrainerPhthong? = targets.getOrNull(index)

    /**
     * Feeds one analysis frame. Returns a [GreeningResult] on the frame that commits a
     * target (so the caller can colour that row and advance), otherwise null.
     */
    fun onFrame(match: PitchMatch?): GreeningResult? {
        if (isComplete) return null

        val phthong = match?.phthong
        if (phthong == null) {
            // Silence ends the current sung segment; the next pitch starts a fresh one.
            resetSegment()
            return null
        }

        if (phthong != candidate) {
            candidate = phthong
            stableFrames = 1
            segmentCommitted = false
            return null
        }

        stableFrames++
        if (segmentCommitted || stableFrames < minStableFrames) {
            return null
        }

        val target = targets[index]
        val matched = phthong == target && abs(match.deviationMoria) <= toleranceMoria
        val result = GreeningResult(targetIndex = index, matched = matched, sungPhthong = phthong)
        index++
        // If the next target is the same phthong, let a continuously-held note commit it too
        // after another stable window; otherwise wait for the pitch to change so a sustained
        // note is not charged against a different upcoming target.
        if (!isComplete && targets[index] == phthong) {
            stableFrames = 0
            segmentCommitted = false
        } else {
            segmentCommitted = true
        }
        return result
    }

    private fun resetSegment() {
        candidate = null
        stableFrames = 0
        segmentCommitted = false
    }

    fun reset() {
        index = 0
        resetSegment()
    }

    companion object {
        /** ±4 moria ≈ ±67 cents — a forgiving but meaningful intonation window. */
        const val DEFAULT_TOLERANCE_MORIA = 4.0
        const val DEFAULT_MIN_STABLE_FRAMES = 3
    }
}
