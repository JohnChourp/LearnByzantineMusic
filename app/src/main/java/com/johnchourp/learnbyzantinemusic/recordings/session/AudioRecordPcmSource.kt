package com.johnchourp.learnbyzantinemusic.recordings.session

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

/**
 * The microphone as a [PcmSource]: `AudioRecord` at [WavFormat]'s 44.1 kHz mono PCM16, with the
 * buffer the recording screen has always used — twice the platform minimum, at least 8 KB.
 */
class AudioRecordPcmSource private constructor(
    private val recorder: AudioRecord,
    override val bufferSize: Int,
) : PcmSource {

    override fun read(buffer: ByteArray): Int = recorder.read(buffer, 0, buffer.size)

    override fun stop() {
        recorder.stop()
    }

    override fun release() {
        recorder.release()
    }

    companion object {
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT

        /** Starts the microphone, or throws. `RECORD_AUDIO` is checked by the screen before a session starts. */
        @SuppressLint("MissingPermission")
        fun open(): AudioRecordPcmSource {
            val minBuffer = AudioRecord.getMinBufferSize(WavFormat.SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_ENCODING)
            check(minBuffer > 0) { "audio_record_min_buffer_unavailable" }
            val bufferSize = (minBuffer * 2).coerceAtLeast(8192)
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                WavFormat.SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_ENCODING,
                bufferSize,
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                error("audio_record_not_initialized")
            }
            try {
                recorder.startRecording()
            } catch (error: Throwable) {
                recorder.release()
                throw error
            }
            return AudioRecordPcmSource(recorder, bufferSize)
        }
    }
}
