package com.johnchourp.learnbyzantinemusic.recordings.analysis

import java.io.BufferedInputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream

/**
 * Streams the samples of a 16-bit PCM WAV file as mono floats in [-1, 1] (channels are averaged),
 * skipping any non-audio chunks (ffmpeg writes a LIST chunk before the data).
 */
class WavPcmReader private constructor(
    private val input: InputStream,
    val sampleRate: Int,
    private val channels: Int,
    private val dataBytes: Long,
) {
    private var bytesRead = 0L
    private val frameBytes = channels * 2
    private val raw = ByteArray(4096 * frameBytes)

    /** Total mono samples in the data chunk (a best effort for progress; streaming WAVs may lie). */
    val totalSamples: Long get() = dataBytes / frameBytes

    /** Fills [target] from [offset]; returns how many samples were written, or -1 at the end. */
    fun read(target: FloatArray, offset: Int = 0, length: Int = target.size - offset): Int {
        if (bytesRead >= dataBytes) return -1
        val maxFrames = minOf(length, raw.size / frameBytes, ((dataBytes - bytesRead) / frameBytes).toInt())
        if (maxFrames <= 0) return -1
        val wanted = maxFrames * frameBytes
        var filled = 0
        while (filled < wanted) {
            val count = input.read(raw, filled, wanted - filled)
            if (count < 0) break
            filled += count
        }
        val frames = filled / frameBytes
        if (frames == 0) return -1
        bytesRead += frames * frameBytes
        for (frame in 0 until frames) {
            var sum = 0
            for (channel in 0 until channels) {
                val index = frame * frameBytes + channel * 2
                sum += ((raw[index + 1].toInt() shl 8) or (raw[index].toInt() and 0xFF)).toShort().toInt()
            }
            target[offset + frame] = sum / (channels * 32768f)
        }
        return frames
    }

    companion object {
        fun open(stream: InputStream): WavPcmReader {
            val input = if (stream is BufferedInputStream) stream else BufferedInputStream(stream)
            if (readTag(input) != "RIFF") throw IOException("not_a_riff_file")
            readIntLe(input)
            if (readTag(input) != "WAVE") throw IOException("not_a_wave_file")
            var sampleRate = 0
            var channels = 0
            var bitsPerSample = 0
            var format = 0
            while (true) {
                val tag = readTag(input)
                val size = readIntLe(input).toLong() and 0xFFFFFFFFL
                when (tag) {
                    "fmt " -> {
                        format = readShortLe(input)
                        channels = readShortLe(input)
                        sampleRate = readIntLe(input)
                        readIntLe(input) // byte rate
                        readShortLe(input) // block align
                        bitsPerSample = readShortLe(input)
                        skipFully(input, size - 16)
                    }
                    "data" -> {
                        // 1 = PCM; 0xFFFE = WAVE_FORMAT_EXTENSIBLE, which ffmpeg only uses for PCM here.
                        if ((format != 1 && format != 0xFFFE) || bitsPerSample != 16 || channels !in 1..8 || sampleRate <= 0) {
                            throw IOException("unsupported_wav_format:$format/$bitsPerSample/$channels/$sampleRate")
                        }
                        // A streaming writer may leave the size at 0 or 0xFFFFFFFF: read to the end then.
                        val dataBytes = if (size == 0L || size == 0xFFFFFFFFL) Long.MAX_VALUE else size
                        return WavPcmReader(input, sampleRate, channels, dataBytes)
                    }
                    else -> skipFully(input, size + (size and 1L)) // chunks are word-aligned
                }
            }
        }

        private fun readTag(input: InputStream): String {
            val bytes = ByteArray(4)
            readFully(input, bytes)
            return String(bytes, Charsets.US_ASCII)
        }

        private fun readIntLe(input: InputStream): Int {
            val bytes = ByteArray(4)
            readFully(input, bytes)
            return (bytes[0].toInt() and 0xFF) or
                ((bytes[1].toInt() and 0xFF) shl 8) or
                ((bytes[2].toInt() and 0xFF) shl 16) or
                ((bytes[3].toInt() and 0xFF) shl 24)
        }

        private fun readShortLe(input: InputStream): Int {
            val bytes = ByteArray(2)
            readFully(input, bytes)
            return (bytes[0].toInt() and 0xFF) or ((bytes[1].toInt() and 0xFF) shl 8)
        }

        private fun readFully(input: InputStream, target: ByteArray) {
            var offset = 0
            while (offset < target.size) {
                val count = input.read(target, offset, target.size - offset)
                if (count < 0) throw EOFException("wav_truncated")
                offset += count
            }
        }

        private fun skipFully(input: InputStream, bytes: Long) {
            var remaining = bytes
            while (remaining > 0) {
                val skipped = input.skip(remaining)
                if (skipped <= 0) {
                    if (input.read() < 0) throw EOFException("wav_truncated")
                    remaining -= 1
                } else {
                    remaining -= skipped
                }
            }
        }
    }
}
