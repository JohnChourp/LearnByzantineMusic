package com.johnchourp.learnbyzantinemusic.recordings.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

class PitchTrackAnalyzerTest {
    private val sampleRate = 22_050

    private fun tone(hz: Double, seconds: Double, amplitude: Double = 0.5): FloatArray {
        val count = (seconds * sampleRate).toInt()
        return FloatArray(count) { (amplitude * sin(2 * PI * hz * it / sampleRate)).toFloat() }
    }

    private fun silence(seconds: Double) = FloatArray((seconds * sampleRate).toInt())

    @Test
    fun tracksTwoTonesAndTheSilenceBetween() {
        val samples = tone(220.0, 0.5) + silence(0.3) + tone(330.0, 0.5)
        val track = PitchTrackAnalyzer.analyze(samples, sampleRate)

        val firstTone = track.frames.filter { it.timeMs in 50..400 }
        assertTrue(firstTone.isNotEmpty())
        firstTone.forEach { assertEquals(220.0, it.frequencyHz.toDouble(), 2.2) }

        val gap = track.frames.filter { it.timeMs in 560..740 }
        assertTrue(gap.isNotEmpty())
        gap.forEach { assertFalse("${it.timeMs}ms should be silent", it.isVoiced) }

        val secondTone = track.frames.filter { it.timeMs in 900..1200 }
        assertTrue(secondTone.isNotEmpty())
        secondTone.forEach { assertEquals(330.0, it.frequencyHz.toDouble(), 3.3) }
    }

    @Test
    fun streamingAndInMemoryAnalysisAgree() {
        val samples = tone(261.63, 0.4) + silence(0.1) + tone(392.0, 0.4)
        val pcm = ShortArray(samples.size) { (samples[it] * 32767f).roundToInt().toShort() }
        val reader = WavPcmReader.open(ByteArrayInputStream(WavPcmReaderTest.wav(pcm, channels = 1, sampleRate = sampleRate)))
        var lastProgress = 0f
        val streamed = PitchTrackAnalyzer.analyze(reader) { lastProgress = it }
        val inMemory = PitchTrackAnalyzer.analyze(samples, sampleRate)

        assertEquals(inMemory.frames.size, streamed.frames.size)
        inMemory.frames.zip(streamed.frames).forEach { (a, b) ->
            assertEquals(a.timeMs, b.timeMs)
            assertEquals(a.isVoiced, b.isVoiced)
            if (a.isVoiced) assertEquals(a.frequencyHz.toDouble(), b.frequencyHz.toDouble(), 1.0)
        }
        assertTrue(lastProgress > 0.9f)
    }
}
