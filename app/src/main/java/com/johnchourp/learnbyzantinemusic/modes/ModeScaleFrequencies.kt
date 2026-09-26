package com.johnchourp.learnbyzantinemusic.modes

import com.johnchourp.learnbyzantinemusic.music.ByzantineTuning

/**
 * Frequency math for the 8 Ήχοι scale diagram, extracted out of `EightModesActivity` so it can be
 * unit-tested.
 *
 * ## Retained deliberately as the reference implementation
 *
 * Since ClickUp `869f4tpxj`, the screen builds a
 * [com.johnchourp.learnbyzantinemusic.music.ModeLadder] instead of calling this, so nothing in the
 * app reaches it any more. It is kept on purpose: `ModeLadderTest` asserts the ladder reproduces
 * **this** function's output **exactly** (delta 0.0) for all four scales across the whole base-shift
 * range, `BaseShift.RANGE`.
 *
 * That is a second implementation of the same arithmetic, which is normally the thing to avoid —
 * `ByzantineTuning` exists precisely because two copies drift. The difference is that this copy is
 * *pinned* to the other by an exact equality test over every combination, so it cannot drift
 * silently; it is an oracle, not a duplicate in use. Delete it only together with that test, and
 * only knowingly.
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
     * [baseShiftMoria] (the «Μεταφορά βάσης», within `BaseShift.RANGE`) is added to each φθόγγος' distance
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
