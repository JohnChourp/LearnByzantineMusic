package com.johnchourp.learnbyzantinemusic.recordings.player

import com.johnchourp.learnbyzantinemusic.music.ByzantineTuning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The in-app player's speed and shift (ClickUp `869f5x268`): speed stays between ½× and 1×, the shift
 * within ±12 μόρια, and the shift becomes a pitch factor only through [ByzantineTuning]. Out-of-range
 * values are clamped, never rejected — a slider cannot hand in anything the player refuses.
 */
class PlaybackTuningTest {

    @Test
    fun speedStaysBetweenHalfAndOne() {
        assertEquals(0.5f, PlaybackTuning().withSpeed(0.25f).speed)
        assertEquals(1f, PlaybackTuning().withSpeed(1.5f).speed)
        assertEquals(0.75f, PlaybackTuning().withSpeed(0.75f).speed)
        assertEquals("the edges themselves are allowed", 0.5f, PlaybackTuning().withSpeed(0.5f).speed)
        assertEquals("nonsense falls back to normal speed", 1f, PlaybackTuning().withSpeed(Float.NaN).speed)
    }

    @Test
    fun theShiftStaysWithinTwelveMoriaEitherWay() {
        assertEquals(12, PlaybackTuning().withShift(30).shiftMoria)
        assertEquals(-12, PlaybackTuning().withShift(-30).shiftMoria)
        assertEquals(5, PlaybackTuning().withShift(5).shiftMoria)
        assertEquals(12, PlaybackTuning.MAX_SHIFT_MORIA)
    }

    @Test
    fun theShiftBecomesAPitchFactorThroughByzantineTuning() {
        listOf(-12, -1, 0, 7, 12).forEach { moria ->
            assertEquals(
                "at $moria μόρια",
                ByzantineTuning.ratioForMoria(moria.toDouble()).toFloat(),
                PlaybackTuning(shiftMoria = moria).pitchRatio,
            )
        }
        assertTrue("up is higher", PlaybackTuning(shiftMoria = 3).pitchRatio > 1f)
        assertTrue("down is lower", PlaybackTuning(shiftMoria = -3).pitchRatio < 1f)
    }

    @Test
    fun speedAndShiftAreIndependent() {
        val tuning = PlaybackTuning().withSpeed(0.5f).withShift(4)
        assertEquals(0.5f, tuning.speed)
        assertEquals("slowing down must not move the pitch", PlaybackTuning(shiftMoria = 4).pitchRatio, tuning.pitchRatio)
        assertEquals(1f, PlaybackTuning().withSpeed(0.5f).pitchRatio)
    }

    @Test
    fun theDefaultIsTheRecordingAsItIs() {
        assertTrue(PlaybackTuning().isDefault)
        assertEquals(1f, PlaybackTuning().pitchRatio)
        assertFalse(PlaybackTuning().withSpeed(0.9f).isDefault)
        assertFalse(PlaybackTuning().withShift(-1).isDefault)
    }
}
