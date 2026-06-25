package com.johnchourp.learnbyzantinemusic.trainer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class YinPitchDetectorTest {

    private val sampleRate = 44_100
    private val windowSize = 2_048

    private fun sine(frequencyHz: Double, harmonicGain: Double = 0.0): FloatArray =
        FloatArray(windowSize) { i ->
            val t = i.toDouble() / sampleRate
            val fundamental = sin(2.0 * PI * frequencyHz * t)
            val harmonic = harmonicGain * sin(2.0 * PI * 2.0 * frequencyHz * t)
            (0.9 * (fundamental + harmonic)).toFloat()
        }

    private fun assertNear(expected: Double, actual: Float, tolerancePercent: Double) {
        val tolerance = expected * tolerancePercent
        assertEquals("detected $actual, expected ~$expected", expected, actual.toDouble(), tolerance)
    }

    @Test
    fun `detects pure sine fundamentals`() {
        assertNear(220.0, YinPitchDetector.detect(sine(220.0), sampleRate), 0.01)
        assertNear(330.0, YinPitchDetector.detect(sine(330.0), sampleRate), 0.01)
        assertNear(440.0, YinPitchDetector.detect(sine(440.0), sampleRate), 0.01)
    }

    @Test
    fun `detects the fundamental of a tone with a harmonic`() {
        assertNear(246.94, YinPitchDetector.detect(sine(246.94, harmonicGain = 0.4), sampleRate), 0.015)
    }

    @Test
    fun `returns no pitch for silence`() {
        assertTrue(YinPitchDetector.detect(FloatArray(windowSize), sampleRate) <= 0f)
    }

    @Test
    fun `returns no pitch for a too-small buffer`() {
        assertTrue(YinPitchDetector.detect(FloatArray(4), sampleRate) <= 0f)
    }
}
