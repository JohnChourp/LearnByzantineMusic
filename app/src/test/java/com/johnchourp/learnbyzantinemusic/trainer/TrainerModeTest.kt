package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.ByzantineTuning
import com.johnchourp.learnbyzantinemusic.music.IntonationProfile
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.music.PhthongName.DI
import com.johnchourp.learnbyzantinemusic.music.PhthongName.VOU
import com.johnchourp.learnbyzantinemusic.music.PhthongName.ZO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * «Σωστός Δι του μαλακού χρωματικού πρασινίζει στον ήχο του και όχι στον διατονικό»
 * (ClickUp `869f5x24v`, F2).
 *
 * The Β΄ ήχος is soft chromatic: its Δι sits at 38 μόρια above Νη, the diatonic one at 42. Four μόρια
 * used to be exactly on the old ±4 tolerance; since H1 it is outside ±3, so the Trainer can now tell
 * them apart — which is the point. Βου (16 against 22) and Ζω (58 against 64) are 6 apart and are
 * checked too, so the test does not hang on one boundary.
 */
class TrainerModeTest {

    private val second = TrainerScale(Mode.SECOND)

    /** A voice that sings [phthong] exactly where the Β΄ ήχος puts it. */
    private fun softChromaticHz(phthong: PhthongName): Double = second.frequencyHz(TrainerNote(phthong))

    /** The voice check's verdict on one note of [target], sung at [hz], on [scale]. */
    private fun greens(scale: TrainerScale, target: PhthongName, hz: Double): Boolean {
        val evaluator = PitchGreeningEvaluator(listOf(target))
        var verdict: GreeningResult? = null
        repeat(PitchGreeningEvaluator.DEFAULT_MIN_STABLE_FRAMES) {
            evaluator.onFrame(scale.match(hz))?.let { verdict = it }
        }
        return verdict!!.matched
    }

    @Test
    fun theSecondModeIsWhereTheTicketSaysItIs() {
        assertEquals(38.0, ByzantineTuning.moriaFromNi(softChromaticHz(DI)), 1e-9)
        assertEquals(16.0, ByzantineTuning.moriaFromNi(softChromaticHz(VOU)), 1e-9)
        assertEquals(58.0, ByzantineTuning.moriaFromNi(softChromaticHz(ZO)), 1e-9)
        // And the diatonic ones are 4 and 6 μόρια higher — beyond the one tolerance of IntonationProfile.
        assertEquals(42.0, ByzantineTuning.moriaFromNi(TrainerScale.DIATONIC.frequencyHz(TrainerNote(DI))), 1e-9)
        assertTrue(4.0 > IntonationProfile.IN_TUNE_MORIA)
    }

    @Test
    fun aSoftChromaticDiGreensInTheSecondModeAndNotOnTheDiatonic() {
        assertTrue("Β΄", greens(second, DI, softChromaticHz(DI)))
        assertFalse("«Διατονικός»", greens(TrainerScale.DIATONIC, DI, softChromaticHz(DI)))
        assertFalse("Α΄, diatonic too", greens(TrainerScale(Mode.FIRST), DI, softChromaticHz(DI)))
        // On the diatonic it is still heard as Δι — just 4 μόρια flat, which is out.
        val heard = TrainerScale.DIATONIC.match(softChromaticHz(DI))!!
        assertEquals(DI, heard.phthong)
        assertEquals(-4.0, heard.deviationMoria, 1e-9)
    }

    @Test
    fun softChromaticVouAndZoGreenInTheSecondModeAndNotOnTheDiatonic() {
        listOf(VOU, ZO).forEach { phthong ->
            assertTrue("Β΄ $phthong", greens(second, phthong, softChromaticHz(phthong)))
            assertFalse("«Διατονικός» $phthong", greens(TrainerScale.DIATONIC, phthong, softChromaticHz(phthong)))
            val heard = TrainerScale.DIATONIC.match(softChromaticHz(phthong))!!
            val outOrElsewhere = heard.phthong != phthong || !IntonationProfile.isInTune(heard.deviationMoria)
            assertTrue("6 μόρια off is another φθόγγος, or out", outOrElsewhere)
        }
    }

    @Test
    fun everyModeGreensItsOwnNotes() {
        Mode.entries.forEach { mode ->
            val scale = TrainerScale(mode)
            PhthongName.entries.forEach { phthong ->
                assertTrue("$mode $phthong", greens(scale, phthong, scale.frequencyHz(TrainerNote(phthong))))
            }
        }
    }
}
