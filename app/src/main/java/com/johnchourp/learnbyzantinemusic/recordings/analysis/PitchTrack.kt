package com.johnchourp.learnbyzantinemusic.recordings.analysis

import com.johnchourp.learnbyzantinemusic.music.IntonationProfile
import com.johnchourp.learnbyzantinemusic.trainer.YinPitchDetector
import kotlin.math.sqrt

/** One analysis frame: its start time and the detected pitch, or [UNVOICED]. */
data class PitchFrame(val timeMs: Long, val frequencyHz: Float) {
    val isVoiced: Boolean get() = frequencyHz > 0f

    companion object {
        const val UNVOICED = -1f
    }
}

data class PitchTrack(val frames: List<PitchFrame>, val hopMs: Double, val durationMs: Long)

/**
 * Pitch over time for a whole recording: [WINDOW]-sample windows every [HOP] samples, a silence
 * gate on RMS, then the Melody Trainer's [YinPitchDetector]. At the 22 050 Hz the decoder writes,
 * a frame is 46 ms of audio every 23 ms — the same window length the live trainer listens with.
 *
 * Window, hop and silence gate all come from [IntonationProfile], in milliseconds. The sample counts
 * are taken at [RecordingDecoder.SAMPLE_RATE] because every WAV this reads was written by it.
 */
object PitchTrackAnalyzer {
    /** [IntonationProfile.WINDOW_MS] at the decoder's rate: 1024 samples. */
    val WINDOW: Int = IntonationProfile.samplesIn(IntonationProfile.WINDOW_MS, RecordingDecoder.SAMPLE_RATE)

    /** [IntonationProfile.OFFLINE_HOP_MS] at the decoder's rate: 512 samples, half a window. */
    val HOP: Int = IntonationProfile.samplesIn(IntonationProfile.OFFLINE_HOP_MS, RecordingDecoder.SAMPLE_RATE)

    /** Streams [reader] to the end; [onProgress] gets 0..1 when the length is known. */
    fun analyze(reader: WavPcmReader, onProgress: ((Float) -> Unit)? = null): PitchTrack {
        val sampleRate = reader.sampleRate
        val window = FloatArray(WINDOW)
        val chunk = FloatArray(HOP)
        val frames = ArrayList<PitchFrame>()
        var filled = 0
        var consumed = 0L
        val total = reader.totalSamples.takeIf { it in 1 until Long.MAX_VALUE / 4 }
        var lastReported = -1

        fun readSamples(target: FloatArray, offset: Int, length: Int): Int {
            var got = 0
            while (got < length) {
                val count = reader.read(target, offset + got, length - got)
                if (count <= 0) break
                got += count
            }
            return got
        }

        // Prime the first window, then slide by HOP.
        filled = readSamples(window, 0, WINDOW)
        consumed += filled
        while (filled == WINDOW) {
            frames += PitchFrame(
                timeMs = ((frames.size.toLong() * HOP) * 1000L) / sampleRate,
                frequencyHz = detect(window, sampleRate),
            )
            val got = readSamples(chunk, 0, HOP)
            if (got < HOP) break
            consumed += got
            System.arraycopy(window, HOP, window, 0, WINDOW - HOP)
            System.arraycopy(chunk, 0, window, WINDOW - HOP, HOP)
            if (total != null && onProgress != null) {
                val percent = ((consumed * 100) / total).toInt().coerceIn(0, 100)
                if (percent != lastReported) {
                    lastReported = percent
                    onProgress(percent / 100f)
                }
            }
        }
        return PitchTrack(
            frames = frames,
            hopMs = HOP * 1000.0 / sampleRate,
            durationMs = (consumed * 1000L) / sampleRate,
        )
    }

    /** Same pipeline on in-memory samples (tests, short clips). */
    fun analyze(samples: FloatArray, sampleRate: Int): PitchTrack {
        val frames = ArrayList<PitchFrame>()
        var start = 0
        while (start + WINDOW <= samples.size) {
            frames += PitchFrame(
                timeMs = (start.toLong() * 1000L) / sampleRate,
                frequencyHz = detect(samples.copyOfRange(start, start + WINDOW), sampleRate),
            )
            start += HOP
        }
        return PitchTrack(frames, HOP * 1000.0 / sampleRate, (samples.size.toLong() * 1000L) / sampleRate)
    }

    private fun detect(window: FloatArray, sampleRate: Int): Float {
        var sumSquares = 0.0
        for (sample in window) sumSquares += sample * sample
        if (sqrt(sumSquares / window.size) < IntonationProfile.SILENCE_RMS) return PitchFrame.UNVOICED
        val hz = YinPitchDetector.detect(window, sampleRate)
        return if (hz > 0f) hz else PitchFrame.UNVOICED
    }
}
