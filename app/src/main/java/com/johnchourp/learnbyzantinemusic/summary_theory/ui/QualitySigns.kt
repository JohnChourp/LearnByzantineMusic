package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.johnchourp.learnbyzantinemusic.R

/**
 * Model of the «Ποιότητος» (Quality) reference: the eight quality signs (σημάδια ποιότητος) and the
 * worked examples that show what each one does to a φθόγγος.
 *
 * Distilled from the original `res/layout/layout_quality.xml`, where every example was built by
 * overlaying glyph `ImageView`s inside `FrameLayout`s with hand-tuned `translation` offsets. Here
 * each example is a small, legible [NeumeForm] (or a left→right `=` decomposition) so the renderer
 * can stack, highlight and animate it consistently — the same model + [NeumeStack] renderer used by
 * the «Ανιόντες» / «Συνθέσεις ανάβασης» pages. Pure Kotlin (no Android deps) so it stays
 * unit-testable; the UI layer maps [Neume] → drawable and [NeumeAlign] → `Alignment`.
 */

/** Diagram tone for an example row: a plain example, a recommended «do», or a «watch out». */
enum class ExampleTone { NEUTRAL, DO, DONT }

/**
 * One worked example for a quality sign: the written character(s) [combined]; optionally the simpler
 * neumes it is read as [parts] (revealed after an animated «=»); a plain-Greek [readingRes] shown
 * below; and a [tone] that colours do/don't cases.
 */
data class QualityExample(
    val combined: List<NeumeForm>,
    val parts: List<NeumeForm> = emptyList(),
    @StringRes val readingRes: Int,
    val tone: ExampleTone = ExampleTone.NEUTRAL,
)

/**
 * One quality sign: its [titleRes] name, a [tagRes] one-liner of what it changes, the single [glyph]
 * drawn highlighted (described by [glyphCdRes]), the [definitionRes] paragraphs, its worked
 * [examples], and an optional static [counterImageRes] illustration (with [counterTextRes] /
 * [counterCdRes]). Every [Neume] in [highlight] is tinted crimson wherever it appears in an example,
 * so the sign visually separates from the black base neumes it modifies.
 */
data class QualitySign(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val tagRes: Int,
    @StringRes val glyphCdRes: Int,
    val glyph: NeumeForm,
    val highlight: Set<Neume>,
    val definitionRes: List<Int>,
    val examples: List<QualityExample>,
    @DrawableRes val counterImageRes: Int = 0,
    @StringRes val counterTextRes: Int = 0,
    @StringRes val counterCdRes: Int = 0,
)

/** One base neume shown in the page's legend. */
data class QualityLegendItem(
    val neume: Neume,
    @StringRes val nameRes: Int,
    @StringRes val meaningRes: Int,
    @StringRes val cdRes: Int,
)

/* ---- Glyph atoms (sizes mirror the styles in res/values/styles.xml, scaled for a clean row) ---- */

private fun ison(w: Int = 60, h: Int = 18, a: NeumeAlign = NeumeAlign.CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(Neume.ISON, w, h, a, dx, dy)

private fun oligon(w: Int = 64, a: NeumeAlign = NeumeAlign.CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(Neume.OLIGON, w, 18, a, dx, dy)

private fun apostrophe(a: NeumeAlign = NeumeAlign.CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(Neume.APOSTROPHE, 30, 18, a, dx, dy)

private fun gorgo(a: NeumeAlign = NeumeAlign.TOP_CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(Neume.GORGO, 18, 9, a, dx, dy)

private fun embroidery(w: Int = 15, h: Int = 12, a: NeumeAlign = NeumeAlign.TOP_CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(Neume.EMBROIDERY, w, h, a, dx, dy)

private fun embroideries(w: Int = 26, h: Int = 16, a: NeumeAlign = NeumeAlign.TOP_CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(Neume.EMBROIDERIES, w, h, a, dx, dy)

private fun fraction(a: NeumeAlign = NeumeAlign.BOTTOM_CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(Neume.FRACTION, 22, 16, a, dx, dy)

private fun doubleDots(a: NeumeAlign = NeumeAlign.BOTTOM_CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(Neume.DOUBLE_DOTS, 15, 12, a, dx, dy)

private fun yfen(w: Int = 44, h: Int = 14, dx: Int = 0) =
    NeumeGlyph(Neume.YFEN, w, h, NeumeAlign.BOTTOM_CENTER, dx, 0)

/** A quality sign drawn beneath its note (54×14, hugging the bottom of the box). */
private fun under(n: Neume, w: Int = 54, h: Int = 14, dx: Int = 0) =
    NeumeGlyph(n, w, h, NeumeAlign.BOTTOM_CENTER, dx, 0)

private fun f(h: Int, vararg g: NeumeGlyph) = NeumeForm(h, g.toList())

/* ---- Reusable composite tiles ---- */

private fun isonF() = f(18, ison())
private fun oligonF() = f(18, oligon())
private fun apostropheF() = f(18, apostrophe())

/** Απόστροφος with a γοργόν above it. */
private fun apGorgoF() = f(32, apostrophe(dy = 5), gorgo(dx = -1, dy = 2))

/** Κεντήματα with a γοργόν above them. */
private fun embGorgoF() = f(34, embroideries(a = NeumeAlign.CENTER, dy = 4), gorgo(dx = 2, dy = 0))

object QualitySigns {

    /** Base neumes a beginner needs to read the examples, shown once in the page legend. */
    val legend: List<QualityLegendItem> = listOf(
        QualityLegendItem(Neume.ISON, R.string.name_ison, R.string.q_legend_ison, R.string.cd_ison),
        QualityLegendItem(Neume.OLIGON, R.string.name_oligon, R.string.q_legend_oligon, R.string.cd_oligon),
        QualityLegendItem(Neume.APOSTROPHE, R.string.name_apostrophe, R.string.q_legend_apostrophe, R.string.cd_apostrophe),
        QualityLegendItem(Neume.GORGO, R.string.name_gorgo, R.string.q_legend_gorgo, R.string.cd_gorgo),
        QualityLegendItem(Neume.EMBROIDERIES, R.string.name_embroideries, R.string.q_legend_embroideries, R.string.cd_embroideries),
        QualityLegendItem(Neume.FRACTION, R.string.name_fraction, R.string.q_legend_fraction, R.string.cd_fraction),
        QualityLegendItem(Neume.SLIGHT, R.string.name_slight, R.string.q_legend_slight, R.string.cd_slight),
        QualityLegendItem(Neume.FLYER, R.string.name_flyer, R.string.q_legend_flyer, R.string.cd_flyer),
        QualityLegendItem(Neume.UNDERFLOW, R.string.name_underflow, R.string.q_legend_underflow, R.string.cd_underflow),
        QualityLegendItem(Neume.DOUBLE_DOTS, R.string.name_double_dots, R.string.q_legend_double_dots, R.string.cd_double_dots),
    )

    val all: List<QualitySign> = listOf(

        // 1 — Βαρεία: heavier accent on the following note; a comparison (even vs heavy), then the
        // before-dots «watch out».
        QualitySign(
            id = "heavy",
            titleRes = R.string.heavy,
            tagRes = R.string.q_tag_heavy,
            glyphCdRes = R.string.cd_heavy,
            glyph = f(50, NeumeGlyph(Neume.HEAVY, 30, 44, NeumeAlign.CENTER)),
            highlight = setOf(
                Neume.HEAVY, Neume.HEAVY_SIMPLE_DOT, Neume.HEAVY_DOUBLE_DOTS, Neume.HEAVY_TRIPLE_DOTS,
            ),
            definitionRes = listOf(R.string.heavy_definition, R.string.heavy_example_definition),
            examples = listOf(
                QualityExample(
                    combined = listOf(isonF(), apGorgoF(), oligonF()),
                    readingRes = R.string.q_read_heavy_even,
                    tone = ExampleTone.NEUTRAL,
                ),
                QualityExample(
                    combined = listOf(
                        f(50, NeumeGlyph(Neume.HEAVY, 26, 40, NeumeAlign.CENTER)),
                        isonF(), apGorgoF(), oligonF(),
                    ),
                    readingRes = R.string.q_read_heavy_with,
                    tone = ExampleTone.DO,
                ),
                QualityExample(
                    combined = listOf(
                        f(50, NeumeGlyph(Neume.HEAVY_SIMPLE_DOT, 30, 42, NeumeAlign.CENTER)),
                        f(50, NeumeGlyph(Neume.HEAVY_DOUBLE_DOTS, 40, 42, NeumeAlign.CENTER)),
                        f(50, NeumeGlyph(Neume.HEAVY_TRIPLE_DOTS, 52, 42, NeumeAlign.CENTER)),
                    ),
                    readingRes = R.string.q_read_heavy_dots,
                    tone = ExampleTone.DONT,
                ),
            ),
        ),

        // 2 — Υφέν: a single timed note expands into repeated isons bridged by the υφέν.
        QualitySign(
            id = "yfen",
            titleRes = R.string.yfen,
            tagRes = R.string.q_tag_yfen,
            glyphCdRes = R.string.cd_yfen,
            glyph = f(24, NeumeGlyph(Neume.YFEN, 56, 16, NeumeAlign.CENTER)),
            highlight = setOf(Neume.YFEN),
            definitionRes = listOf(R.string.yfen_definition),
            examples = listOf(
                QualityExample(
                    combined = listOf(f(34, ison(a = NeumeAlign.TOP_CENTER), fraction(dx = -2))),
                    parts = listOf(f(30, ison(dx = -16), ison(dx = 16), yfen(w = 46))),
                    readingRes = R.string.q_read_yfen_1,
                ),
                QualityExample(
                    combined = listOf(f(34, ison(a = NeumeAlign.TOP_CENTER), doubleDots(dx = 4))),
                    parts = listOf(
                        f(30, ison(w = 40, dx = -28), ison(w = 40), ison(w = 40, dx = 28), yfen(w = 28, dx = -14), yfen(w = 28, dx = 14)),
                    ),
                    readingRes = R.string.q_read_yfen_2,
                ),
            ),
        ),

        // 3 — Συνεχές ελαφρόν: απόστροφος + ελαφρόν almost glued; apostrophe acts, then a one-note
        // descent. With a do/don't static example below.
        QualitySign(
            id = "continuous_slight",
            titleRes = R.string.continuous_slight,
            tagRes = R.string.q_tag_continuous_slight,
            glyphCdRes = R.string.cd_continuous_slight,
            glyph = f(20, NeumeGlyph(Neume.SLIGHT_CONTINUOUS, 60, 16, NeumeAlign.CENTER)),
            highlight = setOf(Neume.SLIGHT_CONTINUOUS, Neume.SLIGHT),
            definitionRes = listOf(R.string.continuous_slight_definition),
            examples = listOf(
                QualityExample(
                    combined = listOf(f(18, NeumeGlyph(Neume.SLIGHT_CONTINUOUS, 60, 16, NeumeAlign.CENTER))),
                    parts = listOf(apGorgoF(), apostropheF()),
                    readingRes = R.string.q_read_cs_1,
                ),
                QualityExample(
                    combined = listOf(isonF(), f(18, NeumeGlyph(Neume.SLIGHT_CONTINUOUS, 60, 16, NeumeAlign.CENTER))),
                    parts = listOf(isonF(), apGorgoF(), apostropheF()),
                    readingRes = R.string.q_read_cs_2,
                ),
            ),
            counterImageRes = R.drawable.lesson5_continuous_slight_example,
            counterTextRes = R.string.continuous_slight_definition_2,
            counterCdRes = R.string.cd_continuous_slight_example,
        ),

        // 4 — Ψηφιστόν: written under a note, sung with liveliness / force.
        QualitySign(
            id = "digital",
            titleRes = R.string.digital,
            tagRes = R.string.q_tag_digital,
            glyphCdRes = R.string.cd_digital,
            glyph = f(20, NeumeGlyph(Neume.DIGITAL, 60, 16, NeumeAlign.CENTER)),
            highlight = setOf(Neume.DIGITAL),
            definitionRes = listOf(R.string.digital_definition, R.string.digital_example_definition),
            examples = listOf(
                QualityExample(
                    combined = listOf(
                        f(50, oligon(a = NeumeAlign.TOP_CENTER), embroidery(dx = 16, dy = 1), under(Neume.DIGITAL)),
                        apostropheF(), apostropheF(),
                    ),
                    readingRes = R.string.q_read_digital_1,
                    tone = ExampleTone.DO,
                ),
                QualityExample(
                    combined = listOf(
                        f(50, oligon(a = NeumeAlign.TOP_CENTER), embroideries(dy = 1), under(Neume.DIGITAL)),
                    ),
                    readingRes = R.string.q_read_digital_3,
                ),
            ),
        ),

        // 5 — Ομαλόν: smooth wave of the voice up to the high note.
        QualitySign(
            id = "all_right",
            titleRes = R.string.all_right,
            tagRes = R.string.q_tag_all_right,
            glyphCdRes = R.string.cd_all_right,
            glyph = f(20, NeumeGlyph(Neume.ALL_RIGHT, 60, 16, NeumeAlign.CENTER)),
            highlight = setOf(Neume.ALL_RIGHT),
            definitionRes = listOf(R.string.all_right_definition),
            examples = listOf(
                QualityExample(
                    combined = listOf(f(50, oligon(a = NeumeAlign.TOP_CENTER), ison(w = 34, a = NeumeAlign.TOP_END, dy = 2), under(Neume.ALL_RIGHT))),
                    parts = listOf(oligonF(), embGorgoF(), apostropheF()),
                    readingRes = R.string.q_read_omalon_1,
                ),
                QualityExample(
                    combined = listOf(
                        f(50, oligon(a = NeumeAlign.TOP_CENTER, dx = -18), ison(w = 36, a = NeumeAlign.TOP_CENTER, dx = 18), under(Neume.ALL_RIGHT, w = 70)),
                    ),
                    readingRes = R.string.q_read_omalon_3,
                ),
            ),
        ),

        // 6 — Αντικένωμα: a small «flick» of the voice, always followed by a descent.
        QualitySign(
            id = "vaccum",
            titleRes = R.string.vaccum,
            tagRes = R.string.q_tag_vaccum,
            glyphCdRes = R.string.cd_vaccum,
            glyph = f(20, NeumeGlyph(Neume.VACCUM, 60, 16, NeumeAlign.CENTER)),
            highlight = setOf(Neume.VACCUM, Neume.VACCUM_SIMPLE),
            definitionRes = listOf(R.string.vaccum_definition),
            examples = listOf(
                QualityExample(
                    combined = listOf(f(50, oligon(a = NeumeAlign.TOP_CENTER), under(Neume.VACCUM)), apostropheF()),
                    readingRes = R.string.q_read_vaccum_1,
                    tone = ExampleTone.DO,
                ),
                QualityExample(
                    combined = listOf(f(50, ison(a = NeumeAlign.TOP_CENTER), under(Neume.VACCUM_SIMPLE)), apGorgoF()),
                    readingRes = R.string.q_read_vaccum_2,
                ),
            ),
        ),

        // 7 — Σύνδεσμος: joins notes into one continuous, single-syllable phrase.
        QualitySign(
            id = "link",
            titleRes = R.string.link,
            tagRes = R.string.q_tag_link,
            glyphCdRes = R.string.cd_link,
            glyph = f(20, NeumeGlyph(Neume.LINK, 56, 16, NeumeAlign.CENTER)),
            highlight = setOf(Neume.LINK),
            definitionRes = listOf(R.string.link_definition, R.string.link_pronounced),
            examples = listOf(
                QualityExample(
                    combined = listOf(f(44, ison(w = 38, a = NeumeAlign.TOP_CENTER, dx = -16), ison(w = 38, a = NeumeAlign.TOP_CENTER, dx = 16), under(Neume.LINK, w = 58))),
                    readingRes = R.string.q_read_link_1,
                    tone = ExampleTone.DO,
                ),
                QualityExample(
                    combined = listOf(f(50, oligon(w = 40, a = NeumeAlign.TOP_CENTER, dx = -14), ison(w = 36, a = NeumeAlign.TOP_CENTER, dx = 18), under(Neume.LINK, w = 60))),
                    readingRes = R.string.q_read_link_3,
                ),
            ),
        ),

        // 8 — Ενδόφωνο: sung with a closed mouth — weakly, through the nose.
        QualitySign(
            id = "intercom",
            titleRes = R.string.intercom,
            tagRes = R.string.q_tag_intercom,
            glyphCdRes = R.string.cd_intercom,
            glyph = f(20, NeumeGlyph(Neume.INTERCOM, 60, 16, NeumeAlign.CENTER)),
            highlight = setOf(Neume.INTERCOM),
            definitionRes = listOf(R.string.intercom_definition),
            examples = listOf(
                QualityExample(
                    combined = listOf(f(44, ison(a = NeumeAlign.TOP_CENTER), under(Neume.INTERCOM))),
                    readingRes = R.string.q_read_intercom_1,
                ),
            ),
        ),
    )
}
