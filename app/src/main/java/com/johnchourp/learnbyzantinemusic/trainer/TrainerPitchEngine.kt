package com.johnchourp.learnbyzantinemusic.trainer

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresPermission
import kotlin.math.sqrt

/**
 * Captures the microphone, slices it into analysis windows, runs [YinPitchDetector] on
 * each, maps the result to the nearest phthong, and delivers it on the main thread. A
 * loudness gate suppresses background hiss so silence reads as "no pitch" rather than a
 * spurious note. The caller must hold RECORD_AUDIO before calling [start].
 */
class TrainerPitchEngine(
    private val onPitch: (PitchMatch?) -> Unit,
    private val sampleRate: Int = DEFAULT_SAMPLE_RATE,
    private val windowSize: Int = DEFAULT_WINDOW_SIZE
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var audioRecord: AudioRecord? = null
    private var captureThread: Thread? = null

    @Volatile
    private var running = false

    val isRunning: Boolean get() = running

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(): Boolean {
        stop()
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) return false
        val bufferSize = maxOf(minBuffer, windowSize * 2)

        val recorder = buildRecorder(bufferSize) ?: return false
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return false
        }

        audioRecord = recorder
        running = true
        return try {
            recorder.startRecording()
            captureThread = Thread({ captureLoop(recorder) }, "TrainerPitchEngine").apply {
                isDaemon = true
                start()
            }
            true
        } catch (_: IllegalStateException) {
            stop()
            false
        }
    }

    @SuppressLint("MissingPermission") // start() is annotated @RequiresPermission(RECORD_AUDIO)
    private fun buildRecorder(bufferSize: Int): AudioRecord? =
        try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun captureLoop(recorder: AudioRecord) {
        val shortBuffer = ShortArray(windowSize)
        val floatBuffer = FloatArray(windowSize)
        while (running) {
            var read = 0
            while (read < windowSize && running) {
                val r = recorder.read(shortBuffer, read, windowSize - read)
                if (r <= 0) break
                read += r
            }
            if (!running || read < windowSize) continue

            for (i in 0 until windowSize) {
                floatBuffer[i] = shortBuffer[i] / SHORT_FULL_SCALE
            }

            val match = if (rootMeanSquare(floatBuffer) < SILENCE_RMS) {
                null
            } else {
                val frequency = YinPitchDetector.detect(floatBuffer, sampleRate)
                if (frequency <= 0f) null else TrainerPitchTable.nearestPhthong(frequency.toDouble())
            }
            mainHandler.post { if (running) onPitch(match) }
        }
    }

    fun stop() {
        running = false
        captureThread = null
        val recorder = audioRecord
        audioRecord = null
        if (recorder != null) {
            try {
                recorder.stop()
            } catch (_: IllegalStateException) {
                // Already stopped.
            }
            recorder.release()
        }
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun rootMeanSquare(samples: FloatArray): Double {
        var sum = 0.0
        for (sample in samples) {
            sum += sample * sample
        }
        return sqrt(sum / samples.size)
    }

    companion object {
        const val DEFAULT_SAMPLE_RATE = 44_100
        const val DEFAULT_WINDOW_SIZE = 2_048
        private const val SHORT_FULL_SCALE = 32_768f
        private const val SILENCE_RMS = 0.012
    }
}
