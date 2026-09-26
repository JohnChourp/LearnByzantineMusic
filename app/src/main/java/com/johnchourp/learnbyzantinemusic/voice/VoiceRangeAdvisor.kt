package com.johnchourp.learnbyzantinemusic.voice

import com.johnchourp.learnbyzantinemusic.modes.IsonDrone
import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.ByzantineTuning
import com.johnchourp.learnbyzantinemusic.music.Mode
import kotlin.math.roundToInt

/**
 * «Βρες τη φωνή σου»: from the lowest and the highest note a singer holds comfortably, the one
 * global «Μεταφορά βάσης» that seats every mode's scale in that voice (ClickUp `869f5x2dd`, J4).
 *
 * **Why.** A comfortable voice does not always sit in an octave of Νη = 220 Hz. Checks that listen
 * ignore the octave, but not the offset inside it: a singer who is at home a little lower or higher
 * strains, or «fails» every pitch check. The shift already existed, per mode; a beginner did not know
 * he needed it, nor which value to set.
 *
 * **What it places.** Each mode's ison rests on its own base (the end of its απήχημα), and those bases
 * sit at different heights. On average they lie [anchorMoria] μόρια above Νη; the advice puts that
 * average about [BASE_FRACTION] of the way up the comfortable range — room for the melody above the
 * base, as a chanter needs, and a little below it.
 *
 * **What it never does** is name a new reference pitch: Νη stays 220 Hz by design, and the result is
 * a shift in μόρια. The app folds octaves when it listens, so the shift is taken modulo the octave
 * into [BaseShift.RANGE] — half an octave each way reaches any voice.
 *
 * Pure: every pitch goes through [ByzantineTuning], every ladder through [IsonDrone].
 */
object VoiceRangeAdvisor {

    /** How far up the comfortable range the modes' bases should sit, on average. */
    const val BASE_FRACTION = 0.3

    /** Pitches the pitch engine can report at all — YIN's own limits. */
    const val LOWEST_HEARD_HZ = 70.0
    const val HIGHEST_HEARD_HZ = 1200.0

    /** How many detections make a held note: about half a second of steady voice. */
    const val MIN_SAMPLES = 10

    /** Only the most recent detections count: the note as it settles, not the slide into it. */
    const val MAX_SAMPLES = 80

    /** The suggestion, and what it rests on, so the page can explain it. */
    data class Advice(
        val lowestHz: Double,
        val highestHz: Double,
        val globalShiftMoria: Int,
    ) {
        /** The comfortable range in octaves. */
        val octaves: Double get() = (moria(highestHz) - moria(lowestHz)) / ByzantineTuning.MORIA_PER_OCTAVE
    }

    /**
     * Where the eight modes' ison bases sit on average, in μόρια above Νη, with no shift: the point
     * of the scales that the advice places in the voice.
     */
    val anchorMoria: Double by lazy {
        Mode.entries.map { mode ->
            val base = IsonDrone.held(IsonDrone.Request(mode, BaseShift.DEFAULT_MORIA))
                ?: error("${mode.key}: no ison base on its own ladder")
            base.moriaFromNi.value.toDouble()
        }.average()
    }

    /**
     * The note a singer held: the median of what was heard, which a stray octave jump or the slide
     * into the note cannot move. Null when too little was heard to call it a note.
     */
    fun heldPitchHz(heardHz: List<Double>): Double? {
        val usable = heardHz.filter { it in LOWEST_HEARD_HZ..HIGHEST_HEARD_HZ }.takeLast(MAX_SAMPLES)
        if (usable.size < MIN_SAMPLES) return null
        val sorted = usable.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2.0
    }

    /**
     * The global shift for a voice that is comfortable from [lowestHz] to [highestHz], or null when
     * that is not a range: the high note not above the low one, or either outside what can be heard.
     */
    fun advise(lowestHz: Double, highestHz: Double): Advice? {
        if (lowestHz !in LOWEST_HEARD_HZ..HIGHEST_HEARD_HZ || highestHz !in LOWEST_HEARD_HZ..HIGHEST_HEARD_HZ) return null
        if (highestHz <= lowestHz) return null
        val low = moria(lowestHz)
        val high = moria(highestHz)
        val target = low + BASE_FRACTION * (high - low)
        val octave = ByzantineTuning.MORIA_PER_OCTAVE.toDouble()
        val ideal = target - anchorMoria
        // Modulo the octave, to the nearest: the listening folds octaves, so this is the same voice.
        val folded = ideal - octave * Math.round(ideal / octave)
        return Advice(lowestHz, highestHz, BaseShift.clamp(folded.roundToInt()))
    }

    private fun moria(frequencyHz: Double): Double = ByzantineTuning.moriaFromNi(frequencyHz)
}
