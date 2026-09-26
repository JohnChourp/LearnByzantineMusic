package com.johnchourp.learnbyzantinemusic.trainer

/**
 * The last few seconds of the voice, as «Παραλλαγή με αναμονή» draws them (ClickUp `869f5x2cd`, J1):
 * where the voice was against the φθόγγος the line waits on, frame by frame, scrolling left as time
 * goes on.
 *
 * Each point is kept with its capture time and its offset from the target in μόρια — null for
 * silence, which breaks the line. [points] gives each one its place on the time axis, 0 at the left
 * edge [windowMillis] ago and 1 at the newest frame; older points are dropped as new ones come.
 * Pure, so what the trace shows is tested without a screen.
 */
class VoiceTrace(private val windowMillis: Long = DEFAULT_WINDOW_MILLIS) {

    /** One point: [x] from 0 (oldest shown) to 1 (newest), and the offset from the target, or null. */
    data class Point(val x: Float, val offsetMoria: Double?)

    private val samples = ArrayDeque<Pair<Long, Double?>>()

    fun add(capturedAtMillis: Long, offsetMoria: Double?) {
        samples.addLast(capturedAtMillis to offsetMoria)
        while (samples.isNotEmpty() && samples.first().first < capturedAtMillis - windowMillis) {
            samples.removeFirst()
        }
    }

    /** Starts again: a new target, a new run. */
    fun clear() {
        samples.clear()
    }

    /** The points to draw, oldest first. */
    fun points(): List<Point> {
        val newest = samples.lastOrNull()?.first ?: return emptyList()
        val left = newest - windowMillis
        return samples.map { (at, offset) -> Point(((at - left).toFloat() / windowMillis).coerceIn(0f, 1f), offset) }
    }

    companion object {
        /** Three seconds: long enough to see a slide into the note, short enough to follow it. */
        const val DEFAULT_WINDOW_MILLIS = 3_000L
    }
}
