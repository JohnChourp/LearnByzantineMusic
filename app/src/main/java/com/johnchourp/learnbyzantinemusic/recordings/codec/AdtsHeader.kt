package com.johnchourp.learnbyzantinemusic.recordings.codec

/**
 * The 7-byte ADTS header that turns raw AAC frames from `MediaCodec` into a playable `.aac` stream
 * (ISO/IEC 13818-7, no CRC). Android's muxer has no ADTS output, so every frame gets one by hand.
 *
 * Pure JVM, so `AdtsHeaderTest` can pin every bit.
 */
object AdtsHeader {
    const val SIZE = 7

    /** AAC LC = audio object type 2; ADTS stores it minus one. */
    private const val PROFILE_AAC_LC = 1

    /** The sampling frequency indexes ADTS knows, by rate. */
    private val SAMPLE_RATE_INDEX = mapOf(
        96_000 to 0, 88_200 to 1, 64_000 to 2, 48_000 to 3, 44_100 to 4, 32_000 to 5,
        24_000 to 6, 22_050 to 7, 16_000 to 8, 12_000 to 9, 11_025 to 10, 8_000 to 11, 7_350 to 12,
    )

    fun isSupportedRate(sampleRate: Int): Boolean = sampleRate in SAMPLE_RATE_INDEX

    /** The header for one AAC frame of [payloadSize] bytes. */
    fun forFrame(payloadSize: Int, sampleRate: Int, channels: Int): ByteArray {
        val rateIndex = requireNotNull(SAMPLE_RATE_INDEX[sampleRate]) { "ADTS has no index for $sampleRate Hz" }
        require(channels in 1..7) { "ADTS channel configuration must be 1..7, was $channels" }
        val frameLength = payloadSize + SIZE
        require(frameLength < (1 shl 13)) { "an ADTS frame is at most 8191 bytes, was $frameLength" }
        return byteArrayOf(
            0xFF.toByte(),
            0xF1.toByte(), // sync (12 bits) · MPEG-4 · layer 0 · no CRC
            ((PROFILE_AAC_LC shl 6) or (rateIndex shl 2) or (channels shr 2)).toByte(),
            (((channels and 3) shl 6) or (frameLength shr 11)).toByte(),
            ((frameLength shr 3) and 0xFF).toByte(),
            (((frameLength and 7) shl 5) or 0x1F).toByte(),
            0xFC.toByte(), // buffer fullness 0x7FF (variable) · one raw data block
        )
    }
}
