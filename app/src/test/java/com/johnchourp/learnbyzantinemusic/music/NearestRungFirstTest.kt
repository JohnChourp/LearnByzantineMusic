package com.johnchourp.learnbyzantinemusic.music

import com.johnchourp.learnbyzantinemusic.modes.EightModeScaleDefinitions
import com.johnchourp.learnbyzantinemusic.modes.LadderPitchMirror
import com.johnchourp.learnbyzantinemusic.recordings.analysis.ModeScalePositions
import com.johnchourp.learnbyzantinemusic.recordings.analysis.PhthongSegmenter
import com.johnchourp.learnbyzantinemusic.recordings.analysis.PitchFrame
import com.johnchourp.learnbyzantinemusic.recordings.analysis.PitchTrack
import com.johnchourp.learnbyzantinemusic.trainer.ComboPitchGate
import com.johnchourp.learnbyzantinemusic.trainer.GreeningResult
import com.johnchourp.learnbyzantinemusic.trainer.PitchGreeningEvaluator
import com.johnchourp.learnbyzantinemusic.trainer.TrainerPitchTable
import com.johnchourp.learnbyzantinemusic.trainer.diatonicMoriaFromNi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Why the tolerance is ±3 and not ±4 (ClickUp `869f5x28t`, H1): every screen gives a pitch to the
 * **nearest rung first** and only then asks whether it is close enough. A tolerance of half the gap
 * to a neighbour or more can therefore never say «έξω» on that side — and the diatonic Βου–Γα step is
 * 8 μόρια, so ±4 sat exactly on that limit.
 *
 * The probe is a voice [OFF] μόρια above Βου: nearer Βου than Γα, inside the old ±4, outside ±3. It
 * goes through each screen's own matcher, not through [IntonationProfile] directly, so a screen that
 * went back to its own tolerance turns its test red.
 */
class NearestRungFirstTest {

    @Test
    fun theProbeSitsBetweenTheNewAndTheOldTolerance() {
        val step = PhthongName.GA.diatonicMoriaFromNi - PhthongName.VOU.diatonicMoriaFromNi
        assertEquals("the diatonic Βου–Γα step", 8, step)
        assertTrue("nearer Βου than Γα, so no screen can hand it to the neighbour", OFF < step / 2.0)
        assertTrue("the old ±4 let it through", OFF <= 4.0)
        assertTrue("±3 does not", OFF > IntonationProfile.IN_TUNE_MORIA)
    }

    @Test
    fun theTrainerCallsItOut() {
        val match = TrainerPitchTable.nearestPhthong(
            ByzantineTuning.frequencyHz(PhthongName.VOU.diatonicMoriaFromNi + OFF)
        )!!
        assertEquals(PhthongName.VOU, match.phthong)
        assertEquals(OFF, match.deviationMoria, 1e-9)

        assertNull("φθόγγος + time: not an in-tune Βου", ComboPitchGate.inTunePhthong(match))

        val voiceCheck = PitchGreeningEvaluator(listOf(PhthongName.VOU))
        var verdict: GreeningResult? = null
        repeat(PitchGreeningEvaluator.DEFAULT_MIN_STABLE_FRAMES) { voiceCheck.onFrame(match)?.let { verdict = it } }
        assertEquals("voice check: Βου, but not green", GreeningResult(0, false, PhthongName.VOU), verdict)
    }

    @Test
    fun whereAmICallsItOutOnADiatonicMode() {
        val ladder = EightModeScaleDefinitions.DIATONIC.ladder(octaves = 3)
        val vou = ladder.stepFor(Phthong(PhthongName.VOU))!!
        val reading = LadderPitchMirror.read(ladder, ByzantineTuning.frequencyHz(vou.moriaFromNi.value + OFF))!!
        assertEquals(vou.phthong, reading.step.phthong)
        assertEquals(OFF, reading.deviationMoria, 1e-9)
        assertFalse("«Πού είμαι» must say ψηλά, not «είσαι μέσα»", reading.isInTune())
    }

    @Test
    fun theAnalysisCallsItOutOnADiatonicMode() {
        // Half a second held 3.5 μόρια above Βου of the first mode, calibrated to the app's own Νη.
        val positions = ModeScalePositions.forMode("first")
        val hz = ByzantineTuning.frequencyHz(positions[PhthongName.VOU.ordinal] + OFF).toFloat()
        val hopMs = IntonationProfile.OFFLINE_HOP_MS
        val frames = List(22) { PitchFrame((it * hopMs).toLong(), hz) }
        val track = PitchTrack(frames, hopMs, (frames.size * hopMs).toLong())

        val notes = PhthongSegmenter.segment(track, ByzantineTuning.NI_BASE_HZ, positions)
        assertEquals(1, notes.size)
        assertEquals(PhthongName.VOU, notes.single().phthong)
        assertEquals(OFF, notes.single().deviationMoria, 1e-3)
        assertFalse("an orange bar, not a green one", notes.single().isInTune)
    }

    /**
     * The reason in one sweep: every pitch from Βου to Γα, as the Trainer reads it. With ±4 each one
     * is some φθόγγος in tune; with the profile's ±3 a band in the middle is out.
     *
     * The probes are offset by half a step so none lands on the exact midpoint, where the verdict
     * would hang on the last bit of a floating-point round trip rather than on the rule.
     */
    @Test
    fun withFourMoriaNothingBetweenVouAndGaCouldEverBeOut() {
        val vou = PhthongName.VOU.diatonicMoriaFromNi
        val deviations = (0 until 160).map { index ->
            val moria = vou + 0.025 + index * 0.05
            TrainerPitchTable.nearestPhthong(ByzantineTuning.frequencyHz(moria))!!.deviationMoria
        }
        assertTrue("±4: never out", deviations.all { IntonationProfile.isInTune(it, toleranceMoria = 4.0) })
        // ±3: out from 3 above Βου to 3 below Γα — a 2-μόρια band, 40 probes at 0.05 apart.
        assertEquals(40, deviations.count { !IntonationProfile.isInTune(it) })
    }

    /**
     * The known limit written in [IntonationProfile]'s KDoc: a step of twice the tolerance or less
     * can never say «έξω». With ±3 none is diatonic or soft chromatic; the hard chromatic and the
     * enharmonic scales have such steps, and between their rungs a voice is always given to one.
     */
    @Test
    fun theKnownLimitIsTheStepsOfSixMoriaOrLess() {
        val blind = listOf(
            EightModeScaleDefinitions.DIATONIC,
            EightModeScaleDefinitions.SOFT_CHROMATIC,
            EightModeScaleDefinitions.HARD_CHROMATIC,
            EightModeScaleDefinitions.ENHARMONIC,
        ).associate { scale ->
            scale.genus to scale.intervals.filter { it / 2.0 <= IntonationProfile.IN_TUNE_MORIA }
        }
        assertEquals(emptyList<Int>(), blind[Genus.DIATONIC])
        assertEquals(emptyList<Int>(), blind[Genus.SOFT_CHROMATIC])
        assertEquals("Νη–Πα 6, Βου–Γα 4, Δι–Κε 6, Ζω–Νη΄ 4", listOf(6, 4, 6, 4), blind[Genus.HARD_CHROMATIC])
        assertEquals("Βου–Γα 6, Κε–Ζω 6", listOf(6, 6), blind[Genus.ENHARMONIC])
    }

    @Test
    fun onTheHardChromaticVouGaStepWhereAmICannotSayOut() {
        // The limit, observed on the real matcher: Βου–Γα of πλ. Β΄ is 4 μόρια, so every pitch between
        // the two is within 2 of one of them.
        val ladder = EightModeScaleDefinitions.HARD_CHROMATIC.ladder(octaves = 3)
        val vou = ladder.stepFor(Phthong(PhthongName.VOU))!!.moriaFromNi.value
        val ga = ladder.stepFor(Phthong(PhthongName.GA))!!.moriaFromNi.value
        assertEquals(4, ga - vou)
        val readings = (0 until 80).map { index ->
            LadderPitchMirror.read(ladder, ByzantineTuning.frequencyHz(vou + 0.025 + index * 0.05))!!
        }
        assertTrue(readings.all { it.isInTune() })
    }

    private companion object {
        /** μόρια above Βου: inside the old ±4, outside ±3, and nearer Βου than Γα. */
        const val OFF = 3.5
    }
}
