package com.johnchourp.learnbyzantinemusic.lectern

import com.johnchourp.learnbyzantinemusic.modes.IsonDrone
import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.Phthong

/**
 * What the lectern's ison bar holds from [pageIndex] on (ClickUp `869f5x2e7`): a ήχος, its «Μεταφορά
 * βάσης» and the φθόγγος «Ίσον σε…» chose — until a later page says otherwise.
 *
 * [pageIndex] counts from 0, as `PdfRenderer` does; the screen shows it + 1. [isonChoice] null is the
 * mode's base ([IsonDrone.base]), asked for as «no choice» exactly as the 8 Ήχοι page asks for it.
 * [baseShiftMoria] is the page's **own** shift, as a mode's own is on the 8 Ήχοι page.
 *
 * **No second pitch calculation.** [request] is the one thing the bar sounds, and only [LecternIson] —
 * the 8 Ήχοι page's own lookup, ladder and choices — turns it into a pitch.
 */
data class PageAssignment(
    val pageIndex: Int,
    val mode: Mode,
    val baseShiftMoria: Int,
    val isonChoice: Phthong? = null,
) {
    /**
     * The ison this assignment sounds for a voice whose global shift («Βρες τη φωνή σου», ClickUp
     * `869f5x2dd`) is [globalShiftMoria]: the page's own shift plus the global one, added and clamped
     * by [BaseShift.combined] at the point of use — exactly how the 8 Ήχοι page builds every ladder.
     * Neither value is ever written into the other.
     */
    fun request(globalShiftMoria: Int): IsonDrone.Request =
        IsonDrone.Request(mode, BaseShift.combined(baseShiftMoria, globalShiftMoria), isonChoice)
}

/**
 * The page → ήχος map of one PDF, as plain rules (ClickUp `869f5x2e7`).
 *
 * A chanter sets the ήχος where a hymn starts, not on every page: a page uses **the most recent
 * assignment at or before it** ([resolve]), so setting page 5 covers pages 5, 6, 7… until the next one.
 * A page before the first assignment has none, and the bar asks for a ήχος there.
 *
 * Every edit is written on the page the reader is on, starting from what holds there — its own
 * setting, or the one it follows — so changing the shift on page 9 of a hymn set on page 5 makes page 9
 * the start of the new setting and leaves pages 5–8 as they were. Lists stay sorted by page, with at
 * most one assignment per page; every function returns a new list.
 */
object PageAssignments {

    /** The assignment that holds on [pageIndex]: the latest one at or before it, or null. */
    fun resolve(assignments: List<PageAssignment>, pageIndex: Int): PageAssignment? =
        assignments.filter { it.pageIndex <= pageIndex }.maxByOrNull { it.pageIndex }

    /** True when [pageIndex] carries its own assignment rather than following an earlier page's. */
    fun isSetOn(assignments: List<PageAssignment>, pageIndex: Int): Boolean =
        assignments.any { it.pageIndex == pageIndex }

    /** [assignments] with [assignment] on its page, replacing whatever that page had. */
    fun assign(assignments: List<PageAssignment>, assignment: PageAssignment): List<PageAssignment> =
        (assignments.filterNot { it.pageIndex == assignment.pageIndex } + assignment).sortedBy { it.pageIndex }

    /** [assignments] without [pageIndex]'s own: that page follows the pages before it again. */
    fun clear(assignments: List<PageAssignment>, pageIndex: Int): List<PageAssignment> =
        assignments.filterNot { it.pageIndex == pageIndex }

    /**
     * [mode] chosen on [pageIndex]. A new ήχος starts on its own base (as the 8 Ήχοι page's mode picker
     * does) at [shiftForMode] — the «Μεταφορά βάσης» the user keeps for that ήχος on the 8 Ήχοι page.
     * The ήχος that already holds there keeps its shift and φθόγγος.
     */
    fun chooseMode(
        assignments: List<PageAssignment>,
        pageIndex: Int,
        mode: Mode,
        shiftForMode: Int,
    ): List<PageAssignment> {
        val holding = resolve(assignments, pageIndex)
        val chosen = if (holding?.mode == mode) {
            holding.copy(pageIndex = pageIndex)
        } else {
            PageAssignment(pageIndex, mode, BaseShift.clamp(shiftForMode))
        }
        return assign(assignments, chosen)
    }

    /** The shift moved on [pageIndex], clamped to [BaseShift]; unchanged when no ήχος holds there yet. */
    fun chooseShift(assignments: List<PageAssignment>, pageIndex: Int, shiftMoria: Int): List<PageAssignment> {
        val holding = resolve(assignments, pageIndex) ?: return assignments
        return assign(assignments, holding.copy(pageIndex = pageIndex, baseShiftMoria = BaseShift.clamp(shiftMoria)))
    }

    /**
     * «Ίσον σε…» on [pageIndex]: [choice], or null for the base. A choice the ήχος does not offer is
     * the base — the same normalisation the stored form applies ([normalizedChoice]).
     */
    fun chooseIson(assignments: List<PageAssignment>, pageIndex: Int, choice: Phthong?): List<PageAssignment> {
        val holding = resolve(assignments, pageIndex) ?: return assignments
        val normalized = normalizedChoice(holding.mode, holding.baseShiftMoria, choice)
        return assign(assignments, holding.copy(pageIndex = pageIndex, isonChoice = normalized))
    }

    /**
     * [choice] as the app keeps it: null for the base — «no choice», as the 8 Ήχοι page asks for it —
     * and null for anything «Ίσον σε…» does not offer for [mode], so no stored value can name a φθόγγος
     * the ladder cannot sound.
     */
    fun normalizedChoice(mode: Mode, baseShiftMoria: Int, choice: Phthong?): Phthong? {
        if (choice == null) return null
        val choices = LecternIson.choices(IsonDrone.Request(mode, baseShiftMoria)) ?: return null
        return choice.takeIf { it != choices.base && it in choices.all }
    }
}
