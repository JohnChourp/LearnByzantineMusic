package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import com.johnchourp.learnbyzantinemusic.music.Beats
import com.johnchourp.learnbyzantinemusic.music.ByzantineRhythmMapper
import com.johnchourp.learnbyzantinemusic.music.RhythmProblem
import com.johnchourp.learnbyzantinemusic.music.RhythmTimeline
import com.johnchourp.learnbyzantinemusic.music.ShownBeats
import com.johnchourp.learnbyzantinemusic.music.TimeSign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What «Άκου» plays for every example of the «Χαρακτήρες Χρόνου» page (ClickUp `869f5x25n`, F4): the
 * lengths the time rules give, rests included — never numbers typed into the page or the player.
 *
 * The expected sequences are written out here as fractions of a χρόνος, one per character, so a
 * change to a rule, to an example's rhythm or to how the page turns it into a melody shows up as the
 * character it breaks.
 */
class TimeExamplesPlayTheRulesTest {

    private val half = Beats.of(1, 2)
    private val third = Beats.of(1, 3)
    private val quarter = Beats.of(1, 4)
    private val threeQuarters = Beats.of(3, 4)
    private fun whole(count: Int) = Beats.whole(count)

    /** Every equation of the page, by the name TimeCharacters declares it under. */
    private val equations = TimePageEquations.all

    private val expectedEquations: Map<String, List<Beats>> = mapOf(
        "gorgo" to listOf(half, half),
        "presentedGorgo[0]" to listOf(threeQuarters, quarter),
        "presentedGorgo[1]" to listOf(quarter, threeQuarters),
        "digorgo" to listOf(third, third, third),
        "presentedDigorgo[0]" to listOf(half, quarter, quarter),
        "presentedDigorgo[1]" to listOf(quarter, half, quarter),
        "presentedDigorgo[2]" to listOf(quarter, quarter, half),
        "trigorgo" to listOf(quarter, quarter, quarter, quarter),
        "argo" to listOf(half, half, whole(2)),
        "diargo" to listOf(half, half, whole(3)),
        "triargo" to listOf(half, half, whole(4)),
    )

    /** The κλάσμα / κουκίδες table, then the rests: one note, or one rest, each. */
    private val expectedRows: Map<TimeSign, Beats> = mapOf(
        TimeSign.KLASMA to whole(2),
        TimeSign.APLI to whole(2),
        TimeSign.DIPLI to whole(3),
        TimeSign.TRIPLI to whole(4),
        TimeSign.VAREIA_APLI to whole(1),
        TimeSign.VAREIA_DIPLI to whole(2),
        TimeSign.VAREIA_TRIPLI to whole(3),
    )

    private val rows = TimeCharacters.examples + TimeCharacters.pauses

    private fun sounded(cues: List<RhythmTimeline.Cue>) = cues.filterIsInstance<RhythmTimeline.Sound>().map { it.note }

    private fun silent(cues: List<RhythmTimeline.Cue>) = cues.filterIsInstance<RhythmTimeline.Silence>().map { it.note }

    @Test
    fun everyExampleOfThePageHasItsSequence() {
        // Guards the slice: an example added to the page must be added here, with its lengths.
        assertEquals(expectedEquations.keys, equations.keys)
        assertEquals(7, rows.size)
    }

    @Test
    fun everyCharacterPlaysTheLengthsTheRulesGive() {
        expectedEquations.forEach { (name, lengths) ->
            val equation = equations.getValue(name)
            assertEquals(name, emptyList<RhythmProblem>(), ByzantineRhythmMapper.problems(equation.rhythm))
            assertEquals(name, lengths, ByzantineRhythmMapper.durations(equation.rhythm))
            // Every note sounds: the page's equations have no rest.
            assertEquals(name, lengths.indices.toList(), sounded(TimeExamplePlayback.cues(equation, 80)))
        }
    }

    @Test
    fun theKlasmaAndDotsRowsHoldOneNoteAndTheRestsAreSilent() {
        rows.forEach { row ->
            val sign = checkNotNull(row.form.glyphs.single().neume.timeSign)
            val rhythm = row.rhythm
            assertEquals("$sign", listOf(expectedRows.getValue(sign)), ByzantineRhythmMapper.durations(rhythm))
            val cues = TimeExamplePlayback.cues(row, 80)
            if (sign.isRest) {
                assertEquals("$sign sounds nothing", emptyList<Int>(), sounded(cues))
                assertEquals("$sign is a rest", listOf(0), silent(cues))
            } else {
                assertEquals("$sign sounds its one note", listOf(0), sounded(cues))
                assertEquals(emptyList<Int>(), silent(cues))
            }
            // The clicks go on through a rest: one per χρόνος, silent or not.
            val beats = expectedRows.getValue(sign).ticks / Beats.TICKS_PER_BEAT
            assertEquals("$sign", (0 until beats).toList(), cues.filterIsInstance<RhythmTimeline.Click>().map { it.beat })
        }
    }

    @Test
    fun eachRestSaysItsLengthInEveryLanguage() {
        // «1 χρόνο παύση», «Rest of 1 beat»: the number the page prints is the rest's length.
        var checked = 0
        ShownBeats.languages.keys.forEach { folder ->
            TimeCharacters.pauses.forEach { row ->
                val text = ShownBeats.text(folder, row.meaningRes)
                val shown = Regex("""\d+""").find(text)?.value?.toInt() ?: error("$folder: «$text» names no length")
                assertEquals("$folder: «$text»", ByzantineRhythmMapper.total(row.rhythm), Beats.whole(shown))
                checked++
            }
        }
        assertTrue("expected the three rests in Greek and English, checked $checked", checked >= 6)
    }
}
