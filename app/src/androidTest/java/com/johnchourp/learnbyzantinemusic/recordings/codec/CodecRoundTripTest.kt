package com.johnchourp.learnbyzantinemusic.recordings.codec

import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.util.Log
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
import java.io.IOException
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
 * Every encoded file must also **declare** its length (container/STREAMINFO duration), because the
 * in-app player shows and seeks by it — decoding to the end would not notice a FLAC that says 0:00.
 *
 * The `old_*` tests decode recordings the way FFmpegKit wrote them before this change (fixtures in
 * `androidTest/assets/old_recordings`, made once with `ffmpeg -f lavfi -i sine=frequency=220:...` and
 * the app's old settings: MP3 `-q:a 2`, AAC/M4A 192k, Opus 96k), plus a 24-bit WAV.
 *
 * FLAC is skipped (not failed) on a device without a FLAC encoder: there the app saves WAV with the
 * «η μετατροπή απέτυχε» message, which `RecordingSaverTranscodeFailureTest` already proves.
 */
@RunWith(AndroidJUnit4::class)
class CodecRoundTripTest {
    private companion object {
        const val TAG = "CodecRoundTripTest"
    }

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
        assertEquals("$format declared duration (s)", 2.0, declaredSeconds(encoded), 0.15)

        try {
            assertDecodesToTone(encoded, expectedSeconds = 2.0, label = format.name)
        } finally {
            encoded.delete()
        }
    }

    @Test
    fun oldFlac() = oldRecording("old.flac")

    @Test
    fun oldMp3() = oldRecording("old.mp3")

    @Test
    fun oldAac() = oldRecording("old.aac")

    @Test
    fun oldM4a() = oldRecording("old.m4a")

    /**
     * From Android 10 an old Ogg Opus recording must decode. Below that Android may not read Ogg Opus
     * at all, so either outcome is accepted there — a decoded tone, or a clean IOException (the analysis
     * then shows «αποτυχία» instead of crashing) — and the run logs which one this device gave.
     */
    @Test
    fun oldOpus() {
        if (Build.VERSION.SDK_INT >= RecordingFormatOption.OPUS_MIN_SDK) {
            oldRecording("old.opus")
            return
        }
        val outcome = runCatching { oldRecording("old.opus") }
        val error = outcome.exceptionOrNull()
        Log.i(TAG, "old Ogg Opus on API ${Build.VERSION.SDK_INT}: ${if (error == null) "decoded" else "not readable ($error)"}")
        assertTrue("below API 29 only a clean IOException is acceptable, got $error", error == null || error is IOException)
    }

    @Test
    fun old24BitWav() = oldRecording("old_24bit.wav")

    private fun oldRecording(asset: String) {
        val file = File(dir, asset)
        InstrumentationRegistry.getInstrumentation().context.assets.open("old_recordings/$asset").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        try {
            assertDecodesToTone(file, expectedSeconds = 1.0, label = asset)
        } finally {
            file.delete()
        }
    }

    private fun declaredSeconds(file: File): Double {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
            val format = extractor.getTrackFormat(0)
            return if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) / 1e6 else 0.0
        } finally {
            extractor.release()
        }
    }

    private fun assertDecodesToTone(encoded: File, expectedSeconds: Double, label: String) {
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
                // Lossy codecs add a little priming/padding; anything this near is the same recording.
                assertEquals("$label length (s)", expectedSeconds, seconds, 0.15)
                val middle = samples.copyOfRange(count / 4, count * 3 / 4)
                val hz = upwardCrossings(middle) * RecordingDecoder.SAMPLE_RATE.toDouble() / middle.size
                assertEquals("$label pitch (Hz)", 220.0, hz, 3.0)
            }
        } finally {
            decoded.delete()
        }
    }

    /** The capture input. Named apart from every encoded output, so a WAV "encode" is never a copy onto itself. */
    private fun toneWav(): File {
        val file = File(dir, "capture_source.wav")
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
