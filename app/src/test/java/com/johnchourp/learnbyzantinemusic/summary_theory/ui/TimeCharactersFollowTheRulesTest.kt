package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import com.johnchourp.learnbyzantinemusic.music.Beats
import com.johnchourp.learnbyzantinemusic.music.ByzantineRhythmMapper
import com.johnchourp.learnbyzantinemusic.music.RhythmNote
import com.johnchourp.learnbyzantinemusic.music.RhythmProblem
import com.johnchourp.learnbyzantinemusic.music.ShownBeats
import com.johnchourp.learnbyzantinemusic.music.TimeSign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The «Χαρακτήρες Χρόνου» page says what the time rules do (ClickUp `869f5x29r`, H5).
 *
 * Every beat value on the page is typed by hand, and nothing held it to the rules: the τρίγοργον
 * read «½ + ¼ + ¼ + ¼ = 1 χρόνο», which adds up to 1¼. Here each equation's [TimeEquation.rhythm]
 * goes through [ByzantineRhythmMapper], and the labels must show exactly those lengths — read as the
 * text the reader sees, in every language the page ships. Where the page decomposes a character into
 * simpler signs (the αργόν family, the κλάσμα/κουκίδες table), the simpler signs must give the same
 * lengths too.
 */
class TimeCharactersFollowTheRulesTest {

    private val equations = TimePageEquations.all

    /** Test-only: the glyphs the page's readings use, as the time signs they are. */
    private val signOfGlyph = mapOf(
        Neume.GORGO to TimeSign.GORGON,
        Neume.FRACTION to TimeSign.KLASMA,
        Neume.SIMPLE_DOT to TimeSign.APLI,
        Neume.DOUBLE_DOTS to TimeSign.DIPLI,
        Neume.TRIPLE_DOTS to TimeSign.TRIPLI,
    )

    private fun shown(folder: String, labelRes: Int, where: String): Beats {
        val text = ShownBeats.text(folder, labelRes)
        return ShownBeats.parse(text) ?: error("$where: the label «$text» shows no length")
    }

    @Test
    fun theSweepCoversEveryEquationOnThePage() {
        // Guards the slice: γοργόν 1 + παρεστιγμένο γοργόν 2 + δίγοργον 1 + παρεστιγμένο δίγοργον 3
        // + τρίγοργον 1 + αργόν, δίαργον, τρίαργον 3. An empty map would make every check below pass.
        assertEquals(equations.keys.toString(), 11, equations.size)
        assertTrue("found ${ShownBeats.languages.keys}", listOf("values", "values-en").all { it in ShownBeats.languages })
    }

    @Test
    fun everyEquationIsAMelodyTheRulesAccept() {
        equations.forEach { (name, equation) ->
            assertEquals(name, emptyList<RhythmProblem>(), ByzantineRhythmMapper.problems(equation.rhythm))
        }
    }

    @Test
    fun everyLabelShowsTheLengthTheRulesGive() {
        var checked = 0
        ShownBeats.languages.keys.forEach { folder ->
            equations.forEach { (name, equation) ->
                val where = "$folder, $name"
                val equalsAt = equation.terms.indexOfFirst { it.isEquals }
                assertTrue("$where has no «=»", equalsAt > 0)
                val before = equation.terms.take(equalsAt).filter { it.labelRes != 0 }.map { shown(folder, it.labelRes, where) }
                val after = equation.terms.drop(equalsAt + 1).filter { it.labelRes != 0 }.map { shown(folder, it.labelRes, where) }
                val durations = ByzantineRhythmMapper.durations(equation.rhythm)
                if (before.isNotEmpty()) {
                    // The γοργόν family: one label per written note, then their total after the «=».
                    assertEquals("$where: the notes", durations, before)
                    assertEquals("$where: the total", listOf(ByzantineRhythmMapper.total(equation.rhythm)), after)
                } else {
                    // The αργόν family: the written side is unlabelled, the reading carries one label per note.
                    assertEquals("$where: the notes it is read as", durations, after)
                }
                checked++
            }
        }
        assertTrue("expected every equation in Greek and English, checked $checked", checked >= 2 * 11)
    }

    @Test
    fun theArgonFamilyIsReadAsTheGorgonAndTheDotsItIsDrawnWith() {
        val readings = equations.filterValues { eq -> eq.terms.takeWhile { !it.isEquals }.all { it.labelRes == 0 } }
        assertEquals("αργόν, δίαργον, τρίαργον", setOf("argo", "diargo", "triargo"), readings.keys)
        readings.forEach { (name, equation) ->
            val reading = equation.terms.dropWhile { !it.isEquals }.drop(1).map { term ->
                RhythmNote(term.form!!.glyphs.mapNotNull { signOfGlyph[it.neume] }.toSet())
            }
            assertEquals(name, ByzantineRhythmMapper.durations(equation.rhythm), ByzantineRhythmMapper.durations(reading))
        }
    }

    @Test
    fun theKlasmaAndDotsTableShowsWhatTheyAdd() {
        val rows = TimeCharacters.examples
        assertEquals("κλάσμα, απλή, διπλή, τριπλή", 4, rows.size)
        ShownBeats.languages.keys.forEach { folder ->
            rows.forEach { row ->
                val sign = signOfGlyph.getValue(row.form.glyphs.single().neume)
                val text = ShownBeats.text(folder, row.meaningRes)
                // «1 φθόγγο για N χρόνους»: the last number is how long the note lasts.
                val shownBeats = Regex("""\d+""").findAll(text).last().value.toInt()
                val rule = ByzantineRhythmMapper.durations(listOf(RhythmNote(setOf(sign)))).single()
                assertEquals("$folder, $sign: «$text»", Beats.whole(shownBeats), rule)
            }
        }
    }
}
