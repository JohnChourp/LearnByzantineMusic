package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QualitySignsTest {

    @Test
    fun the_eight_quality_signs_are_present_in_on_screen_order() {
        // Mirrors the order rendered by res/layout/layout_quality.xml before the Compose redesign.
        val signs = QualitySigns.all.map { it.neume }
        assertEquals(
            listOf(
                Neume.VAREIA, Neume.YFEN, Neume.SYNECHES_ELAFRON, Neume.PSIFISTON,
                Neume.OMALON, Neume.ANTIKENOMA, Neume.SYNDESMOS, Neume.ENDOFONON,
            ),
            signs,
        )
    }

    @Test
    fun every_sign_is_fully_specified() {
        QualitySigns.all.forEach { s ->
            // The title and the glyph's TalkBack text are the sign table's name for it.
            assertTrue("${s.neume}: missing title", s.neume.nameRes != 0)
            assertEquals("${s.neume}: not a quality sign", NeumeKind.QUALITY, s.neume.kind)
            assertTrue("${s.neume}: missing tag", s.tagRes != 0)
            assertTrue("${s.neume}: empty glyph", s.glyph.glyphs.isNotEmpty())
            assertTrue("${s.neume}: glyph frame height not positive", s.glyph.frameHeight > 0)
            assertTrue("${s.neume}: no definition", s.definitionRes.isNotEmpty())
            assertTrue("${s.neume}: no definition string", s.definitionRes.all { it != 0 })
            assertTrue("${s.neume}: no examples", s.examples.isNotEmpty())
            assertTrue("${s.neume}: empty highlight set", s.highlight.isNotEmpty())
        }
    }

    @Test
    fun every_example_has_a_combined_form_a_reading_and_sane_geometry() {
        QualitySigns.all.forEach { s ->
            s.examples.forEach { e ->
                assertTrue("${s.neume}: example has no combined form", e.combined.isNotEmpty())
                assertTrue("${s.neume}: example has a zero reading", e.readingRes != 0)
                (e.combined + e.parts).forEach { form ->
                    assertTrue("${s.neume}: empty form glyphs", form.glyphs.isNotEmpty())
                    assertTrue("${s.neume}: non-positive frame height", form.frameHeight > 0)
                    form.glyphs.forEach { g ->
                        assertTrue("${s.neume}: non-positive glyph size", g.w > 0 && g.h > 0)
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
                "${s.neume}: none of its highlighted neumes ${s.highlight} appear on the card",
                s.highlight.any { it in glyphs },
            )
        }
    }

    @Test
    fun only_continuous_slight_carries_a_static_counter_example() {
        QualitySigns.all.forEach { s ->
            if (s.neume == Neume.SYNECHES_ELAFRON) {
                assertTrue("συνεχές ελαφρόν: missing counter image", s.counterImageRes != 0)
                assertTrue("συνεχές ελαφρόν: missing counter text", s.counterTextRes != 0)
                assertTrue("συνεχές ελαφρόν: missing counter content description", s.counterCdRes != 0)
            } else {
                assertEquals("${s.neume}: unexpected counter image", 0, s.counterImageRes)
            }
        }
    }

    @Test
    fun the_legend_lists_distinct_base_neumes() {
        val neumes = QualitySigns.legend.map { it.neume }
        assertTrue("legend is empty", neumes.isNotEmpty())
        assertEquals("legend has duplicate neumes", neumes.size, neumes.toSet().size)
        QualitySigns.legend.forEach {
            // Name and TalkBack text come from the sign table: the sign must have a name there.
            assertTrue("legend item ${it.neume} has no name", it.neume.nameRes != 0)
            assertTrue("legend item missing meaning", it.meaningRes != 0)
        }
    }
}
