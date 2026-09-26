package com.johnchourp.learnbyzantinemusic.modes

import com.johnchourp.learnbyzantinemusic.music.ByzantineTuning
import com.johnchourp.learnbyzantinemusic.music.IntonationProfile
import com.johnchourp.learnbyzantinemusic.music.ModeLadder
import kotlin.math.abs

/**
 * «Πού είμαι σε σχέση με τον φθόγγο» — the sung pitch, read against the **mode's own ladder**
 * (ClickUp `869f4tqad`, E1).
 *
 * ## Why not the trainer's table
 *
 * `TrainerPitchTable.nearestPhthong` answers the same question against the fixed **diatonic**
 * positions with Νη at 220 Hz. On the «8 Ήχοι» page that would be wrong twice over: the page has a
 * «Μεταφορά βάσης» of up to half an octave, and the chromatic and enharmonic modes put their φθόγγοι
 * at genuinely different distances. A singer holding a perfect Δι of the soft chromatic mode would
 * be told they are several μόρια off.
 *
 * So the mirror reads against the [ModeLadder] the screen is already drawing, droning and playing —
 * the same object, so the indicator cannot drift from the diagram the way three hand-rolled copies
 * once did.
 *
 * ## The comparison is a subtraction in μόρια, and that is all it is
 *
 * A [ModeLadder.Step] already stores `moriaFromNi` **with the base shift folded in** — `ModeLadder`
 * computes `fromNi = cumulative - referenceMoria + baseShift` and derives the frequency from it —
 * so the rung's own μόρια are the right thing to subtract. An earlier version of this file routed
 * the comparison through `frequencyHz` and back, justified by the claim that the stored μόρια
 * ignored the shift. A mutation test disproved that: both forms produced identical results on every
 * case. The round-trip is gone, and so is the reasoning that argued for it.
 *
 * The result is in **μόρια**, never cents: the acceptance criterion is that the indicator speaks the
 * language of the field.
 */
object LadderPitchMirror {

    /** How far a sung pitch sits from the nearest rung of the ladder. */
    data class Reading(
        val step: ModeLadder.Step,
        /** Signed μόρια from [step]: positive = sharp (above it), negative = flat. */
        val deviationMoria: Double,
    ) {
        /** The φθόγγος label to show, decorations and all. */
        val label: String get() = step.phthong.label

        /**
         * On the φθόγγος by the app's one rule, [IntonationProfile.isInTune] (±3 μόρια ≈ 50 cents),
         * so this screen, the Trainer and the recording analysis cannot disagree about a voice.
         */
        fun isInTune(toleranceMoria: Double = IntonationProfile.IN_TUNE_MORIA): Boolean =
            IntonationProfile.isInTune(deviationMoria, toleranceMoria)
    }

    /**
     * The rung of [ladder] closest to [frequencyHz], or null when the input is not a usable pitch
     * or the ladder is empty.
     *
     * Null rather than a nearest-guess on bad input: a silent indicator is honest, an indicator
     * pointing at Νη because the detector returned 0 Hz is not.
     */
    fun read(ladder: ModeLadder, frequencyHz: Double): Reading? {
        if (frequencyHz <= 0.0 || !frequencyHz.isFinite()) return null
        val steps = ladder.steps
        if (steps.isEmpty()) return null
        val sungMoria = ByzantineTuning.moriaFromNi(frequencyHz)
        var best = steps.first()
        var bestDeviation = Double.MAX_VALUE
        for (step in steps) {
            val deviation = sungMoria - step.moriaFromNi.value.toDouble()
            if (abs(deviation) < abs(bestDeviation)) {
                bestDeviation = deviation
                best = step
            }
        }
        return Reading(best, bestDeviation)
    }
}
