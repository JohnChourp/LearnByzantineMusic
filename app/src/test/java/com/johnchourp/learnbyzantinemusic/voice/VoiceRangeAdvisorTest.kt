package com.johnchourp.learnbyzantinemusic.voice

import com.johnchourp.learnbyzantinemusic.modes.IsonDrone
import com.johnchourp.learnbyzantinemusic.modes.ModeLadders
import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.ByzantineTuning
import com.johnchourp.learnbyzantinemusic.music.Mode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * «Βρες τη φωνή σου» (ClickUp `869f5x2dd`, J4): from the lowest and the highest note a singer holds
 * comfortably, one global shift that seats every mode's scale in that voice — the modes' bases about
 * 30% up the range, the octave folded away, the result inside ±36 μόρια.
 */
class VoiceRangeAdvisorTest {

    private data class Voice(val name: String, val lowestHz: Double, val highestHz: Double)

    /** Comfortable ranges, from a low bass to a high soprano — what a singer might really hold. */
    private val voices = listOf(
        Voice("bass F2–C4", 87.31, 261.63),
        Voice("baritone G2–E4", 98.00, 329.63),
        Voice("tenor C3–A4", 130.81, 440.00),
        Voice("alto F3–D5", 174.61, 587.33),
        Voice("soprano C4–A5", 261.63, 880.00),
        Voice("high soprano E4–C6", 329.63, 1046.50),
    )

    private val octave = ByzantineTuning.MORIA_PER_OCTAVE.toDouble()

    private fun moria(hz: Double): Double = ByzantineTuning.moriaFromNi(hz)

    /** Where 30% of [voice]'s range lies, in μόρια above Νη. */
    private fun target(voice: Voice): Double =
        moria(voice.lowestHz) + VoiceRangeAdvisor.BASE_FRACTION * (moria(voice.highestHz) - moria(voice.lowestHz))

    private fun advice(voice: Voice): VoiceRangeAdvisor.Advice {
        val advice = VoiceRangeAdvisor.advise(voice.lowestHz, voice.highestHz)
        assertNotNull("${voice.name}: no advice", advice)
        return advice!!
    }

    /** [value] brought into −36..+36 by whole octaves. */
    private fun folded(value: Double): Double = value - octave * Math.round(value / octave)

    @Test
    fun theAnchorIsTheAverageOfTheEightIsonBases() {
        // Α΄ Πα, Β΄ Δι, Γ΄ Γα, Δ΄ Δι, Πλ.Α΄ Κε, Πλ.Β΄ Νη, Βαρύς Ζω, Πλ.Δ΄ Νη — each on its own mode's
        // ladder, unshifted. A change to any base (an owner decision) moves every suggestion.
        val bases = Mode.entries.map { IsonDrone.held(IsonDrone.Request(it, BaseShift.DEFAULT_MORIA))!!.moriaFromNi.value }
        assertEquals(8, bases.size)
        assertEquals(bases.average(), VoiceRangeAdvisor.anchorMoria, 0.0)
        assertEquals(29.5, VoiceRangeAdvisor.anchorMoria, 0.0)
        assertTrue(VoiceRangeAdvisor.anchorMoria > bases.min() && VoiceRangeAdvisor.anchorMoria < bases.max())
    }

    @Test
    fun everyVoiceGetsAShiftInsideTheRange() {
        voices.forEach { voice ->
            val shift = advice(voice).globalShiftMoria
            assertTrue("${voice.name}: $shift is outside ${BaseShift.RANGE}", shift in BaseShift.RANGE)
        }
        // Not all the same: the advice depends on the voice.
        assertTrue(voices.map { advice(it).globalShiftMoria }.toSet().size > 1)
    }

    @Test
    fun theModesBasesLandAboutThirtyPercentUpTheRange() {
        voices.forEach { voice ->
            val shift = advice(voice).globalShiftMoria
            // The average base, moved by the shift, is the 30% point up to whole octaves and rounding.
            val offset = folded(VoiceRangeAdvisor.anchorMoria + shift - target(voice))
            assertTrue("${voice.name}: off by $offset μόρια", abs(offset) <= 0.5)
        }
    }

    @Test
    fun everyModesBaseSitsInTheVoiceOnTheLadderTheAppBuilds() {
        var checked = 0
        voices.forEach { voice ->
            val shift = advice(voice).globalShiftMoria
            val low = moria(voice.lowestHz)
            val high = moria(voice.highestHz)
            Mode.entries.forEach { mode ->
                // The 8 Ήχοι ladder at that shift (the diagram's, the ison's, the background ison's):
                // the base's rung the advice aims at is the one nearest the 30% point.
                val base = IsonDrone.base(mode)
                val rung = ModeLadders.ladder(mode, shift).steps
                    .filter { it.phthong.name == base.name }
                    .minByOrNull { abs(it.moriaFromNi.value - target(voice)) }
                val where = "${voice.name}, ${mode.key} at $shift"
                assertNotNull("$where: the ladder has no ${base.label}", rung)
                val at = rung!!.moriaFromNi.value.toDouble()
                assertTrue("$where: ${rung.phthong.label} at $at is outside $low..$high", at in low..high)
                assertEquals(where, rung.frequencyHz, ByzantineTuning.frequencyHz(at), 1e-9)
                checked++
            }
        }
        assertEquals(voices.size * Mode.entries.size, checked)
    }

    @Test
    fun aVoiceAlreadyWhereTheAppSingsNeedsNoShiftAndAnOctaveAwayIsTheSameVoice() {
        // A range whose 30% point is exactly the average base: no shift.
        val span = 1.5 * octave
        val low = VoiceRangeAdvisor.anchorMoria - VoiceRangeAdvisor.BASE_FRACTION * span
        val at = VoiceRangeAdvisor.advise(ByzantineTuning.frequencyHz(low), ByzantineTuning.frequencyHz(low + span))
        assertEquals(BaseShift.DEFAULT_MORIA, at!!.globalShiftMoria)
        // The same voice an octave lower: the app folds octaves, so the advice is the same.
        val lower = VoiceRangeAdvisor.advise(
            ByzantineTuning.frequencyHz(low - octave),
            ByzantineTuning.frequencyHz(low + span - octave),
        )
        assertEquals(BaseShift.DEFAULT_MORIA, lower!!.globalShiftMoria)
        // And a voice sitting 20 μόρια higher gets +20.
        val higher = VoiceRangeAdvisor.advise(ByzantineTuning.frequencyHz(low + 20), ByzantineTuning.frequencyHz(low + span + 20))
        assertEquals(20, higher!!.globalShiftMoria)
    }

    @Test
    fun whatIsNotARangeGetsNoAdvice() {
        assertNull(VoiceRangeAdvisor.advise(220.0, 220.0))
        assertNull(VoiceRangeAdvisor.advise(330.0, 220.0))
        assertNull(VoiceRangeAdvisor.advise(60.0, 220.0)) // below what the pitch engine hears
        assertNull(VoiceRangeAdvisor.advise(220.0, 1500.0)) // above it
        assertNotNull(VoiceRangeAdvisor.advise(VoiceRangeAdvisor.LOWEST_HEARD_HZ, VoiceRangeAdvisor.HIGHEST_HEARD_HZ))
    }

    @Test
    fun theAdviceSaysHowWideTheRangeIs() {
        assertEquals(2.0, VoiceRangeAdvisor.advise(110.0, 440.0)!!.octaves, 1e-9)
        assertEquals(110.0, VoiceRangeAdvisor.advise(110.0, 440.0)!!.lowestHz, 0.0)
        assertEquals(440.0, VoiceRangeAdvisor.advise(110.0, 440.0)!!.highestHz, 0.0)
    }

    @Test
    fun theHeldNoteIsTheMedianOfWhatWasHeard() {
        // Too little to call it a note.
        assertNull(VoiceRangeAdvisor.heldPitchHz(List(VoiceRangeAdvisor.MIN_SAMPLES - 1) { 200.0 }))
        assertEquals(200.0, VoiceRangeAdvisor.heldPitchHz(List(VoiceRangeAdvisor.MIN_SAMPLES) { 200.0 })!!, 0.0)
        // A slide into the note and a stray octave jump do not move it.
        val heard = listOf(150.0, 170.0, 190.0) + List(20) { 200.0 } + listOf(400.0, 400.0)
        assertEquals(200.0, VoiceRangeAdvisor.heldPitchHz(heard)!!, 0.0)
        // What the pitch engine cannot hear is ignored, and does not count towards enough.
        assertNull(VoiceRangeAdvisor.heldPitchHz(List(5) { 200.0 } + List(20) { 30.0 }))
        // Only the most recent detections count: the note as it settles.
        val settled = List(100) { 150.0 } + List(VoiceRangeAdvisor.MAX_SAMPLES) { 210.0 }
        assertEquals(210.0, VoiceRangeAdvisor.heldPitchHz(settled)!!, 0.0)
    }
}
