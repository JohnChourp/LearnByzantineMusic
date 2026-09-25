package com.johnchourp.learnbyzantinemusic.modes.ui

import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.modes.ApichimaSequence
import com.johnchourp.learnbyzantinemusic.modes.ApichimaSequence.Step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * «Άκου το απήχημα» must sound in every language the app ships, and sound the same φθόγγοι in all
 * of them (ClickUp `869f5x281`).
 *
 * In English the button played nothing, for every ήχος. The player parsed the **displayed** string,
 * and the English one spells the φθόγγοι in Latin letters — `A(Pa) - na(Vou) - nes(Pa)` — which
 * `Phthong.parse` refuses by design, so every απήχημα resolved to nothing. `ApichimaSequenceTest`
 * could not see it: it reads `values/strings.xml` alone, and resolves through the diagram's label
 * list rather than the typed ladder the screen uses.
 *
 * So this test reads the strings.xml of **every** `values` folder that ships the απήχημα strings
 * and, for each row of the screen, does what the screen's `ApichimaPlayer` does:
 * [ApichimaSequence.playable] on the Greek and the shown text, then [ApichimaSequence.frequencies]
 * on the screen's own ladder.
 *
 * The Latin→Greek table below exists here only, to compare what a translation *shows* with what the
 * page *plays*. The app itself never parses a φθόγγος out of translated text (`music/Phthong.kt`).
 */
class ApichimaInEveryLanguageTest {

    private val res: File by lazy {
        listOf(File("app/src/main/res"), File("src/main/res")).firstOrNull { it.isDirectory }
            ?: error("Cannot locate the resources from ${File("").absolutePath}")
    }

    /** R id → resource name, so each row is checked on exactly the strings the screen reads. */
    private val resourceNames: Map<Int, String> by lazy {
        R.string::class.java.fields.associate { it.getInt(null) to it.name }
    }

    private fun nameOf(id: Int): String = resourceNames[id] ?: error("no string resource has id $id")

    /** Every απήχημα string the screen shows: each ήχος's own, and its alternative where it has one. */
    private val apichimaNames: Set<String> by lazy {
        EIGHT_MODES.flatMap { row ->
            listOfNotNull(row.apichimaSyllablesRes, row.apichimaAlternativeSyllablesRes)
        }.map(::nameOf).toSet()
    }

    /**
     * Every `values*` folder whose strings.xml ships the απήχημα, by folder name. `values-night` has
     * no strings.xml, so it drops out here rather than by name.
     */
    private val languages: Map<String, Map<String, String>> by lazy {
        res.listFiles().orEmpty()
            .filter { it.isDirectory && it.name.startsWith("values") }
            .sortedBy { it.name }
            .mapNotNull { folder ->
                val xml = File(folder, "strings.xml").takeIf { it.isFile } ?: return@mapNotNull null
                val strings = stringsOf(xml)
                if (strings.keys.none { it in apichimaNames }) null else folder.name to strings
            }
            .toMap()
    }

    /** What a context configured for Greek resolves to: the default folder, as nothing shadows it. */
    private val greek: Map<String, String> by lazy { languages.getValue("values") }

    private fun stringsOf(file: File): Map<String, String> {
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            .getElementsByTagName("string")
        return (0 until nodes.length)
            .map { nodes.item(it) as Element }
            .associate { it.getAttribute("name") to it.textContent }
    }

    private fun shown(folder: String, strings: Map<String, String>, name: String): String =
        strings[name] ?: error("$folder ships the απήχημα strings but not $name — the page would mix languages")

    /** Test-only: the Latin spellings a translation uses, back to the Greek names the app parses. */
    private val latinToGreek = mapOf(
        "Ni" to "Νη", "Pa" to "Πα", "Vou" to "Βου", "Ga" to "Γα", "Di" to "Δι", "Ke" to "Κε", "Zo" to "Ζω",
    )

    /** Rewrites a translated step's φθόγγος in Greek, keeping its octave mark (`Ni΄` → `Νη΄`). */
    private fun inGreek(step: Step): Step {
        val latin = Regex("""^[A-Za-z]+""").find(step.phthongLabel)?.value ?: return step
        val greekName = latinToGreek[latin] ?: return step
        return step.copy(phthongLabel = greekName + step.phthongLabel.removePrefix(latin))
    }

    // ---- the sweep ---------------------------------------------------------------------------------

    @Test
    fun theSweepCoversEveryLanguageThatShipsTheApichima() {
        // Guards the slice: with Greek alone every assertion below passes — which is exactly how the
        // English bug went unseen.
        assertTrue("found ${languages.keys}", "values" in languages && "values-en" in languages)
        assertFalse("values-night has colours only", "values-night" in languages)
        // The screen reads its pitches through a context configured for Locale el. That resolves to
        // values/ only while no values-el ships these strings; if one ever does, it is the Greek.
        assertTrue("a values-el would shadow values/", languages.keys.none { it.startsWith("values-el") })
        assertTrue("every ήχος has its own απήχημα, found $apichimaNames", apichimaNames.size >= EIGHT_MODES.size)
    }

    // ---- what the page plays -----------------------------------------------------------------------

    @Test
    fun everyLanguagePlaysTheGreekPhthongsOnTheScreensLadder() {
        var checked = 0
        languages.forEach { (folder, strings) ->
            EIGHT_MODES.forEach { row ->
                val name = nameOf(row.apichimaSyllablesRes)
                val where = "$folder/$name"
                val greekText = greek.getValue(name)
                val shownText = shown(folder, strings, name)
                val ladder = row.scale.ladder(octaves = SCALE_OCTAVES)

                // Exactly what ApichimaPlayer does with these two strings.
                val steps = ApichimaSequence.playable(greekText, shownText)
                val tones = ApichimaSequence.frequencies(steps, ladder)

                val greekSteps = ApichimaSequence.parse(greekText)
                assertEquals("$where: not the Greek φθόγγοι", greekSteps.map { it.phthong }, steps.map { it.phthong })
                assertNotNull("$where: the button would stay silent, ${steps.map { it.phthongLabel }}", tones)
                assertEquals("$where: not the Greek pitches", ApichimaSequence.frequencies(greekSteps, ladder), tones)
                assertEquals(
                    "$where: the lit syllables are not the ones on the page",
                    ApichimaSequence.parse(shownText).map { it.syllable },
                    steps.map { it.syllable },
                )
                checked++
            }
        }
        assertTrue("expected Greek and English × 8 ήχοι, checked $checked", checked >= 16)
    }

    @Test
    fun theGreekGivesThePitchesAndTheTranslationTheSyllables() {
        assertEquals(
            listOf(Step("A", "Πα"), Step("na", "Βου"), Step("nes", "Πα")),
            ApichimaSequence.playable("Α(Πα) - να(Βου) - νές(Πα)", "A(Pa) - na(Vou) - nes(Pa)"),
        )
    }

    @Test
    fun aTranslationThatSplitsDifferentlyKeepsTheGreekSyllables() {
        // Pairing three pitches with two syllables would light a syllable other than the one sounding.
        val steps = ApichimaSequence.playable("Α(Πα) - να(Βου) - νές(Πα)", "A(Pa) - nanes(Pa)")
        assertEquals(listOf("Α", "να", "νές"), steps.map { it.syllable })
        assertEquals(listOf("Πα", "Βου", "Πα"), steps.map { it.phthongLabel })
    }

    // ---- what the page shows -----------------------------------------------------------------------

    @Test
    fun everyTranslationShowsTheSamePhthongsThePagePlays() {
        var checked = 0
        languages.forEach { (folder, strings) ->
            EIGHT_MODES.forEach { row ->
                val ladder = row.scale.ladder(octaves = SCALE_OCTAVES)
                // The alternative is shown beside the primary, so it has to agree as well.
                listOfNotNull(row.apichimaSyllablesRes, row.apichimaAlternativeSyllablesRes).forEach { id ->
                    val name = nameOf(id)
                    val where = "$folder/$name"
                    val greekSteps = ApichimaSequence.parse(greek.getValue(name))
                    val shownSteps = ApichimaSequence.parse(shown(folder, strings, name)).map(::inGreek)

                    assertEquals("$where: a different number of steps", greekSteps.size, shownSteps.size)
                    val greekTones = ApichimaSequence.frequencies(greekSteps, ladder)
                    assertNotNull("$where: the Greek does not sound on this ήχος's ladder", greekTones)
                    assertEquals(
                        "$where shows other φθόγγοι than the page plays: ${shownSteps.map { it.phthongLabel }}",
                        greekTones,
                        ApichimaSequence.frequencies(shownSteps, ladder),
                    )
                    checked++
                }
            }
        }
        assertTrue(
            "expected Greek and English × the ${apichimaNames.size} απηχήματα shown, checked $checked",
            checked >= 2 * apichimaNames.size,
        )
    }
}
