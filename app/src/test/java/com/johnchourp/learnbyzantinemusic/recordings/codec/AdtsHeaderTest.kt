package com.johnchourp.learnbyzantinemusic.recordings.codec

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdtsHeaderTest {

    @Test
    fun headerForA371ByteMono44100Frame() {
        // frame length 378 = 0b001_0111_1010; AAC LC (1), rate index 4 (44.1 kHz), mono.
        val header = AdtsHeader.forFrame(payloadSize = 371, sampleRate = 44_100, channels = 1)
        assertArrayEquals(
            byteArrayOf(0xFF.toByte(), 0xF1.toByte(), 0x50, 0x40, 0x2F, 0x5F, 0xFC.toByte()),
            header,
        )
    }

    @Test
    fun fieldsDecodeBackToWhatWasAskedFor() {
        for ((rate, index) in listOf(48_000 to 3, 44_100 to 4, 22_050 to 7, 8_000 to 11)) {
            for (channels in 1..2) {
                val payload = 1_234
                val h = AdtsHeader.forFrame(payload, rate, channels).map { it.toInt() and 0xFF }
                assertEquals(0xFFF, (h[0] shl 4) or (h[1] shr 4))
                assertEquals("protection absent", 1, h[1] and 1)
                assertEquals("AAC LC", 1, h[2] shr 6)
                assertEquals(index, (h[2] shr 2) and 0xF)
                assertEquals(channels, ((h[2] and 1) shl 2) or (h[3] shr 6))
                val frameLength = ((h[3] and 3) shl 11) or (h[4] shl 3) or (h[5] shr 5)
                assertEquals(payload + AdtsHeader.SIZE, frameLength)
                assertEquals("buffer fullness = variable", 0x7FF, ((h[5] and 0x1F) shl 6) or (h[6] shr 2))
                assertEquals("one raw data block", 0, h[6] and 3)
            }
        }
    }

    @Test
    fun onlyAdtsRatesAreSupported() {
        assertTrue(AdtsHeader.isSupportedRate(44_100))
        assertFalse(AdtsHeader.isSupportedRate(44_000))
    }

    @Test(expected = IllegalArgumentException::class)
    fun aFrameTooLongForThe13BitFieldIsRefused() {
        AdtsHeader.forFrame(payloadSize = 8_191, sampleRate = 44_100, channels = 1)
    }
}
