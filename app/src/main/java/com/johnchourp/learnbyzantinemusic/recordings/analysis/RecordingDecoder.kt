package com.johnchourp.learnbyzantinemusic.recordings.analysis

import android.content.Context
import android.net.Uri
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.io.IOException

/**
 * Decodes a saved recording (FLAC, MP3, WAV, AAC, M4A or OPUS — whatever the recorder wrote) to
 * a mono 16-bit WAV at [SAMPLE_RATE] in the app cache, with the same FFmpegKit the recorder
 * encodes with. The document is copied to the cache first so FFmpeg reads a plain file.
 */
object RecordingDecoder {
    const val SAMPLE_RATE = 22_050

    /** Returns the decoded WAV; the caller deletes it. Blocking: call off the main thread. */
    fun decodeToWav(context: Context, uri: Uri, displayName: String): File {
        val workDir = File(context.cacheDir, "recording_analysis").apply { mkdirs() }
        val extension = displayName.substringAfterLast('.', "").lowercase().takeIf { it.matches(Regex("[a-z0-9]{1,5}")) } ?: "audio"
        val source = File.createTempFile("source_", ".$extension", workDir)
        val output = File.createTempFile("pcm_", ".wav", workDir)
        try {
            val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                source.outputStream().use { input.copyTo(it) }
            }
            if (copied == null) throw IOException("recording_not_readable")
            val command = "-y -i ${quote(source.absolutePath)} -vn -ac 1 -ar $SAMPLE_RATE -c:a pcm_s16le -f wav ${quote(output.absolutePath)}"
            val session = FFmpegKit.execute(command)
            if (!ReturnCode.isSuccess(session.returnCode)) {
                output.delete()
                throw IOException("decode_failed:${session.returnCode?.value}")
            }
            return output
        } catch (error: Throwable) {
            output.delete()
            throw error
        } finally {
            source.delete()
        }
    }

    private fun quote(value: String): String = "'${value.replace("'", "'\\''")}'"
}
