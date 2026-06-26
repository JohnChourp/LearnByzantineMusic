package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeapingAscentTest {

    @Test
    fun leaps_cover_two_through_fourteen_in_order() {
        assertEquals((2..14).toList(), LeapingAscents.all.map { it.voices })
    }

    @Test
    fun every_leap_has_at_least_one_form_and_every_form_has_glyphs() {
        LeapingAscents.all.forEach { leap ->
            assertTrue("leap +${leap.voices} has no forms", leap.forms.isNotEmpty())
            leap.forms.forEach { form ->
                assertTrue("empty form in +${leap.voices}", form.glyphs.isNotEmpty())
                assertTrue("non-positive frame height in +${leap.voices}", form.frameHeight > 0)
                form.glyphs.forEach { g ->
                    assertTrue("non-positive glyph size in +${leap.voices}", g.w > 0 && g.h > 0)
                }
            }
        }
    }

    @Test
    fun every_form_is_built_on_an_oligon_or_petasti_base() {
        LeapingAscents.all.forEach { leap ->
            leap.forms.forEach { form ->
                val hasBase = form.glyphs.any { it.neume == Neume.OLIGON || it.neume == Neume.FLYER }
                assertTrue("+${leap.voices} form lacks a base character", hasBase)
            }
        }
    }

    @Test
    fun plus_four_and_plus_five_differ_only_by_position_not_glyphs() {
        // Both leaps use the same neumes (base + a single υψηλή); the rise is encoded by *where*
        // the υψηλή sits (right for +4, left for +5). If the offsets were dropped they'd collapse
        // into the same diagram — this guards that the position stays part of the model.
        val plus4 = LeapingAscents.all.first { it.voices == 4 }
        val plus5 = LeapingAscents.all.first { it.voices == 5 }
        fun highDxOf(leap: LeapingAscent) =
            leap.forms.first().glyphs.first { it.neume == Neume.HIGH }.dx
        assertNotEquals(highDxOf(plus4), highDxOf(plus5))
    }

    @Test
    fun all_authored_forms_are_present() {
        // +2 keeps its three authored spellings; every other leap has the Ολίγον + Πεταστή pair.
        assertEquals(3, LeapingAscents.all.first { it.voices == 2 }.forms.size)
        LeapingAscents.all.filter { it.voices != 2 }.forEach {
            assertEquals("+${it.voices} should have two forms", 2, it.forms.size)
        }
        val totalForms = LeapingAscents.all.sumOf { it.forms.size }
        assertEquals(3 + 12 * 2, totalForms)
    }
}
