package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.ByzantineTuning
import com.johnchourp.learnbyzantinemusic.music.IntonationProfile
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.ModeLadder
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * «Παραλλαγή με αναμονή» hears the voice on the scale the learner chose (ClickUp `869f5x2cd`, J1):
 * octave-folded against the φθόγγος it waits on — a man sings an octave below the middle rung — and
 * then read on that scale's own ladder: the ήχος, its «Μεταφορά βάσης» and the voice's global shift
 * (F2, J4). A voice 12 μόρια low is right on a ladder moved 12 μόρια down, and only there.
 */
class WaitModeOctaveAndShiftTest {

    private val line = listOf(TrainerNote(PhthongName.VOU), TrainerNote(PhthongName.DI, octaveShift = 1))

    private fun targetsOn(scale: TrainerScale): List<ModeLadder.Step> = line.map { scale.ladder.stepFor(it.pitch)!! }

    /** Holds a steady [frequencyHz] for a second of live frames; true if the line moved on. */
    private fun holdsOn(scale: TrainerScale, frequencyHz: Double): Boolean {
        val wait = WaitModeEvaluator(targetsOn(scale), scale.ladder, IntonationProfile.IN_TUNE_MORIA)
        return (0..21).any { wait.onFrame(frequencyHz, (it * IntonationProfile.LIVE_HOP_MS).toLong()).advanced }
    }

    private fun onTheLadder(scale: TrainerScale, moriaAway: Double = 0.0): Double =
        ByzantineTuning.frequencyHz(targetsOn(scale)[0].moriaFromNi.value + moriaAway)

    @Test
    fun aVoiceAnOctaveDownOrUpIsOnTheNote() {
        val scale = TrainerScale(Mode.FIRST, 0)
        val middle = onTheLadder(scale)
        assertTrue("an octave down", holdsOn(scale, middle / 2))
        assertTrue("two octaves down", holdsOn(scale, middle / 4))
        assertTrue("an octave up", holdsOn(scale, middle * 2))
        assertTrue("an octave down and 2 μόρια sharp", holdsOn(scale, ByzantineTuning.frequencyHz(targetsOn(scale)[0].moriaFromNi.value - 72 + 2.0)))
    }

    @Test
    fun theFoldNeverChangesTheNoteOnlyItsOctave() {
        // The note written an octave up (Δι΄) is found by a voice in any octave, too.
        val scale = TrainerScale.DIATONIC
        val high = scale.ladder.stepFor(line[1].pitch)!!
        listOf(-2, -1, 0, 1).forEach { octaves ->
            val reading = TargetReading.of(high, scale.ladder, ByzantineTuning.frequencyHz(high.moriaFromNi.value + 72.0 * octaves + 1.5))!!
            assertEquals(high, reading.nearest)
            assertEquals(1.5, reading.offsetMoria, 1e-9)
        }
    }

    @Test
    fun aVoiceTwelveMoriaLowPassesOnlyOnALadderMovedTwelveDown() {
        val plain = TrainerScale(Mode.SECOND, 0)
        val low = onTheLadder(plain, -12.0)
        assertFalse("12 μόρια flat is not the note on the unmoved ladder", holdsOn(plain, low))
        assertTrue("the ήχος's own shift", holdsOn(TrainerScale(Mode.SECOND, -12), low))
        assertTrue("or the voice's global one", holdsOn(TrainerScale(Mode.SECOND, 0, globalShiftMoria = -12), low))
        assertTrue("or both, adding up", holdsOn(TrainerScale(Mode.SECOND, -5, globalShiftMoria = -7), low))
        assertFalse("but not a ladder moved the other way", holdsOn(TrainerScale(Mode.SECOND, 12), low))
    }

    @Test
    fun theDiatonicDefaultFollowsTheGlobalShiftToo() {
        val low = onTheLadder(TrainerScale.DIATONIC, -12.0)
        assertFalse(holdsOn(TrainerScale.DIATONIC, low))
        assertTrue(holdsOn(TrainerScale(mode = null, globalShiftMoria = -12), low))
    }

    @Test
    fun theArrowSaysHowFarFromTheNoteNotFromTheNearestRung() {
        // Diatonic Βου, 5 μόρια sharp: past the middle of its 8-μόρια step to Γα, so the nearest rung
        // is Γα — the voice is not on the note — and the learner is told 5 μόρια lower.
        val scale = TrainerScale.DIATONIC
        val vou = targetsOn(scale)[0]
        val reading = TargetReading.of(vou, scale.ladder, ByzantineTuning.frequencyHz(vou.moriaFromNi.value + 5.0))!!
        assertEquals(PhthongName.GA, reading.nearest.phthong.name)
        assertEquals(5.0, reading.offsetMoria, 1e-9)
        assertFalse(reading.isOnTarget(IntonationProfile.IN_TUNE_MORIA))
    }
}
