package com.johnchourp.learnbyzantinemusic.recordings.codec

import java.nio.ByteBuffer

/**
 * The PCM sample layouts a `MediaCodec` decoder may hand back (`MediaFormat.KEY_PCM_ENCODING`), and
 * how to read one sample of each as a float in [-1, 1]. Android's raw decoder passes a WAV's own
 * depth through on newer versions, so 8-, 24- and 32-bit integer PCM must be read, not refused.
 *
 * The ids are `android.media.AudioFormat` constants, written out so this stays pure JVM
 * (`PcmSampleFormatTest`). Buffers are read little-endian; the caller sets the order.
 */
enum class PcmSampleFormat(val encodingId: Int, val bytes: Int) {
    /** `ENCODING_PCM_8BIT`: unsigned, 128 = silence. */
    PCM_8(3, 1),

    /** `ENCODING_PCM_16BIT`, the default when the format does not say. */
    PCM_16(2, 2),

    /** `ENCODING_PCM_24BIT_PACKED` (API 31): three bytes per sample. */
    PCM_24_PACKED(21, 3),

    /** `ENCODING_PCM_32BIT` (API 31). */
    PCM_32(22, 4),

    /** `ENCODING_PCM_FLOAT`. */
    FLOAT(4, 4);

    fun read(buffer: ByteBuffer): Float = when (this) {
        PCM_8 -> ((buffer.get().toInt() and 0xFF) - 128) / 128f
        PCM_16 -> buffer.getShort() / 32_768f
        PCM_24_PACKED -> {
            val low = buffer.get().toInt() and 0xFF
            val mid = buffer.get().toInt() and 0xFF
            val high = buffer.get().toInt() // sign-extends the top byte
            ((high shl 16) or (mid shl 8) or low) / 8_388_608f
        }
        PCM_32 -> (buffer.getInt() / 2_147_483_648.0).toFloat()
        FLOAT -> buffer.getFloat()
    }

    companion object {
        /** The format for an encoding id, or null for one this app cannot read. */
        fun fromEncoding(encodingId: Int): PcmSampleFormat? = entries.firstOrNull { it.encodingId == encodingId }
    }
}
