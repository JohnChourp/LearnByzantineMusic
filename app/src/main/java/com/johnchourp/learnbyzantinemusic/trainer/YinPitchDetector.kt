package com.johnchourp.learnbyzantinemusic.trainer

/**
 * Monophonic fundamental-frequency estimator using the YIN algorithm
 * (de Cheveigné & Kawahara, 2002). It is well suited to a single sung voice and carries
 * no Android dependency, so it is fully unit-testable on synthetic waveforms.
 *
 * [detect] returns the estimated frequency in Hz, or a non-positive value when no
 * confident pitch is found (silence, noise, or out of the singing range).
 */
object YinPitchDetector {
    const val DEFAULT_THRESHOLD = 0.15
    const val MIN_FREQUENCY_HZ = 70.0
    const val MAX_FREQUENCY_HZ = 1200.0

    fun detect(
        samples: FloatArray,
        sampleRate: Int,
        threshold: Double = DEFAULT_THRESHOLD
    ): Float {
        val n = samples.size
        val half = n / 2
        if (half < 4 || sampleRate <= 0) return -1f

        val yin = DoubleArray(half)

        // Step 1: difference function.
        for (tau in 0 until half) {
            var sum = 0.0
            for (i in 0 until half) {
                val delta = (samples[i] - samples[i + tau]).toDouble()
                sum += delta * delta
            }
            yin[tau] = sum
        }

        // Step 2: cumulative mean normalized difference.
        yin[0] = 1.0
        var runningSum = 0.0
        for (tau in 1 until half) {
            runningSum += yin[tau]
            yin[tau] = if (runningSum <= 0.0) 1.0 else yin[tau] * tau / runningSum
        }

        // Step 3: absolute threshold within the plausible singing range.
        val minTau = (sampleRate / MAX_FREQUENCY_HZ).toInt().coerceAtLeast(2)
        val maxTau = (sampleRate / MIN_FREQUENCY_HZ).toInt().coerceAtMost(half - 1)
        if (minTau >= maxTau) return -1f

        var tauEstimate = -1
        var tau = minTau
        while (tau <= maxTau) {
            if (yin[tau] < threshold) {
                while (tau + 1 <= maxTau && yin[tau + 1] < yin[tau]) {
                    tau++
                }
                tauEstimate = tau
                break
            }
            tau++
        }
        if (tauEstimate == -1) return -1f

        // Step 4: parabolic interpolation for sub-sample period accuracy.
        val betterTau = parabolicInterpolation(yin, tauEstimate)
        if (betterTau <= 0.0) return -1f

        val frequency = sampleRate / betterTau
        if (frequency < MIN_FREQUENCY_HZ || frequency > MAX_FREQUENCY_HZ) return -1f
        return frequency.toFloat()
    }

    private fun parabolicInterpolation(yin: DoubleArray, tau: Int): Double {
        val x0 = if (tau < 1) tau else tau - 1
        val x2 = if (tau + 1 < yin.size) tau + 1 else tau
        if (x0 == tau) return if (yin[tau] <= yin[x2]) tau.toDouble() else x2.toDouble()
        if (x2 == tau) return if (yin[tau] <= yin[x0]) tau.toDouble() else x0.toDouble()
        val s0 = yin[x0]
        val s1 = yin[tau]
        val s2 = yin[x2]
        val denominator = 2.0 * (2.0 * s1 - s2 - s0)
        if (denominator == 0.0) return tau.toDouble()
        return tau + (s2 - s0) / denominator
    }
}
