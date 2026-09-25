package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import com.johnchourp.learnbyzantinemusic.lessons.ui.ClimbingCompositions
import com.johnchourp.learnbyzantinemusic.music.ShownBeats
import com.johnchourp.learnbyzantinemusic.music.TimeSign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every theory page draws its signs from one table, [Neume] (ClickUp `869f5x29j`, H4).
 *
 * Before H4 a sign had up to four enums and six per-page maps behind it; the descents even had their
 * own enum, glyph type and renderer for the same απόστροφος and ελαφρόν the other pages drew. Here
 * every sign a page uses is collected from the pages' own models, and the table must be exactly
 * those signs, each with one image, its φωνές or its time value — and code names in one convention.
 */
class OneNeumeTableTest {

    private fun glyphsOf(forms: List<NeumeForm>): List<Neume> = forms.flatMap { form -> form.glyphs.map { it.neume } }

    /** Every sign each page's model uses, by page. */
    private val usedByPage: Map<String, Set<Neume>> by lazy {
        mapOf(
            "Ανιόντες" to (
                AscentCharacter.all.map { it.sign } + glyphsOf(LeapingAscents.all.flatMap { it.forms })
                ).toSet(),
            "Κατιόντες" to (
                DescentCharacter.all.map { it.sign } + glyphsOf(LeapingDescents.all.map { it.form })
                ).toSet(),
            "Συνθέσεις ανάβασης" to (
                ClimbingCompositions.legend.map { it.neume } +
                    glyphsOf(ClimbingCompositions.all.flatMap { it.combined + it.parts })
                ).toSet(),
            "Ποιότητος" to (
                QualitySigns.legend.map { it.neume } +
                    QualitySigns.all.flatMap { sign ->
                        listOf(sign.neume) + sign.highlight + glyphsOf(
                            listOf(sign.glyph) + sign.examples.flatMap { it.combined + it.parts },
                        )
                    }
                ).toSet(),
            "Χαρακτήρες Χρόνου" to (
                TimePageEquations.all.values.flatMap { eq -> eq.highlight + glyphsOf(eq.terms.mapNotNull { it.form }) } +
                    (TimeCharacters.examples + TimeCharacters.pauses).flatMap { row -> glyphsOf(listOf(row.form)) + listOfNotNull(row.names) }
                ).toSet(),
        )
    }

    @Test
    fun theSweepCoversEveryPage() {
        // Guards the slice: a page contributing nothing would make "every sign is in the table" trivial.
        usedByPage.forEach { (page, signs) -> assertTrue("$page uses no sign?", signs.isNotEmpty()) }
        assertEquals(38, Neume.entries.size)
    }

    @Test
    fun theTableIsExactlyTheSignsThePagesDraw() {
        val used = usedByPage.values.flatten().toSet()
        assertEquals("in the table but drawn by no page", emptySet<Neume>(), Neume.entries.toSet() - used)
    }

    @Test
    fun everyImageBelongsToExactlyOneSign() {
        val shared = Neume.entries.groupBy { it.drawable }.filterValues { it.size > 1 }.values
        assertEquals("two entries for one glyph: a second copy of a sign", emptyList<List<Neume>>(), shared.toList())
        assertTrue(Neume.entries.all { it.drawable != 0 && it.width > 0 && it.height > 0 })
    }

    @Test
    fun theSimpleCharactersAreViewsOfTheTable() {
        AscentCharacter.all.forEach { assertEquals(it.name, it.sign.voices, it.voices) }
        DescentCharacter.all.forEach { assertEquals(it.name, it.sign.voices, -it.voices) }
        // Same constants on both sides: the view is named as its sign.
        (AscentCharacter.all.map { it.name to it.sign } + DescentCharacter.all.map { it.name to it.sign })
            .forEach { (view, sign) -> assertEquals(view, sign.name) }
    }

    @Test
    fun theVoicesAddUpInEveryLeapingDescent() {
        // −5 = χαμηλή −4 + απόστροφος −1, −7 = ελαφρόν με απόστροφο −3 + χαμηλή −4, … −12 = three χαμηλές.
        LeapingDescents.all.forEach { leap ->
            val sum = leap.form.glyphs.sumOf { checkNotNull(it.neume.voices) { "${it.neume} has no φωνές" } }
            assertEquals("−${leap.voices}", -leap.voices, sum)
        }
    }

    @Test
    fun onlyQuantitySignsMoveTheVoiceAndOnlyTimeSignsTimeIt() {
        Neume.entries.forEach { sign ->
            assertEquals("$sign: φωνές ⇔ ποσότητος", sign.kind == NeumeKind.QUANTITY, sign.voices != null)
            if (sign.timeSign != null) assertEquals("$sign: a time value on a sign of another kind", NeumeKind.TIME, sign.kind)
        }
    }

    @Test
    fun everyTimeRuleIsDrawnByExactlyOneSign() {
        // The durations live in TimeSign only; the table points at them, one glyph per rule.
        val drawn = Neume.entries.mapNotNull { sign -> sign.timeSign?.let { it to sign } }
        assertEquals(TimeSign.entries.toSet(), drawn.map { it.first }.toSet())
        assertEquals("a time rule drawn by two signs", drawn.size, drawn.map { it.first }.toSet().size)
        // One naming convention across the code: the glyph and its rule share their constant.
        drawn.forEach { (rule, sign) -> assertEquals(rule.name, sign.name) }
    }

    @Test
    fun theCodeNameIsTheTransliterationTheEnglishPageShows() {
        val english = ShownBeats.languages.getValue("values-en")
        val named = Neume.entries.filter { it.nameRes != 0 }
        named.forEach { sign ->
            val shown = english.getValue(ShownBeats.nameOf(sign.nameRes))
            assertEquals("«$shown»", shown.uppercase().replace(' ', '_'), sign.name)
        }
        // A variant or composite is named after the sign it varies: GORGON_DOT_LEFT, VAREIA_APLI …
        val stems = named.map { it.name }
        Neume.entries.filter { it.nameRes == 0 }.forEach { variant ->
            assertTrue("$variant is not «<a named sign>_…»", stems.any { variant.name.startsWith(it + "_") })
        }
    }
}
