package com.johnchourp.learnbyzantinemusic.modes

import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.modes.ui.BASE_SHIFT_MAX
import com.johnchourp.learnbyzantinemusic.modes.ui.BASE_SHIFT_MIN
import com.johnchourp.learnbyzantinemusic.modes.ui.EIGHT_MODES
import com.johnchourp.learnbyzantinemusic.modes.ui.EightModeUiModel
import com.johnchourp.learnbyzantinemusic.modes.ui.SCALE_OCTAVES
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.ModeLadder
import com.johnchourp.learnbyzantinemusic.music.Moria
import com.johnchourp.learnbyzantinemusic.music.Phthong
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.abs

/**
 * Where the ison starts and where «Ίσον σε…» can move it (ClickUp `869f5x251`).
 *
 * The drone used to hold the scale's *construction* base — Πα, or Νη for the Νη-based scales — which
 * in six of the eight modes is not where the mode rests. The owner decided it starts on **the end of
 * the απήχημα**, at the very pitch the page's απήχημα sounds it. That base and the mode's δεσπόζοντες
 * are typed data in [IsonDrone]; this test holds them to the Greek text they come from, and holds the
 * drone to the απήχημα's own last tone at every base shift the slider offers.
 */
class IsonChoicesTest {

    private val greek: Map<String, String> by lazy {
        val file = listOf("app/src/main/res/values/strings.xml", "src/main/res/values/strings.xml")
            .map(::File)
            .firstOrNull { it.isFile }
            ?: error("Cannot locate values/strings.xml from ${File("").absolutePath}")
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            .getElementsByTagName("string")
        (0 until nodes.length)
            .map { nodes.item(it) as Element }
            .associate { it.getAttribute("name") to it.textContent }
    }

    /** R id → resource name, so each mode is checked on exactly the strings the app shows. */
    private val resourceNames: Map<Int, String> by lazy {
        R.string::class.java.fields.associate { it.getInt(null) to it.name }
    }

    private fun greekText(id: Int): String {
        val name = resourceNames[id] ?: error("no string resource has id $id")
        return greek[name] ?: error("values/strings.xml has no $name")
    }

    private fun row(mode: Mode): EightModeUiModel = EIGHT_MODES.single { it.mode == mode }

    private fun ladder(mode: Mode, shift: Int = 0): ModeLadder =
        row(mode).scale.ladder(octaves = SCALE_OCTAVES, baseShift = Moria(shift))

    private val shifts: IntRange get() = BASE_SHIFT_MIN..BASE_SHIFT_MAX

    // ---- the base --------------------------------------------------------------------------------

    @Test
    fun theBaseIsTheLastStepOfTheGreekApichimaInEveryMode() {
        Mode.entries.forEach { mode ->
            val steps = ApichimaSequence.parse(greekText(row(mode).apichimaSyllablesRes))
            assertTrue("${mode.key}: the απήχημα parsed to nothing", steps.isNotEmpty())
            assertEquals(
                "${mode.key}: the ison must start where the απήχημα ends, same φθόγγος and octave",
                steps.last().phthong,
                IsonDrone.base(mode),
            )
        }
    }

    @Test
    fun theDroneSoundsExactlyTheApichimasLastToneAtEveryShift() {
        // Guards the sweep: the range must be real and must include "no shift".
        assertTrue("shift range $shifts", 0 in shifts && shifts.first < shifts.last)
        var compared = 0
        Mode.entries.forEach { mode ->
            val apichima = ApichimaSequence.parse(greekText(row(mode).apichimaSyllablesRes))
            shifts.forEach { shift ->
                val ladder = ladder(mode, shift)
                val lastTone = ApichimaSequence.frequencies(apichima, ladder)?.last()
                assertNotNull("${mode.key} shift=$shift: the απήχημα does not sound", lastTone)
                val drone = IsonDrone.step(ladder, IsonDrone.choices(mode, ladder)!!.base)
                assertEquals(
                    "${mode.key} shift=$shift",
                    lastTone!!,
                    drone!!.frequencyHz,
                    0.0, // exact: the same rung of the same ladder, never a second calculation
                )
                compared++
            }
        }
        assertEquals(Mode.entries.size * (shifts.last - shifts.first + 1), compared)
    }

    // ---- the dominants ---------------------------------------------------------------------------

    /**
     * What the theory page lists for [mode]: heirmologic then sticheraric, each φθόγγος once in the
     * order it first appears, and the bracketed ones last. Octave marks name the same φθόγγος.
     */
    private fun dominantsInTheText(mode: Mode): List<PhthongName> {
        val theory = ModeTheoryCatalog.modes.single { it.key == mode.key }
        val plain = mutableListOf<PhthongName>()
        val bracketed = mutableListOf<PhthongName>()
        theory.styleRows
            .map { it.dominantsCadencesRes }
            .filter { it != R.string.mode_theory_not_specified }
            .forEach { id ->
                greekText(id).split(",").map { it.trim() }.forEach { item ->
                    val inBrackets = item.startsWith("(") && item.endsWith(")")
                    val name = Phthong.parse(item.removePrefix("(").removeSuffix(")"))?.name
                        ?: error("${mode.key}: «$item» names no φθόγγος")
                    if (inBrackets) bracketed += name else plain += name
                }
            }
        val order = plain.distinct()
        return order + bracketed.distinct().filterNot { it in order }
    }

    @Test
    fun theDominantsAreTheTheoryPagesInItsOrderBracketedLast() {
        Mode.entries.forEach { mode ->
            val fromText = dominantsInTheText(mode)
            assertTrue("${mode.key}: no dominants found in the text", fromText.isNotEmpty())
            assertEquals("${mode.key}", fromText, IsonDrone.dominants(mode))
        }
    }

    // ---- the menu --------------------------------------------------------------------------------

    @Test
    fun everyPhthongIsOfferedOnceAndNearTheBase() {
        Mode.entries.forEach { mode ->
            val ladder = ladder(mode)
            val choices = IsonDrone.choices(mode, ladder)!!
            val names = choices.all.map { it.name }
            assertEquals("${mode.key}: every φθόγγος once", PhthongName.entries.toSet(), names.toSet())
            assertEquals("${mode.key}: no φθόγγος twice", PhthongName.entries.size, names.size)
            assertEquals(
                "${mode.key}: the dominants come first, in the typed order, without the base",
                IsonDrone.dominants(mode).filter { it != choices.base.name },
                choices.dominants.map { it.name },
            )
            val baseMoria = ladder.stepFor(choices.base)!!.moriaFromNi
            val othersMoria = choices.others.map { ladder.stepFor(it)!!.moriaFromNi }
            assertEquals("${mode.key}: the others run low to high", othersMoria.sorted(), othersMoria)
            choices.all.forEach { phthong ->
                val distance = abs((ladder.stepFor(phthong)!!.moriaFromNi - baseMoria).value)
                // Every rung of that name elsewhere on the ladder is at least as far from the base.
                val nearest = ladder.steps.filter { it.phthong.name == phthong.name }
                    .minOf { abs((it.moriaFromNi - baseMoria).value) }
                val where = "${mode.key}: ${phthong.label}"
                assertEquals("$where is not the rung nearest the base", nearest, distance)
                assertTrue("$where is $distance μόρια away", distance <= 36)
            }
        }
    }

    @Test
    fun theOctaveOfAChoiceDoesNotDependOnTheShift() {
        Mode.entries.forEach { mode ->
            val unshifted = IsonDrone.choices(mode, ladder(mode))
            shifts.forEach { shift ->
                val shifted = IsonDrone.choices(mode, ladder(mode, shift))
                assertEquals("${mode.key} shift=$shift", unshifted, shifted)
            }
        }
    }

    @Test
    fun theDecisionsExamplesHold() {
        // «Νη΄» in Πλ.Α΄ is the Νη just above its base Κε, not the one an octave below.
        val plagalFirst = IsonDrone.choices(Mode.PLAGAL_FIRST, ladder(Mode.PLAGAL_FIRST))!!
        assertTrue(plagalFirst.all.toString(), Phthong(PhthongName.NI, octave = 1) in plagalFirst.dominants)

        // The one tie: Βαρύς's Βου is 36 μόρια below and above its base Ζω. The lower one wins,
        // because an ison sits under the voice.
        val varys = IsonDrone.choices(Mode.VARYS, ladder(Mode.VARYS))!!
        assertTrue(varys.all.toString(), Phthong(PhthongName.VOU, octave = 0) in varys.others)
    }
}
