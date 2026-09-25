package com.johnchourp.learnbyzantinemusic.lessons.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ClickUp `869f5x2d7`: how each device is made to feel a strong θέση and a light άρση — amplitude
 * where it can be controlled, the predefined effects on Android 10+, the duration otherwise.
 */
class BeatVibrationPlanTest {

    private fun strong(sdk: Int, amplitude: Boolean) = BeatVibration.choose(BeatStrength.STRONG, sdk, amplitude)
    private fun light(sdk: Int, amplitude: Boolean) = BeatVibration.choose(BeatStrength.LIGHT, sdk, amplitude)

    @Test
    fun beforeAndroid8TheDurationCarriesTheStrength() {
        listOf(24, 25).forEach { sdk ->
            val s = strong(sdk, amplitude = false) as BeatVibration.Legacy
            val l = light(sdk, amplitude = false) as BeatVibration.Legacy
            assertTrue("API $sdk", s.millis > l.millis)
            // There is no amplitude API before 26, whatever the hardware says.
            assertTrue(strong(sdk, amplitude = true) is BeatVibration.Legacy)
        }
    }

    @Test
    fun withAmplitudeControlItIsTheSameBuzzAtTwoStrengths() {
        listOf(26, 29, 34).forEach { sdk ->
            val s = strong(sdk, amplitude = true) as BeatVibration.Amplitude
            val l = light(sdk, amplitude = true) as BeatVibration.Amplitude
            assertTrue("API $sdk", s.amplitude > l.amplitude)
            assertTrue(s.amplitude in 1..255 && l.amplitude in 1..255)
        }
    }

    @Test
    fun withoutAmplitudeControlAndroid10UsesThePlatformsOwnClicks() {
        listOf(29, 31, 34).forEach { sdk ->
            assertEquals(BeatVibration.Predefined(BeatVibration.Effect.HEAVY_CLICK), strong(sdk, amplitude = false))
            assertEquals(BeatVibration.Predefined(BeatVibration.Effect.TICK), light(sdk, amplitude = false))
        }
    }

    @Test
    fun withoutAmplitudeControlAndroid8And9UseTheDuration() {
        listOf(26, 27, 28).forEach { sdk ->
            val s = strong(sdk, amplitude = false) as BeatVibration.OneShot
            val l = light(sdk, amplitude = false) as BeatVibration.OneShot
            assertTrue("API $sdk", s.millis > l.millis)
        }
    }

    @Test
    fun theStrongPulseEndsLongBeforeTheFastestNextBeat() {
        val fastest = MetronomeSchedule.intervalMillis(MetronomeSchedule.MAX_BPM)
        assertTrue(BeatVibration.STRONG_MILLIS < fastest / 2)
    }
}
