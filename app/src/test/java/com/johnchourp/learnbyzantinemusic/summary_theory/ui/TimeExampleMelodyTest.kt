package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import com.johnchourp.learnbyzantinemusic.modes.EightModeScaleDefinitions
import com.johnchourp.learnbyzantinemusic.modes.ModeLadders
import com.johnchourp.learnbyzantinemusic.music.Phthong
import com.johnchourp.learnbyzantinemusic.music.PhthongName.GA
import com.johnchourp.learnbyzantinemusic.music.PhthongName.PA
import com.johnchourp.learnbyzantinemusic.music.PhthongName.VOU
import com.johnchourp.learnbyzantinemusic.music.RhythmNote
import com.johnchourp.learnbyzantinemusic.music.RhythmTimeline
import com.johnchourp.learnbyzantinemusic.music.TimeSign
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The melody «Άκου» sings for an example (ClickUp `869f5x25n`, F4): it starts on Πα and each note moves
 * by the φωνές of the sign it is written with — the sign table's φωνές, on the 8 Ήχοι page's diatonic
 * ladder. Ίσον, and a note that draws no quantity sign, repeat the note before.
 */
class TimeExampleMelodyTest {

    private val pa = Phthong(PA)
    private val vou = Phthong(VOU)
    private val ga = Phthong(GA)

    private fun equation(name: String) = TimePageEquations.all.getValue(name)

    private fun melody(name: String): List<Phthong?> =
        equation(name).let { TimeExamplePlayback.melody(it.rhythm, it.quantitySigns) }

    @Test
    fun theGorgonFamilyStaysOnTheIson() {
        assertEquals(listOf(Neume.ISON, Neume.ISON), equation("gorgo").quantitySigns)
        assertEquals(listOf(pa, pa), melody("gorgo"))
        assertEquals(listOf(pa, pa, pa), melody("digorgo"))
        assertEquals(listOf(pa, pa, pa, pa), melody("trigorgo"))
    }

    @Test
    fun theArgonClimbsWithItsKentimataAndOligon() {
        // Read as ίσον, κεντήματα (+1), ολίγον (+1): Πα, Βου, Γα.
        listOf("argo", "diargo", "triargo").forEach { name ->
            assertEquals(name, listOf(Neume.ISON, Neume.KENTIMATA, Neume.OLIGON), equation(name).quantitySigns)
            assertEquals(name, listOf(pa, vou, ga), melody(name))
        }
    }

    @Test
    fun aNoteWithNoQuantitySignRepeatsAndARestHasNoPitch() {
        assertEquals(listOf(pa), TimeExamplePlayback.melody(listOf(RhythmNote(setOf(TimeSign.KLASMA))), listOf(null)))
        val withRest = listOf(RhythmNote(), RhythmNote(setOf(TimeSign.VAREIA_APLI)), RhythmNote())
        assertEquals(listOf(vou, null, ga), TimeExamplePlayback.melody(withRest, listOf(Neume.OLIGON, null, Neume.OLIGON)))
    }

    @Test
    fun thePitchesAreTheEightModesDiatonicLadders() {
        // The same ladder the 8 Ήχοι page draws for a diatonic mode, unshifted: no second pitch table.
        val ladder = ModeLadders.ladder(EightModeScaleDefinitions.DIATONIC, baseShiftMoria = 0)
        val sounds = TimeExamplePlayback.cues(equation("argo"), 80).filterIsInstance<RhythmTimeline.Sound>()
        assertEquals(
            listOf(pa, vou, ga).map { ladder.stepFor(it)!!.frequencyHz },
            sounds.map { it.frequencyHz },
        )
    }
}
