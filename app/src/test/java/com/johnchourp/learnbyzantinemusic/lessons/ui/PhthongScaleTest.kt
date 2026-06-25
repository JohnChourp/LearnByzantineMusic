package com.johnchourp.learnbyzantinemusic.lessons.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PhthongScaleTest {

    @Test
    fun octave_has_seven_phthongs_in_scale_order() {
        assertEquals(7, PhthongScale.size)
        assertEquals(
            listOf(Phthong.NI, Phthong.PA, Phthong.VOU, Phthong.GA, Phthong.DI, Phthong.KE, Phthong.ZO),
            PhthongScale.octave,
        )
    }

    @Test
    fun byAlphabet_follows_the_first_seven_greek_letters() {
        assertEquals(
            listOf('Α', 'Β', 'Γ', 'Δ', 'Ε', 'Ζ', 'Η'),
            PhthongScale.byAlphabet.map { it.sourceLetter },
        )
    }

    @Test
    fun phthongAt_wraps_across_octaves_in_both_directions() {
        // Within the base octave.
        assertEquals(Phthong.NI, PhthongScale.phthongAt(0))
        assertEquals(Phthong.ZO, PhthongScale.phthongAt(6))
        // Up into the next octave: Νη appears again.
        assertEquals(Phthong.NI, PhthongScale.phthongAt(7))
        assertEquals(Phthong.PA, PhthongScale.phthongAt(8))
        // Down below Νη: the previous phthong is Ζω.
        assertEquals(Phthong.ZO, PhthongScale.phthongAt(-1))
        assertEquals(Phthong.NI, PhthongScale.phthongAt(-7))
        assertEquals(Phthong.ZO, PhthongScale.phthongAt(-8))
    }

    @Test
    fun every_phthong_derives_from_a_distinct_letter() {
        val letters = Phthong.entries.map { it.sourceLetter }
        assertEquals(letters.size, letters.toSet().size)
    }
}
