package com.johnchourp.learnbyzantinemusic.music

/**
 * A distance in **μόρια** — the Byzantine unit of pitch, 72 to the octave (ClickUp `869f4tpxj`).
 *
 * ## Why a type and not an Int
 *
 * The app carries several different integers that all look alike: μόρια, an octave index, a position
 * in a label list, a beat count. Nothing stopped `octave + moria` from compiling, and the result
 * would be a plausible-looking pitch that is simply wrong — the kind of bug you find by ear, weeks
 * later, if at all.
 *
 * Wrapping it costs nothing at runtime (`value class`) and makes those additions a compile error.
 *
 * Intervals are **signed**: a φθόγγος can sit below Νη, and the «Μεταφορά βάσης» moves the whole
 * ladder either way, so negative values are ordinary rather than exceptional.
 */
@JvmInline
value class Moria(val value: Int) : Comparable<Moria> {

    operator fun plus(other: Moria): Moria = Moria(value + other.value)
    operator fun minus(other: Moria): Moria = Moria(value - other.value)
    operator fun unaryMinus(): Moria = Moria(-value)
    operator fun times(factor: Int): Moria = Moria(value * factor)

    override fun compareTo(other: Moria): Int = value.compareTo(other.value)

    override fun toString(): String = "$value μόρια"

    companion object {
        val ZERO = Moria(0)

        /** One octave. The same 72 [ByzantineTuning] defines — not a second copy of the number. */
        val OCTAVE = Moria(ByzantineTuning.MORIA_PER_OCTAVE)
    }
}

/** Frequency of a pitch [this] μόρια above Νη. Goes through the single tuning source. */
fun Moria.toFrequencyHz(): Double = ByzantineTuning.frequencyHz(value)

/** Reads an Int as μόρια at a boundary — JSON, a stored preference, a slider position. */
fun Int.moria(): Moria = Moria(this)
