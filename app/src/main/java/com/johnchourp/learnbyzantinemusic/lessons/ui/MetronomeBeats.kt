package com.johnchourp.learnbyzantinemusic.lessons.ui

/**
 * What the «Δίσημος / Τρίσημος / Τετράσημος» metronome does on each beat (ClickUp `869f5x2d7`).
 *
 * ## One schedule for everything
 *
 * The click, the vibration, the highlighted χρόνος and the moving hand all come from **one**
 * [MetronomeRun]: one start time on a monotonic clock and [MetronomeSchedule]'s absolute beat
 * times. Each beat is a single [BeatEvent] that says whether it clicks and how it vibrates, so the
 * two cannot come apart. A second `delay()` loop for the vibration would accumulate error and drift
 * away from the click — exactly the bug fixed for the click in `869f4tpg0`.
 *
 * ## θέση and άρση
 *
 * The first χρόνος of a grouping is the θέση and vibrates [BeatStrength.STRONG]; the others are
 * άρσεις and vibrate [BeatStrength.LIGHT]. In «Πόδι» mode only the θέσεις are marked, so a
 * beginner counts from θέση to θέση; the highlight and the hand still move through every χρόνος.
 *
 * Pure Kotlin, no Android, so all of it is unit-tested.
 */
enum class BeatStrength { STRONG, LIGHT }

/**
 * The learner's switches. [vibrate] and [silent] only mean something on a device that can vibrate:
 * see [effectiveOn].
 */
data class MetronomeOptions(
    val vibrate: Boolean = true,
    val silent: Boolean = false,
    val foot: Boolean = false,
) {
    /**
     * What actually happens on a device where vibration is [canVibrate]. Silent needs vibration:
     * on a phone without a vibrator — or with vibration switched off — a stored «Σιωπηλά» must not
     * leave a metronome that neither sounds nor vibrates, with its switch hidden.
     */
    fun effectiveOn(canVibrate: Boolean): MetronomeOptions {
        val vibrates = vibrate && canVibrate
        return copy(vibrate = vibrates, silent = silent && vibrates)
    }
}

/** One beat of the schedule and everything that happens on it. */
data class BeatEvent(
    val index: Int,
    /** When it is due, on the clock the run was started with. */
    val atMillis: Long,
    /** 1-based χρόνος inside the grouping, for the highlight. */
    val beatInGrouping: Int,
    /** True on the θέση, the first χρόνος of the grouping. */
    val thesis: Boolean,
    val click: Boolean,
    /** Null when this beat does not vibrate. */
    val vibration: BeatStrength?,
)

object MetronomeBeats {

    /** Beat [index] of the schedule that started at [startMillis], under [options]. */
    fun event(startMillis: Long, bpm: Int, beats: Int, index: Int, options: MetronomeOptions): BeatEvent {
        val thesis = MetronomeSchedule.isDownbeat(index, beats)
        val marked = thesis || !options.foot
        return BeatEvent(
            index = index,
            atMillis = MetronomeSchedule.beatTimeMillis(startMillis, bpm, index),
            beatInGrouping = MetronomeSchedule.beatInGrouping(index, beats),
            thesis = thesis,
            click = marked && !options.silent,
            vibration = when {
                !marked || !options.vibrate -> null
                thesis -> BeatStrength.STRONG
                else -> BeatStrength.LIGHT
            },
        )
    }

    /** The vibration switches are shown only where there is something to feel. */
    fun showsVibrationOptions(vibrator: BeatVibrator): Boolean = vibrator.available

    /** Clicks and vibrates [event]: the one place both happen, from the same event. */
    fun perform(event: BeatEvent, click: (thesis: Boolean) -> Unit, vibrator: BeatVibrator) {
        if (event.click) click(event.thesis)
        val strength = event.vibration ?: return
        if (vibrator.available) vibrator.pulse(strength)
    }
}

/**
 * One playing session: every beat, and the hand, measured from the same [startMillis].
 *
 * [startMillis] must come from a monotonic clock (`SystemClock.elapsedRealtime` on a device). The
 * wall clock can jump — a time sync or a manual change — and the schedule would jump with it.
 */
class MetronomeRun(val startMillis: Long, val bpm: Int, val beats: Int) {

    private var index = 0

    /** How long to wait from [nowMillis] until the next beat. A late wake-up is not carried forward. */
    fun waitMillis(nowMillis: Long): Long = MetronomeSchedule.delayUntilMillis(startMillis, bpm, index, nowMillis)

    /** The next beat, under the switches as they are now, and move on. */
    fun next(options: MetronomeOptions): BeatEvent = MetronomeBeats.event(startMillis, bpm, beats, index++, options)

    /** Where the hand is at [nowMillis]: 0 — its lowest point — on every θέση. */
    fun handPhase(nowMillis: Long): Double = ChironomyPhase.of((nowMillis - startMillis).toDouble(), bpm, beats)
}
