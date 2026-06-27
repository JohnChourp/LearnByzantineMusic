package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import com.johnchourp.learnbyzantinemusic.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the phthong→martyria→mode mapping that drives the «Μαρτυρίες» (Testimonies) page so the
 * teaching content stays faithful to the app's own evidence (mode_theory_sign_*_desc + the
 * ModeTheoryCatalog signRes drawables) and the octave row keeps its Νη…Νη΄ shape.
 */
class TestimoniesModelTest {

    @Test
    fun rowWalksOneOctaveOfEightColumns() {
        assertEquals(8, TESTIMONY_ROW.size)
        // Begins on Νη and ends on Νη΄ (the same note one octave higher) — the octave wrap, labelled distinctly.
        assertEquals(R.string.phthong_ni, TESTIMONY_ROW.first().phthongRes)
        assertEquals(R.string.phthong_ni_high, TESTIMONY_ROW.last().phthongRes)
        assertNotEquals(TESTIMONY_ROW.first().phthongRes, TESTIMONY_ROW.last().phthongRes)
        // …and the two Νη use DIFFERENT drawables (intermediate vs filamentous).
        assertNotEquals(TESTIMONY_ROW.first().drawable, TESTIMONY_ROW.last().drawable)
        assertEquals(R.drawable.diatonic_intermediates_testimonial_ni, TESTIMONY_ROW.first().drawable)
        assertEquals(R.drawable.diatonic_filamentous_testimonial_ni, TESTIMONY_ROW.last().drawable)
    }

    @Test
    fun openingNiHasNoModeBadgeAndPaHasTwo() {
        // The opening intermediate Νη has no mode_theory_sign_*_desc backing → no badge.
        assertTrue(TESTIMONY_ROW.first().modes.isEmpty())
        // Πα is the only phthong the app maps to two modes (Α΄ and Β΄).
        val pa = TESTIMONY_ROW.first { it.drawable == R.drawable.diatonic_intermediates_testimonial_pa }
        assertEquals(2, pa.modes.size)
        assertEquals(
            listOf(R.string.mode_first, R.string.mode_second),
            pa.modes.map { it.nameRes },
        )
        // Exactly one column is unbacked and exactly one column carries two badges.
        assertEquals(1, TESTIMONY_ROW.count { it.modes.isEmpty() })
        assertEquals(1, TESTIMONY_ROW.count { it.modes.size == 2 })
    }

    @Test
    fun octaveNiCarriesPlagalFourth() {
        val octaveNi = TESTIMONY_ROW.last()
        assertEquals(R.string.cd_testimonial_ni_high, octaveNi.contentDescRes)
        assertEquals(listOf(R.string.mode_plagal_fourth), octaveNi.modes.map { it.nameRes })
    }

    @Test
    fun everyBadgeIsFullyBackedAndAllEightModesAppearOnce() {
        val descs = mutableListOf<Int>()
        TESTIMONY_ROW.forEach { column ->
            assertNotEquals(0, column.phthongRes)
            assertNotEquals(0, column.drawable)
            assertNotEquals(0, column.contentDescRes)
            column.modes.forEach { mode ->
                assertNotEquals(0, mode.nameRes)
                assertNotEquals(0, mode.descRes)
                descs += mode.descRes
            }
        }
        // All eight modes are represented exactly once across the row (each backed by a distinct desc).
        assertEquals(8, descs.size)
        assertEquals(8, descs.toSet().size)
    }
}
