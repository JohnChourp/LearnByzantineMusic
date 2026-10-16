package com.johnchourp.learnbyzantinemusic.recordings.analysis

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.johnchourp.learnbyzantinemusic.recordings.codec.MonoWavWriter
import com.johnchourp.learnbyzantinemusic.recordings.codec.PcmResampler
import com.johnchourp.learnbyzantinemusic.recordings.codec.PcmSampleFormat
import java.io.File
import java.io.IOException
import java.nio.ByteOrder

/**
 * Decodes a saved recording (FLAC, MP3, WAV, AAC, M4A or OPUS — whatever the recorder wrote, now or
 * in an older version) to a mono 16-bit WAV at [SAMPLE_RATE] in the app cache.
 *
 * A 16-bit WAV is read directly ([WavPcmReader]); everything else — other WAVs included — goes through Android's own decoders
 * (`MediaExtractor` + `MediaCodec`), which replaced FFmpegKit (ClickUp `869fhmxpt`). Channels are
 * averaged and the rate converted with [PcmResampler]. A format the device cannot decode — Ogg Opus
 * before Android 10, for example — fails with an [IOException], which the analysis shows as a failed
 * decode instead of a crash.
 *
 * The document is copied to the cache first, so the decoders read a plain file.
 */
object RecordingDecoder {
    const val SAMPLE_RATE = 22_050

    private const val TIMEOUT_US = 10_000L
    private const val MAX_IDLE_ROUNDS = 1_000

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
            val written = if (extension == "wav") {
                // The app's own WAVs are 16-bit PCM and are read directly. Any other WAV (8/24/32-bit,
                // float) still decodes, through Android's decoder, as it did with FFmpeg.
                try {
                    decodeWav(source, output)
                } catch (notSixteenBit: IOException) {
                    decodeWithMediaCodec(source, output)
                }
            } else {
                decodeWithMediaCodec(source, output)
            }
            if (written <= 0L) throw IOException("decode_failed:no_audio")
            return output
        } catch (error: Throwable) {
            output.delete()
            throw if (error is IOException) error else IOException("decode_failed:${error.javaClass.simpleName}:${error.message}", error)
        } finally {
            source.delete()
        }
    }

    private fun decodeWav(source: File, output: File): Long =
        source.inputStream().buffered().use { input ->
            val reader = WavPcmReader.open(input)
            val resampler = PcmResampler(reader.sampleRate, SAMPLE_RATE)
            MonoWavWriter(output, SAMPLE_RATE).use { writer ->
                val chunk = FloatArray(4_096)
                while (true) {
                    val read = reader.read(chunk)
                    if (read < 0) break
                    writer.write(resampler.process(chunk, read))
                }
                writer.samplesWritten
            }
        }

    private fun decodeWithMediaCodec(source: File, output: File): Long {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(source.absolutePath)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw IOException("decode_failed:no_audio_track")
            extractor.selectTrack(trackIndex)
            val inputFormat = extractor.getTrackFormat(trackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: throw IOException("decode_failed:no_mime")
            val codec = try {
                MediaCodec.createDecoderByType(mime)
            } catch (error: Exception) {
                throw IOException("decode_failed:no_decoder:$mime", error)
            }
            try {
                codec.configure(inputFormat, null, null, 0)
                codec.start()
                return MonoWavWriter(output, SAMPLE_RATE).use { writer ->
                    pump(extractor, codec, inputFormat, writer)
                    writer.samplesWritten
                }
            } finally {
                runCatching { codec.stop() }
                runCatching { codec.release() }
            }
        } finally {
            extractor.release()
        }
    }

    private fun pump(extractor: MediaExtractor, codec: MediaCodec, inputFormat: MediaFormat, writer: MonoWavWriter) {
        val info = MediaCodec.BufferInfo()
        var pcm = PcmLayout.from(inputFormat)
        var resampler = PcmResampler(pcm.sampleRate, SAMPLE_RATE)
        var mono = FloatArray(0)
        var inputDone = false
        var idleRounds = 0
        while (true) {
            var progressed = false
            if (!inputDone) {
                val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                if (inputIndex >= 0) {
                    val buffer = requireNotNull(codec.getInputBuffer(inputIndex))
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime.coerceAtLeast(0L), 0)
                        extractor.advance()
                    }
                    progressed = true
                }
            }
            val outputIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)
            when {
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val changed = PcmLayout.from(codec.outputFormat)
                    if (changed.sampleRate != pcm.sampleRate) resampler = PcmResampler(changed.sampleRate, SAMPLE_RATE)
                    pcm = changed
                    progressed = true
                }
                outputIndex >= 0 -> {
                    val buffer = requireNotNull(codec.getOutputBuffer(outputIndex))
                    if (info.size > 0) {
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        buffer.order(ByteOrder.LITTLE_ENDIAN)
                        val frames = info.size / pcm.frameBytes
                        if (mono.size < frames) mono = FloatArray(frames)
                        for (frame in 0 until frames) {
                            var sum = 0f
                            for (channel in 0 until pcm.channels) {
                                sum += pcm.sample.read(buffer)
                            }
                            mono[frame] = sum / pcm.channels
                        }
                        writer.write(resampler.process(mono, frames))
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    progressed = true
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
            }
            idleRounds = if (progressed) 0 else idleRounds + 1
            if (idleRounds >= MAX_IDLE_ROUNDS) throw IOException("decode_failed:decoder_stalled")
        }
    }

    /** How the decoder lays out its PCM: rate, channels and sample format. */
    private data class PcmLayout(val sampleRate: Int, val channels: Int, val sample: PcmSampleFormat) {
        val frameBytes: Int get() = channels * sample.bytes

        companion object {
            fun from(format: MediaFormat): PcmLayout {
                val rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
                val encoding = if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                    format.getInteger(MediaFormat.KEY_PCM_ENCODING)
                } else {
                    PcmSampleFormat.PCM_16.encodingId
                }
                val sample = PcmSampleFormat.fromEncoding(encoding)
                    ?: throw IOException("decode_failed:pcm_encoding:$encoding")
                return PcmLayout(rate, channels, sample)
            }
        }
    }
}
