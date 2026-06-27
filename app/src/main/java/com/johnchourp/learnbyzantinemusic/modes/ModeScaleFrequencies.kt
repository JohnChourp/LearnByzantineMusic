package com.johnchourp.learnbyzantinemusic.modes

import kotlin.math.pow

/**
 * Pure frequency math for the 8 Ήχοι scale diagram, extracted out of `EightModesActivity` so it can
 * be unit-tested. Byzantine pitch is measured in *μόρια* (72 to the octave). The reference phthong
 * Νη sounds at [BASE_NI_FREQUENCY_HZ]; every other φθόγγος is that reference shifted by its μόρια
 * distance, and [baseShiftMoria] transposes the whole ladder up/down without changing the intervals.
 */
object ModeScaleFrequencies {
    const val BASE_NI_FREQUENCY_HZ = 220.0
    const val MORIA_PER_OCTAVE = 72.0

    /**
     * Returns the φθόγγος frequencies ordered **top → bottom** (highest pitch first), to line up with
     * the diagram's top-to-bottom labels. [ascendingIntervals] are the μόρια steps from the bottom
     * φθόγγος upward; [referenceMoriaFromBottom] is where the reference Νη sits in that ascending run.
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
        val transposeFactor = 2.0.pow(baseShiftMoria / MORIA_PER_OCTAVE)
        return cumulativeMoriaBottomToTop
            .map { moria ->
                val moriaFromBaseNi = moria - referenceMoriaFromBottom
                BASE_NI_FREQUENCY_HZ * 2.0.pow(moriaFromBaseNi / MORIA_PER_OCTAVE) * transposeFactor
            }
            .reversed()
    }
}
