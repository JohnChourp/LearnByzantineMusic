package com.johnchourp.learnbyzantinemusic.summary_theory.ui

/**
 * Model of the «Υπερβατές Αναβάσεις» (leaping ascents) reference: the ways a leap of
 * +2 … +14 φωνές is written by combining a base character (Ολίγον or Πεταστή) with stacked
 * κεντήματα / κέντημα / υψηλή continuation characters.
 *
 * The data is transcribed faithfully from the original `layout_ascents.xml`. The relative
 * placement of the upper glyphs is *semantic* in Byzantine notation (e.g. +4 and +5 use the
 * same neumes but with the υψηλή to the right vs. to the left), so the offsets are part of the
 * model and must be preserved — they are not cosmetic.
 *
 * Pure Kotlin with no Android dependencies (dp values are plain [Int]s, alignment is the
 * [NeumeAlign] enum) so it stays unit-testable; the UI layer maps [Neume] → drawable,
 * [NeumeAlign] → `Alignment` and the [Int] sizes/offsets → `dp` (see `NeumeStack`).
 */

/**
 * The individual neume glyphs that compose a written character.
 *
 * The first five back the «Ανιόντες» leaping-ascent diagrams; the rest were added for the
 * «Συνθέσεις ανάβασης» (ClimbingCompositions) page, which reuses this same model + [NeumeStack]
 * renderer to draw its ascending-composition equations. Adding values here is additive — every
 * existing form keeps rendering identically.
 */
enum class Neume {
    OLIGON, FLYER, EMBROIDERY, EMBROIDERIES, HIGH,
    APOSTROPHE, ISON, UNDERFLOW, GORGO, DIGORGO, SIMPLE_DOT, FRACTION,
}

/** Where a glyph sits inside its form box; mirrors the original FrameLayout gravities. */
enum class NeumeAlign { CENTER, TOP_CENTER, TOP_START, TOP_END, BOTTOM_CENTER }

/**
 * One positioned glyph: [neume] drawn at [w]×[h] dp, anchored at [align] and nudged by
 * ([dx], [dy]) dp — exactly as the source ImageView's size + layout_gravity + translation.
 */
data class NeumeGlyph(
    val neume: Neume,
    val w: Int,
    val h: Int,
    val align: NeumeAlign = NeumeAlign.CENTER,
    val dx: Int = 0,
    val dy: Int = 0,
)

/** A single way of writing a leap: a stack of glyphs inside a [frameHeight]-dp tall box. */
data class NeumeForm(val frameHeight: Int, val glyphs: List<NeumeGlyph>)

/** A leap of [voices] φωνές, with every authored way of writing it ([forms]). */
data class LeapingAscent(val voices: Int, val forms: List<NeumeForm>)

private fun oligon(dy: Int = 0, align: NeumeAlign = NeumeAlign.CENTER, dx: Int = 0) =
    NeumeGlyph(Neume.OLIGON, 70, 18, align, dx, dy)

private fun flyer(dy: Int = 0) = NeumeGlyph(Neume.FLYER, 62, 23, NeumeAlign.CENTER, 0, dy)

private fun embroidery(w: Int = 15, h: Int = 18, align: NeumeAlign = NeumeAlign.CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(Neume.EMBROIDERY, w, h, align, dx, dy)

private fun embroideries(w: Int = 25, h: Int = 18, align: NeumeAlign = NeumeAlign.TOP_CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(Neume.EMBROIDERIES, w, h, align, dx, dy)

private fun high(w: Int = 42, h: Int = 36, align: NeumeAlign = NeumeAlign.TOP_CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(Neume.HIGH, w, h, align, dx, dy)

object LeapingAscents {
    /** All leaps +2 … +14, in order, each with its authored neume forms. */
    val all: List<LeapingAscent> = listOf(
        LeapingAscent(
            2,
            listOf(
                NeumeForm(18, listOf(oligon(), embroidery(dx = 45))),
                NeumeForm(36, listOf(oligon(), embroidery(h = 11, dx = 15, dy = 11))),
                NeumeForm(36, listOf(flyer(dy = 7), oligon(align = NeumeAlign.TOP_CENTER, dx = -1))),
            ),
        ),
        LeapingAscent(
            3,
            listOf(
                NeumeForm(36, listOf(oligon(dy = 5), embroidery(align = NeumeAlign.TOP_CENTER))),
                NeumeForm(54, listOf(flyer(), embroidery(align = NeumeAlign.TOP_CENTER, dx = -1))),
            ),
        ),
        LeapingAscent(
            4,
            listOf(
                NeumeForm(72, listOf(oligon(dy = 5), high(dx = 22))),
                NeumeForm(90, listOf(flyer(dy = 2), high(dx = 22))),
            ),
        ),
        LeapingAscent(
            5,
            listOf(
                NeumeForm(90, listOf(oligon(), high(dx = -20, dy = 3))),
                NeumeForm(90, listOf(flyer(dy = 3), high(dx = -15))),
            ),
        ),
        LeapingAscent(
            6,
            listOf(
                NeumeForm(90, listOf(oligon(), embroidery(align = NeumeAlign.TOP_CENTER, dy = 23), high(dx = 22, dy = 3))),
                NeumeForm(90, listOf(flyer(dy = 2), embroidery(align = NeumeAlign.TOP_CENTER, dy = 20), high(dx = 22))),
            ),
        ),
        LeapingAscent(
            7,
            listOf(
                NeumeForm(90, listOf(high(h = 23, dy = 3), embroidery(h = 15, dy = -11), oligon())),
                NeumeForm(90, listOf(high(h = 23, dy = 3), embroidery(h = 15, dy = -11), flyer(dy = 2))),
            ),
        ),
        LeapingAscent(
            8,
            listOf(
                NeumeForm(90, listOf(oligon(), high(dx = -20, dy = 3), high(dx = 22, dy = 3))),
                NeumeForm(90, listOf(flyer(dy = 2), high(dx = -20), high(dx = 22))),
            ),
        ),
        LeapingAscent(
            9,
            listOf(
                NeumeForm(
                    90,
                    listOf(
                        oligon(),
                        high(h = 23, dx = -20, dy = 3),
                        embroideries(dx = -20, dy = 25),
                        high(dx = 22, dy = 3),
                    ),
                ),
                NeumeForm(
                    90,
                    listOf(
                        flyer(dy = 2),
                        high(h = 23, dx = -20),
                        embroideries(dx = -20, dy = 22),
                        high(dx = 22),
                    ),
                ),
            ),
        ),
        LeapingAscent(
            10,
            listOf(
                NeumeForm(
                    90,
                    listOf(
                        oligon(),
                        high(h = 23, dx = -20, dy = 3),
                        embroidery(h = 11, align = NeumeAlign.TOP_CENTER, dx = -20, dy = 28),
                        high(dx = 22, dy = 3),
                    ),
                ),
                NeumeForm(
                    90,
                    listOf(
                        flyer(dy = 2),
                        high(h = 23, dx = -20),
                        embroidery(h = 11, align = NeumeAlign.TOP_CENTER, dx = -20, dy = 24),
                        high(dx = 22),
                    ),
                ),
            ),
        ),
        LeapingAscent(
            11,
            listOf(
                NeumeForm(
                    90,
                    listOf(
                        oligon(),
                        high(dx = -20, dy = 3),
                        embroidery(h = 11, align = NeumeAlign.TOP_CENTER, dx = 20, dy = 28),
                        high(h = 23, dx = 20, dy = 3),
                    ),
                ),
                NeumeForm(
                    90,
                    listOf(
                        flyer(dy = 2),
                        high(dx = -20),
                        embroidery(h = 11, align = NeumeAlign.TOP_CENTER, dx = 20, dy = 25),
                        high(h = 23, dx = 20),
                    ),
                ),
            ),
        ),
        LeapingAscent(
            12,
            listOf(
                NeumeForm(90, listOf(oligon(), high(dx = -20, dy = 3), high(dy = 3), high(dx = 22, dy = 3))),
                NeumeForm(90, listOf(flyer(dy = 2), high(dx = -20), high(), high(dx = 22))),
            ),
        ),
        LeapingAscent(
            13,
            listOf(
                NeumeForm(
                    90,
                    listOf(
                        oligon(),
                        high(dx = -20, dy = 3),
                        high(h = 23, dy = 3),
                        embroideries(dy = 25),
                        high(dx = 25, dy = 3),
                    ),
                ),
                NeumeForm(
                    90,
                    listOf(
                        flyer(dy = 2),
                        high(dx = -20),
                        high(h = 23),
                        embroideries(dy = 22),
                        high(dx = 25),
                    ),
                ),
            ),
        ),
        LeapingAscent(
            14,
            listOf(
                NeumeForm(
                    90,
                    listOf(
                        oligon(),
                        high(dx = -20, dy = 3),
                        high(h = 23, dy = 3),
                        embroidery(h = 15, dy = -12),
                        high(dx = 25, dy = 3),
                    ),
                ),
                NeumeForm(
                    90,
                    listOf(
                        flyer(dy = 2),
                        high(dx = -20),
                        high(h = 23),
                        embroidery(h = 15, dy = -12),
                        high(dx = 25),
                    ),
                ),
            ),
        ),
    )
}
