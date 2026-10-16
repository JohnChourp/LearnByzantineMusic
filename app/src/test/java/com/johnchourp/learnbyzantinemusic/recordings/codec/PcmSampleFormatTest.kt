package com.johnchourp.learnbyzantinemusic.recordings.codec

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PcmSampleFormatTest {

    private fun read(format: PcmSampleFormat, vararg bytes: Int): Float =
        format.read(ByteBuffer.wrap(ByteArray(bytes.size) { bytes[it].toByte() }).order(ByteOrder.LITTLE_ENDIAN))

    @Test
    fun eightBitIsUnsignedAroundOneTwentyEight() {
        assertEquals(0f, read(PcmSampleFormat.PCM_8, 128), 0f)
        assertEquals(-1f, read(PcmSampleFormat.PCM_8, 0), 0f)
        assertEquals(127f / 128f, read(PcmSampleFormat.PCM_8, 255), 1e-6f)
    }

    @Test
    fun sixteenBit() {
        assertEquals(0.5f, read(PcmSampleFormat.PCM_16, 0x00, 0x40), 1e-6f)
        assertEquals(-1f, read(PcmSampleFormat.PCM_16, 0x00, 0x80), 0f)
    }

    @Test
    fun twentyFourBitPackedKeepsItsSign() {
        assertEquals(0.5f, read(PcmSampleFormat.PCM_24_PACKED, 0x00, 0x00, 0x40), 1e-6f)
        assertEquals(-0.5f, read(PcmSampleFormat.PCM_24_PACKED, 0x00, 0x00, 0xC0), 1e-6f)
        assertEquals(-1f, read(PcmSampleFormat.PCM_24_PACKED, 0x00, 0x00, 0x80), 0f)
        assertEquals(-1f / 8_388_608f, read(PcmSampleFormat.PCM_24_PACKED, 0xFF, 0xFF, 0xFF), 1e-9f)
    }

    @Test
    fun thirtyTwoBitAndFloat() {
        assertEquals(0.5f, read(PcmSampleFormat.PCM_32, 0x00, 0x00, 0x00, 0x40), 1e-6f)
        assertEquals(-1f, read(PcmSampleFormat.PCM_32, 0x00, 0x00, 0x00, 0x80), 0f)
        val half = java.lang.Float.floatToIntBits(-0.25f)
        assertEquals(-0.25f, read(PcmSampleFormat.FLOAT, half and 0xFF, (half shr 8) and 0xFF, (half shr 16) and 0xFF, half ushr 24), 0f)
    }

    @Test
    fun androidEncodingIdsMapBackAndUnknownOnesAreRefused() {
        // android.media.AudioFormat: 8BIT=3, 16BIT=2, FLOAT=4, 24BIT_PACKED=21, 32BIT=22.
        assertEquals(PcmSampleFormat.PCM_8, PcmSampleFormat.fromEncoding(3))
        assertEquals(PcmSampleFormat.PCM_16, PcmSampleFormat.fromEncoding(2))
        assertEquals(PcmSampleFormat.FLOAT, PcmSampleFormat.fromEncoding(4))
        assertEquals(PcmSampleFormat.PCM_24_PACKED, PcmSampleFormat.fromEncoding(21))
        assertEquals(PcmSampleFormat.PCM_32, PcmSampleFormat.fromEncoding(22))
        assertNull(PcmSampleFormat.fromEncoding(5)) // AC3, not PCM
    }
}
