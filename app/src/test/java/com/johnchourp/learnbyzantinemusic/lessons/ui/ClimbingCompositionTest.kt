package com.johnchourp.learnbyzantinemusic.lessons.ui

import com.johnchourp.learnbyzantinemusic.summary_theory.ui.Neume
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClimbingCompositionTest {

    @Test
    fun sections_have_the_authored_counts() {
        // Transcribed faithfully from res/layout/climbing_compositions.xml: 5 base rows, 5 with a
        // γοργόν, 1 with a δίγοργον.
        assertEquals(5, ClimbingCompositions.simple.size)
        assertEquals(5, ClimbingCompositions.gorgo.size)
        assertEquals(1, ClimbingCompositions.digorgo.size)
        assertEquals(11, ClimbingCompositions.all.size)
    }

    @Test
    fun every_composition_has_a_combined_form_parts_and_a_reading() {
        ClimbingCompositions.all.forEach { c ->
            assertTrue("a composition has no combined form", c.combined.isNotEmpty())
            assertTrue("a composition has no parts", c.parts.isNotEmpty())
            assertTrue("a composition has a zero reading", c.readingRes != 0)
            (c.combined + c.parts).forEach { form ->
                assertTrue("empty form glyphs", form.glyphs.isNotEmpty())
                assertTrue("non-positive frame height", form.frameHeight > 0)
                form.glyphs.forEach { g ->
                    assertTrue("non-positive glyph size", g.w > 0 && g.h > 0)
                }
            }
        }
    }

    @Test
    fun readings_are_unique_per_composition() {
        val readings = ClimbingCompositions.all.map { it.readingRes }
        assertEquals("readings should be distinct", readings.size, readings.toSet().size)
    }

    @Test
    fun gorgo_section_is_marked_and_actually_contains_a_gorgo_glyph() {
        ClimbingCompositions.gorgo.forEach { c ->
            assertEquals(Emphasis.GORGON, c.emphasis)
            val hasGorgo = (c.combined + c.parts).any { f -> f.glyphs.any { it.neume == Neume.GORGO } }
            assertTrue("a γοργόν composition is missing its GORGO glyph", hasGorgo)
        }
    }

    @Test
    fun digorgo_section_is_marked_and_actually_contains_a_digorgo_glyph() {
        ClimbingCompositions.digorgo.forEach { c ->
            assertEquals(Emphasis.DIGORGON, c.emphasis)
            val hasDigorgo = (c.combined + c.parts).any { f -> f.glyphs.any { it.neume == Neume.DIGORGO } }
            assertTrue("the δίγοργον composition is missing its DIGORGO glyph", hasDigorgo)
        }
    }

    @Test
    fun base_compositions_carry_no_timing_emphasis() {
        ClimbingCompositions.simple.forEach { c ->
            assertEquals(Emphasis.NONE, c.emphasis)
        }
    }
}
