package com.johnchourp.learnbyzantinemusic.recordings.codec

import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.math.roundToInt

/**
 * Writes mono float samples as a 16-bit PCM WAV at any rate. The header is written last, from the
 * number of samples actually written, so a WAV is only ever complete or absent.
 *
 * Used by the analysis decoder (22 050 Hz); the capture keeps its own fixed 44.1 kHz
 * [com.johnchourp.learnbyzantinemusic.recordings.session.WavFormat]. Pure JVM (`MonoWavWriterTest`).
 */
class MonoWavWriter(private val file: File, private val sampleRate: Int) : Closeable {
    private val out = BufferedOutputStream(FileOutputStream(file))
    private val scratch = ByteArray(2 * 4_096)
    private var samples = 0L
    private var closed = false

    init {
        require(sampleRate > 0) { "sample rate must be positive: $sampleRate" }
        out.write(ByteArray(HEADER_SIZE))
    }

    val samplesWritten: Long get() = samples

    fun write(input: FloatArray, count: Int = input.size) {
        var index = 0
        while (index < count) {
            var used = 0
            while (index < count && used < scratch.size) {
                val value = (input[index++] * 32768f).roundToInt().coerceIn(-32768, 32767)
                scratch[used++] = (value and 0xFF).toByte()
                scratch[used++] = ((value shr 8) and 0xFF).toByte()
            }
            out.write(scratch, 0, used)
        }
        samples += count
    }

    /** Flushes the samples and writes the header that declares them. */
    override fun close() {
        if (closed) return
        closed = true
        out.close()
        val dataBytes = samples * 2
        require(dataBytes + HEADER_SIZE - 8 <= 0xFFFFFFFFL) { "a WAV holds at most 4 GiB" }
        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(0)
            raf.writeBytes("RIFF")
            raf.writeInt(Integer.reverseBytes((dataBytes + 36).toInt()))
            raf.writeBytes("WAVE")
            raf.writeBytes("fmt ")
            raf.writeInt(Integer.reverseBytes(16))
            raf.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt()) // PCM
            raf.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt()) // mono
            raf.writeInt(Integer.reverseBytes(sampleRate))
            raf.writeInt(Integer.reverseBytes(sampleRate * 2)) // byte rate
            raf.writeShort(java.lang.Short.reverseBytes(2.toShort()).toInt()) // block align
            raf.writeShort(java.lang.Short.reverseBytes(16.toShort()).toInt())
            raf.writeBytes("data")
            raf.writeInt(Integer.reverseBytes(dataBytes.toInt()))
        }
    }

    private companion object {
        const val HEADER_SIZE = 44
    }
}
