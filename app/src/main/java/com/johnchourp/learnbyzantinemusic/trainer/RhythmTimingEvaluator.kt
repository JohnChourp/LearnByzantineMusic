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
 * voiced)` — and detects sung segments from the voiced↔silent transitions.
 *
 * Each segment is assigned to a scheduled note from the [MelodyPlaybackPlanner] timeline.
 * Crucially, while the singer keeps voicing across a scheduled note boundary the current
 * segment is closed at that boundary and a new one is opened for the next note, so a normal
 * legato run (no silence between phthongi) still advances and greens note-by-note instead
 * of collapsing into one long segment. A note turns green only when its onset is within
 * tolerance of the scheduled start and its held duration is within tolerance of the
 * scheduled duration. A skipped (never voiced) note is simply left un-green. Pure logic,
 * fully unit-testable.
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
    private var segmentNote = -1

    val isComplete: Boolean get() = judgedCount >= plan.size

    /** Index of the note whose scheduled window contains [elapsedMillis], or -1. */
    fun activeNoteIndex(elapsedMillis: Long): Int =
        plan.indexOfFirst { elapsedMillis >= it.startMillis && elapsedMillis < it.endMillis }

    fun onFrame(elapsedMillis: Long, voicedNow: Boolean): RhythmVerdict? {
        var verdict: RhythmVerdict? = null
        if (voicedNow && !voiced) {
            startSegment(elapsedMillis)
        } else if (!voicedNow && voiced) {
            verdict = finalizeSegment(segmentStart, elapsedMillis, segmentNote)
            clearSegment()
        } else if (voicedNow && voiced && segmentNote >= 0) {
            // Still singing: if we have crossed past the current note's scheduled window,
            // close it at the boundary and continue the legato line into the next note.
            val note = plan[segmentNote]
            if (elapsedMillis >= note.endMillis) {
                verdict = finalizeSegment(segmentStart, note.endMillis, segmentNote)
                startSegment(note.endMillis)
            }
        }
        voiced = voicedNow
        return verdict
    }

    /** Closes a still-open sung segment when the exercise ends while voiced. */
    fun finish(elapsedMillis: Long): RhythmVerdict? {
        val verdict = if (voiced) finalizeSegment(segmentStart, elapsedMillis, segmentNote) else null
        clearSegment()
        voiced = false
        return verdict
    }

    private fun startSegment(onsetMillis: Long) {
        segmentStart = onsetMillis
        segmentNote = chooseNote(onsetMillis)
    }

    private fun clearSegment() {
        segmentStart = -1L
        segmentNote = -1
    }

    /** The note this onset belongs to: the one whose window contains it, else nearest. */
    private fun chooseNote(onsetMillis: Long): Int {
        val active = activeNoteIndex(onsetMillis)
        if (active >= 0 && !judged[active]) return active
        return nearestUnjudged(onsetMillis)
    }

    private fun finalizeSegment(onsetMillis: Long, offsetMillis: Long, noteIndex: Int): RhythmVerdict? {
        if (noteIndex < 0 || onsetMillis < 0L || judged[noteIndex]) return null
        judged[noteIndex] = true
        judgedCount++

        val note = plan[noteIndex]
        val onsetError = onsetMillis - note.startMillis
        val durationError = (offsetMillis - onsetMillis) - note.durationMillis
        val durationAllowance = maxOf(
            minDurationToleranceMillis,
            (note.durationMillis * durationToleranceRatio).toLong()
        )
        val matched = abs(onsetError) <= onsetToleranceMillis && abs(durationError) <= durationAllowance
        return RhythmVerdict(noteIndex, matched, onsetError, durationError)
    }

    private fun nearestUnjudged(onsetMillis: Long): Int {
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
        return best
    }

    fun reset() {
        judged.fill(false)
        judgedCount = 0
        voiced = false
        clearSegment()
    }

    companion object {
        const val DEFAULT_ONSET_TOLERANCE_MILLIS = 250L
        const val DEFAULT_DURATION_TOLERANCE_RATIO = 0.5
        const val DEFAULT_MIN_DURATION_TOLERANCE_MILLIS = 150L
        const val COUNTDOWN_SECONDS = 5
    }
}
