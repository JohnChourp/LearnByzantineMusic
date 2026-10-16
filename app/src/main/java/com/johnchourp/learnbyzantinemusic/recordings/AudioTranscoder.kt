package com.johnchourp.learnbyzantinemusic.recordings

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import com.johnchourp.learnbyzantinemusic.recordings.analysis.WavPcmReader
import com.johnchourp.learnbyzantinemusic.recordings.codec.AdtsHeader
import com.johnchourp.learnbyzantinemusic.recordings.codec.PcmResampler
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Turns the capture WAV (16-bit mono, 44 100 Hz) into the chosen format with **Android's own
 * encoders** (`MediaCodec`, `MediaMuxer`). Replaced FFmpegKit, whose native libraries are not
 * 16 KB page aligned and so cannot ship on Google Play (ClickUp `869fhmxpt`).
 *
 * | format | encoder            | container                                  |
 * |--------|--------------------|--------------------------------------------|
 * | M4A    | AAC LC, 192 kb/s   | `MediaMuxer` MPEG-4                        |
 * | AAC    | AAC LC, 192 kb/s   | ADTS, header per frame ([AdtsHeader])      |
 * | FLAC   | FLAC, level 5      | the encoder's own stream (`fLaC` + frames) |
 * | OPUS   | Opus, 96 kb/s      | `MediaMuxer` Ogg, API 29+, at 48 000 Hz    |
 * | WAV    | —                  | a plain copy                               |
 *
 * **It never claims a success it did not have**: any missing encoder, exception or empty output is a
 * failure, and the caller ([com.johnchourp.learnbyzantinemusic.recordings.session.RecordingSaver])
 * then saves the WAV with the «η μετατροπή απέτυχε» message. That is the FLAC fallback of decision D3
 * on a device without a FLAC encoder.
 */
object AudioTranscoder {
    data class TranscodeResult(
        val isSuccess: Boolean,
        val details: String
    )

    private const val AAC_BIT_RATE = 192_000
    private const val OPUS_BIT_RATE = 96_000
    private const val OPUS_SAMPLE_RATE = 48_000
    private const val FLAC_COMPRESSION_LEVEL = 5
    private const val TIMEOUT_US = 10_000L
    private const val CHUNK_SAMPLES = 4_096

    /** A run that makes no progress for this many dequeue rounds is stuck, not slow. */
    private const val MAX_IDLE_ROUNDS = 1_000

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
        if (!format.isRecordableOn(Build.VERSION.SDK_INT)) {
            return TranscodeResult(isSuccess = false, details = "not_recordable_on_api_${Build.VERSION.SDK_INT}:$format")
        }
        return try {
            encode(sourceWav, outputFile, format)
            if (outputFile.length() > 0L) {
                TranscodeResult(isSuccess = true, details = "media_codec_ok")
            } else {
                outputFile.delete()
                TranscodeResult(isSuccess = false, details = "empty_output:$format")
            }
        } catch (error: Throwable) {
            outputFile.delete()
            TranscodeResult(isSuccess = false, details = "media_codec_failed:$format:${error.javaClass.simpleName}:${error.message}")
        }
    }

    private fun encode(sourceWav: File, outputFile: File, format: RecordingFormatOption) {
        sourceWav.inputStream().buffered().use { stream ->
            val reader = WavPcmReader.open(stream)
            val sampleRate = if (format == RecordingFormatOption.OPUS) OPUS_SAMPLE_RATE else reader.sampleRate
            val mediaFormat = mediaFormatFor(format, sampleRate)
            val codecName = MediaCodecList(MediaCodecList.REGULAR_CODECS).findEncoderForFormat(mediaFormat)
                ?: error("no_encoder:${mediaFormat.getString(MediaFormat.KEY_MIME)}@$sampleRate")
            val sink = sinkFor(format, outputFile, sampleRate)
            try {
                val codec = MediaCodec.createByCodecName(codecName)
                try {
                    codec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                    codec.start()
                    pump(codec, PcmSource(reader, PcmResampler(reader.sampleRate, sampleRate)), sampleRate, sink)
                    sink.finish()
                } finally {
                    runCatching { codec.stop() }
                    runCatching { codec.release() }
                }
            } finally {
                sink.close()
            }
        }
    }

    private fun mediaFormatFor(format: RecordingFormatOption, sampleRate: Int): MediaFormat = when (format) {
        RecordingFormatOption.M4A, RecordingFormatOption.AAC ->
            MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 1).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, AAC_BIT_RATE)
            }
        RecordingFormatOption.FLAC ->
            MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_FLAC, sampleRate, 1).apply {
                setInteger(MediaFormat.KEY_FLAC_COMPRESSION_LEVEL, FLAC_COMPRESSION_LEVEL)
            }
        RecordingFormatOption.OPUS ->
            MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, sampleRate, 1).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, OPUS_BIT_RATE)
            }
        RecordingFormatOption.MP3, RecordingFormatOption.WAV -> error("not_encodable:$format")
    }.apply {
        setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, CHUNK_SAMPLES * 2)
    }

    /** Feeds 16-bit little-endian PCM in and hands every encoded buffer to [sink], until end of stream. */
    private fun pump(codec: MediaCodec, source: PcmSource, sampleRate: Int, sink: EncodedSink) {
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var framesQueued = 0L
        var idleRounds = 0
        while (true) {
            var progressed = false
            if (!inputDone) {
                val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                if (inputIndex >= 0) {
                    val buffer = requireNotNull(codec.getInputBuffer(inputIndex))
                    buffer.clear()
                    val frames = source.fill(buffer)
                    val timeUs = framesQueued * 1_000_000L / sampleRate
                    if (frames > 0) {
                        codec.queueInputBuffer(inputIndex, 0, frames * 2, timeUs, 0)
                        framesQueued += frames
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, 0, timeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    }
                    progressed = true
                }
            }
            val outputIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)
            when {
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    sink.onFormat(codec.outputFormat)
                    progressed = true
                }
                outputIndex >= 0 -> {
                    val buffer = requireNotNull(codec.getOutputBuffer(outputIndex))
                    if (info.size > 0) {
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        sink.write(buffer, info)
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    progressed = true
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
            }
            idleRounds = if (progressed) 0 else idleRounds + 1
            check(idleRounds < MAX_IDLE_ROUNDS) { "encoder_stalled" }
        }
    }

    /** Mono floats from the WAV, resampled when the encoder needs another rate, as 16-bit PCM. */
    private class PcmSource(private val reader: WavPcmReader, private val resampler: PcmResampler) {
        private val chunk = FloatArray(CHUNK_SAMPLES)
        private var pending = FloatArray(0)
        private var pendingOffset = 0
        private var exhausted = false

        /** Writes as many whole frames as fit in [buffer]; 0 at the end. */
        fun fill(buffer: ByteBuffer): Int {
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            var frames = 0
            while (buffer.remaining() >= 2) {
                if (pendingOffset >= pending.size) {
                    if (exhausted) break
                    val read = reader.read(chunk)
                    if (read < 0) {
                        exhausted = true
                        break
                    }
                    pending = resampler.process(chunk, read)
                    pendingOffset = 0
                    continue
                }
                val sample = (pending[pendingOffset++] * 32768f).roundToInt().coerceIn(-32768, 32767)
                buffer.putShort(sample.toShort())
                frames++
            }
            return frames
        }
    }

    private fun sinkFor(format: RecordingFormatOption, outputFile: File, sampleRate: Int): EncodedSink = when (format) {
        RecordingFormatOption.M4A -> MuxerSink(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        RecordingFormatOption.OPUS ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MuxerSink(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG)
            } else {
                error("ogg_needs_api_29")
            }
        RecordingFormatOption.AAC -> AdtsSink(FileOutputStream(outputFile), sampleRate)
        RecordingFormatOption.FLAC -> FlacSink(FileOutputStream(outputFile))
        RecordingFormatOption.MP3, RecordingFormatOption.WAV -> error("not_encodable:$format")
    }

    private interface EncodedSink {
        fun onFormat(format: MediaFormat) {}
        fun write(data: ByteBuffer, info: MediaCodec.BufferInfo)
        fun finish() {}
        fun close()
    }

    /** M4A and Ogg Opus: the muxer takes the codec config from the format, not from the stream. */
    private class MuxerSink(outputFile: File, outputFormat: Int) : EncodedSink {
        private val muxer = MediaMuxer(outputFile.absolutePath, outputFormat)
        private var track = -1
        private var started = false
        private var wroteSamples = false

        override fun onFormat(format: MediaFormat) {
            // A muxer takes one track format, before it starts. Some encoders report the format again
            // (an updated csd, for example); the track and its config are already set, so it is ignored.
            if (started) return
            track = muxer.addTrack(format)
            muxer.start()
            started = true
        }

        override fun write(data: ByteBuffer, info: MediaCodec.BufferInfo) {
            if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) return
            check(started) { "data_before_format" }
            muxer.writeSampleData(track, data, info)
            wroteSamples = true
        }

        override fun finish() {
            check(wroteSamples) { "no_audio_written" }
            muxer.stop()
            started = false
        }

        override fun close() {
            if (started) runCatching { muxer.stop() }
            runCatching { muxer.release() }
        }
    }

    /** Raw AAC frames, each behind its own ADTS header. */
    private class AdtsSink(private val out: OutputStream, private val sampleRate: Int) : EncodedSink {
        private val frame = ByteArray(8 * 1024)

        init {
            require(AdtsHeader.isSupportedRate(sampleRate)) { "adts_rate:$sampleRate" }
        }

        override fun write(data: ByteBuffer, info: MediaCodec.BufferInfo) {
            if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) return
            out.write(AdtsHeader.forFrame(info.size, sampleRate, channels = 1))
            copy(data, info.size, out, frame)
        }

        override fun close() {
            out.close()
        }
    }

    /**
     * The FLAC encoder emits the stream header (`fLaC` + STREAMINFO) first, as codec config, then the
     * frames. Some encoders leave the `fLaC` marker out of the config: it is added when missing.
     */
    private class FlacSink(private val out: OutputStream) : EncodedSink {
        private val frame = ByteArray(16 * 1024)
        private var headerWritten = false

        override fun onFormat(format: MediaFormat) {
            if (headerWritten) return
            val csd = if (format.containsKey("csd-0")) format.getByteBuffer("csd-0") else null
            if (csd != null && csd.remaining() > 0) writeHeader(csd.duplicate(), csd.remaining())
        }

        override fun write(data: ByteBuffer, info: MediaCodec.BufferInfo) {
            if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                if (!headerWritten) writeHeader(data, info.size)
                return
            }
            check(headerWritten) { "flac_frames_before_header" }
            copy(data, info.size, out, frame)
        }

        private fun writeHeader(data: ByteBuffer, size: Int) {
            val header = ByteArray(size).also { data.get(it) }
            if (!header.startsWithFlacMarker()) out.write(FLAC_MARKER)
            out.write(header)
            headerWritten = true
        }

        override fun close() {
            out.close()
        }

        private fun ByteArray.startsWithFlacMarker(): Boolean =
            size >= FLAC_MARKER.size && FLAC_MARKER.indices.all { this[it] == FLAC_MARKER[it] }

        private companion object {
            val FLAC_MARKER = byteArrayOf('f'.code.toByte(), 'L'.code.toByte(), 'a'.code.toByte(), 'C'.code.toByte())
        }
    }

    private fun copy(data: ByteBuffer, size: Int, out: OutputStream, scratch: ByteArray) {
        var left = size
        while (left > 0) {
            val count = minOf(left, scratch.size)
            data.get(scratch, 0, count)
            out.write(scratch, 0, count)
            left -= count
        }
    }
}
