package com.johnchourp.learnbyzantinemusic.recordings

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File

object AudioTranscoder {
    data class TranscodeResult(
        val isSuccess: Boolean,
        val details: String
    )

    fun transcode(
        sourceWav: File,
        outputFile: File,
        format: RecordingFormatOption
    ): TranscodeResult {
        if (format == RecordingFormatOption.WAV) {
            sourceWav.copyTo(outputFile, overwrite = true)
            return TranscodeResult(
                isSuccess = true,
                details = "wav_copy_ok"
            )
        }

        val command = buildCommand(sourceWav, outputFile, format)
        val session = FFmpegKit.execute(command)
        val returnCode = session.returnCode
        if (ReturnCode.isSuccess(returnCode)) {
            return TranscodeResult(
                isSuccess = true,
                details = "ffmpeg_success"
            )
        }

        val log = session.allLogsAsString ?: ""
        return TranscodeResult(
            isSuccess = false,
            details = "ffmpeg_failed:${returnCode?.value}:$log"
        )
    }

    private fun buildCommand(
        sourceWav: File,
        outputFile: File,
        format: RecordingFormatOption
    ): String {
        val input = quoteForShell(sourceWav.absolutePath)
        val output = quoteForShell(outputFile.absolutePath)
        val codecPart = when (format) {
            RecordingFormatOption.FLAC -> "-c:a flac"
            RecordingFormatOption.MP3 -> "-c:a libmp3lame -q:a 2"
            RecordingFormatOption.WAV -> "-c:a pcm_s16le"
            RecordingFormatOption.AAC -> "-c:a aac -b:a 192k -f adts"
            RecordingFormatOption.M4A -> "-c:a aac -b:a 192k -movflags +faststart"
            RecordingFormatOption.OPUS -> "-c:a libopus -b:a 96k"
        }
        return "-y -i $input $codecPart $output"
    }

    private fun quoteForShell(value: String): String {
        val escaped = value.replace("'", "'\\''")
        return "'$escaped'"
    }
}
