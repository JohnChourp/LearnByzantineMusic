package com.johnchourp.learnbyzantinemusic.summary_theory.ui

/**
 * Model of the «Υπερβατές Καταβάσεις» (leaping/transcendent descents) reference: the way a
 * descent of −2 … −12 φωνές is written by combining the descending characters (ελαφρόν,
 * ελαφρόν-απόστροφος, χαμηλή and a smaller χαμηλή) into a single stacked neume.
 *
 * The data is transcribed faithfully from the original `layout_descents.xml`. The relative
 * placement of the glyphs is *semantic* in Byzantine notation (e.g. the −6 stacks a χαμηλή on
 * top of an ελαφρόν, while the deep descents repeat the χαμηλή), so the offsets are part of the
 * model and must be preserved — they are not cosmetic.
 *
 * Pure Kotlin with no Android dependencies (dp values are plain [Int]s, alignment is the shared
 * [NeumeAlign] enum) so it stays unit-testable; the UI layer maps [DescentNeume] → drawable,
 * [NeumeAlign] → `Alignment` and the [Int] sizes/offsets → `dp` (see `DescentNeumeStack`).
 */

/** The individual neume glyphs that compose a descending leap. */
enum class DescentNeume { APOSTROPHE, SLIGHT, SLIGHT_APOSTROPHE, LOW }

/**
 * One positioned glyph: [neume] drawn at [w]×[h] dp, anchored at [align] and nudged by
 * ([dx], [dy]) dp — exactly as the source ImageView's size + layout_gravity + translation.
 * Reuses the shared [NeumeAlign] (defined alongside the ascending model) since the geometry is
 * notation-agnostic.
 */
data class DescentGlyph(
    val neume: DescentNeume,
    val w: Int,
    val h: Int,
    val align: NeumeAlign = NeumeAlign.CENTER,
    val dx: Int = 0,
    val dy: Int = 0,
)

/** A single way of writing a descent: a stack of glyphs inside a [frameHeight]-dp tall box. */
data class DescentForm(val frameHeight: Int, val glyphs: List<DescentGlyph>)

/** A descent of [voices] φωνές down, with its authored neume spelling ([form]). */
data class LeapingDescent(val voices: Int, val form: DescentForm)

private fun apostrophe(align: NeumeAlign = NeumeAlign.CENTER, dx: Int = 0, dy: Int = 0) =
    DescentGlyph(DescentNeume.APOSTROPHE, 30, 18, align, dx, dy)

private fun slight(align: NeumeAlign = NeumeAlign.CENTER, dx: Int = 0, dy: Int = 0) =
    DescentGlyph(DescentNeume.SLIGHT, 52, 18, align, dx, dy)

private fun slightApostrophe(align: NeumeAlign = NeumeAlign.CENTER, dx: Int = 0, dy: Int = 0) =
    DescentGlyph(DescentNeume.SLIGHT_APOSTROPHE, 42, 22, align, dx, dy)

private fun low(w: Int = 58, h: Int = 36, align: NeumeAlign = NeumeAlign.CENTER, dx: Int = 0, dy: Int = 0) =
    DescentGlyph(DescentNeume.LOW, w, h, align, dx, dy)

/** The smaller χαμηλή (44×27) used when the deep descents stack two or three of them. */
private fun lowSmall(align: NeumeAlign = NeumeAlign.CENTER, dx: Int = 0, dy: Int = 0) =
    DescentGlyph(DescentNeume.LOW, 44, 27, align, dx, dy)

object LeapingDescents {
    /** All descents −2 … −12, in order, each with its authored neume form. */
    val all: List<LeapingDescent> = listOf(
        LeapingDescent(2, DescentForm(18, listOf(slight()))),
        LeapingDescent(3, DescentForm(22, listOf(slightApostrophe()))),
        LeapingDescent(4, DescentForm(36, listOf(low()))),
        LeapingDescent(
            5,
            DescentForm(90, listOf(low(align = NeumeAlign.TOP_CENTER, dy = 5), apostrophe())),
        ),
        LeapingDescent(
            6,
            DescentForm(90, listOf(slight(dx = 2), low(align = NeumeAlign.TOP_CENTER, dy = 5))),
        ),
        LeapingDescent(
            7,
            DescentForm(90, listOf(slightApostrophe(dx = 2), low(align = NeumeAlign.TOP_CENTER, dy = 5))),
        ),
        LeapingDescent(
            8,
            DescentForm(90, listOf(lowSmall(align = NeumeAlign.TOP_CENTER, dy = 10), lowSmall())),
        ),
        LeapingDescent(
            9,
            DescentForm(
                90,
                listOf(lowSmall(align = NeumeAlign.TOP_CENTER, dy = 10), lowSmall(), apostrophe(dy = 20)),
            ),
        ),
        LeapingDescent(
            10,
            DescentForm(
                90,
                listOf(lowSmall(align = NeumeAlign.TOP_CENTER, dy = 10), lowSmall(), slight(dy = 20)),
            ),
        ),
        LeapingDescent(
            11,
            DescentForm(
                90,
                listOf(lowSmall(align = NeumeAlign.TOP_CENTER, dy = 10), lowSmall(), slightApostrophe(dy = 20)),
            ),
        ),
        LeapingDescent(
            12,
            DescentForm(
                90,
                listOf(lowSmall(align = NeumeAlign.TOP_CENTER, dy = 10), lowSmall(), lowSmall(dy = 21)),
            ),
        ),
    )
}
