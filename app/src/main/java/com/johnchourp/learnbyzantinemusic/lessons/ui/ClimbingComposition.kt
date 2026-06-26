package com.johnchourp.learnbyzantinemusic.lessons.ui

import androidx.annotation.StringRes
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.summary_theory.ui.Neume
import com.johnchourp.learnbyzantinemusic.summary_theory.ui.NeumeAlign
import com.johnchourp.learnbyzantinemusic.summary_theory.ui.NeumeForm
import com.johnchourp.learnbyzantinemusic.summary_theory.ui.NeumeGlyph

/**
 * Model of the «Συνθέσεις ανάβασης» (ascending compositions) reference: how a single written
 * character is read as a combination of simpler neumes, and how the γοργόν / δίγοργον timing marks
 * change its speed.
 *
 * The data is transcribed faithfully from the original `res/layout/climbing_compositions.xml`. As in
 * the leaping-ascent model, the relative placement of the stacked glyphs is *semantic* in Byzantine
 * notation (e.g. κεντήματα above vs. below the Ολίγον is a different character), so the sizes,
 * alignments and (dx, dy) offsets are part of the model and must be preserved — they are not
 * cosmetic. Pure Kotlin with no Android dependencies so it stays unit-testable; the UI layer maps
 * [Neume] → drawable and [NeumeAlign] → `Alignment` via the shared `NeumeStack` renderer.
 */

/** Which timing accent a composition demonstrates; drives the section highlight in the UI. */
enum class Emphasis { NONE, GORGON, DIGORGON }

/**
 * One ascending composition: the written character ([combined] — usually one stacked form, but the
 * δίγοργον example is written as two) and the simpler neumes it is read as ([parts]). [readingRes]
 * is the plain-Greek «Διαβάζεται…» explanation; [emphasis] flags the γοργόν / δίγοργον rows.
 */
data class Composition(
    val combined: List<NeumeForm>,
    val parts: List<NeumeForm>,
    @StringRes val readingRes: Int,
    val emphasis: Emphasis = Emphasis.NONE,
)

/* ---- Glyph atoms (sizes mirror the styles in res/values/styles.xml exactly) ---- */

private fun oligon(align: NeumeAlign = NeumeAlign.CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(Neume.OLIGON, 70, 18, align, dx, dy)

private fun ison(w: Int = 62, h: Int = 18, align: NeumeAlign = NeumeAlign.CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(Neume.ISON, w, h, align, dx, dy)

private fun apostrophe(align: NeumeAlign = NeumeAlign.CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(Neume.APOSTROPHE, 30, 18, align, dx, dy)

private fun embroideries(w: Int = 30, h: Int = 18, align: NeumeAlign = NeumeAlign.CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(Neume.EMBROIDERIES, w, h, align, dx, dy)

private fun underflow(align: NeumeAlign = NeumeAlign.CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(Neume.UNDERFLOW, 19, 18, align, dx, dy)

private fun gorgo(align: NeumeAlign = NeumeAlign.TOP_CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(Neume.GORGO, 18, 9, align, dx, dy)

private fun digorgo(align: NeumeAlign = NeumeAlign.TOP_CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(Neume.DIGORGO, 22, 15, align, dx, dy)

private fun dot(align: NeumeAlign = NeumeAlign.BOTTOM_CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(Neume.SIMPLE_DOT, 9, 6, align, dx, dy)

private fun fraction(align: NeumeAlign = NeumeAlign.TOP_CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(Neume.FRACTION, 23, 18, align, dx, dy)

private fun form(frameHeight: Int, vararg glyphs: NeumeGlyph) = NeumeForm(frameHeight, glyphs.toList())

object ClimbingCompositions {

    /** The base ascending compositions (no timing accent). */
    val simple: List<Composition> = listOf(
        // Ολίγον with κεντήματα above ⇒ Ολίγον · Κεντήματα
        Composition(
            combined = listOf(form(36, oligon(), embroideries(w = 25, align = NeumeAlign.TOP_CENTER, dy = -2))),
            parts = listOf(form(18, oligon()), form(18, embroideries())),
            readingRes = R.string.climbing_reading_simple_1,
        ),
        // Ολίγον with κεντήματα below-right ⇒ Κεντήματα · Ολίγον
        Composition(
            combined = listOf(form(36, oligon(), embroideries(w = 25, align = NeumeAlign.TOP_END, dx = -4, dy = 20))),
            parts = listOf(form(18, embroideries()), form(18, oligon())),
            readingRes = R.string.climbing_reading_simple_2,
        ),
        // Ολίγον with απόστροφος and κεντήματα ⇒ Απόστροφος · Κεντήματα
        Composition(
            combined = listOf(
                form(
                    54,
                    oligon(),
                    apostrophe(align = NeumeAlign.TOP_START, dy = 4),
                    embroideries(w = 25, align = NeumeAlign.TOP_END, dx = -5, dy = 6),
                ),
            ),
            parts = listOf(form(18, apostrophe()), form(18, embroideries())),
            readingRes = R.string.climbing_reading_simple_3,
        ),
        // Ολίγον with ίσον and κεντήματα ⇒ Ίσον · Κεντήματα
        Composition(
            combined = listOf(
                form(
                    54,
                    oligon(),
                    ison(w = 36, h = 20, align = NeumeAlign.TOP_START, dy = 6),
                    embroideries(w = 25, align = NeumeAlign.TOP_END, dx = -5, dy = 6),
                ),
            ),
            parts = listOf(form(18, ison()), form(18, embroideries())),
            readingRes = R.string.climbing_reading_simple_4,
        ),
        // Υπορροή with στιγμή ⇒ Ίσον · Απόστροφος · (Απόστροφος με κλάσμα)
        Composition(
            combined = listOf(form(36, underflow(), dot(align = NeumeAlign.BOTTOM_CENTER, dx = -3))),
            parts = listOf(
                form(18, ison()),
                form(18, apostrophe()),
                form(36, apostrophe(), fraction(align = NeumeAlign.TOP_CENTER, dx = -3, dy = -4)),
            ),
            readingRes = R.string.climbing_reading_simple_5,
        ),
    )

    /** The same compositions carrying a γοργόν (½ χρόνος / double speed). */
    val gorgo: List<Composition> = listOf(
        // Ολίγον + κεντήματα + γοργόν ⇒ Ολίγον · (Κεντήματα με γοργόν)
        Composition(
            combined = listOf(
                form(
                    54,
                    oligon(),
                    embroideries(w = 25, align = NeumeAlign.TOP_CENTER, dy = 7),
                    gorgo(dx = 1),
                ),
            ),
            parts = listOf(
                form(18, oligon()),
                form(36, embroideries(), gorgo(dx = 2, dy = 27)),
            ),
            readingRes = R.string.climbing_reading_gorgo_1,
            emphasis = Emphasis.GORGON,
        ),
        // Ολίγον + κεντήματα(κάτω) + γοργόν ⇒ (Κεντήματα με γοργόν) · Ολίγον
        Composition(
            combined = listOf(
                form(
                    54,
                    oligon(),
                    embroideries(w = 25, align = NeumeAlign.TOP_END, dx = -4, dy = 28),
                    gorgo(dx = 5, dy = 8),
                ),
            ),
            parts = listOf(
                form(36, embroideries(), gorgo(dx = 2, dy = 27)),
                form(18, oligon()),
            ),
            readingRes = R.string.climbing_reading_gorgo_2,
            emphasis = Emphasis.GORGON,
        ),
        // Ολίγον + ίσον + κεντήματα + γοργόν ⇒ Ίσον · (Κεντήματα με γοργόν)
        Composition(
            combined = listOf(
                form(
                    72,
                    oligon(),
                    ison(w = 40, align = NeumeAlign.TOP_START, dy = 15),
                    embroideries(w = 25, align = NeumeAlign.TOP_END, dx = -5, dy = 15),
                    gorgo(dx = 21, dy = 6),
                ),
            ),
            parts = listOf(
                form(18, ison(dy = -4)),
                form(36, embroideries(), gorgo(dx = 2, dy = 27)),
            ),
            readingRes = R.string.climbing_reading_gorgo_3,
            emphasis = Emphasis.GORGON,
        ),
        // Ολίγον + απόστροφος + κεντήματα + γοργόν ⇒ Απόστροφος · (Κεντήματα με γοργόν)
        Composition(
            combined = listOf(
                form(
                    72,
                    oligon(),
                    apostrophe(align = NeumeAlign.TOP_CENTER, dx = -18, dy = 13),
                    embroideries(w = 25, align = NeumeAlign.TOP_END, dx = -5, dy = 15),
                    gorgo(dx = 21, dy = 6),
                ),
            ),
            parts = listOf(
                form(18, apostrophe()),
                form(36, embroideries(), gorgo(dx = 2, dy = 27)),
            ),
            readingRes = R.string.climbing_reading_gorgo_4,
            emphasis = Emphasis.GORGON,
        ),
        // Υπορροή + γοργόν ⇒ (Απόστροφος με γοργόν) · Απόστροφος
        Composition(
            combined = listOf(form(54, underflow(), gorgo(dx = 5, dy = 5))),
            parts = listOf(
                form(54, apostrophe(), gorgo(dx = -1, dy = 5)),
                form(18, apostrophe()),
            ),
            readingRes = R.string.climbing_reading_gorgo_5,
            emphasis = Emphasis.GORGON,
        ),
    )

    /** The δίγοργον (¼ χρόνος / triple speed) composition, written as two characters. */
    val digorgo: List<Composition> = listOf(
        // Ίσον · (Ολίγον + κεντήματα + δίγοργον) ⇒ Ίσον · (Κεντήματα με δίγοργον) · Ολίγον
        Composition(
            combined = listOf(
                form(18, ison(dy = -4)),
                form(
                    54,
                    oligon(),
                    embroideries(w = 25, align = NeumeAlign.TOP_END, dx = -4, dy = 28),
                    digorgo(dx = 12, dy = 4),
                ),
            ),
            parts = listOf(
                form(18, ison(dy = -4)),
                form(54, embroideries(), digorgo(dx = 2, dy = 4)),
                form(18, oligon()),
            ),
            readingRes = R.string.climbing_reading_digorgo_1,
            emphasis = Emphasis.DIGORGON,
        ),
    )

    /** All compositions in display order. */
    val all: List<Composition> = simple + gorgo + digorgo
}
