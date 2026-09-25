package com.johnchourp.learnbyzantinemusic.music

import kotlin.math.log2
import kotlin.math.pow

/**
 * The single place where Byzantine pitch becomes a frequency.
 *
 * Every audible pitch in the app — the 8 Ήχοι scale diagram, touch playback, the ison drone, the
 * Melody Trainer's playback and its listening side, and the per-mode base shift — resolves through
 * [frequencyHz] here. There is deliberately no second copy of this arithmetic: two copies drift, and
 * a drifted copy means the trainer listens for a pitch the diagram does not play.
 *
 * ## Why these two numbers are not settings
 *
 * - [NI_BASE_HZ] = 220.0 — the reference phthong **Νη** sounds at 220 Hz. It is the anchor the whole
 *   ladder hangs from, not a tuning preference. Moving it retunes every screen at once.
 * - [MORIA_PER_OCTAVE] = 72 — Byzantine theory divides the octave into **72 μόρια**. This is the
 *   definition of the unit, in the same way 12 semitones define equal temperament. It is not a
 *   resolution that can be raised for "more precision".
 *
 * Both look adjustable when they appear as bare `220.0` and `72.0` in the middle of a function, which
 * is exactly why they live here with this comment — see ClickUp `869f4tq0p`.
 *
 * ## The formula
 *
 *     f = NI_BASE_HZ * 2^(moriaFromNi / MORIA_PER_OCTAVE)
 *
 * Transposition is the same operation: the base shift (within [BaseShift.RANGE] μόρια) is simply added
 * to `moriaFromNi` before the power, so it cannot round differently from the scale it transposes.
 *
 * Users of this object express pitch in **μόρια above Νη**, which may be negative (below Νη) and may
 * exceed [MORIA_PER_OCTAVE] (higher octaves). No caller does its own `2.0.pow(...)`.
 */
object ByzantineTuning {

    /** Frequency of the reference phthong Νη, in Hz. */
    const val NI_BASE_HZ = 220.0

    /** Μόρια in one octave. The unit's definition, not a precision knob. */
    const val MORIA_PER_OCTAVE = 72

    /** Cents in one octave, for translating μόρια into the unit other tuners speak. */
    const val CENTS_PER_OCTAVE = 1200.0

    /** [MORIA_PER_OCTAVE] as a Double, for the division inside the exponent. */
    private const val MORIA_PER_OCTAVE_D = MORIA_PER_OCTAVE.toDouble()

    /**
     * Frequency of the pitch sitting [moriaFromNi] μόρια above Νη. Negative values go below Νη,
     * values past [MORIA_PER_OCTAVE] go into higher octaves; both are ordinary inputs, because the
     * scale diagram spans three octaves either side of the reference.
     */
    fun frequencyHz(moriaFromNi: Double): Double =
        NI_BASE_HZ * 2.0.pow(moriaFromNi / MORIA_PER_OCTAVE_D)

    /** Convenience overload for whole μόρια. */
    fun frequencyHz(moriaFromNi: Int): Double = frequencyHz(moriaFromNi.toDouble())

    /**
     * Inverse of [frequencyHz]: how many μόρια above Νη a measured frequency sits. Used by the
     * listening side (microphone pitch detection), so the singer's deviation is reported in the
     * unit the domain actually uses rather than in cents.
     */
    fun moriaFromNi(frequencyHz: Double): Double =
        MORIA_PER_OCTAVE_D * log2(frequencyHz / NI_BASE_HZ)

    /** μόρια → cents, for comparing with instruments and tuners outside the tradition. */
    fun moriaToCents(moria: Double): Double = moria * (CENTS_PER_OCTAVE / MORIA_PER_OCTAVE_D)
}
