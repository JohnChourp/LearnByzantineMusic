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

    /**
     * The same increments as [createBasePhaseIncrements], written into [increments] instead of a new
     * array — a glide recomputes them on every sample, on the audio thread. Same formula, so the
     * result for a given frequency is identical to a fresh array's.
     */
    fun fillBasePhaseIncrements(frequencyHz: Double, increments: DoubleArray) {
        for (index in increments.indices) {
            increments[index] = TWO_PI * frequencyHz * detuneFactors[index] / AUDIO_SAMPLE_RATE
        }
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

/**
 * One sustained tone as samples: the pure half of [PhthongTonePlayer] (ClickUp `869f5x251`), so the
 * thing that decides whether moving the ison clicks can be tested without an AudioTrack.
 *
 * [retune] glides to the new pitch over [GLIDE_MS] **inside the same stream**. The phase
 * accumulators carry on and only their increment changes, so the waveform stays continuous and there
 * is nothing to click. Moving the drone used to mean stop and start: a stepped fade to silence, a new
 * track and a fresh attack — an audible gap every time.
 *
 * Without a retune the samples are exactly the ones the player always wrote: the loop below is the
 * old one, moved here unchanged, and the glide branch does nothing until a retune asks for it.
 */
internal class ToneVoice(frequencyHz: Double, private val timbre: ToneTimbre) {
    private val voicePhases = timbre.createVoicePhases()
    private val basePhaseIncrements = timbre.createBasePhaseIncrements(frequencyHz)
    private val vibratoPhaseIncrement = timbre.vibratoIncrement()
    private var vibratoPhase = 0.0
    private var sampleCounter = 0L

    /** Where the pitch should go. Written by [retune] from any thread; read once per [render]. */
    @Volatile
    private var requestedHz = frequencyHz

    private var currentHz = frequencyHz
    private var glideFromHz = frequencyHz
    private var glideToHz = frequencyHz
    private var glideSample = GLIDE_SAMPLES

    /** The pitch the last rendered sample sounded. Exactly the requested one once a glide ends. */
    val frequencyHz: Double get() = currentHz

    fun retune(frequencyHz: Double) {
        requestedHz = frequencyHz
    }

    /** Writes the next [out].size samples, scaled to [amplitude]. */
    fun render(out: ShortArray, amplitude: Double) {
        val requested = requestedHz
        if (requested != glideToHz) {
            // A retune mid-glide starts from wherever the pitch is now, so it cannot jump either.
            glideFromHz = currentHz
            glideToHz = requested
            glideSample = 0
        }
        for (index in out.indices) {
            if (glideSample < GLIDE_SAMPLES) {
                glideSample++
                // Even in μόρια (a straight line in log-frequency), and exactly the target at the
                // end: the drone must land on the ladder's own frequency, not next to it.
                currentHz = if (glideSample == GLIDE_SAMPLES) {
                    glideToHz
                } else {
                    glideFromHz * (glideToHz / glideFromHz).pow(glideSample.toDouble() / GLIDE_SAMPLES)
                }
                timbre.fillBasePhaseIncrements(currentHz, basePhaseIncrements)
            }
            val vibratoFactor = timbre.vibratoFactor(vibratoPhase)
            val sample = timbre.buildSample(voicePhases, basePhaseIncrements, vibratoFactor)
            val attackGain = if (sampleCounter < ATTACK_SAMPLES) {
                sampleCounter.toDouble() / ATTACK_SAMPLES
            } else {
                1.0
            }
            out[index] = (sample * amplitude * attackGain)
                .coerceIn(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble())
                .roundToInt()
                .toShort()
            sampleCounter++
            vibratoPhase += vibratoPhaseIncrement
            if (vibratoPhase >= 2.0 * PI) {
                vibratoPhase -= 2.0 * PI
            }
        }
    }

    companion object {
        /** How long a retune takes to reach its new pitch. */
        const val GLIDE_MS = 50L
        val GLIDE_SAMPLES = ((AUDIO_SAMPLE_RATE * GLIDE_MS) / 1000.0).roundToInt()
        const val ATTACK_MS = 12L
        val ATTACK_SAMPLES = ((AUDIO_SAMPLE_RATE * ATTACK_MS) / 1000.0).roundToInt().coerceAtLeast(1)
    }
}

class PhthongTonePlayer {
    private val lock = Any()

    @Volatile
    private var shouldRun: Boolean = false

    private var audioTrack: AudioTrack? = null
    private var playbackThread: Thread? = null
    private var voice: ToneVoice? = null

    fun start(frequencyHz: Double, timbre: ToneTimbre = ToneTimbre.CLEAN) {
        if (!isPlayable(frequencyHz)) {
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
            val toneVoice = ToneVoice(frequencyHz, timbre)
            voice = toneVoice
            playbackThread = Thread(
                { streamTone(track, toneVoice) },
                "PhthongTonePlayerThread"
            ).apply {
                isDaemon = true
                start()
            }
        }
    }

    /**
     * Moves a sounding tone to [frequencyHz] with a short glide inside the same stream, instead of
     * stopping and starting it (ClickUp `869f5x251`). Returns false when nothing is sounding, and the
     * caller then [start]s one.
     *
     * [start] and [stop] are untouched on purpose: `MelodySequencePlayer` relies on every note being
     * a fresh stream with its own attack, and a glide between melody notes would smear them.
     */
    fun retune(frequencyHz: Double): Boolean {
        if (!isPlayable(frequencyHz)) {
            return false
        }
        synchronized(lock) {
            val current = voice ?: return false
            if (!shouldRun) {
                return false
            }
            current.retune(frequencyHz)
            return true
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
            voice = null
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

    private fun isPlayable(frequencyHz: Double): Boolean =
        frequencyHz > 0.0 && !frequencyHz.isNaN() && !frequencyHz.isInfinite()

    private fun streamTone(track: AudioTrack, voice: ToneVoice) {
        val samples = ShortArray(SAMPLES_PER_CHUNK)
        val amplitude = Short.MAX_VALUE * AMPLITUDE
        try {
            while (shouldRun) {
                voice.render(samples, amplitude)
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
    }
}
