package com.johnchourp.learnbyzantinemusic.recordings.codec

import kotlin.math.ceil

/**
 * Streaming sample-rate conversion of mono float PCM, chunk by chunk, so an hour-long recording never
 * has to fit in memory.
 *
 * Linear interpolation between neighbouring samples. When the rate goes **down** (the analysis reads
 * at 22 050 Hz), a moving average as wide as the ratio runs first, so content above the new Nyquist
 * frequency is damped instead of folding back as false pitch. Good enough for pitch tracking and for
 * feeding an encoder that only takes certain rates (Opus); not a mastering-grade resampler.
 *
 * Pure JVM, so `PcmResamplerTest` pins length, pitch and continuity across chunks.
 */
class PcmResampler(private val inputRate: Int, private val outputRate: Int) {
    init {
        require(inputRate > 0 && outputRate > 0) { "rates must be positive: $inputRate → $outputRate" }
    }

    private val step = inputRate.toDouble() / outputRate
    private val averageWidth = if (outputRate < inputRate) ceil(step).toInt().coerceAtLeast(1) else 1
    private val averageWindow = FloatArray(averageWidth)
    private var averageIndex = 0
    private var averageSum = 0.0
    private var averageFilled = 0

    /** Position of the next output sample, in input samples, relative to [previous]. */
    private var position = 0.0
    private var previous = 0f
    private var hasPrevious = false

    val isIdentity: Boolean get() = inputRate == outputRate

    /** Converts [count] samples of [input]; returns the new samples (possibly none). */
    fun process(input: FloatArray, count: Int = input.size): FloatArray {
        if (count <= 0) return FloatArray(0)
        if (isIdentity) return input.copyOf(count)
        val out = FloatArray(ceil((count + 1) / step).toInt() + 1)
        var written = 0
        for (i in 0 until count) {
            val current = smooth(input[i])
            if (!hasPrevious) {
                previous = current
                hasPrevious = true
                continue
            }
            // Emit every output sample that falls between [previous] (0.0) and [current] (1.0).
            while (position < 1.0) {
                out[written++] = (previous + (current - previous) * position).toFloat()
                position += step
            }
            position -= 1.0
            previous = current
        }
        return out.copyOf(written)
    }

    private fun smooth(sample: Float): Float {
        if (averageWidth == 1) return sample
        averageSum -= averageWindow[averageIndex]
        averageWindow[averageIndex] = sample
        averageSum += sample
        averageIndex = (averageIndex + 1) % averageWidth
        if (averageFilled < averageWidth) averageFilled++
        return (averageSum / averageFilled).toFloat()
    }
}
