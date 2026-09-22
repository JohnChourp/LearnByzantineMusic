package com.johnchourp.learnbyzantinemusic.modes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * ClickUp `869f4tpkv` (A6). The risk here is not the parser — it is whether every mode's απήχημα can
 * actually be sounded on *that mode's* ladder. A φθόγγος that the scale does not contain would make
 * the button silently skip a syllable, and the highlight would drift out of step with the audio.
 *
 * So the important test reads the **real shipped strings** for all eight modes, including the
 * alternative απηχήματα, and resolves them against the real scale definitions.
 */
class ApichimaSequenceTest {

    private val octaves = 3
    private val reference = "Νη"

    // ---- parsing ---------------------------------------------------------------------------------

    @Test
    fun parsesSyllableAndPhthongPairs() {
        assertEquals(
            listOf(
                ApichimaSequence.Step("Α", "Πα"),
                ApichimaSequence.Step("να", "Βου"),
                ApichimaSequence.Step("νές", "Πα"),
            ),
            ApichimaSequence.parse("Α(Πα) - να(Βου) - νές(Πα)")
        )
    }

    @Test
    fun keepsOctaveDecorationBecauseItSelectsThePitch() {
        val steps = ApichimaSequence.parse("Α(Ζω) - α(Νη΄) - νές(Ζω)")
        assertEquals(listOf("Ζω", "Νη΄", "Ζω"), steps.map { it.phthongLabel })
    }

    @Test
    fun skipsSegmentsWithoutAPhthongInsteadOfGuessingOne() {
        val steps = ApichimaSequence.parse("Α(Πα) - να - νές(Πα)")
        assertEquals(listOf("Α", "νές"), steps.map { it.syllable })
    }

    @Test
    fun emptyInputYieldsNoSteps() {
        assertTrue(ApichimaSequence.parse("").isEmpty())
        assertTrue(ApichimaSequence.parse("   ").isEmpty())
    }

    // ---- resolution -------------------------------------------------------------------------------

    @Test
    fun aStepOffTheLadderRefusesTheWholeSequence() {
        // All-or-nothing: a sequence with a hole teaches the απήχημα wrong, so the button must stay
        // disabled rather than play an incomplete phrase.
        val steps = listOf(ApichimaSequence.Step("Α", "Πα"), ApichimaSequence.Step("να", "Ωμέγα"))
        assertNull(ApichimaSequence.frequencies(steps, listOf("Πα", "Βου"), listOf(220.0, 240.0)))
    }

    @Test
    fun noStepsMeansNothingToPlay() {
        assertNull(ApichimaSequence.frequencies(emptyList(), listOf("Πα"), listOf(220.0)))
    }

    @Test
    fun frequenciesComeFromTheDiagramsListInOrder() {
        val steps = ApichimaSequence.parse("Α(Πα) - να(Βου) - νές(Πα)")
        val labels = listOf("Βου", "Πα")
        val freqs = listOf(300.0, 200.0)
        assertEquals(listOf(200.0, 300.0, 200.0), ApichimaSequence.frequencies(steps, labels, freqs))
    }

    // ---- the real data ----------------------------------------------------------------------------

    private fun stringsXml(): String {
        val candidates = listOf(
            File("app/src/main/res/values/strings.xml"),
            File("src/main/res/values/strings.xml"),
        )
        return (candidates.firstOrNull { it.exists() }
            ?: error("strings.xml not found from ${File("").absolutePath}")).readText()
    }

    private fun stringValue(xml: String, name: String): String? =
        Regex("""<string name="$name">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .find(xml)?.groupValues?.get(1)

    @Test
    fun everyModesApichimaSoundsOnItsOwnLadder() {
        val xml = stringsXml()
        var checked = 0

        EightModeScaleDefinitions.MODE_SCALES.forEach { (modeKey, scale) ->
            val labels = scale.ascendingPhthongs(octaves).reversed()
            val freqs = ModeScaleFrequencies.topToBottom(
                scale.repeatedIntervals(octaves),
                scale.referenceMoriaFromBottom(reference, octaves),
                0,
            )
            listOf(
                "mode_apichima_syllables_$modeKey",
                "mode_apichima_alternative_syllables_$modeKey",
            ).forEach { resName ->
                val teaching = stringValue(xml, resName) ?: return@forEach
                val steps = ApichimaSequence.parse(teaching)
                assertTrue("$resName parsed to nothing: $teaching", steps.isNotEmpty())
                assertNotNull(
                    "$resName has a φθόγγος that $modeKey's scale does not contain: " +
                        steps.map { it.phthongLabel }.filterNot { it in labels },
                    ApichimaSequence.frequencies(steps, labels, freqs)
                )
                checked++
            }
        }

        // Guards the slice: if the resource names ever change, this must fail rather than pass over
        // an empty sweep.
        assertTrue("expected at least 8 απηχήματα, checked $checked", checked >= 8)
    }
}
