package com.johnchourp.learnbyzantinemusic.trainer

import kotlin.math.abs

/** Timing verdict for one target note in the rhythm exercise. */
data class RhythmVerdict(
    val noteIndex: Int,
    val matched: Boolean,
    /** Signed onset error in ms: positive = the singer started late, negative = early. */
    val onsetErrorMillis: Long,
    /** Signed duration error in ms: positive = held too long, negative = too short. */
    val durationErrorMillis: Long
)

/**
 * Streaming evaluator for the rhythm-timing ("άσκηση χρόνου") mode. After the countdown,
 * it is fed one frame per analysis window — `(elapsedMillis since the clock started,
 * voiced)` — and detects sung segments from the voiced↔silent transitions. Each completed
 * segment is assigned to the nearest not-yet-judged scheduled note (by onset time) from the
 * [MelodyPlaybackPlanner] schedule, so a skipped note is simply left un-green rather than
 * dragging every later note out of alignment. A note turns green only when the singer
 * started near its scheduled time and held it for roughly its scheduled duration. Pure
 * logic, fully unit-testable.
 */
class RhythmTimingEvaluator(
    private val plan: List<PlannedNoteEvent>,
    private val onsetToleranceMillis: Long = DEFAULT_ONSET_TOLERANCE_MILLIS,
    private val durationToleranceRatio: Double = DEFAULT_DURATION_TOLERANCE_RATIO,
    private val minDurationToleranceMillis: Long = DEFAULT_MIN_DURATION_TOLERANCE_MILLIS
) {
    private val judged = BooleanArray(plan.size)
    private var judgedCount = 0
    private var voiced = false
    private var segmentStart = -1L

    val isComplete: Boolean get() = judgedCount >= plan.size

    /** Index of the note whose scheduled window contains [elapsedMillis], or -1. */
    fun activeNoteIndex(elapsedMillis: Long): Int =
        plan.indexOfFirst { elapsedMillis >= it.startMillis && elapsedMillis < it.endMillis }

    fun onFrame(elapsedMillis: Long, voicedNow: Boolean): RhythmVerdict? {
        var verdict: RhythmVerdict? = null
        if (voicedNow && !voiced) {
            segmentStart = elapsedMillis
        } else if (!voicedNow && voiced) {
            verdict = closeSegment(elapsedMillis)
        }
        voiced = voicedNow
        return verdict
    }

    /** Closes a still-open sung segment when the exercise ends while voiced. */
    fun finish(elapsedMillis: Long): RhythmVerdict? {
        val verdict = if (voiced) closeSegment(elapsedMillis) else null
        voiced = false
        return verdict
    }

    private fun closeSegment(offsetMillis: Long): RhythmVerdict? {
        val onset = segmentStart
        segmentStart = -1L
        if (onset < 0L) return null

        val target = nearestUnjudged(onset) ?: return null
        judged[target] = true
        judgedCount++

        val note = plan[target]
        val onsetError = onset - note.startMillis
        val durationError = (offsetMillis - onset) - note.durationMillis
        val durationAllowance = maxOf(
            minDurationToleranceMillis,
            (note.durationMillis * durationToleranceRatio).toLong()
        )
        val matched = abs(onsetError) <= onsetToleranceMillis && abs(durationError) <= durationAllowance
        return RhythmVerdict(target, matched, onsetError, durationError)
    }

    private fun nearestUnjudged(onsetMillis: Long): Int? {
        var best = -1
        var bestDistance = Long.MAX_VALUE
        for (index in plan.indices) {
            if (judged[index]) continue
            val distance = abs(onsetMillis - plan[index].startMillis)
            if (distance < bestDistance) {
                bestDistance = distance
                best = index
            }
        }
        return if (best >= 0) best else null
    }

    fun reset() {
        judged.fill(false)
        judgedCount = 0
        voiced = false
        segmentStart = -1L
    }

    companion object {
        const val DEFAULT_ONSET_TOLERANCE_MILLIS = 250L
        const val DEFAULT_DURATION_TOLERANCE_RATIO = 0.5
        const val DEFAULT_MIN_DURATION_TOLERANCE_MILLIS = 150L
        const val COUNTDOWN_SECONDS = 5
    }
}
