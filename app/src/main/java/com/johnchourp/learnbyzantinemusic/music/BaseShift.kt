package com.johnchourp.learnbyzantinemusic.music

/**
 * The «Μεταφορά βάσης»: how far a mode's whole ladder may be moved up or down, in μόρια
 * (ClickUp `869f5x24v`, F2).
 *
 * The shift is stored **per mode** under `AppPrefs.baseShiftKeyName(modeKey)`, and since F2 two
 * screens read and write that same key: the 8 Ήχοι page and the Melody Trainer. Both must therefore
 * accept exactly the same range — if one clamped wider, the other would silently cut the user's
 * value back on its next read.
 *
 * This is the one place the range is declared for code outside the 8 Ήχοι screen. That screen and
 * its activity still carry their own copies from before; `BaseShiftRangeTest` fails if they ever
 * disagree with this one, until ClickUp `869f5x2dd` (J4) folds them in here — J4 is also where the
 * range widens to ±36.
 */
object BaseShift {

    /** The lowest shift, in μόρια. ±12 μόρια ≈ ±2 semitones. */
    const val MIN_MORIA = -12

    /** The highest shift, in μόρια. */
    const val MAX_MORIA = 12

    /** No shift: the mode's ladder as written, Νη = 220 Hz. */
    const val DEFAULT_MORIA = 0

    val RANGE: IntRange = MIN_MORIA..MAX_MORIA

    /** A stored or dragged value brought inside [RANGE], as both screens do on read and on write. */
    fun clamp(moria: Int): Int = moria.coerceIn(MIN_MORIA, MAX_MORIA)
}
