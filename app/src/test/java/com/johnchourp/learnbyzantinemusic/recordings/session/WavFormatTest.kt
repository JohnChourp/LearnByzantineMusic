package com.johnchourp.learnbyzantinemusic.recordings.session

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The capture format did not change when the header code moved out of `RecordingsActivity`
 * (ClickUp `869f5x26x`): 44.1 kHz, mono, PCM16, and the same 44 bytes, field by field.
 *
 * [WavFormat.finalizeHeader] is also what recovers a capture left by a killed process, so it is
 * pinned on a file whose header is still all zeros — the state such a file is found in.
 */
class WavFormatTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun theHeaderIsTheOneTheRecorderHasAlwaysWritten() {
        val file = tmp.newFile("a.wav").apply { writeBytes(ByteArray(WavFormat.HEADER_SIZE + 1000)) }

        WavFormat.writeHeader(file, 1000)

        val header = file.readBytes().copyOfRange(0, WavFormat.HEADER_SIZE)
        val le = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals("RIFF", String(header, 0, 4, Charsets.US_ASCII))
        assertEquals("RIFF size = data + 36", 1036, le.getInt(4))
        assertEquals("WAVEfmt ", String(header, 8, 8, Charsets.US_ASCII))
        assertEquals("fmt chunk size", 16, le.getInt(16))
        assertEquals("PCM", 1, le.getShort(20).toInt())
        assertEquals("mono", 1, le.getShort(22).toInt())
        assertEquals("sample rate", 44_100, le.getInt(24))
        assertEquals("byte rate", 88_200, le.getInt(28))
        assertEquals("block align", 2, le.getShort(32).toInt())
        assertEquals("bits per sample", 16, le.getShort(34).toInt())
        assertEquals("data", String(header, 36, 4, Charsets.US_ASCII))
        assertEquals("data size", 1000, le.getInt(40))
    }

    @Test
    fun finalizingAZeroHeaderDeclaresTheAudioOnDiskAndKeepsItIntact() {
        val audio = SessionTestKit.pcm(1001)
        val file = SessionTestKit.captureFile(tmp.newFile("orphan.wav"), audio)
        file.setLastModified(1_700_000_000_000L)

        val declared = WavFormat.finalizeHeader(file)

        assertEquals("whole samples only: the trailing half sample is not declared", 1000L, declared)
        val bytes = file.readBytes()
        assertEquals(1000, ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt(40))
        assertArrayEquals("the audio itself is untouched", audio, bytes.copyOfRange(WavFormat.HEADER_SIZE, bytes.size))
        assertEquals("the time that names a recovered recording is kept", 1_700_000_000_000L, file.lastModified())
    }

    @Test
    fun finalizingTwiceChangesNothing() {
        val file = SessionTestKit.captureFile(tmp.newFile("b.wav"), SessionTestKit.pcm(400))
        WavFormat.finalizeHeader(file)
        val once = file.readBytes()
        WavFormat.finalizeHeader(file)
        assertArrayEquals(once, file.readBytes())
    }

    @Test
    fun aHeaderAloneHoldsNoAudio() {
        assertEquals(0L, WavFormat.pcmBytesOf(44))
        assertEquals(0L, WavFormat.pcmBytesOf(0))
        assertEquals(0L, WavFormat.pcmBytesOf(45))
        assertEquals(2L, WavFormat.pcmBytesOf(46))
    }

    @Test
    fun durationFollowsTheByteRate() {
        assertEquals(1_000L, WavFormat.durationMs(88_200))
        assertEquals(60_000L, WavFormat.durationMs(88_200L * 60))
    }
}
