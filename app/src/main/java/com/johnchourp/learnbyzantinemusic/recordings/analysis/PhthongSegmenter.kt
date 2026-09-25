package com.johnchourp.learnbyzantinemusic.recordings.analysis

import com.johnchourp.learnbyzantinemusic.music.IntonationProfile
import com.johnchourp.learnbyzantinemusic.trainer.TrainerPhthong
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.pow

/**
 * A note the singer held: a scale degree ([octave] × 7 + phthong index, octave 0 holding the
 * starting phthong's Νη) over a time span, with its mean deviation from the scale in moria
 * (positive = sharp).
 */
data class SungNote(
    val phthong: TrainerPhthong,
    val octave: Int,
    val startMs: Long,
    val endMs: Long,
    val deviationMoria: Double,
    val moria: Double,
) {
    val degree: Int get() = octave * 7 + phthong.ordinal

    /**
     * Green or orange in the analysis — its summary, its chips and its diagram — by the same rule
     * the Trainer and «Πού είμαι» use: [IntonationProfile.isInTune]. Until ClickUp `869f5x28t` the
     * analysis allowed ±4 μόρια while «Πού είμαι» allowed ±3.
     */
    val isInTune: Boolean get() = IntonationProfile.isInTune(deviationMoria)
}

/**
 * Turns a [PitchTrack] into the notes that were sung, on the scale of a mode.
 *
 * The recording has no absolute reference, so it is calibrated to the singer: the first steady
 * voiced stretch is taken to be [TrainerPhthong] the user says the melody starts on, and every
 * other pitch is placed on the mode's scale ([ModeScalePositions]) relative to it.
 *
 * A note is a run of frames on the same degree lasting at least [MIN_NOTE_FRAMES]. Shorter runs
 * (glides, consonants, detection blips) are dropped; a gap of at most [MAX_GAP_FRAMES] inside a
 * note is bridged, while a longer silence ends it — so a repeated note counts twice only when the
 * singer separates the repetitions audibly.
 *
 * Those thresholds are durations in [IntonationProfile], counted here in frames that start
 * [IntonationProfile.OFFLINE_HOP_MS] apart, the hop of the [PitchTrackAnalyzer] every track comes from.
 */
object PhthongSegmenter {
    /** [IntonationProfile.OFFLINE_MIN_NOTE_MS] in recording frames: 4. */
    val MIN_NOTE_FRAMES: Int = offlineFrames(IntonationProfile.OFFLINE_MIN_NOTE_MS)

    /** [IntonationProfile.OFFLINE_MAX_GAP_MS] in recording frames: 2. */
    val MAX_GAP_FRAMES: Int = offlineFrames(IntonationProfile.OFFLINE_MAX_GAP_MS)

    /** [IntonationProfile.OFFLINE_STEADY_MS] in recording frames: 5. */
    internal val STEADY_FRAMES: Int = offlineFrames(IntonationProfile.OFFLINE_STEADY_MS)
    private const val STEADY_SPREAD_MORIA = 3.0
    private const val OCTAVE = ModeScalePositions.MORIA_PER_OCTAVE.toDouble()

    /** Frequency of the starting phthong's Νη such that the first steady pitch is [startPhthong]. */
    fun calibrate(track: PitchTrack, positions: IntArray, startPhthong: TrainerPhthong): Double? {
        val voiced = track.frames.map { it.frequencyHz }
        var run = ArrayList<Double>()
        for (hz in voiced) {
            if (hz <= 0f) {
                run = ArrayList()
                continue
            }
            val moria = OCTAVE * log2(hz.toDouble())
            if (run.isNotEmpty() && abs(moria - median(run)) > STEADY_SPREAD_MORIA) run = ArrayList()
            run += moria
            if (run.size >= STEADY_FRAMES) return niFrequency(2.0.pow(median(run) / OCTAVE), positions, startPhthong)
        }
        // No steady stretch: fall back to the median of the first voiced frames.
        val firstVoiced = voiced.filter { it > 0f }.take(STEADY_FRAMES).map { OCTAVE * log2(it.toDouble()) }
        if (firstVoiced.isEmpty()) return null
        return niFrequency(2.0.pow(median(firstVoiced) / OCTAVE), positions, startPhthong)
    }

    private fun niFrequency(startHz: Double, positions: IntArray, startPhthong: TrainerPhthong): Double =
        startHz / 2.0.pow(positions[startPhthong.ordinal] / OCTAVE)

    /** Nearest scale degree to [moria] above the calibrated Νη, with the signed deviation. */
    fun nearestDegree(moria: Double, positions: IntArray): Pair<Int, Double> {
        val octave = floor(moria / OCTAVE).toInt()
        val within = moria - octave * OCTAVE
        var bestIndex = 0
        var bestDeviation = Double.MAX_VALUE
        for (index in 0..7) {
            // index 7 is the next octave's Νη, so pitches just below Νη΄ resolve upwards.
            val position = if (index == 7) OCTAVE else positions[index].toDouble()
            val deviation = within - position
            if (abs(deviation) < abs(bestDeviation)) {
                bestDeviation = deviation
                bestIndex = index
            }
        }
        val degree = if (bestIndex == 7) (octave + 1) * 7 else octave * 7 + bestIndex
        return degree to bestDeviation
    }

    fun segment(track: PitchTrack, niHz: Double, positions: IntArray): List<SungNote> {
        val frames = track.frames
        if (frames.isEmpty() || niHz <= 0.0) return emptyList()
        val moria = smoothedMoria(frames, niHz)
        val degrees = IntArray(frames.size) { index ->
            val value = moria[index]
            if (value.isNaN()) NO_DEGREE else nearestDegree(value, positions).first
        }

        // Runs of equal degree (NO_DEGREE runs are gaps).
        data class Run(val degree: Int, val start: Int, var end: Int)
        val runs = ArrayList<Run>()
        for (index in degrees.indices) {
            val last = runs.lastOrNull()
            if (last != null && last.degree == degrees[index]) last.end = index else runs += Run(degrees[index], index, index)
        }
        // Too-short voiced runs are noise: treat them as gaps.
        val cleaned = runs.map { run ->
            if (run.degree != NO_DEGREE && run.end - run.start + 1 < MIN_NOTE_FRAMES) run.copy(degree = NO_DEGREE) else run
        }
        // Merge: same degree separated only by a short gap is one note.
        val notes = ArrayList<Run>()
        var pendingGap = 0
        for (run in cleaned) {
            if (run.degree == NO_DEGREE) {
                pendingGap += run.end - run.start + 1
                continue
            }
            val last = notes.lastOrNull()
            if (last != null && last.degree == run.degree && pendingGap <= MAX_GAP_FRAMES) {
                last.end = run.end
            } else {
                notes += Run(run.degree, run.start, run.end)
            }
            pendingGap = 0
        }

        return notes.map { run ->
            val values = (run.start..run.end).map { moria[it] }.filter { !it.isNaN() && nearestDegree(it, positions).first == run.degree }
            val mean = values.average()
            val octave = Math.floorDiv(run.degree, 7)
            val index = Math.floorMod(run.degree, 7)
            SungNote(
                phthong = TrainerPhthong.ascending[index],
                octave = octave,
                startMs = frames[run.start].timeMs,
                endMs = frames[run.end].timeMs + track.hopMs.toLong(),
                deviationMoria = mean - (octave * OCTAVE + positions[index]),
                moria = mean,
            )
        }
    }

    /** Moria above Νη per frame (NaN when unvoiced), median-filtered over voiced neighbours to drop single-frame octave slips. */
    fun smoothedMoria(frames: List<PitchFrame>, niHz: Double): DoubleArray {
        val raw = DoubleArray(frames.size) { index ->
            val hz = frames[index].frequencyHz
            if (hz > 0f) OCTAVE * log2(hz / niHz) else Double.NaN
        }
        return DoubleArray(raw.size) { index ->
            if (raw[index].isNaN()) {
                Double.NaN
            } else {
                val neighbours = (index - 1..index + 1).mapNotNull { raw.getOrNull(it)?.takeIf { value -> !value.isNaN() } }
                median(neighbours)
            }
        }
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2.0
    }

    private fun offlineFrames(durationMs: Double): Int =
        IntonationProfile.framesIn(durationMs, IntonationProfile.OFFLINE_HOP_MS)

    private const val NO_DEGREE = Int.MIN_VALUE
}
