package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import androidx.annotation.StringRes
import com.johnchourp.learnbyzantinemusic.R

/**
 * Model of the «Χαρακτήρες Χρόνου» (Time Characters) reference: the marks that add or divide the
 * time (χρόνος) of a φθόγγος — the κλάσμα/κουκίδες that lengthen a note, the βαρεία rests, and the
 * γοργό / δίγοργο / τρίγοργο / αργό family that splits a beat into smaller notes.
 *
 * Distilled from the original `res/layout/layout_time.xml`, where every diagram was built by
 * overlaying glyph `ImageView`s inside `FrameLayout`s with hand-tuned `translation` offsets, paired
 * with a second `TableRow` of fraction labels. Here each diagram is a small [TimeEquation] of
 * [TimeTerm]s (each a [NeumeForm] glyph stack with the beat value below it), so the same model +
 * [NeumeStack] renderer used by the «Ανιόντες» / «Ποιότητος» pages can stack, highlight and animate
 * it consistently. The relative placement of the upper glyphs is *semantic* in Byzantine notation,
 * so every size / gravity / translation is transcribed faithfully from the source — the offsets are
 * part of the model, not cosmetic. Pure Kotlin (no Android deps) so it stays unit-testable; the UI
 * layer maps [Neume] → drawable and [NeumeAlign] → `Alignment`.
 */

/**
 * One symbol→meaning row of the «examples» and «pause» tables: the [form] glyph, the plain-Greek
 * [meaningRes] it stands for, and an optional [nameRes] (the examples table names each κουκίδα).
 */
data class TimeSymbolRow(
    val form: NeumeForm,
    @StringRes val meaningRes: Int,
    @StringRes val nameRes: Int = 0,
)

/**
 * One term of a time equation: a glyph [form] with an optional [labelRes] beat value shown beneath
 * it, or the «=» connector when [isEquals] is true (then [form] is null).
 */
data class TimeTerm(
    val form: NeumeForm? = null,
    @StringRes val labelRes: Int = 0,
    val isEquals: Boolean = false,
)

/**
 * A worked time diagram read left→right: the written character(s), an «=», then the simpler notes it
 * is read as, with each note's beat value below. Every [Neume] in [highlight] is tinted crimson
 * wherever it appears, so the time character separates from the plain black base neumes it modifies.
 */
data class TimeEquation(
    val terms: List<TimeTerm>,
    val highlight: Set<Neume>,
)

/* ---- Glyph atoms (sizes mirror the ImageView styles in res/values/styles.xml) ---- */

private fun g(neume: Neume, w: Int, h: Int, a: NeumeAlign = NeumeAlign.CENTER, dx: Int = 0, dy: Int = 0) =
    NeumeGlyph(neume, w, h, a, dx, dy)

private fun form(h: Int, vararg glyphs: NeumeGlyph) = NeumeForm(h, glyphs.toList())

private fun ison(a: NeumeAlign = NeumeAlign.CENTER, dx: Int = 0, dy: Int = 0) = g(Neume.ISON, 62, 18, a, dx, dy)
private fun oligon(a: NeumeAlign = NeumeAlign.CENTER, dx: Int = 0, dy: Int = 0) = g(Neume.OLIGON, 70, 18, a, dx, dy)

/** A plain ίσον on its own tile (the unit note in every gorgó equation). */
private fun isonTile() = form(18, ison())

/** The result-side ίσον, nudged up 4dp as in the αργό/δίαργο/τρίαργο rows. */
private fun isonRaised() = form(18, ison(dy = -4))

/* ---- Composite tiles: a note carrying its time character (offsets from layout_time.xml) ---- */

private fun isonGorgo() =
    form(36, ison(), g(Neume.GORGO, 18, 9, NeumeAlign.CENTER, dx = 5, dy = -5))

private fun isonPresentedGorgo() =
    form(54, ison(), g(Neume.PRESENTED_GORGO, 21, 10, NeumeAlign.TOP_CENTER, dx = 2, dy = 12))

private fun isonGorgoPresented() =
    form(54, ison(), g(Neume.GORGO_PRESENTED, 21, 10, NeumeAlign.TOP_CENTER, dx = 2, dy = 12))

private fun isonDigorgo() =
    form(54, ison(), g(Neume.DIGORGO, 22, 15, NeumeAlign.TOP_CENTER, dx = 8, dy = 9))

private fun isonPresentedDigorgo(n: Neume, w: Int) =
    form(54, ison(), g(n, w, 18, NeumeAlign.TOP_CENTER, dx = 8, dy = 6))

private fun isonTrigorgo() =
    form(54, ison(), g(Neume.TRIGORGO, 36, 18, NeumeAlign.TOP_CENTER, dx = 12, dy = 4))

/** Ολίγον carrying an argó-family character above and κεντήματα below-right (the written form). */
private fun oligonArgoFamily(n: Neume, w: Int, dy: Int) =
    form(
        54,
        oligon(),
        g(Neume.EMBROIDERIES, 25, 18, NeumeAlign.TOP_END, dx = -4, dy = 28),
        g(n, w, 18, NeumeAlign.TOP_CENTER, dx = 4, dy = dy),
    )

/** Argó has a shorter glyph (13×10) sitting slightly lower; the δι/τρι variants are 18-tall. */
private fun oligonArgo() =
    form(
        54,
        oligon(),
        g(Neume.EMBROIDERIES, 25, 18, NeumeAlign.TOP_END, dx = -4, dy = 28),
        g(Neume.ARGO, 13, 10, NeumeAlign.TOP_CENTER, dx = 6, dy = 6),
    )

/** Κεντήματα carrying a γοργόν — the shared middle term of every αργό decomposition. */
private fun embGorgo() =
    form(54, g(Neume.EMBROIDERIES, 30, 18, NeumeAlign.CENTER), g(Neume.GORGO, 18, 9, NeumeAlign.TOP_CENTER, dx = 2, dy = 6))

/** Ολίγον carrying a κλάσμα/κουκίδες below it — the lengthened note that closes an αργό decomposition. */
private fun oligonWith(n: Neume, w: Int, dx: Int, dy: Int) =
    form(36, oligon(), g(n, w, 18, NeumeAlign.TOP_CENTER, dx = dx, dy = dy))

/* ---- Equation builders ---- */

private fun term(form: NeumeForm, @StringRes labelRes: Int = 0) = TimeTerm(form = form, labelRes = labelRes)
private fun equals() = TimeTerm(isEquals = true)

object TimeCharacters {

    /** The κλάσμα / κουκίδες that add time to any σημαδόφωνο (ίσον, ολίγον, απόστροφο …). */
    val examples: List<TimeSymbolRow> = listOf(
        TimeSymbolRow(form(18, g(Neume.FRACTION, 48, 18)), R.string.phthong_1_for_2_time, R.string.fraction),
        TimeSymbolRow(form(18, g(Neume.SIMPLE_DOT, 22, 18)), R.string.phthong_1_for_2_time, R.string.dot_simple),
        TimeSymbolRow(form(18, g(Neume.DOUBLE_DOTS, 56, 18)), R.string.phthong_1_for_3_time, R.string.dot_double),
        TimeSymbolRow(form(18, g(Neume.TRIPLE_DOTS, 70, 18)), R.string.phthong_1_for_4_time, R.string.dot_triple),
    )

    /** The βαρεία used as a rest, in its 1 / 2 / 3-beat forms. */
    val pauses: List<TimeSymbolRow> = listOf(
        TimeSymbolRow(form(54, g(Neume.HEAVY_SIMPLE_DOT, 36, 54)), R.string.pause_time_1),
        TimeSymbolRow(form(54, g(Neume.HEAVY_DOUBLE_DOTS, 50, 54)), R.string.pause_time_2),
        TimeSymbolRow(form(54, g(Neume.HEAVY_TRIPLE_DOTS, 66, 54)), R.string.pause_time_3),
    )

    /** Γοργό: two ίσον in one beat (½ + ½). */
    val gorgo = TimeEquation(
        terms = listOf(
            term(isonTile(), R.string.time_1_by_2),
            term(isonGorgo(), R.string.time_1_by_2),
            equals(),
            term(isonTile(), R.string.time_1),
        ),
        highlight = setOf(Neume.GORGO),
    )

    /** Παρεστιγμένο γοργό: the dot gives the larger share to its side (¾ + ¼ or ¼ + ¾). */
    val presentedGorgo: List<TimeEquation> = listOf(
        TimeEquation(
            terms = listOf(
                term(isonTile(), R.string.time_3_by_4),
                term(isonPresentedGorgo(), R.string.time_1_by_4),
                equals(),
                term(isonTile(), R.string.time_1),
            ),
            highlight = setOf(Neume.PRESENTED_GORGO),
        ),
        TimeEquation(
            terms = listOf(
                term(isonTile(), R.string.time_1_by_4),
                term(isonGorgoPresented(), R.string.time_3_by_4),
                equals(),
                term(isonTile(), R.string.time_1),
            ),
            highlight = setOf(Neume.GORGO_PRESENTED),
        ),
    )

    /** Δίγοργο: three ίσον in one beat (⅓ + ⅓ + ⅓). */
    val digorgo = TimeEquation(
        terms = listOf(
            term(isonTile(), R.string.time_1_by_3),
            term(isonDigorgo(), R.string.time_1_by_3),
            term(isonTile(), R.string.time_1_by_3),
            equals(),
            term(isonTile(), R.string.time_1),
        ),
        highlight = setOf(Neume.DIGORGO),
    )

    /** Παρεστιγμένο δίγοργο: the dot's position decides which of the three notes gets the ½ share. */
    val presentedDigorgo: List<TimeEquation> = listOf(
        TimeEquation(
            terms = listOf(
                term(isonTile(), R.string.time_1_by_2),
                term(isonPresentedDigorgo(Neume.PRESENTED_BOTTOM_DIGORGO, 35), R.string.time_1_by_4),
                term(isonTile(), R.string.time_1_by_4),
                equals(),
                term(isonTile(), R.string.time_1),
            ),
            highlight = setOf(Neume.PRESENTED_BOTTOM_DIGORGO),
        ),
        TimeEquation(
            terms = listOf(
                term(isonTile(), R.string.time_1_by_4),
                term(isonPresentedDigorgo(Neume.PRESENTED_MIDDLE_DIGORGO, 27), R.string.time_1_by_2),
                term(isonTile(), R.string.time_1_by_4),
                equals(),
                term(isonTile(), R.string.time_1),
            ),
            highlight = setOf(Neume.PRESENTED_MIDDLE_DIGORGO),
        ),
        TimeEquation(
            terms = listOf(
                term(isonTile(), R.string.time_1_by_4),
                term(isonPresentedDigorgo(Neume.PRESENTED_TOP_DIGORGO, 35), R.string.time_1_by_4),
                term(isonTile(), R.string.time_1_by_2),
                equals(),
                term(isonTile(), R.string.time_1),
            ),
            highlight = setOf(Neume.PRESENTED_TOP_DIGORGO),
        ),
    )

    /** Τρίγοργο: four notes in one beat (½ + ¼ + ¼ + ¼). */
    val trigorgo = TimeEquation(
        terms = listOf(
            term(isonTile(), R.string.time_1_by_2),
            term(isonTrigorgo(), R.string.time_1_by_4),
            term(isonTile(), R.string.time_1_by_4),
            term(isonTile(), R.string.time_1_by_4),
            equals(),
            term(isonTile(), R.string.time_1),
        ),
        highlight = setOf(Neume.TRIGORGO),
    )

    /** Αργό: read as ίσον + (κεντήματα-γοργό) + (ολίγον-κλάσμα), totalling 2 beats. */
    val argo = TimeEquation(
        terms = listOf(
            term(isonRaised()),
            term(oligonArgo()),
            equals(),
            term(isonRaised(), R.string.time_1_by_2),
            term(embGorgo(), R.string.time_1_by_2),
            term(oligonWith(Neume.FRACTION, 23, dx = 0, dy = 14), R.string.time_2),
        ),
        highlight = setOf(Neume.ARGO),
    )

    /** Δίαργο: like αργό but the closing ολίγον carries διπλή κουκίδα, totalling 3 beats. */
    val diargo = TimeEquation(
        terms = listOf(
            term(isonRaised()),
            term(oligonArgoFamily(Neume.DIARGO, 17, dy = 1)),
            equals(),
            term(isonRaised(), R.string.time_1_by_2),
            term(embGorgo(), R.string.time_1_by_2),
            term(oligonWith(Neume.DOUBLE_DOTS, 15, dx = 8, dy = 19), R.string.time_3),
        ),
        highlight = setOf(Neume.DIARGO),
    )

    /** Τρίαργο: like αργό but the closing ολίγον carries τριπλή κουκίδα, totalling 4 beats. */
    val triargo = TimeEquation(
        terms = listOf(
            term(isonRaised()),
            term(oligonArgoFamily(Neume.TRIARGO, 27, dy = 1)),
            equals(),
            term(isonRaised(), R.string.time_1_by_2),
            term(embGorgo(), R.string.time_1_by_2),
            term(oligonWith(Neume.TRIPLE_DOTS, 25, dx = 6, dy = 19), R.string.time_4),
        ),
        highlight = setOf(Neume.TRIARGO),
    )
}
