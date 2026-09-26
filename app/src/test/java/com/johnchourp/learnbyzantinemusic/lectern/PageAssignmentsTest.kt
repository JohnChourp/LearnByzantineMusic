package com.johnchourp.learnbyzantinemusic.lectern

import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.Phthong
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The lectern's page → ήχος map (ClickUp `869f5x2e7`): a page uses the most recent assignment at or
 * before it, and every edit lands on the page the reader is on.
 */
class PageAssignmentsTest {

    private val first = PageAssignment(pageIndex = 0, mode = Mode.FIRST, baseShiftMoria = 0)
    private val plagalFirst = PageAssignment(pageIndex = 4, mode = Mode.PLAGAL_FIRST, baseShiftMoria = -3)
    private val varys = PageAssignment(pageIndex = 9, mode = Mode.VARYS, baseShiftMoria = 5, isonChoice = Phthong(PhthongName.DI))
    private val book = listOf(first, plagalFirst, varys)

    @Test
    fun `a page uses the most recent assignment at or before it`() {
        val expected = mapOf(
            0 to first, 1 to first, 3 to first,
            4 to plagalFirst, 5 to plagalFirst, 8 to plagalFirst,
            9 to varys, 10 to varys, 500 to varys,
        )
        expected.forEach { (page, assignment) -> assertEquals("page $page", assignment, PageAssignments.resolve(book, page)) }
    }

    @Test
    fun `the order the list is in does not change the answer`() {
        (0..12).forEach { page ->
            assertEquals("page $page", PageAssignments.resolve(book, page), PageAssignments.resolve(book.reversed(), page))
        }
    }

    @Test
    fun `a page before the first assignment has none, and neither has an empty map`() {
        val fromPageThree = listOf(plagalFirst.copy(pageIndex = 3))
        assertNull(PageAssignments.resolve(fromPageThree, 0))
        assertNull(PageAssignments.resolve(fromPageThree, 2))
        assertEquals(fromPageThree.single(), PageAssignments.resolve(fromPageThree, 3))
        assertNull(PageAssignments.resolve(emptyList(), 0))
    }

    @Test
    fun `assigning replaces that page only and keeps the list sorted`() {
        val replaced = PageAssignments.assign(book, PageAssignment(4, Mode.SECOND, 1))
        assertEquals(listOf(0, 4, 9), replaced.map { it.pageIndex })
        assertEquals(Mode.SECOND, PageAssignments.resolve(replaced, 6)?.mode)
        assertEquals(first, replaced[0])
        assertEquals(varys, replaced[2])

        val inserted = PageAssignments.assign(book, PageAssignment(6, Mode.THIRD, 0))
        assertEquals(listOf(0, 4, 6, 9), inserted.map { it.pageIndex })
    }

    @Test
    fun `clearing a page makes it follow the pages before it again`() {
        val cleared = PageAssignments.clear(book, 4)
        assertEquals(first, PageAssignments.resolve(cleared, 4))
        assertEquals(first, PageAssignments.resolve(cleared, 8))
        assertEquals(varys, PageAssignments.resolve(cleared, 9))
        assertFalse(PageAssignments.isSetOn(cleared, 4))
        assertTrue(PageAssignments.isSetOn(book, 4))
        assertFalse("a page that follows is not set on", PageAssignments.isSetOn(book, 5))
        assertEquals("clearing a page without its own setting changes nothing", book, PageAssignments.clear(book, 5))
    }

    @Test
    fun `a new ήχος starts on its base at the shift the user keeps for it`() {
        val chosen = PageAssignments.chooseMode(book, 6, Mode.SECOND, shiftForMode = 7)
        assertEquals(PageAssignment(6, Mode.SECOND, 7, null), PageAssignments.resolve(chosen, 6))
        // The pages before it are untouched; the pages after it follow it up to the next setting.
        assertEquals(plagalFirst, PageAssignments.resolve(chosen, 5))
        assertEquals(Mode.SECOND, PageAssignments.resolve(chosen, 8)?.mode)
        assertEquals(varys, PageAssignments.resolve(chosen, 9))
    }

    @Test
    fun `choosing the ήχος that already holds keeps its shift and φθόγγος on this page`() {
        val chosen = PageAssignments.chooseMode(book, 11, Mode.VARYS, shiftForMode = -12)
        assertEquals(varys.copy(pageIndex = 11), PageAssignments.resolve(chosen, 11))
    }

    @Test
    fun `the shift starting a ήχος is brought inside the shared range`() {
        val chosen = PageAssignments.chooseMode(emptyList(), 0, Mode.FIRST, shiftForMode = BaseShift.MAX_MORIA + 40)
        assertEquals(BaseShift.MAX_MORIA, chosen.single().baseShiftMoria)
    }

    @Test
    fun `moving the shift on a later page starts a new setting there and leaves the earlier pages alone`() {
        val moved = PageAssignments.chooseShift(book, 7, 2)
        assertEquals(PageAssignment(7, Mode.PLAGAL_FIRST, 2, null), PageAssignments.resolve(moved, 7))
        assertEquals(plagalFirst, PageAssignments.resolve(moved, 6))
        assertEquals(BaseShift.MIN_MORIA, PageAssignments.resolve(PageAssignments.chooseShift(book, 7, -99), 7)?.baseShiftMoria)
        assertEquals("no ήχος holds before page 0 of an empty map", emptyList<PageAssignment>(), PageAssignments.chooseShift(emptyList(), 0, 3))
    }

    @Test
    fun `the ison φθόγγος is kept only when the ήχος offers it, and the base is no choice`() {
        // Πλ.Α΄: base Κε; Πα is one of its δεσπόζοντες.
        val pa = PageAssignments.chooseIson(book, 4, Phthong(PhthongName.PA))
        assertEquals(Phthong(PhthongName.PA), PageAssignments.resolve(pa, 4)?.isonChoice)

        val base = PageAssignments.chooseIson(book, 4, Phthong(PhthongName.KE))
        assertNull("the base is asked for as «no choice»", PageAssignments.resolve(base, 4)?.isonChoice)

        val offLadder = PageAssignments.chooseIson(book, 4, Phthong(PhthongName.PA, octave = 5))
        assertNull("a φθόγγος the ladder does not hold is the base", PageAssignments.resolve(offLadder, 4)?.isonChoice)
        assertEquals(Mode.PLAGAL_FIRST, PageAssignments.resolve(offLadder, 4)?.mode)
    }
}
