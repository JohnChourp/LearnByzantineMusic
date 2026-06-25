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
 * Streaming evaluator for the rhythm exercise. After the countdown it is fed one frame per
 * analysis window — `(elapsedMillis, voiced, phthong)` — and detects sung segments.
 *
 * In the **time-only** mode ([requirePitch] = false) a segment is a run of voicing; the
 * detected phthong is ignored and a note greens purely on timing.
 *
 * In the **combined phthong+time** mode ([requirePitch] = true) a segment is a run of the
 * *same in-tune phthong* (the caller passes the phthong only while it is in tune, else null,
 * which reads as silence). A phthong change while voiced therefore starts a new segment, so
 * an early pitch change is captured as an early onset; and a note greens only when the
 * segment's phthong matches the scheduled note AND the timing is right.
 *
 * Each segment is assigned to a scheduled note from the [MelodyPlaybackPlanner] timeline. A
 * legato run is split at scheduled note boundaries (so repeated phthongi advance), except the
 * final note is left open until the real release or the safety cutoff, so an over-held last
 * note is judged on its true offset. Pure logic, fully unit-testable.
 */
class RhythmTimingEvaluator(
    private val plan: List<PlannedNoteEvent>,
    private val requirePitch: Boolean = false,
    private val onsetToleranceMillis: Long = DEFAULT_ONSET_TOLERANCE_MILLIS,
    private val durationToleranceRatio: Double = DEFAULT_DURATION_TOLERANCE_RATIO,
    private val minDurationToleranceMillis: Long = DEFAULT_MIN_DURATION_TOLERANCE_MILLIS
) {
    private val judged = BooleanArray(plan.size)
    private var judgedCount = 0
    private var singing = false
    private var segmentStart = -1L
    private var segmentNote = -1
    private var segmentPhthong: TrainerPhthong? = null

    val isComplete: Boolean get() = judgedCount >= plan.size

    /** Index of the note whose scheduled window contains [elapsedMillis], or -1. */
    fun activeNoteIndex(elapsedMillis: Long): Int =
        plan.indexOfFirst { elapsedMillis >= it.startMillis && elapsedMillis < it.endMillis }

    /**
     * One analysis frame. [phthong] is the in-tune detected phthong (null = silent/off-tune);
     * it is only used when [requirePitch] is true.
     */
    fun onFrame(elapsedMillis: Long, voicedNow: Boolean, phthong: TrainerPhthong? = null): RhythmVerdict? {
        val singingNow = if (requirePitch) voicedNow && phthong != null else voicedNow
        var verdict: RhythmVerdict? = null

        if (singingNow && !singing) {
            startSegment(elapsedMillis, phthong)
        } else if (!singingNow && singing) {
            verdict = finalizeSegment(segmentStart, elapsedMillis, segmentNote, segmentPhthong)
            clearSegment()
        } else if (singingNow && singing && segmentNote >= 0) {
            if (requirePitch && phthong != segmentPhthong) {
                // Pitch changed mid-phrase → the previous phthong's attempt ends here and a new
                // one begins, so an early entrance onto the next note is captured as an early onset.
                verdict = finalizeSegment(segmentStart, elapsedMillis, segmentNote, segmentPhthong)
                startSegment(elapsedMillis, phthong)
            } else {
                // Same phthong (or time-only): split at the scheduled boundary so repeated notes
                // advance — but leave the final note open until the real release / safety cutoff.
                val note = plan[segmentNote]
                if (elapsedMillis >= note.endMillis && segmentNote < plan.lastIndex) {
                    verdict = finalizeSegment(segmentStart, note.endMillis, segmentNote, segmentPhthong)
                    startSegment(note.endMillis, phthong)
                }
            }
        }
        singing = singingNow
        return verdict
    }

    /**
     * Closes a still-open segment when the exercise ends while voiced. Reaching here while
     * singing means the safety cutoff fired before the singer released the note, so an
     * indefinitely held note is forced to *not* match — otherwise a long note's generous
     * duration tolerance could let an unreleased note count as correctly timed.
     */
    fun finish(elapsedMillis: Long): RhythmVerdict? {
        val verdict = if (singing) {
            finalizeSegment(segmentStart, elapsedMillis, segmentNote, segmentPhthong)?.copy(matched = false)
        } else {
            null
        }
        clearSegment()
        singing = false
        return verdict
    }

    private fun startSegment(onsetMillis: Long, phthong: TrainerPhthong?) {
        segmentStart = onsetMillis
        segmentNote = chooseNote(onsetMillis)
        segmentPhthong = phthong
    }

    private fun clearSegment() {
        segmentStart = -1L
        segmentNote = -1
        segmentPhthong = null
    }

    /**
     * The note this sung onset belongs to:
     *  1. the NEAREST not-yet-judged note whose scheduled start is within onset tolerance; then
     *  2. otherwise (not near any start: late/sloppy) the earliest already-started one — so a
     *     late onset fails its OWN note instead of consuming a future one; then
     *  3. otherwise (the onset precedes every remaining note) the earliest not-yet-judged note.
     */
    private fun chooseNote(onsetMillis: Long): Int {
        var nearest = -1
        var nearestDistance = Long.MAX_VALUE
        for (index in plan.indices) {
            if (judged[index]) continue
            val distance = abs(onsetMillis - plan[index].startMillis)
            if (distance <= onsetToleranceMillis && distance < nearestDistance) {
                nearestDistance = distance
                nearest = index
            }
        }
        if (nearest >= 0) return nearest
        for (index in plan.indices) {
            if (!judged[index] && plan[index].startMillis <= onsetMillis) return index
        }
        for (index in plan.indices) {
            if (!judged[index]) return index
        }
        return -1
    }

    private fun finalizeSegment(
        onsetMillis: Long,
        offsetMillis: Long,
        noteIndex: Int,
        phthong: TrainerPhthong?
    ): RhythmVerdict? {
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
        val timingOk = abs(onsetError) <= onsetToleranceMillis && abs(durationError) <= durationAllowance
        val pitchOk = !requirePitch || phthong == note.phthong
        return RhythmVerdict(noteIndex, timingOk && pitchOk, onsetError, durationError)
    }

    fun reset() {
        judged.fill(false)
        judgedCount = 0
        singing = false
        clearSegment()
    }

    companion object {
        const val DEFAULT_ONSET_TOLERANCE_MILLIS = 250L
        const val DEFAULT_DURATION_TOLERANCE_RATIO = 0.5
        const val DEFAULT_MIN_DURATION_TOLERANCE_MILLIS = 150L
        const val COUNTDOWN_SECONDS = 5
    }
}
