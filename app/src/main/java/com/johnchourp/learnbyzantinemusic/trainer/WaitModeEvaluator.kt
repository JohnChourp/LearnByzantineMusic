package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.modes.LadderPitchMirror
import com.johnchourp.learnbyzantinemusic.music.ByzantineTuning
import com.johnchourp.learnbyzantinemusic.music.IntonationProfile
import com.johnchourp.learnbyzantinemusic.music.ModeLadder
import kotlin.math.roundToInt

/**
 * A sung pitch read against the φθόγγος the line waits on (ClickUp `869f5x2cd`, J1).
 *
 * [offsetMoria] is how far the voice is from [target], after moving it by whole octaves as close to
 * the target as it goes — men sing an octave below the middle rung, and a Δι is a Δι in any octave.
 * [nearest] is the rung of the ladder that folded pitch is closest to, by [LadderPitchMirror]: the
 * same nearest-rung-first reading as «Πού είμαι», so a voice nearer the neighbour is on the neighbour.
 */
data class TargetReading(
    val target: ModeLadder.Step,
    val nearest: ModeLadder.Step,
    /** Signed μόρια from [target]: positive = sharp (sing lower), negative = flat (sing higher). */
    val offsetMoria: Double,
) {
    /** On the target: it is the nearest rung, and within [toleranceMoria] of it by the profile's rule. */
    fun isOnTarget(toleranceMoria: Double): Boolean =
        nearest == target && IntonationProfile.isInTune(offsetMoria, toleranceMoria)

    companion object {
        /**
         * [frequencyHz] read against [target] on [ladder], or null when it is not a usable pitch. The
         * fold is a whole number of octaves, so the φθόγγος is never changed by it — only its octave.
         */
        fun of(target: ModeLadder.Step, ladder: ModeLadder, frequencyHz: Double): TargetReading? {
            if (frequencyHz <= 0.0 || !frequencyHz.isFinite()) return null
            val octave = ByzantineTuning.MORIA_PER_OCTAVE.toDouble()
            val targetMoria = target.moriaFromNi.value.toDouble()
            val away = ByzantineTuning.moriaFromNi(frequencyHz) - targetMoria
            val offset = away - octave * (away / octave).roundToInt()
            val reading = LadderPitchMirror.read(ladder, ByzantineTuning.frequencyHz(targetMoria + offset)) ?: return null
            return TargetReading(target, reading.step, offset)
        }
    }
}

/** What one frame of the microphone did to the waiting line. */
data class WaitFrame(
    /** The note the line was waiting on when the frame came, or null once the run is over. */
    val targetIndex: Int?,
    /** The voice against that note, or null in silence. */
    val reading: TargetReading?,
    /** The voice is on the note, within the tolerance. */
    val onTarget: Boolean,
    /** How long it has been held on the note so far, in capture time; 0 when it is not on it. */
    val heldMillis: Long,
    /** This frame completed the hold: the line moved on. */
    val advanced: Boolean,
)

/**
 * «Παραλλαγή με αναμονή» (ClickUp `869f5x2cd`, J1): the line moves on **only** when the φθόγγος it
 * waits on is held within the tolerance for [holdMillis] — never by itself, never on a wrong note or
 * on silence. [skip] is the one other way forward, and only an explicit tap calls it.
 *
 * This is a new evaluator, not a flag on the voice check's (`PitchGreeningEvaluator`), which moves on
 * whether the voice was right or wrong: the two answer different questions, and the voice check keeps
 * its behaviour and its tests.
 *
 * - **Where it is:** a [PracticeCursor], the one J2's «Ψάλλε μαζί» moves by the timeline; here only a
 *   held note or a skip moves it. One pass over the line is the run; [isComplete] after its last note.
 * - **Matching:** each frame is read against the target with [TargetReading.of] — octave-folded, then
 *   nearest rung first on the scale's own ladder, mode, «Μεταφορά βάσης» and the voice's global shift
 *   included — and judged by [IntonationProfile.isInTune] at [toleranceMoria].
 * - **Time** is the pitch engine's capture time, in milliseconds, never a count of frames: a hold is
 *   the time from its first frame on the note to the current one, so a slower or faster frame rate
 *   holds for the same 300 ms. Any frame off the note, or silent, starts the hold again.
 * - **Time to lock** of a note runs from the first frame the line waited on it to the frame that
 *   completed its hold; [result] hands them to [WaitModeScore].
 *
 * Pure Kotlin: every rule is tested without a microphone.
 */
class WaitModeEvaluator(
    private val targets: List<ModeLadder.Step>,
    private val ladder: ModeLadder,
    val toleranceMoria: Double,
    private val holdMillis: Long = IntonationProfile.WAIT_HOLD_MS.roundToInt().toLong(),
) {
    var cursor: PracticeCursor = PracticeCursor.start(targets.size)
        private set

    /** The run is over: every note was held or skipped. An empty line has nothing to wait on. */
    val isComplete: Boolean get() = targets.isEmpty() || cursor.round > 0

    /** The note the line waits on, or null once the run is over. */
    val currentIndex: Int? get() = if (isComplete) null else cursor.current

    var skips: Int = 0
        private set

    private var waitingSinceMillis: Long? = null
    private var holdStartMillis: Long? = null
    private val lockMillis = mutableListOf<Long>()

    fun onFrame(frequencyHz: Double?, capturedAtMillis: Long): WaitFrame {
        val index = currentIndex ?: return WaitFrame(null, null, onTarget = false, heldMillis = 0, advanced = false)
        val since = waitingSinceMillis ?: capturedAtMillis.also { waitingSinceMillis = it }
        val reading = frequencyHz?.let { TargetReading.of(targets[index], ladder, it) }
        if (reading == null || !reading.isOnTarget(toleranceMoria)) {
            holdStartMillis = null
            return WaitFrame(index, reading, onTarget = false, heldMillis = 0, advanced = false)
        }
        val start = holdStartMillis ?: capturedAtMillis.also { holdStartMillis = it }
        val held = capturedAtMillis - start
        if (held < holdMillis) {
            return WaitFrame(index, reading, onTarget = true, heldMillis = held, advanced = false)
        }
        lockMillis += capturedAtMillis - since
        moveOn()
        return WaitFrame(index, reading, onTarget = true, heldMillis = held, advanced = true)
    }

    /** «Παράλειψη»: the one way on without the voice, and only when the learner taps it. */
    fun skip(): Boolean {
        if (isComplete) return false
        skips++
        moveOn()
        return true
    }

    fun result(): WaitModeScore.Run = WaitModeScore.Run(noteCount = targets.size, skips = skips, lockMillis = lockMillis.toList())

    private fun moveOn() {
        cursor = cursor.advance()
        waitingSinceMillis = null
        holdStartMillis = null
    }
}

/**
 * The stars at the end of a run of «Παραλλαγή με αναμονή» (ClickUp `869f5x2cd`, J1): from how many
 * notes were skipped and how quickly the others were found — nothing else.
 *
 * | | Stars |
 * |---|---|
 * | every note skipped: nothing was sung | 0 |
 * | otherwise start at | 3 |
 * | at least one skip | −1 |
 * | more skips than a quarter of the notes | −1 more |
 * | the median time to lock over [SLOW_MEDIAN_MS] | −1 |
 * | and never under | 1 |
 *
 * The median, not the mean: one note found after a long search does not spoil a run that was quick
 * everywhere else. A note's time to lock includes the hold itself, so it is never under 300 ms.
 */
object WaitModeScore {

    /** A median time to lock above this is slow: 3 seconds of searching per φθόγγος. */
    const val SLOW_MEDIAN_MS = 3_000L

    data class Run(val noteCount: Int, val skips: Int, val lockMillis: List<Long>)

    /** The median time to lock of the notes that were held, or null when none was. */
    fun medianLockMillis(lockMillis: List<Long>): Long? {
        if (lockMillis.isEmpty()) return null
        val sorted = lockMillis.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
    }

    fun stars(run: Run): Int {
        if (run.lockMillis.isEmpty()) return 0
        var stars = 3
        if (run.skips > 0) stars--
        if (run.skips * 4 > run.noteCount) stars--
        val median = medianLockMillis(run.lockMillis)
        if (median != null && median > SLOW_MEDIAN_MS) stars--
        return stars.coerceAtLeast(1)
    }
}
