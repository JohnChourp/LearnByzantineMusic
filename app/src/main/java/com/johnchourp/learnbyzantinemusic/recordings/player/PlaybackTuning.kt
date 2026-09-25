package com.johnchourp.learnbyzantinemusic.recordings.player

import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.ByzantineTuning

/**
 * How the in-app player plays a recording: slower, and higher or lower (ClickUp `869f5x268`).
 *
 * **Speed** runs from ½× to 1× and **never changes the pitch** — slowed down, the phthongs must stay
 * where they are, or the learner practises against an out-of-tune teacher. Faster than 1× is left
 * out on purpose: this is for learning a passage, not skimming it.
 *
 * **Shift** moves the whole recording by whole μόρια, so a teacher's recording can meet the
 * learner's voice. The factor comes from [ByzantineTuning.ratioForMoria] — the app's one conversion
 * of μόρια — never from a second copy of the 72. The range is the «Μεταφορά βάσης» range,
 * [BaseShift], declared once for every screen that moves a pitch.
 *
 * Values are clamped, never rejected: a slider cannot hand in anything the player refuses.
 */
data class PlaybackTuning(
    val speed: Float = 1f,
    val shiftMoria: Int = 0,
) {
    /** The pitch factor for [shiftMoria]: 1 unshifted, above 1 higher, below 1 lower. */
    val pitchRatio: Float
        get() = ByzantineTuning.ratioForMoria(shiftMoria.toDouble()).toFloat()

    val isDefault: Boolean
        get() = speed == 1f && shiftMoria == 0

    fun withSpeed(value: Float): PlaybackTuning = copy(speed = clampSpeed(value))

    fun withShift(moria: Int): PlaybackTuning = copy(shiftMoria = BaseShift.clamp(moria))

    companion object {
        const val MIN_SPEED = 0.5f
        const val MAX_SPEED = 1f

        fun clampSpeed(value: Float): Float =
            if (value.isNaN()) MAX_SPEED else value.coerceIn(MIN_SPEED, MAX_SPEED)
    }
}
