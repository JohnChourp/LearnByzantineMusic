package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QualitySignsTest {

    @Test
    fun the_eight_quality_signs_are_present_in_on_screen_order() {
        // Mirrors the order rendered by res/layout/layout_quality.xml before the Compose redesign.
        val ids = QualitySigns.all.map { it.id }
        assertEquals(
            listOf("heavy", "yfen", "continuous_slight", "digital", "all_right", "vaccum", "link", "intercom"),
            ids,
        )
    }

    @Test
    fun every_sign_is_fully_specified() {
        QualitySigns.all.forEach { s ->
            assertTrue("${s.id}: missing title", s.titleRes != 0)
            assertTrue("${s.id}: missing tag", s.tagRes != 0)
            assertTrue("${s.id}: missing glyph content description", s.glyphCdRes != 0)
            assertTrue("${s.id}: empty glyph", s.glyph.glyphs.isNotEmpty())
            assertTrue("${s.id}: glyph frame height not positive", s.glyph.frameHeight > 0)
            assertTrue("${s.id}: no definition", s.definitionRes.isNotEmpty())
            assertTrue("${s.id}: no definition string", s.definitionRes.all { it != 0 })
            assertTrue("${s.id}: no examples", s.examples.isNotEmpty())
            assertTrue("${s.id}: empty highlight set", s.highlight.isNotEmpty())
        }
    }

    @Test
    fun every_example_has_a_combined_form_a_reading_and_sane_geometry() {
        QualitySigns.all.forEach { s ->
            s.examples.forEach { e ->
                assertTrue("${s.id}: example has no combined form", e.combined.isNotEmpty())
                assertTrue("${s.id}: example has a zero reading", e.readingRes != 0)
                (e.combined + e.parts).forEach { form ->
                    assertTrue("${s.id}: empty form glyphs", form.glyphs.isNotEmpty())
                    assertTrue("${s.id}: non-positive frame height", form.frameHeight > 0)
                    form.glyphs.forEach { g ->
                        assertTrue("${s.id}: non-positive glyph size", g.w > 0 && g.h > 0)
                    }
                }
            }
        }
    }

    @Test
    fun each_sign_highlight_glyph_actually_appears_on_its_card() {
        // The crimson highlight is meaningless if the sign's own glyph never shows up.
        QualitySigns.all.forEach { s ->
            val glyphs = (s.glyph.glyphs + s.examples.flatMap { (it.combined + it.parts).flatMap { f -> f.glyphs } })
                .map { it.neume }
                .toSet()
            assertTrue(
                "${s.id}: none of its highlighted neumes ${s.highlight} appear on the card",
                s.highlight.any { it in glyphs },
            )
        }
    }

    @Test
    fun only_continuous_slight_carries_a_static_counter_example() {
        QualitySigns.all.forEach { s ->
            if (s.id == "continuous_slight") {
                assertTrue("continuous_slight: missing counter image", s.counterImageRes != 0)
                assertTrue("continuous_slight: missing counter text", s.counterTextRes != 0)
                assertTrue("continuous_slight: missing counter content description", s.counterCdRes != 0)
            } else {
                assertEquals("${s.id}: unexpected counter image", 0, s.counterImageRes)
            }
        }
    }

    @Test
    fun the_legend_lists_distinct_base_neumes() {
        val neumes = QualitySigns.legend.map { it.neume }
        assertTrue("legend is empty", neumes.isNotEmpty())
        assertEquals("legend has duplicate neumes", neumes.size, neumes.toSet().size)
        QualitySigns.legend.forEach {
            assertTrue("legend item missing name", it.nameRes != 0)
            assertTrue("legend item missing meaning", it.meaningRes != 0)
            assertTrue("legend item missing content description", it.cdRes != 0)
        }
    }
}
