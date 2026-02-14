package com.johnchourp.learnbyzantinemusic.modes

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

class PhthongTonePlayer {
    private val lock = Any()

    @Volatile
    private var shouldRun: Boolean = false

    private var audioTrack: AudioTrack? = null
    private var playbackThread: Thread? = null

    fun start(frequencyHz: Double) {
        if (frequencyHz <= 0.0 || frequencyHz.isNaN() || frequencyHz.isInfinite()) {
            return
        }
        stop()
        val track = createAudioTrack() ?: return
        synchronized(lock) {
            try {
                track.play()
            } catch (_: IllegalStateException) {
                track.release()
                return
            }
            shouldRun = true
            audioTrack = track
            playbackThread = Thread(
                { streamSine(track, frequencyHz) },
                "PhthongTonePlayerThread"
            ).apply {
                isDaemon = true
                start()
            }
        }
    }

    fun stop() {
        val trackToStop: AudioTrack?
        val threadToJoin: Thread?
        synchronized(lock) {
            if (!shouldRun && audioTrack == null) {
                return
            }
            shouldRun = false
            threadToJoin = playbackThread
            playbackThread = null
            trackToStop = audioTrack
            audioTrack = null
        }
        threadToJoin?.join(120L)
        if (trackToStop != null) {
            fadeOut(trackToStop)
            safeStopAndRelease(trackToStop)
        }
    }

    fun release() {
        stop()
    }

    private fun streamSine(track: AudioTrack, frequencyHz: Double) {
        val samples = ShortArray(SAMPLES_PER_CHUNK)
        val amplitude = (Short.MAX_VALUE * AMPLITUDE).toInt()
        var phase = 0.0
        val phaseIncrement = 2.0 * PI * frequencyHz / SAMPLE_RATE
        try {
            while (shouldRun) {
                for (index in samples.indices) {
                    samples[index] = (sin(phase) * amplitude).toInt().toShort()
                    phase += phaseIncrement
                    if (phase >= 2.0 * PI) {
                        phase -= 2.0 * PI
                    }
                }
                val written = track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
                if (written <= 0) {
                    break
                }
            }
        } catch (_: IllegalStateException) {
            // Ignore and allow caller to cleanly release.
        }
    }

    private fun fadeOut(track: AudioTrack) {
        for (step in FADE_OUT_STEPS downTo 1) {
            try {
                track.setVolume(step.toFloat() / FADE_OUT_STEPS)
                Thread.sleep(FADE_OUT_SLEEP_MS)
            } catch (_: IllegalStateException) {
                return
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            }
        }
    }

    private fun safeStopAndRelease(track: AudioTrack) {
        try {
            track.pause()
            track.flush()
            track.stop()
        } catch (_: IllegalStateException) {
            // Ignore and continue to release.
        } finally {
            track.release()
        }
    }

    private fun createAudioTrack(): AudioTrack? {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferSize <= 0) {
            return null
        }
        val bufferSize = minBufferSize.coerceAtLeast(MIN_BUFFER_BYTES)
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val SAMPLES_PER_CHUNK = 1_024
        const val MIN_BUFFER_BYTES = 4_096
        const val AMPLITUDE = 0.18
        const val FADE_OUT_STEPS = 5
        const val FADE_OUT_SLEEP_MS = 8L
    }
}
