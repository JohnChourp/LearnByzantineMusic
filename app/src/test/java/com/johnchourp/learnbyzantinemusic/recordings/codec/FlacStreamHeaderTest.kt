package com.johnchourp.learnbyzantinemusic.recordings.codec

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlacStreamHeaderTest {

    /** `fLaC`, a last-block STREAMINFO header, then STREAMINFO for 44.1 kHz, mono, 16-bit, [total] samples. */
    private fun header(total: Long = 0L, withMarker: Boolean = true): ByteArray {
        val streamInfo = ByteArray(34)
        streamInfo[0] = 0x10; streamInfo[1] = 0x00 // min block 4096
        streamInfo[2] = 0x10; streamInfo[3] = 0x00 // max block 4096
        // 20-bit rate 44100 (0x0AC44) · 3-bit channels-1 (0) · 5-bit bps-1 (15) · 36-bit total
        val packed = (44_100L shl 44) or (0L shl 41) or (15L shl 36) or total
        for (i in 0 until 8) streamInfo[10 + i] = (packed ushr (56 - 8 * i)).toByte()
        val block = byteArrayOf(0x80.toByte(), 0x00, 0x00, 34) + streamInfo
        return if (withMarker) FlacStreamHeader.MARKER + block else block
    }

    @Test
    fun theFixtureIsARealStreamInfo() {
        assertTrue(FlacStreamHeader.hasStreamInfo(header()))
        assertEquals(0L, FlacStreamHeader.totalSamples(header()))
        assertEquals(1_234_567L, FlacStreamHeader.totalSamples(header(1_234_567L)))
    }

    @Test
    fun thePatchWritesTheCountAndKeepsBitsPerSample() {
        val original = header(total = 0L)
        for (total in listOf(1L, 88_200L, 158_760_000L, FlacStreamHeader.MAX_TOTAL_SAMPLES)) {
            val patched = original.copyOf()
            val at = FlacStreamHeader.TOTAL_SAMPLES_OFFSET
            FlacStreamHeader.totalSamplesPatch(patched[at], total).copyInto(patched, at)
            assertEquals(total, FlacStreamHeader.totalSamples(patched))
            // Everything but the 36-bit field is untouched: same as a header built with that count.
            assertArrayEquals("total $total", header(total), patched)
        }
    }

    @Test
    fun assembleJoinsSplitConfigAndAddsAMissingMarker() {
        val full = header(total = 0L)
        assertArrayEquals(full, FlacStreamHeader.assemble(listOf(full)))
        assertArrayEquals(full, FlacStreamHeader.assemble(listOf(full.copyOfRange(0, 4), full.copyOfRange(4, full.size))))
        assertArrayEquals(full, FlacStreamHeader.assemble(listOf(header(withMarker = false))))
    }

    @Test
    fun onlyAFullStreamInfoIsConsideredPatchable() {
        assertFalse(FlacStreamHeader.hasStreamInfo(FlacStreamHeader.MARKER))
        assertFalse(FlacStreamHeader.hasStreamInfo(header(withMarker = false)))
        val vorbisFirst = header().copyOf().also { it[4] = 0x84.toByte() } // block type 4
        assertFalse(FlacStreamHeader.hasStreamInfo(vorbisFirst))
    }

    @Test(expected = IllegalArgumentException::class)
    fun aCountBeyond36BitsIsRefused() {
        FlacStreamHeader.totalSamplesPatch(0, FlacStreamHeader.MAX_TOTAL_SAMPLES + 1)
    }
}
