package com.johnchourp.learnbyzantinemusic.modes

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

private const val AUDIO_SAMPLE_RATE = 44_100

enum class ToneTimbre(
    private val harmonics: List<Harmonic>,
    private val detuneCents: List<Double> = listOf(0.0),
    private val vibratoHz: Double = 0.0,
    private val vibratoDepthCents: Double = 0.0
) {
    CLEAN(
        harmonics = listOf(Harmonic(1.0, 1.0))
    ),
    SOFT(
        harmonics = listOf(
            Harmonic(1.0, 1.0),
            Harmonic(2.0, 0.22),
            Harmonic(3.0, 0.08)
        )
    ),
    CRYSTAL(
        harmonics = listOf(
            Harmonic(1.0, 1.0),
            Harmonic(2.0, 0.74),
            Harmonic(3.0, 0.52),
            Harmonic(4.0, 0.37),
            Harmonic(5.0, 0.22),
            Harmonic(6.0, 0.11)
        ),
        vibratoHz = 6.5,
        vibratoDepthCents = 3.0
    );

    private val detuneFactors: DoubleArray = detuneCents
        .map { 2.0.pow(it / 1200.0) }
        .toDoubleArray()

    private val harmonicGainSum: Double = harmonics.sumOf { it.gain }.coerceAtLeast(1e-6)

    fun buildSample(
        voicePhases: DoubleArray,
        basePhaseIncrements: DoubleArray,
        vibratoFactor: Double
    ): Double {
        var mixed = 0.0
        for (voiceIndex in voicePhases.indices) {
            var phase = voicePhases[voiceIndex] + (basePhaseIncrements[voiceIndex] * vibratoFactor)
            if (phase >= TWO_PI) {
                phase -= TWO_PI
            }
            voicePhases[voiceIndex] = phase
            for (harmonic in harmonics) {
                mixed += sin(phase * harmonic.multiplier) * harmonic.gain
            }
        }
        return mixed / (harmonicGainSum * voicePhases.size)
    }

    fun createBasePhaseIncrements(frequencyHz: Double): DoubleArray =
        DoubleArray(detuneFactors.size) { index ->
            TWO_PI * frequencyHz * detuneFactors[index] / AUDIO_SAMPLE_RATE
        }

    fun vibratoIncrement(): Double =
        if (vibratoHz <= 0.0) 0.0 else TWO_PI * vibratoHz / AUDIO_SAMPLE_RATE

    fun vibratoFactor(phase: Double): Double {
        if (vibratoDepthCents <= 0.0) {
            return 1.0
        }
        val cents = sin(phase) * vibratoDepthCents
        return 2.0.pow(cents / 1200.0)
    }

    fun createVoicePhases(): DoubleArray = DoubleArray(detuneFactors.size)

    private data class Harmonic(val multiplier: Double, val gain: Double)

    private companion object {
        const val TWO_PI = 2.0 * PI
    }
}

class PhthongTonePlayer {
    private val lock = Any()

    @Volatile
    private var shouldRun: Boolean = false

    private var audioTrack: AudioTrack? = null
    private var playbackThread: Thread? = null

    fun start(frequencyHz: Double, timbre: ToneTimbre = ToneTimbre.CLEAN) {
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
                { streamTone(track, frequencyHz, timbre) },
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

    private fun streamTone(track: AudioTrack, frequencyHz: Double, timbre: ToneTimbre) {
        val samples = ShortArray(SAMPLES_PER_CHUNK)
        val amplitude = Short.MAX_VALUE * AMPLITUDE
        val voicePhases = timbre.createVoicePhases()
        val basePhaseIncrements = timbre.createBasePhaseIncrements(frequencyHz)
        val vibratoPhaseIncrement = timbre.vibratoIncrement()
        var vibratoPhase = 0.0
        var sampleCounter = 0L
        try {
            while (shouldRun) {
                for (index in samples.indices) {
                    val vibratoFactor = timbre.vibratoFactor(vibratoPhase)
                    val sample = timbre.buildSample(voicePhases, basePhaseIncrements, vibratoFactor)
                    val attackGain = if (sampleCounter < ATTACK_SAMPLES) {
                        sampleCounter.toDouble() / ATTACK_SAMPLES
                    } else {
                        1.0
                    }
                    samples[index] = (sample * amplitude * attackGain)
                        .coerceIn(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble())
                        .roundToInt()
                        .toShort()
                    sampleCounter++
                    vibratoPhase += vibratoPhaseIncrement
                    if (vibratoPhase >= 2.0 * PI) {
                        vibratoPhase -= 2.0 * PI
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
        const val SAMPLE_RATE = AUDIO_SAMPLE_RATE
        const val SAMPLES_PER_CHUNK = 1_024
        const val MIN_BUFFER_BYTES = 4_096
        const val AMPLITUDE = 0.18
        const val FADE_OUT_STEPS = 5
        const val FADE_OUT_SLEEP_MS = 8L
        const val ATTACK_MS = 12L
        val ATTACK_SAMPLES = ((SAMPLE_RATE * ATTACK_MS) / 1000.0).roundToInt().coerceAtLeast(1)
    }
}
