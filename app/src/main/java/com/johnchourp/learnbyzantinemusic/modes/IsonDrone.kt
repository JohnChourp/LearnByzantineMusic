package com.johnchourp.learnbyzantinemusic.modes

import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.ModeLadder
import com.johnchourp.learnbyzantinemusic.music.Phthong
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import kotlin.math.abs

/**
 * Which φθόγγος the ισοκράτημα holds, and at what frequency (ClickUp `869f4tpeu`, `869f5x251`).
 *
 * Byzantine music is chanted **over** a drone, so practising a scale without one trains the wrong
 * thing: the ear never learns the interval against the base. The 8 Ήχοι page could only sound a
 * pitch while a finger held it down, which is exactly the one situation where you cannot also sing.
 *
 * ## Why this is a lookup and not a second calculation
 *
 * The drone must sound the *same* pitch as the diagram's key — including after the «Μεταφορά
 * βάσης» slider moves it. The safe way to guarantee that is not to recompute the frequency from the
 * μόρια a second time, but to **index into the ladder the diagram itself is drawn from** ([step]).
 * Then there is no arithmetic that can disagree, however the transposition is applied later.
 *
 * ## Where the ison starts, and where it can go
 *
 * It starts on the mode's [base] — the last φθόγγος of its απήχημα, at the pitch the page's απήχημα
 * sounds it — and «Ίσον σε…» moves it to any other φθόγγος: the mode's [dominants] first, then the
 * rest ([choices]). Everything here is typed data; nothing is parsed out of displayed text.
 */
object IsonDrone {

    /**
     * What «Ίσον σε…» offers for one mode: the [base], the mode's [dominants] without it, then the
     * [others] from low to high. Every φθόγγος appears exactly once, each at one octave.
     */
    data class Choices(
        val base: Phthong,
        val dominants: List<Phthong>,
        val others: List<Phthong>,
    ) {
        /** Everything the ison can hold, base first. */
        val all: List<Phthong> get() = listOf(base) + dominants + others
    }

    /**
     * The φθόγγος the ison starts on and returns to with «Επαναφορά στη βάση».
     *
     * **Owner decision, 2026-09-25: the end of the mode's απήχημα**, same φθόγγος and same octave as
     * the page's απήχημα plays it. Until then the drone held the scale's *construction* base (Πα, or
     * Νη for the Νη-based scales), which is where six of the eight modes do not rest: Β΄ moved from Πα
     * to Δι, Γ΄ from Νη to Γα, Δ΄ from Πα to Δι, Πλ.Α΄ from Πα to Κε, Βαρύς from Νη to Ζω and Πλ.Δ΄
     * from Πα to Νη. `IsonChoicesTest` pins every entry to the last step of the Greek απήχημα.
     */
    fun base(mode: Mode): Phthong = when (mode) {
        Mode.FIRST -> Phthong(PhthongName.PA)
        Mode.SECOND -> Phthong(PhthongName.DI)
        Mode.THIRD -> Phthong(PhthongName.GA)
        Mode.FOURTH -> Phthong(PhthongName.DI)
        Mode.PLAGAL_FIRST -> Phthong(PhthongName.KE)
        Mode.PLAGAL_SECOND -> Phthong(PhthongName.NI)
        Mode.VARYS -> Phthong(PhthongName.ZO)
        Mode.PLAGAL_FOURTH -> Phthong(PhthongName.NI)
    }

    /**
     * The mode's δεσπόζοντες φθόγγοι: the heirmologic ones and then the sticheraric ones, each once,
     * in the order the theory page lists them, with the bracketed ones last. Names only — the octave
     * each is sounded at is chosen on the ladder by [choices]. `IsonChoicesTest` pins these to the
     * Greek `mode_theory_dominants_*` strings.
     */
    fun dominants(mode: Mode): List<PhthongName> = when (mode) {
        Mode.FIRST -> listOf(PhthongName.PA, PhthongName.DI, PhthongName.GA)
        Mode.SECOND -> listOf(PhthongName.PA, PhthongName.DI, PhthongName.VOU, PhthongName.ZO)
        Mode.THIRD -> listOf(PhthongName.PA, PhthongName.GA, PhthongName.KE)
        Mode.FOURTH -> listOf(PhthongName.VOU, PhthongName.DI, PhthongName.PA, PhthongName.ZO)
        Mode.PLAGAL_FIRST -> listOf(PhthongName.KE, PhthongName.NI, PhthongName.PA, PhthongName.DI)
        Mode.PLAGAL_SECOND -> listOf(PhthongName.DI, PhthongName.VOU, PhthongName.PA, PhthongName.ZO)
        Mode.VARYS -> listOf(PhthongName.GA, PhthongName.DI, PhthongName.ZO)
        Mode.PLAGAL_FOURTH -> listOf(PhthongName.NI, PhthongName.VOU, PhthongName.DI, PhthongName.GA)
    }

    /**
     * The selector's options for [mode] on [ladder], or null when the ladder does not hold the base.
     *
     * Each φθόγγος is offered at the rung **nearest the base in μόρια** (owner decision), so the ison
     * never leaves the base's register: Πλ.Α΄'s «Νη΄» is the Νη just above Κε, not the one an octave
     * lower. A tie — only Βαρύς's Βου, exactly 36 μόρια either way — goes to the lower rung, because
     * an ison sits under the voice. The base shift moves every rung alike, so the choice of octave
     * does not depend on it.
     */
    fun choices(mode: Mode, ladder: ModeLadder): Choices? {
        val base = base(mode)
        val baseStep = ladder.stepFor(base) ?: return null
        val nearest = compareBy<ModeLadder.Step>(
            { abs((it.moriaFromNi - baseStep.moriaFromNi).value) },
            { it.moriaFromNi },
        )

        fun nearestRung(name: PhthongName): ModeLadder.Step? =
            ladder.steps.filter { it.phthong.name == name }.minWithOrNull(nearest)

        val dominantNames = dominants(mode)
        val dominants = dominantNames
            .filter { it != base.name }
            .mapNotNull { nearestRung(it)?.phthong }
        val others = PhthongName.entries
            .filter { it != base.name && it !in dominantNames }
            .mapNotNull(::nearestRung)
            .sortedBy { it.moriaFromNi }
            .map { it.phthong }
        return Choices(base, dominants, others)
    }

    /**
     * The rung the drone holds for [phthong] on [ladder], or null when the ladder does not contain
     * it — in which case the caller must not start a drone rather than guess.
     *
     * This is the typed primary. It matches **by φθόγγος value, octave included**, so it cannot pick
     * the same name an octave away; the label-based function below is the boundary for callers that
     * still hold display text.
     */
    fun step(ladder: ModeLadder, phthong: Phthong): ModeLadder.Step? = ladder.stepFor(phthong)

    /**
     * Index of the mode's base φθόγγος in a **top-to-bottom** label list, or `-1` when absent.
     *
     * Matches the bare label exactly, so the octave decorations (`,` and `΄`) pick out the middle
     * octave on their own — no suffix stripping, which would match all four occurrences.
     */
    fun baseLabelIndex(phthongsTopToBottom: List<String>, basePhthong: String): Int =
        phthongsTopToBottom.indexOf(basePhthong)

    /**
     * Frequency the drone should hold, taken from [frequenciesTopToBottom] — the very list the
     * diagram sounds — or `null` when the base is not present, in which case the caller must not
     * start a drone rather than guess a pitch.
     */
    fun frequencyHz(
        phthongsTopToBottom: List<String>,
        frequenciesTopToBottom: List<Double>,
        basePhthong: String,
    ): Double? {
        val index = baseLabelIndex(phthongsTopToBottom, basePhthong)
        if (index !in frequenciesTopToBottom.indices) return null
        return frequenciesTopToBottom[index]
    }
}
