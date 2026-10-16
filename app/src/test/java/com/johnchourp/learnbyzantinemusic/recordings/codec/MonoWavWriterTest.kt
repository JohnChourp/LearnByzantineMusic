package com.johnchourp.learnbyzantinemusic.recordings.codec

import com.johnchourp.learnbyzantinemusic.recordings.analysis.WavPcmReader
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MonoWavWriterTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun whatIsWrittenReadsBackAtItsRate() {
        val file = tmp.newFile("out.wav")
        val samples = floatArrayOf(0f, 0.5f, -0.5f, 0.999f, -1f, 0.25f)
        MonoWavWriter(file, 22_050).use { writer ->
            writer.write(samples, 3)
            writer.write(samples.copyOfRange(3, 6))
            assertEquals(6L, writer.samplesWritten)
        }
        assertEquals(44L + 12L, file.length())
        file.inputStream().use { input ->
            val reader = WavPcmReader.open(input)
            assertEquals(22_050, reader.sampleRate)
            assertEquals(6L, reader.totalSamples)
            val back = FloatArray(10)
            assertEquals(6, reader.read(back))
            for (i in samples.indices) assertEquals("sample $i", samples[i], back[i], 1f / 32_768f)
        }
    }

    @Test
    fun outOfRangeSamplesAreClippedNotWrapped() {
        val file = tmp.newFile("clip.wav")
        MonoWavWriter(file, 44_100).use { it.write(floatArrayOf(1.5f, -1.5f)) }
        file.inputStream().use { input ->
            val back = FloatArray(2)
            WavPcmReader.open(input).read(back)
            assertEquals(32_767f / 32_768f, back[0], 1e-6f)
            assertEquals(-1f, back[1], 1e-6f)
        }
    }

    @Test
    fun anEmptyRecordingIsAValidEmptyWav() {
        val file = tmp.newFile("empty.wav")
        MonoWavWriter(file, 22_050).close()
        assertEquals(44L, file.length())
        file.inputStream().use { assertEquals(-1, WavPcmReader.open(it).read(FloatArray(1))) }
    }
}
