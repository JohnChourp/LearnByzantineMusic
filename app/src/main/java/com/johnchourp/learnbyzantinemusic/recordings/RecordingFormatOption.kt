package com.johnchourp.learnbyzantinemusic.recordings

import com.johnchourp.learnbyzantinemusic.R

/** Ogg Opus: `MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG` and the Opus encoder are API 29. */
private const val OPUS_RECORD_MIN_SDK = 29

/**
 * Every audio format the app **recognises** — for listing, sharing and playing files already in the
 * user's folders — and, through [isRecordableOn], the ones it can still **record** to.
 *
 * Recording goes through Android's own encoders since FFmpegKit was removed (ClickUp `869fhmxpt`):
 * - **MP3**: Android has no MP3 encoder, so it is no longer offered; a stored choice of MP3 reads as
 *   [M4A]. Existing `.mp3` recordings keep working everywhere.
 * - **OPUS**: Android encodes and writes Ogg Opus only from Android 10 (API 29); below that it is
 *   hidden and a stored choice reads as [M4A].
 * - **FLAC**: a device without a FLAC encoder saves WAV with the usual «η μετατροπή απέτυχε» message
 *   ([com.johnchourp.learnbyzantinemusic.recordings.session.RecordingSaver]).
 */
enum class RecordingFormatOption(
    val extension: String,
    val mimeType: String,
    val labelResId: Int,
    val descriptionResId: Int,
    /** The first Android API level that can record to this format; null = never. */
    private val minRecordSdk: Int?,
) {
    FLAC("flac", "audio/flac", R.string.recordings_format_flac, R.string.recordings_format_flac_description, 1),
    MP3("mp3", "audio/mpeg", R.string.recordings_format_mp3, R.string.recordings_format_mp3_description, null),
    WAV("wav", "audio/wav", R.string.recordings_format_wav, R.string.recordings_format_wav_description, 1),
    AAC("aac", "audio/aac", R.string.recordings_format_aac, R.string.recordings_format_aac_description, 1),
    M4A("m4a", "audio/mp4", R.string.recordings_format_m4a, R.string.recordings_format_m4a_description, 1),
    OPUS("opus", "audio/ogg", R.string.recordings_format_opus, R.string.recordings_format_opus_description, OPUS_RECORD_MIN_SDK);

    fun isRecordableOn(sdkInt: Int): Boolean = minRecordSdk != null && sdkInt >= minRecordSdk

    companion object {
        /** Ogg Opus: `MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG` and the Opus encoder are API 29. */
        const val OPUS_MIN_SDK = OPUS_RECORD_MIN_SDK

        /** What a format the device cannot record to is recorded as instead. */
        val RECORDING_REPLACEMENT = M4A

        /** The stored name as a format; unknown or blank values fall back to FLAC, MP3 reads as M4A. */
        fun fromStoredValue(rawValue: String?): RecordingFormatOption {
            if (rawValue.isNullOrBlank()) {
                return FLAC
            }
            val stored = entries.firstOrNull { it.name == rawValue.uppercase() } ?: return FLAC
            return if (stored == MP3) RECORDING_REPLACEMENT else stored
        }

        /** The stored choice as something this device can record to. */
        fun forRecording(rawValue: String?, sdkInt: Int): RecordingFormatOption {
            val stored = fromStoredValue(rawValue)
            return if (stored.isRecordableOn(sdkInt)) stored else RECORDING_REPLACEMENT
        }

        /** The formats the recordings page offers on this device, in display order. */
        fun recordableOn(sdkInt: Int): List<RecordingFormatOption> = entries.filter { it.isRecordableOn(sdkInt) }

        fun supportsFileName(fileName: String?): Boolean {
            val extension = fileName?.substringAfterLast('.', "")?.trim()?.lowercase() ?: return false
            return entries.any { it.extension == extension }
        }

        fun resolveMimeTypeByFileName(fileName: String?): String? {
            val extension = fileName?.substringAfterLast('.', "")?.trim()?.lowercase() ?: return null
            return entries.firstOrNull { it.extension == extension }?.mimeType
        }
    }
}
