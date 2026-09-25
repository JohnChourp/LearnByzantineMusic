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
 * [NeumeAlign] enum) so it stays unit-testable. The glyphs are the same [Neume]s, [NeumeGlyph]s and
 * [NeumeForm]s as the ascending model, drawn by the same `NeumeStack`; before H4 the descents had
 * their own enum, glyph, form and renderer.
 */

/** A descent of [voices] φωνές down, with its authored neume spelling ([form]). */
data class LeapingDescent(val voices: Int, val form: NeumeForm)

private fun apostrophe(align: NeumeAlign = NeumeAlign.CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(Neume.APOSTROPHOS, 30, 18, align, dx, dy)

private fun slight(align: NeumeAlign = NeumeAlign.CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(Neume.ELAFRON, 52, 18, align, dx, dy)

private fun slightApostrophe(align: NeumeAlign = NeumeAlign.CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(Neume.ELAFRON_APOSTROPHOS, 42, 22, align, dx, dy)

private fun low(w: Int = 58, h: Int = 36, align: NeumeAlign = NeumeAlign.CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(Neume.CHAMILI, w, h, align, dx, dy)

/** The smaller χαμηλή (44×27) used when the deep descents stack two or three of them. */
private fun lowSmall(align: NeumeAlign = NeumeAlign.CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(Neume.CHAMILI, 44, 27, align, dx, dy)

object LeapingDescents {
    /** All descents −2 … −12, in order, each with its authored neume form. */
    val all: List<LeapingDescent> = listOf(
        LeapingDescent(2, NeumeForm(18, listOf(slight()))),
        LeapingDescent(3, NeumeForm(22, listOf(slightApostrophe()))),
        LeapingDescent(4, NeumeForm(36, listOf(low()))),
        LeapingDescent(
            5,
            NeumeForm(90, listOf(low(align = NeumeAlign.TOP_CENTER, dy = 5), apostrophe())),
        ),
        LeapingDescent(
            6,
            NeumeForm(90, listOf(slight(dx = 2), low(align = NeumeAlign.TOP_CENTER, dy = 5))),
        ),
        LeapingDescent(
            7,
            NeumeForm(90, listOf(slightApostrophe(dx = 2), low(align = NeumeAlign.TOP_CENTER, dy = 5))),
        ),
        LeapingDescent(
            8,
            NeumeForm(90, listOf(lowSmall(align = NeumeAlign.TOP_CENTER, dy = 10), lowSmall())),
        ),
        LeapingDescent(
            9,
            NeumeForm(
                90,
                listOf(lowSmall(align = NeumeAlign.TOP_CENTER, dy = 10), lowSmall(), apostrophe(dy = 20)),
            ),
        ),
        LeapingDescent(
            10,
            NeumeForm(
                90,
                listOf(lowSmall(align = NeumeAlign.TOP_CENTER, dy = 10), lowSmall(), slight(dy = 20)),
            ),
        ),
        LeapingDescent(
            11,
            NeumeForm(
                90,
                listOf(lowSmall(align = NeumeAlign.TOP_CENTER, dy = 10), lowSmall(), slightApostrophe(dy = 20)),
            ),
        ),
        LeapingDescent(
            12,
            NeumeForm(
                90,
                listOf(lowSmall(align = NeumeAlign.TOP_CENTER, dy = 10), lowSmall(), lowSmall(dy = 21)),
            ),
        ),
    )
}
