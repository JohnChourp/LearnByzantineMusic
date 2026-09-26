package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.ByzantineTuning
import com.johnchourp.learnbyzantinemusic.music.IntonationProfile
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.Phthong
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The narrowing tolerance of «Παραλλαγή με αναμονή» (ClickUp `869f5x2cd`, J1), from the table in
 * `IntonationProfile`: ±3 → ±2.5 → ±2, one level narrower after each run through the line without a
 * skip, never wider by itself — and, by the nearest-rung-first rule of H1, a level only matters on a
 * step wider than twice it. On the hard chromatic's 4-μόρια steps no level changes anything.
 */
class WaitModeToleranceTest {

    @Test
    fun theLevelsAreThreeTwoAndAHalfAndTwo() {
        assertEquals(listOf(3.0, 2.5, 2.0), IntonationProfile.WAIT_TOLERANCES_MORIA)
        assertEquals("the first level is every screen's tolerance", IntonationProfile.IN_TUNE_MORIA, IntonationProfile.waitTolerance(0), 0.0)
        assertEquals(2.5, IntonationProfile.waitTolerance(1), 0.0)
        assertEquals(2.0, IntonationProfile.waitTolerance(2), 0.0)
        assertEquals("held inside the table", 2.0, IntonationProfile.waitTolerance(9), 0.0)
        assertEquals(3.0, IntonationProfile.waitTolerance(-1), 0.0)
    }

    @Test
    fun aRunWithoutSkipsNarrowsItAndARunWithSkipsKeepsIt() {
        assertEquals(1, IntonationProfile.nextWaitLevel(0, withoutSkips = true))
        assertEquals(2, IntonationProfile.nextWaitLevel(1, withoutSkips = true))
        assertEquals("never past the narrowest", 2, IntonationProfile.nextWaitLevel(2, withoutSkips = true))
        assertEquals("a skip never widens it back", 2, IntonationProfile.nextWaitLevel(2, withoutSkips = false))
        assertEquals(1, IntonationProfile.nextWaitLevel(1, withoutSkips = false))
        assertEquals(0, IntonationProfile.nextWaitLevel(0, withoutSkips = false))
    }

    /** Whether a steady voice [moriaAway] from [phthong] moves the line at narrowing [level] on [scale]. */
    private fun locks(scale: TrainerScale, phthong: PhthongName, moriaAway: Double, level: Int): Boolean {
        val target = scale.ladder.stepFor(Phthong(phthong))!!
        val wait = WaitModeEvaluator(listOf(target), scale.ladder, IntonationProfile.waitTolerance(level))
        val voice = ByzantineTuning.frequencyHz(target.moriaFromNi.value + moriaAway)
        return (0..10).any { wait.onFrame(voice, it * 50L).advanced }
    }

    @Test
    fun onAnEightMoriaStepEveryLevelCanSayOut() {
        // Diatonic Βου–Γα is 8 μόρια: 2.8 above Βου is still nearest Βου.
        val diatonic = TrainerScale.DIATONIC
        assertEquals(listOf(true, false, false), (0..2).map { locks(diatonic, PhthongName.VOU, 2.8, it) })
        assertEquals(listOf(true, true, false), (0..2).map { locks(diatonic, PhthongName.VOU, 2.3, it) })
        assertEquals(listOf(true, true, true), (0..2).map { locks(diatonic, PhthongName.VOU, 1.8, it) })
        // The soft chromatic's smallest step is 8 too (Πα–Βου in the second mode).
        assertEquals(listOf(true, false, false), (0..2).map { locks(TrainerScale(Mode.SECOND, 0), PhthongName.PA, 2.8, it) })
    }

    @Test
    fun onASixMoriaStepOnlyTheNarrowerLevelsCanSayOut() {
        // Enharmonic Βου–Γα is 6 μόρια (third mode): at ±3 a voice 2.9 above Βου is still «on» it —
        // anything nearer Γα is simply Γα — so level 0 cannot say «έξω» there; levels 1 and 2 can.
        val enharmonic = TrainerScale(Mode.THIRD, 0)
        assertEquals(listOf(true, false, false), (0..2).map { locks(enharmonic, PhthongName.VOU, 2.9, it) })
        assertEquals(listOf(false, false, false), (0..2).map { locks(enharmonic, PhthongName.VOU, 3.1, it) })
    }

    @Test
    fun onTheHardChromaticsFourMoriaStepNoLevelChangesAnything() {
        // Βου–Γα is 4 μόρια in the plagal second: up to its middle the voice is on Βου at every level,
        // past it the voice is on Γα at every level.
        val hardChromatic = TrainerScale(Mode.PLAGAL_SECOND, 0)
        assertEquals(listOf(true, true, true), (0..2).map { locks(hardChromatic, PhthongName.VOU, 1.9, it) })
        assertEquals(listOf(false, false, false), (0..2).map { locks(hardChromatic, PhthongName.VOU, 2.1, it) })
    }
}
