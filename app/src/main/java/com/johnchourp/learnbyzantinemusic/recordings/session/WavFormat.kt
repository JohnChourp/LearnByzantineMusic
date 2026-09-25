package com.johnchourp.learnbyzantinemusic.recordings.session

import java.io.File
import java.io.RandomAccessFile

/**
 * The format every recording is captured in — 44.1 kHz, mono, PCM16 — and the 44-byte RIFF header
 * that turns the raw capture into a WAV.
 *
 * **Why the header is written last.** Capture writes 44 zero bytes, then PCM as it arrives; only the
 * end of a recording knows its length. [finalizeHeader] therefore derives the header from what is
 * actually on disk, which is also what makes a capture file left behind by a killed process
 * recoverable: its audio is intact, only its header is still zero.
 *
 * The bytes are exactly those the recording screen has always written (pinned by `WavFormatTest`).
 */
object WavFormat {
    const val SAMPLE_RATE = 44_100
    const val CHANNELS = 1
    const val BITS_PER_SAMPLE = 16
    const val HEADER_SIZE = 44
    const val BYTES_PER_SECOND = SAMPLE_RATE * CHANNELS * (BITS_PER_SAMPLE / 8)
    private const val BLOCK_ALIGN = CHANNELS * (BITS_PER_SAMPLE / 8)

    /** Writes the header for [pcmBytes] of audio over the first 44 bytes of [file]. */
    fun writeHeader(file: File, pcmBytes: Long) {
        val totalDataLen = pcmBytes + 36
        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(0)
            raf.writeBytes("RIFF")
            raf.writeInt(Integer.reverseBytes(totalDataLen.toInt()))
            raf.writeBytes("WAVE")
            raf.writeBytes("fmt ")
            raf.writeInt(Integer.reverseBytes(16))
            raf.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt())
            raf.writeShort(java.lang.Short.reverseBytes(CHANNELS.toShort()).toInt())
            raf.writeInt(Integer.reverseBytes(SAMPLE_RATE))
            raf.writeInt(Integer.reverseBytes(BYTES_PER_SECOND))
            raf.writeShort(java.lang.Short.reverseBytes(BLOCK_ALIGN.toShort()).toInt())
            raf.writeShort(java.lang.Short.reverseBytes(BITS_PER_SAMPLE.toShort()).toInt())
            raf.writeBytes("data")
            raf.writeInt(Integer.reverseBytes(pcmBytes.toInt()))
        }
    }

    /**
     * Writes the header that matches the audio on disk and returns how many audio bytes it declares.
     *
     * Idempotent, so it is safe on a file that already has one. A trailing half sample (a process
     * killed mid-write) is left in place but not declared. The file's modification time — the moment
     * the capture last wrote, which names a recovered recording — is kept.
     */
    fun finalizeHeader(file: File): Long {
        val modified = file.lastModified()
        val pcmBytes = pcmBytesOf(file.length())
        writeHeader(file, pcmBytes)
        if (modified > 0L) file.setLastModified(modified)
        return pcmBytes
    }

    /** Whole samples of audio in a capture file of [fileLength] bytes; 0 when there is only a header. */
    fun pcmBytesOf(fileLength: Long): Long =
        ((fileLength - HEADER_SIZE).coerceAtLeast(0L) / BLOCK_ALIGN) * BLOCK_ALIGN

    fun durationMs(pcmBytes: Long): Long = pcmBytes * 1000L / BYTES_PER_SECOND
}
