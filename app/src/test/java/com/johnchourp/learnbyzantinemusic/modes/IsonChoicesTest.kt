package com.johnchourp.learnbyzantinemusic.modes

import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.modes.ui.EIGHT_MODES
import com.johnchourp.learnbyzantinemusic.modes.ui.EightModeUiModel
import com.johnchourp.learnbyzantinemusic.modes.ui.SCALE_OCTAVES
import com.johnchourp.learnbyzantinemusic.music.BaseShift
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
import kotlin.math.sign

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

    private val shifts: IntRange get() = BaseShift.MIN_MORIA..BaseShift.MAX_MORIA

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

    /** One dominant as the theory page writes it: the φθόγγος, its octave mark, its brackets. */
    private data class Written(val name: PhthongName, val mark: Char?, val bracketed: Boolean)

    /** Heirmologic then sticheraric, exactly as the Greek theory strings list them. */
    private fun dominantsAsWritten(mode: Mode): List<Written> {
        val theory = ModeTheoryCatalog.modes.single { it.key == mode.key }
        return theory.styleRows
            .map { it.dominantsCadencesRes }
            .filter { it != R.string.mode_theory_not_specified }
            // ", " and not ",": a lower-octave mark is a comma too, and must stay on its φθόγγος.
            .flatMap { id -> greekText(id).split(", ").map { it.trim() } }
            .map { item ->
                val bracketed = item.startsWith("(") && item.endsWith(")")
                val label = item.removePrefix("(").removeSuffix(")")
                val phthong = Phthong.parse(label) ?: error("${mode.key}: «$item» names no φθόγγος")
                Written(phthong.name, label.last().takeIf { it == '΄' || it == ',' }, bracketed)
            }
    }

    /** Each φθόγγος once, in the order it first appears, the bracketed ones last. */
    private fun dominantsInTheText(mode: Mode): List<PhthongName> {
        val written = dominantsAsWritten(mode)
        val plain = written.filterNot { it.bracketed }.map { it.name }.distinct()
        return plain + written.filter { it.bracketed }.map { it.name }.distinct().filterNot { it in plain }
    }

    @Test
    fun theDominantsAreTheTheoryPagesInItsOrderBracketedLast() {
        Mode.entries.forEach { mode ->
            val fromText = dominantsInTheText(mode)
            assertTrue("${mode.key}: no dominants found in the text", fromText.isNotEmpty())
            assertEquals("${mode.key}", fromText, IsonDrone.dominants(mode).map { it.name })
        }
    }

    /**
     * The owner's decision (2026-09-25), written out in full so that moving any one dominant to
     * another octave fails here. Octave 0 is the απήχημα's middle octave, so it prints bare; ΄ is the
     * octave above it.
     */
    private val decidedDominants = mapOf(
        Mode.FIRST to "Πα Δι Γα",
        Mode.SECOND to "Πα Δι Βου Ζω",
        Mode.THIRD to "Πα Γα Κε",
        Mode.FOURTH to "Βου Δι Πα Ζω",
        Mode.PLAGAL_FIRST to "Κε Νη΄ Πα Δι",
        Mode.PLAGAL_SECOND to "Δι Βου Πα Ζω",
        Mode.VARYS to "Γα Δι Ζω",
        Mode.PLAGAL_FOURTH to "Νη Βου Δι Γα",
    )

    @Test
    fun everyModesDominantsSitAtTheDecidedOctaves() {
        assertEquals(Mode.entries.toSet(), decidedDominants.keys)
        Mode.entries.forEach { mode ->
            assertEquals(
                "${mode.key}",
                decidedDominants.getValue(mode),
                IsonDrone.dominants(mode).joinToString(" ") { it.label },
            )
        }
    }

    /** +1 when [phthong] lies above [mode]'s base, -1 below, 0 on it. */
    private fun sideOfTheBase(mode: Mode, phthong: Phthong): Int {
        val ladder = ladder(mode)
        val base = ladder.stepFor(IsonDrone.base(mode))!!.moriaFromNi
        return (ladder.stepFor(phthong)!!.moriaFromNi - base).value.sign
    }

    @Test
    fun theWrittenOctaveMarksAreRespected() {
        // ΄ lies above the base and `,` below it. Only ΄ is written today: Πλ.Α΄'s Νη΄ and Πλ.Β΄'s
        // «(Ζω΄)» — the Ζω just under Νη΄, never the one 4 μόρια under the base.
        var marked = 0
        Mode.entries.forEach { mode ->
            dominantsAsWritten(mode).filter { it.mark != null }.forEach { written ->
                val typed = IsonDrone.dominants(mode).single { it.name == written.name }
                val expected = if (written.mark == '΄') 1 else -1
                val where = "${mode.key}: ${typed.label} is on the wrong side of the base"
                assertEquals(where, expected, sideOfTheBase(mode, typed))
                marked++
            }
        }
        assertTrue("expected the marks of Πλ.Α΄ and Πλ.Β΄, found $marked", marked >= 2)
    }

    @Test
    fun thePlagalSecondAndFourthRiseFromTheirBaseSoEveryDominantLiesAboveIt() {
        listOf(Mode.PLAGAL_SECOND, Mode.PLAGAL_FOURTH).forEach { mode ->
            IsonDrone.dominants(mode).filter { it != IsonDrone.base(mode) }.forEach { phthong ->
                val where = "${mode.key}: ${phthong.label} lies below the base"
                assertEquals(where, 1, sideOfTheBase(mode, phthong))
            }
        }
    }

    // ---- the menu --------------------------------------------------------------------------------

    @Test
    fun everyPhthongIsOfferedOnceAndTheOthersNearTheBase() {
        Mode.entries.forEach { mode ->
            val ladder = ladder(mode)
            val choices = IsonDrone.choices(mode, ladder)!!
            val names = choices.all.map { it.name }
            assertEquals("${mode.key}: every φθόγγος once", PhthongName.entries.toSet(), names.toSet())
            assertEquals("${mode.key}: no φθόγγος twice", PhthongName.entries.size, names.size)
            assertEquals(
                "${mode.key}: the dominants come first, at their typed octaves, without the base",
                IsonDrone.dominants(mode).filter { it != choices.base },
                choices.dominants,
            )
            val baseMoria = ladder.stepFor(choices.base)!!.moriaFromNi
            val othersMoria = choices.others.map { ladder.stepFor(it)!!.moriaFromNi }
            assertEquals("${mode.key}: the others run low to high", othersMoria.sorted(), othersMoria)
            choices.others.forEach { phthong ->
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
    fun theOneTieAmongTheOthersGoesToTheLowerRung() {
        // Βαρύς's Βου is 36 μόρια below and above its base Ζω. The lower one wins, because an ison
        // sits under the voice.
        val varys = IsonDrone.choices(Mode.VARYS, ladder(Mode.VARYS))!!
        assertTrue(varys.all.toString(), Phthong(PhthongName.VOU, octave = 0) in varys.others)
    }
}
