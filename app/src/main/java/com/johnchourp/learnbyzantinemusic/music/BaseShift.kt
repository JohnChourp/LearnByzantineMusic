package com.johnchourp.learnbyzantinemusic.music

/**
 * The «Μεταφορά βάσης»: how far a ladder may be moved up or down, in μόρια (ClickUp `869f5x24v`,
 * F2; `869f5x2dd`, J4).
 *
 * **The one place the range is declared.** The shift is stored **per mode** under
 * `AppPrefs.baseShiftKeyName(modeKey)`, and two screens read and write that same key: the 8 Ήχοι
 * page and the Melody Trainer. Both must accept exactly the same range — if one clamped wider, the
 * other would silently cut the user's value back on its next read. J4 folded the 8 Ήχοι page's own
 * copies in here; `BaseShiftRangeTest` fails if a copy ever comes back.
 *
 * **±36 μόρια** (owner decision, 2026-09-25): half an octave either way. Νη stays 220 Hz by design
 * — a shift moves the ladder, it never names a new reference pitch — and since the app folds octaves
 * when it listens, half an octave each way reaches any voice. The values saved before J4, within
 * ±12, are inside the new range and are never rewritten.
 *
 * **The global shift** (J4, «Βρες τη φωνή σου») is added on top of each mode's own, wherever a
 * ladder is built: [combined] adds the two and clamps the sum here, at the point of use. Neither
 * stored value is ever changed by the other; a global 0 leaves every ladder exactly as before.
 */
object BaseShift {

    /** The lowest shift, in μόρια: half an octave down. */
    const val MIN_MORIA = -36

    /** The highest shift, in μόρια: half an octave up. */
    const val MAX_MORIA = 36

    /** No shift: the mode's ladder as written, Νη = 220 Hz. */
    const val DEFAULT_MORIA = 0

    val RANGE: IntRange = MIN_MORIA..MAX_MORIA

    /** A stored or dragged value brought inside [RANGE], as every screen does on read and on write. */
    fun clamp(moria: Int): Int = moria.coerceIn(MIN_MORIA, MAX_MORIA)

    /**
     * The shift a ladder is built with: a mode's own [modeShiftMoria] plus the voice's global
     * [globalShiftMoria], clamped to [RANGE] — here, where it is used, and never written back.
     */
    fun combined(modeShiftMoria: Int, globalShiftMoria: Int): Int = clamp(modeShiftMoria + globalShiftMoria)
}
