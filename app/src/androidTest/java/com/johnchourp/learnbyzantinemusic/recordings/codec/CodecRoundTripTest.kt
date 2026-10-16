package com.johnchourp.learnbyzantinemusic.recordings.codec

import android.media.MediaCodecList
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.johnchourp.learnbyzantinemusic.recordings.AudioTranscoder
import com.johnchourp.learnbyzantinemusic.recordings.RecordingFormatOption
import com.johnchourp.learnbyzantinemusic.recordings.analysis.RecordingDecoder
import com.johnchourp.learnbyzantinemusic.recordings.analysis.WavPcmReader
import com.johnchourp.learnbyzantinemusic.recordings.session.WavFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * **Scenario: what FFmpegKit used to do, now on the device's own codecs** (ClickUp `869fhmxpt`).
 *
 * A 2-second 220 Hz tone in the capture format (44.1 kHz mono WAV) is encoded to every format this
 * device offers, then decoded the way the analysis reads recordings. Each round trip must keep the
 * length and the pitch — a wrong rate, a dropped channel or a broken container shows up as one of
 * the two. Only runs on a device or emulator: `MediaCodec` does not exist on the JVM.
 *
 * FLAC is skipped (not failed) on a device without a FLAC encoder: there the app saves WAV with the
 * «η μετατροπή απέτυχε» message, which `RecordingSaverTranscodeFailureTest` already proves.
 */
@RunWith(AndroidJUnit4::class)
class CodecRoundTripTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dir = File(context.cacheDir, "codec_round_trip").apply { mkdirs() }

    @Test
    fun m4a() = roundTrip(RecordingFormatOption.M4A)

    @Test
    fun aac() = roundTrip(RecordingFormatOption.AAC)

    @Test
    fun flac() {
        assumeTrue("no FLAC encoder on this device", hasEncoder(MediaFormat.MIMETYPE_AUDIO_FLAC))
        roundTrip(RecordingFormatOption.FLAC)
    }

    @Test
    fun opus() {
        assumeTrue("Ogg Opus needs API 29", Build.VERSION.SDK_INT >= RecordingFormatOption.OPUS_MIN_SDK)
        roundTrip(RecordingFormatOption.OPUS)
    }

    @Test
    fun wavIsReadDirectly() = roundTrip(RecordingFormatOption.WAV)

    @Test
    fun mp3IsRefusedSoTheSaverKeepsTheWav() {
        val result = AudioTranscoder.transcode(toneWav(), File(dir, "tone.mp3"), RecordingFormatOption.MP3)
        assertTrue("MP3 must not claim success", !result.isSuccess)
    }

    private fun roundTrip(format: RecordingFormatOption) {
        val encoded = File(dir, "tone.${format.extension}").apply { delete() }
        val result = AudioTranscoder.transcode(toneWav(), encoded, format)
        assertTrue("$format: ${result.details}", result.isSuccess)
        assertTrue("$format: empty file", encoded.length() > 0L)

        val decoded = RecordingDecoder.decodeToWav(context, Uri.fromFile(encoded), encoded.name)
        try {
            decoded.inputStream().buffered().use { input ->
                val reader = WavPcmReader.open(input)
                assertEquals(RecordingDecoder.SAMPLE_RATE, reader.sampleRate)
                val samples = FloatArray(RecordingDecoder.SAMPLE_RATE * 3)
                var count = 0
                while (true) {
                    val read = reader.read(samples, count, samples.size - count)
                    if (read < 0) break
                    count += read
                }
                val seconds = count.toDouble() / RecordingDecoder.SAMPLE_RATE
                // Lossy codecs add a little priming/padding; anything near 2 s is the same recording.
                assertEquals("$format length (s)", 2.0, seconds, 0.15)
                val middle = samples.copyOfRange(count / 4, count * 3 / 4)
                val hz = upwardCrossings(middle) * RecordingDecoder.SAMPLE_RATE.toDouble() / middle.size
                assertEquals("$format pitch (Hz)", 220.0, hz, 3.0)
            }
        } finally {
            decoded.delete()
            encoded.delete()
        }
    }

    private fun toneWav(): File {
        val file = File(dir, "tone.wav")
        val samples = WavFormat.SAMPLE_RATE * 2
        file.outputStream().use { out ->
            out.write(ByteArray(WavFormat.HEADER_SIZE))
            val bytes = ByteArray(samples * 2)
            for (i in 0 until samples) {
                val value = (0.6 * 32767 * sin(2 * PI * 220.0 * i / WavFormat.SAMPLE_RATE)).roundToInt()
                bytes[2 * i] = (value and 0xFF).toByte()
                bytes[2 * i + 1] = ((value shr 8) and 0xFF).toByte()
            }
            out.write(bytes)
        }
        WavFormat.finalizeHeader(file)
        return file
    }

    private fun hasEncoder(mime: String): Boolean =
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any { info ->
            info.isEncoder && info.supportedTypes.any { it.equals(mime, ignoreCase = true) }
        }

    private fun upwardCrossings(samples: FloatArray): Int =
        (1 until samples.size).count { samples[it - 1] < 0f && samples[it] >= 0f }
}
