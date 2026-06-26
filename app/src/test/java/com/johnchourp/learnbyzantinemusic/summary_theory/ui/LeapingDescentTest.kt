package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeapingDescentTest {

    @Test
    fun descents_cover_two_through_twelve_in_order() {
        assertEquals((2..12).toList(), LeapingDescents.all.map { it.voices })
    }

    @Test
    fun every_descent_has_a_form_with_glyphs_and_positive_dimensions() {
        LeapingDescents.all.forEach { descent ->
            val form = descent.form
            assertTrue("empty form in −${descent.voices}", form.glyphs.isNotEmpty())
            assertTrue("non-positive frame height in −${descent.voices}", form.frameHeight > 0)
            form.glyphs.forEach { g ->
                assertTrue("non-positive glyph size in −${descent.voices}", g.w > 0 && g.h > 0)
            }
        }
    }

    @Test
    fun every_glyph_is_one_of_the_four_descending_neumes() {
        val allowed = DescentNeume.entries.toSet()
        LeapingDescents.all.forEach { descent ->
            descent.form.glyphs.forEach { g ->
                assertTrue("unexpected neume in −${descent.voices}", g.neume in allowed)
            }
        }
    }

    @Test
    fun minus_five_stacks_a_chamili_over_an_apostrophos() {
        // −5 = χαμηλή (−4) with an απόστροφος (−1) below it.
        val minus5 = LeapingDescents.all.first { it.voices == 5 }.form
        assertTrue(minus5.glyphs.any { it.neume == DescentNeume.LOW })
        assertTrue(minus5.glyphs.any { it.neume == DescentNeume.APOSTROPHE })
    }

    @Test
    fun the_deep_descents_repeat_the_chamili() {
        // −8 … −12 are written by stacking (at least) two χαμηλή — the doubled "low" is their core.
        LeapingDescents.all.filter { it.voices in 8..12 }.forEach { descent ->
            val lows = descent.form.glyphs.count { it.neume == DescentNeume.LOW }
            assertTrue("−${descent.voices} should stack ≥2 χαμηλή", lows >= 2)
        }
    }

    @Test
    fun stacked_chamili_stay_distinct_by_position_not_glyph() {
        // The −8 form is two χαμηλή; if their alignment/offset were dropped they'd collapse into a
        // single diagram. This guards that the position stays part of the model.
        val minus8 = LeapingDescents.all.first { it.voices == 8 }.form
        val lows = minus8.glyphs.filter { it.neume == DescentNeume.LOW }
        assertEquals(2, lows.size)
        val (a, b) = lows[0] to lows[1]
        assertTrue("stacked χαμηλή must differ in position", a.align != b.align || a.dy != b.dy)
    }

    @Test
    fun there_is_exactly_one_authored_form_per_descent() {
        assertEquals(11, LeapingDescents.all.size)
        assertEquals(11, LeapingDescents.all.map { it.form }.size)
    }
}
