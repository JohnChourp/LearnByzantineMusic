package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import androidx.annotation.StringRes
import com.johnchourp.learnbyzantinemusic.R

/**
 * The simple descending "quantity characters" (χαρακτήρες ποσότητος κατιόντες) taught on the
 * «Κατιόντες» page, each lowering the voice by a fixed number of φωνές (phthongs):
 *
 *  - Απόστροφος −1  (a plain step down)
 *  - Ελαφρόν    −2  (a step of two phthongs down)
 *  - Υπορροή    −2  (written as one neume but worth two apostrophes — a "run" down two phthongs)
 *  - Χαμηλή     −4  (the deepest single descending character)
 *
 * [voices] is the *magnitude* of the descent (always positive); the page renders it as "−N".
 *
 * A view of the sign table: each character is its [sign] — name, glyph, TalkBack text, size and
 * φωνές all come from [Neume] — in the page's order, plus what only this page adds: a one-line
 * [definitionRes] for the characters that have one. Pure Kotlin (resource ids are plain Ints), so
 * the interval logic stays unit-testable.
 */
enum class DescentCharacter(val sign: Neume, @StringRes val definitionRes: Int? = null) {
    APOSTROPHOS(Neume.APOSTROPHOS),
    ELAFRON(Neume.ELAFRON),
    YPORROI(Neume.YPORROI, definitionRes = R.string.yporroi_definition),
    CHAMILI(Neume.CHAMILI);

    /** How many φωνές the character lowers the voice; the sign table stores it negative. */
    val voices: Int get() = -checkNotNull(sign.voices) { "$sign has no φωνές in the sign table" }

    val hasDefinition: Boolean get() = definitionRes != null

    /** Width × height (dp) the page's simple row draws the sign at: its natural size. */
    val glyphSize: Pair<Int, Int> get() = sign.width to sign.height

    companion object {
        /** Ordered Απόστροφος → Ελαφρόν → Υπορροή → Χαμηλή. */
        val all: List<DescentCharacter> = entries.toList()

        /** The deepest descent among the simple characters (Χαμηλή = −4). */
        val maxVoices: Int = entries.maxOf { it.voices }
    }
}
