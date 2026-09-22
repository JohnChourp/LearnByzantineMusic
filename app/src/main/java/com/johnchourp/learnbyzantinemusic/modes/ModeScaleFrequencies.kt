package com.johnchourp.learnbyzantinemusic.modes

import com.johnchourp.learnbyzantinemusic.music.ByzantineTuning

/**
 * Frequency math for the 8 Ήχοι scale diagram, extracted out of `EightModesActivity` so it can be
 * unit-tested.
 *
 * This object only turns the diagram's *shape* — a run of μόρια intervals plus where Νη sits in it —
 * into μόρια-above-Νη. The pitch itself comes from [ByzantineTuning], which owns the reference
 * frequency and the size of the octave; see ClickUp `869f4tq0p` for why there is exactly one such
 * place.
 */
object ModeScaleFrequencies {

    /**
     * Returns the φθόγγος frequencies ordered **top → bottom** (highest pitch first), to line up with
     * the diagram's top-to-bottom labels. [ascendingIntervals] are the μόρια steps from the bottom
     * φθόγγος upward; [referenceMoriaFromBottom] is where the reference Νη sits in that ascending run.
     *
     * [baseShiftMoria] (the per-mode «Μεταφορά βάσης», `-12..+12`) is added to each φθόγγος' distance
     * from Νη *before* the frequency is computed, so a transposed ladder is the same arithmetic as an
     * untransposed one and cannot round differently from it.
     */
    fun topToBottom(
        ascendingIntervals: List<Int>,
        referenceMoriaFromBottom: Int,
        baseShiftMoria: Int,
    ): List<Double> {
        val cumulativeMoriaBottomToTop = ArrayList<Int>(ascendingIntervals.size + 1)
        cumulativeMoriaBottomToTop.add(0)
        var currentMoria = 0
        for (interval in ascendingIntervals) {
            currentMoria += interval
            cumulativeMoriaBottomToTop.add(currentMoria)
        }
        return cumulativeMoriaBottomToTop
            .map { moria ->
                ByzantineTuning.frequencyHz(moria - referenceMoriaFromBottom + baseShiftMoria)
            }
            .reversed()
    }
}
