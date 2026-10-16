package com.johnchourp.learnbyzantinemusic.recordings.codec

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class PcmResamplerTest {

    @Test
    fun sameRateIsACopy() {
        val input = floatArrayOf(0.1f, -0.2f, 0.3f)
        assertArrayEquals(input, PcmResampler(44_100, 44_100).process(input), 0f)
    }

    @Test
    fun lengthFollowsTheRatioBothWays() {
        val second = sine(440.0, 44_100, seconds = 1.0)
        assertEquals(22_050.0, PcmResampler(44_100, 22_050).process(second).size.toDouble(), 2.0)
        assertEquals(48_000.0, PcmResampler(44_100, 48_000).process(second).size.toDouble(), 2.0)
    }

    @Test
    fun pitchIsKeptWhenTheRateChanges() {
        for (target in listOf(22_050, 48_000)) {
            val out = PcmResampler(44_100, target).process(sine(220.0, 44_100, seconds = 1.0))
            // A sine crosses zero upwards once per period: 220 times in a second, at any rate.
            assertEquals("→ $target Hz", 220.0, upwardCrossings(out).toDouble(), 2.0)
        }
    }

    @Test
    fun chunksGiveTheSameResultAsOnePass() {
        val signal = sine(330.0, 44_100, seconds = 0.5)
        for (target in listOf(22_050, 48_000)) {
            val whole = PcmResampler(44_100, target).process(signal)
            val chunked = PcmResampler(44_100, target).let { resampler ->
                signal.toList().chunked(1_000).flatMap { resampler.process(it.toFloatArray()).toList() }.toFloatArray()
            }
            assertArrayEquals("→ $target Hz", whole, chunked, 1e-6f)
        }
    }

    @Test
    fun downsamplingDampsWhatTheNewRateCannotHold() {
        // 15 kHz is above 11 025 Hz, the Nyquist frequency of 22 050 Hz: it must not come back loud.
        val out = PcmResampler(44_100, 22_050).process(sine(15_000.0, 44_100, seconds = 0.2))
        val peak = out.drop(10).maxOf { abs(it) }
        assertTrue("peak was $peak", peak < 0.5f)
    }

    private fun sine(hz: Double, rate: Int, seconds: Double): FloatArray =
        FloatArray((rate * seconds).toInt()) { i -> (0.8 * sin(2 * PI * hz * i / rate)).toFloat() }

    private fun upwardCrossings(samples: FloatArray): Int =
        (1 until samples.size).count { samples[it - 1] < 0f && samples[it] >= 0f }
}
