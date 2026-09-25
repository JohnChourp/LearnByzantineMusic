package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import androidx.annotation.StringRes
import com.johnchourp.learnbyzantinemusic.R

/**
 * The simple ascending "quantity characters" (χαρακτήρες ποσότητος ανιόντες) taught on the
 * «Ανιόντες» page, each raising the voice by a fixed number of φωνές (phthongs):
 *
 *  - Ίσον      0  (stays on the same phthong)
 *  - Ολίγον   +1  (a plain step up)
 *  - Πεταστή  +1  (an emphatic step up)
 *  - Κεντήματα +1 (a gentle, connecting step up)
 *  - Κέντημα  +2
 *  - Υψηλή    +4
 *
 * A view of the sign table: each character is its [sign] — name, glyph, TalkBack text, size and
 * φωνές all come from [Neume] — in the page's order, plus what only this page adds: a one-line
 * [definitionRes] for the characters that have one. Pure Kotlin (resource ids are plain Ints), so
 * the interval logic stays unit-testable.
 */
enum class AscentCharacter(
    val sign: Neume,
    @StringRes val definitionRes: Int? = null,
    private val rowHeight: Int? = null,
) {
    ISON(Neume.ISON),

    // The original simple Ολίγον row drew it 8dp tall, a thin, flat mark; the leaping section keeps
    // the table's 18dp.
    OLIGON(Neume.OLIGON, rowHeight = 8),
    PETASTI(Neume.PETASTI, definitionRes = R.string.flyer_definition),
    KENTIMATA(Neume.KENTIMATA, definitionRes = R.string.embroideries_definition),
    KENTIMA(Neume.KENTIMA),
    YPSILI(Neume.YPSILI);

    /** How many φωνές the character raises the voice: the sign table's value. */
    val voices: Int get() = checkNotNull(sign.voices) { "$sign has no φωνές in the sign table" }

    val hasDefinition: Boolean get() = definitionRes != null

    /** Width × height (dp) the page's simple row draws the sign at: its natural size, except [OLIGON]. */
    val glyphSize: Pair<Int, Int> get() = sign.width to (rowHeight ?: sign.height)

    companion object {
        /** Ordered Ίσον → Ολίγον → Πεταστή → Κεντήματα → Κέντημα → Υψηλή. */
        val all: List<AscentCharacter> = entries.toList()

        /** The largest voice rise among the simple characters (Υψηλή = +4). */
        val maxVoices: Int = entries.maxOf { it.voices }
    }
}
