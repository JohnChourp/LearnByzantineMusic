package com.johnchourp.learnbyzantinemusic.lessons.ui

import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.junit.Assert.assertEquals
import org.junit.Test

class PhthongScaleTest {

    @Test
    fun octave_has_seven_phthongs_in_scale_order() {
        assertEquals(7, PhthongScale.size)
        assertEquals(
            listOf(
                PhthongName.NI, PhthongName.PA, PhthongName.VOU, PhthongName.GA,
                PhthongName.DI, PhthongName.KE, PhthongName.ZO,
            ),
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
        assertEquals(PhthongName.NI, PhthongScale.phthongAt(0))
        assertEquals(PhthongName.ZO, PhthongScale.phthongAt(6))
        // Up into the next octave: Νη appears again.
        assertEquals(PhthongName.NI, PhthongScale.phthongAt(7))
        assertEquals(PhthongName.PA, PhthongScale.phthongAt(8))
        // Down below Νη: the previous phthong is Ζω.
        assertEquals(PhthongName.ZO, PhthongScale.phthongAt(-1))
        assertEquals(PhthongName.NI, PhthongScale.phthongAt(-7))
        assertEquals(PhthongName.ZO, PhthongScale.phthongAt(-8))
    }

    @Test
    fun every_phthong_derives_from_a_distinct_letter() {
        val letters = PhthongName.entries.map { it.sourceLetter }
        assertEquals(letters.size, letters.toSet().size)
    }
}
