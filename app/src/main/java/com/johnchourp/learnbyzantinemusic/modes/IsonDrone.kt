package com.johnchourp.learnbyzantinemusic.modes

import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.ModeLadder
import com.johnchourp.learnbyzantinemusic.music.Phthong
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.music.PhthongName.DI
import com.johnchourp.learnbyzantinemusic.music.PhthongName.GA
import com.johnchourp.learnbyzantinemusic.music.PhthongName.KE
import com.johnchourp.learnbyzantinemusic.music.PhthongName.NI
import com.johnchourp.learnbyzantinemusic.music.PhthongName.PA
import com.johnchourp.learnbyzantinemusic.music.PhthongName.VOU
import com.johnchourp.learnbyzantinemusic.music.PhthongName.ZO
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
        Mode.FIRST -> Phthong(PA)
        Mode.SECOND -> Phthong(DI)
        Mode.THIRD -> Phthong(GA)
        Mode.FOURTH -> Phthong(DI)
        Mode.PLAGAL_FIRST -> Phthong(KE)
        Mode.PLAGAL_SECOND -> Phthong(NI)
        Mode.VARYS -> Phthong(ZO)
        Mode.PLAGAL_FOURTH -> Phthong(NI)
    }

    /**
     * The mode's δεσπόζοντες φθόγγοι, each at the octave the ison sounds it: the heirmologic ones and
     * then the sticheraric ones, each once, in the order the theory page lists them, bracketed last.
     *
     * **The octaves are data, not a rule** (owner decision, 2026-09-25), counted like [base]'s:
     * octave 0 is the middle octave the απήχημα uses. A "nearest rung to the base" rule put the
     * plagal modes' dominants in the wrong place — Πλ.Β΄'s «(Ζω΄)» came out as the Ζω 4 μόρια under
     * its Νη, Πλ.Α΄'s Πα as the Πα΄ above its Κε. The placement follows where each mode's melody
     * lives. In Β΄, Γ΄ and Δ΄ the base sits inside the melodic range, so their dominants lie on both
     * sides of it; Α΄'s lie above its Πα. In Πλ.Β΄ and Πλ.Δ΄ the range rises from the low base, so
     * all of their dominants lie above it. Πλ.Α΄ holds its drone on Κε while its sticheraric range
     * rises from Πα, so its Πα (that lower base) and its Δι lie below Κε and its Νη΄ above; Βαρύς
     * has its Γα and Δι under its Ζω. The written marks are respected: ΄ lies above the base, `,`
     * below it — Πλ.Β΄'s «(Ζω΄)» is the Ζω just under Νη΄, above Δι.
     *
     * `IsonChoicesTest` pins every list, octaves included, and holds the names to the Greek
     * `mode_theory_dominants_*` strings.
     */
    fun dominants(mode: Mode): List<Phthong> = when (mode) {
        Mode.FIRST -> listOf(Phthong(PA), Phthong(DI), Phthong(GA))
        Mode.SECOND -> listOf(Phthong(PA), Phthong(DI), Phthong(VOU), Phthong(ZO))
        Mode.THIRD -> listOf(Phthong(PA), Phthong(GA), Phthong(KE))
        Mode.FOURTH -> listOf(Phthong(VOU), Phthong(DI), Phthong(PA), Phthong(ZO))
        Mode.PLAGAL_FIRST -> listOf(Phthong(KE), Phthong(NI, octave = 1), Phthong(PA), Phthong(DI))
        Mode.PLAGAL_SECOND -> listOf(Phthong(DI), Phthong(VOU), Phthong(PA), Phthong(ZO))
        Mode.VARYS -> listOf(Phthong(GA), Phthong(DI), Phthong(ZO))
        Mode.PLAGAL_FOURTH -> listOf(Phthong(NI), Phthong(VOU), Phthong(DI), Phthong(GA))
    }

    /**
     * The selector's options for [mode] on [ladder], or null when the ladder does not hold the base.
     *
     * The dominants are offered at their typed octaves ([dominants]). Every other φθόγγος is offered
     * at the rung **nearest the base in μόρια**, so the rest of the menu stays in the base's register;
     * a tie — only Βαρύς's Βου, exactly 36 μόρια either way — goes to the lower rung, because an ison
     * sits under the voice. The base shift moves every rung alike, so no octave depends on it.
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

        val dominants = dominants(mode)
            .filter { it != base }
            .mapNotNull { ladder.stepFor(it)?.phthong }
        val taken = dominants.map { it.name }.toSet() + base.name
        val others = PhthongName.entries
            .filter { it !in taken }
            .mapNotNull(::nearestRung)
            .sortedBy { it.moriaFromNi }
            .map { it.phthong }
        return Choices(base, dominants, others)
    }

    /**
     * What the ison should hold: a [mode], that mode's «Μεταφορά βάσης», and the φθόγγος «Ίσον σε…»
     * chose — null for the base (ClickUp `869f5x2dq`). Everything needed to find the pitch, and
     * nothing that could disagree with it: whoever plays the ison — the page, or the service that
     * keeps it going in the background — turns this into a frequency with [held], never by hand.
     */
    data class Request(val mode: Mode, val baseShiftMoria: Int, val choice: Phthong? = null)

    /**
     * The rung [request] holds: [step] on the very ladder the page draws ([ModeLadders]), so the
     * background ison sounds exactly the page's pitch. Null when the ladder does not hold it.
     */
    fun held(request: Request): ModeLadder.Step? =
        step(ModeLadders.ladder(request.mode, request.baseShiftMoria), request.choice ?: base(request.mode))

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
