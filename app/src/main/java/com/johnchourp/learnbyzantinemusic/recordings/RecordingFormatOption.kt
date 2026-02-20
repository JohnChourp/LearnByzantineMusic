package com.johnchourp.learnbyzantinemusic.recordings

import com.johnchourp.learnbyzantinemusic.R

enum class RecordingFormatOption(
    val extension: String,
    val mimeType: String,
    val labelResId: Int,
    val descriptionResId: Int
) {
    FLAC("flac", "audio/flac", R.string.recordings_format_flac, R.string.recordings_format_flac_description),
    MP3("mp3", "audio/mpeg", R.string.recordings_format_mp3, R.string.recordings_format_mp3_description),
    WAV("wav", "audio/wav", R.string.recordings_format_wav, R.string.recordings_format_wav_description),
    AAC("aac", "audio/aac", R.string.recordings_format_aac, R.string.recordings_format_aac_description),
    M4A("m4a", "audio/mp4", R.string.recordings_format_m4a, R.string.recordings_format_m4a_description),
    OPUS("opus", "audio/ogg", R.string.recordings_format_opus, R.string.recordings_format_opus_description);

    companion object {
        fun fromStoredValue(rawValue: String?): RecordingFormatOption {
            if (rawValue.isNullOrBlank()) {
                return FLAC
            }
            return entries.firstOrNull { it.name == rawValue.uppercase() } ?: FLAC
        }

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
