package com.johnchourp.learnbyzantinemusic.lessons.ui

import com.johnchourp.learnbyzantinemusic.music.ByzantineRhythmMapper
import com.johnchourp.learnbyzantinemusic.music.RhythmProblem
import com.johnchourp.learnbyzantinemusic.music.ShownBeats
import com.johnchourp.learnbyzantinemusic.music.TimeSign
import com.johnchourp.learnbyzantinemusic.summary_theory.ui.Neume
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The «Συνθέσεις ανάβασης» page times its δίγοργον by the time rules (ClickUp `869f5x29r`, H5).
 *
 * The page's δίγοργον section said the notes read «στο ¼ του χρόνου» — in the legend, the section
 * introduction, the badge and the reading — and its timing strip showed ½ ¼ ¼. The example is written
 * with the plain δίγοργον, the same sign the «Χαρακτήρες Χρόνου» page times at ⅓ each; the rules agree
 * with that page. So the strip is checked against the rules, and every sentence about the δίγοργον
 * must give the rules' share and no other, in every language.
 */
class ClimbingDigorgonTimingTest {

    private val durations = ByzantineRhythmMapper.durations(ClimbingCompositions.digorgoRhythm)

    private val sentences = listOf(
        "climbing_legend_digorgo",
        "digorgo_section_intro",
        "climbing_digorgo_badge",
        "climbing_reading_digorgo_1",
    )

    @Test
    fun theExampleIsAPlainDigorgon() {
        val example = ClimbingCompositions.digorgo.single()
        val drawn = (example.combined + example.parts).flatMap { form -> form.glyphs.map { it.neume } }
        assertTrue("the example is drawn with the δίγοργον", Neume.DIGORGO in drawn)
        val dotted = setOf(Neume.PRESENTED_BOTTOM_DIGORGO, Neume.PRESENTED_MIDDLE_DIGORGO, Neume.PRESENTED_TOP_DIGORGO)
        assertFalse("a dotted δίγοργον would give ½ ¼ ¼", drawn.any { it in dotted })
        // Read as ίσον · κεντήματα with the δίγοργον · ολίγον: three notes, the sign on the middle one.
        assertEquals(example.parts.size, ClimbingCompositions.digorgoRhythm.size)
        assertEquals(setOf(TimeSign.DIGORGON), ClimbingCompositions.digorgoRhythm[1].signs)
        assertEquals(emptyList<RhythmProblem>(), ByzantineRhythmMapper.problems(ClimbingCompositions.digorgoRhythm))
    }

    @Test
    fun theTimingStripShowsWhatTheRulesGive() {
        ShownBeats.languages.keys.forEach { folder ->
            val parts = ClimbingCompositions.digorgoTimingParts.map { ShownBeats.parse(ShownBeats.text(folder, it)) }
            assertEquals("$folder: «Ανάλυση», one value per note", durations, parts)
            // «Σύνθετο»: the ίσον on its own, then κεντήματα + ολίγον, which are written as one character.
            val combined = ClimbingCompositions.digorgoTimingCombined.map { ShownBeats.parse(ShownBeats.text(folder, it)) }
            assertEquals("$folder: «Σύνθετο»", listOf(durations[0], durations[1] + durations[2]), combined)
        }
    }

    @Test
    fun everySentenceAboutTheDigorgonGivesItsShare() {
        val share = ShownBeats.glyphOf(durations.distinct().single())
        val others = ShownBeats.fractionGlyphs - share
        var checked = 0
        ShownBeats.languages.forEach { (folder, strings) ->
            sentences.forEach { name ->
                val text = strings[name] ?: return@forEach
                assertTrue("$folder/$name should say $share: «$text»", share in text)
                assertTrue("$folder/$name gives another share: «$text»", text.none { it in others })
                checked++
            }
        }
        assertTrue("expected the four sentences in Greek and English, checked $checked", checked >= 2 * sentences.size)
    }
}
