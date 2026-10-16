package com.johnchourp.learnbyzantinemusic.recordings.codec

/**
 * The start of a FLAC file: the `fLaC` marker and the STREAMINFO metadata block.
 *
 * Android's FLAC encoder hands this header out as codec config before it has seen the audio, and
 * it may arrive in more than one config buffer, sometimes without the marker. [assemble] joins the
 * parts and adds the marker when missing. Because the encoder cannot know the length in advance,
 * STREAMINFO may declare 0 total samples, so players show a 0:00 duration and cannot seek.
 * [totalSamplesPatch] gives the bytes that write the real count once the encode is done.
 *
 * Pure JVM, so `FlacStreamHeaderTest` can pin every bit of the layout.
 */
object FlacStreamHeader {
    val MARKER = byteArrayOf('f'.code.toByte(), 'L'.code.toByte(), 'a'.code.toByte(), 'C'.code.toByte())

    /** STREAMINFO is block type 0 and holds 34 bytes, after the 4-byte marker and 4-byte block header. */
    private const val STREAMINFO_TYPE = 0
    private const val STREAMINFO_LENGTH = 34
    private const val MIN_SIZE = 4 + 4 + STREAMINFO_LENGTH

    /** The byte where the 36-bit total-samples field starts (its low nibble), counted from the file start. */
    const val TOTAL_SAMPLES_OFFSET = 21

    /** The largest count the 36-bit field can hold. */
    const val MAX_TOTAL_SAMPLES = (1L shl 36) - 1

    /** Joins the config parts in order and puts the `fLaC` marker first if no part carried it. */
    fun assemble(parts: List<ByteArray>): ByteArray {
        val joined = parts.fold(ByteArray(0)) { acc, part -> acc + part }
        return if (startsWithMarker(joined)) joined else MARKER + joined
    }

    /** True for a header the patch below can safely touch: marker, then a full STREAMINFO block. */
    fun hasStreamInfo(header: ByteArray): Boolean {
        if (header.size < MIN_SIZE || !startsWithMarker(header)) return false
        val type = header[4].toInt() and 0x7F
        val length = ((header[5].toInt() and 0xFF) shl 16) or ((header[6].toInt() and 0xFF) shl 8) or (header[7].toInt() and 0xFF)
        return type == STREAMINFO_TYPE && length == STREAMINFO_LENGTH
    }

    /**
     * The 5 bytes to write at [TOTAL_SAMPLES_OFFSET] so STREAMINFO declares [totalSamples]. The high
     * nibble of the first byte belongs to bits-per-sample and is kept from [currentByte].
     */
    fun totalSamplesPatch(currentByte: Byte, totalSamples: Long): ByteArray {
        require(totalSamples in 0..MAX_TOTAL_SAMPLES) { "FLAC holds at most 2^36 - 1 samples, was $totalSamples" }
        return byteArrayOf(
            ((currentByte.toInt() and 0xF0) or ((totalSamples ushr 32).toInt() and 0x0F)).toByte(),
            (totalSamples ushr 24).toByte(),
            (totalSamples ushr 16).toByte(),
            (totalSamples ushr 8).toByte(),
            totalSamples.toByte(),
        )
    }

    /** The total-samples field of a header, for checks and tests. */
    fun totalSamples(header: ByteArray): Long {
        require(hasStreamInfo(header)) { "not a FLAC header with STREAMINFO" }
        var value = (header[TOTAL_SAMPLES_OFFSET].toLong() and 0x0F)
        for (i in 1..4) value = (value shl 8) or (header[TOTAL_SAMPLES_OFFSET + i].toLong() and 0xFF)
        return value
    }

    private fun startsWithMarker(bytes: ByteArray): Boolean =
        bytes.size >= MARKER.size && MARKER.indices.all { bytes[it] == MARKER[it] }
}
