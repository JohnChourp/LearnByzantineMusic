package com.johnchourp.learnbyzantinemusic.lessons.ui

/**
 * The metronome's vibration, as the page needs it (ClickUp `869f5x2d7`).
 *
 * The metronome already "vibrated" before this — through `View.performHapticFeedback`, which
 * follows the system's touch-feedback setting and, on many phones, makes the θέση and the άρση
 * feel the same. A learner in a church, with the sound off, needs to tell them apart without
 * looking, so the vibration now goes through the `Vibrator` itself ([DeviceBeatVibrator]), with
 * haptic feedback kept only as the fallback when the `VIBRATE` permission is missing.
 */
interface BeatVibrator {
    /** False on a device without a vibrator: the page then hides the vibration switches. */
    val available: Boolean

    fun pulse(strength: BeatStrength)
}

/** How one beat is felt on a given device. Chosen by [BeatVibration.choose]; pure, so tested. */
sealed interface BeatVibration {
    /** API 26+ with amplitude control: the same short buzz at two strengths. */
    data class Amplitude(val millis: Long, val amplitude: Int) : BeatVibration

    /** API 29+ without amplitude control: the platform's own heavy click and tick. */
    data class Predefined(val effect: Effect) : BeatVibration

    /** API 26–28 without amplitude control: the strength is carried by the duration alone. */
    data class OneShot(val millis: Long) : BeatVibration

    /** API 24–25, before `VibrationEffect`: `vibrate(ms)`, strength carried by the duration. */
    data class Legacy(val millis: Long) : BeatVibration

    enum class Effect { HEAVY_CLICK, TICK }

    companion object {
        const val STRONG_MILLIS = 60L
        const val LIGHT_MILLIS = 25L
        const val STRONG_AMPLITUDE = 255
        const val LIGHT_AMPLITUDE = 90

        /**
         * The clearest strong/light difference the device offers: amplitude where it can be
         * controlled, the predefined effects on 29+, then duration.
         */
        fun choose(strength: BeatStrength, sdkInt: Int, hasAmplitudeControl: Boolean): BeatVibration {
            val strong = strength == BeatStrength.STRONG
            val millis = if (strong) STRONG_MILLIS else LIGHT_MILLIS
            return when {
                sdkInt < 26 -> Legacy(millis)
                hasAmplitudeControl -> Amplitude(millis, if (strong) STRONG_AMPLITUDE else LIGHT_AMPLITUDE)
                sdkInt >= 29 -> Predefined(if (strong) Effect.HEAVY_CLICK else Effect.TICK)
                else -> OneShot(millis)
            }
        }
    }
}
