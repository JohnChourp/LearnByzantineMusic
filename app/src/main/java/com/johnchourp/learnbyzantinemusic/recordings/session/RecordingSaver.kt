package com.johnchourp.learnbyzantinemusic.recordings.session

import com.johnchourp.learnbyzantinemusic.recordings.RecordingFormatOption
import java.io.File
import java.io.OutputStream

/** Turns the capture WAV into another format: FFmpeg in the app, a fake in tests. */
fun interface Transcoder {
    /** True only when [output] now holds the whole recording in [format]. */
    fun transcode(sourceWav: File, output: File, format: RecordingFormatOption): Boolean
}

/** The user's folder — or the hymn sub-folder a recording was made for — reduced to what saving needs. */
interface RecordingSink {
    /** A new, empty file in the folder; null when the folder refuses one. */
    fun createFile(displayName: String, mimeType: String): SinkFile?

    /** Records a verified save in the app's own history (the «πρόσφατες» list). */
    suspend fun register(file: SinkFile) {}
}

interface SinkFile {
    val name: String

    fun openOutputStream(): OutputStream?

    /** The size the folder reports for the file, or null when it cannot tell. */
    fun reportedSize(): Long?

    fun delete(): Boolean
}

sealed interface SaveOutcome {
    /** In the user's folder and verified. [fellBackFrom] = the format whose conversion failed, so WAV was saved. */
    data class Saved(
        val file: SinkFile,
        val format: RecordingFormatOption,
        val fellBackFrom: RecordingFormatOption?,
    ) : SaveOutcome

    /** The folder could not take it; the WAV is safe in app-private storage ([PendingRecordings]). */
    data class KeptPending(
        val recording: PendingRecording,
        val fellBackFrom: RecordingFormatOption?,
    ) : SaveOutcome

    /** Not even app-private storage could take it; the WAV is untouched where it was. */
    data object Failed : SaveOutcome
}

/**
 * Saves one finished recording, in an order in which no failure can cost the audio (ClickUp
 * `869f5x26x`):
 *
 * 1. **transcode** into the chosen format — before the user's folder is touched at all;
 * 2. **create** the file in the folder;
 * 3. **copy** into it;
 * 4. **verify** the size;
 * 5. **only then** delete the temporary audio.
 *
 * **A failed transcode produces no file in the chosen format**, empty or otherwise: the WAV itself is
 * saved instead, same folder, same base name, `.wav` — and the outcome says so, for the message the
 * user sees (`RecordingSaverTranscodeFailureTest`). Before this, the target file was created *first*,
 * so a failed FFmpeg run left an empty file in the folder and the audio orphaned in the cache.
 *
 * **A failed copy** deletes the partial file and keeps the audio. Whenever the folder does not end up
 * with a verified file — permission lost, folder deleted, storage full, a write cut short — the WAV
 * moves into [PendingRecordings], where the «Ηχογραφήσεις» page lists it until the user saves or
 * deletes it. (No second attempt as WAV follows a refused converted file: a folder that refused one
 * file refuses the next, and the WAV is the larger of the two.)
 *
 * Pure JVM on purpose: the order is the whole point, and fakes prove it.
 */
class RecordingSaver(
    private val transcoder: Transcoder,
    private val pending: PendingRecordings,
    /** Where the converted copy is written before it goes to the folder. */
    private val workDir: File,
) {
    fun save(wav: File, meta: RecordingMeta, format: RecordingFormatOption, sink: RecordingSink?): SaveOutcome {
        if (!wav.isFile) return SaveOutcome.Failed
        val named = meta.namedAt(wav.lastModified())
        if (runCatching { WavFormat.finalizeHeader(wav) }.isFailure) {
            // Without its header the WAV is not playable yet — and there is nothing to verify against.
            return keepInApp(wav, named, fellBackFrom = null)
        }

        // 1. Transcode. Until this has succeeded, nothing exists in the user's folder.
        val encoded = if (format == RecordingFormatOption.WAV) null else transcode(wav, format)
        val fellBackFrom = if (format != RecordingFormatOption.WAV && encoded == null) format else null
        val savedFormat = if (encoded != null) format else RecordingFormatOption.WAV
        try {
            // 2–4. Create, copy, verify.
            val written = sink?.let {
                writeVerified(it, "${named.baseName}.${savedFormat.extension}", savedFormat.mimeType, encoded ?: wav)
            }
            if (written != null) {
                // 5. The copy is verified: only now may the temporary audio go.
                RecordingFiles.deleteBundle(wav)
                return SaveOutcome.Saved(written, savedFormat, fellBackFrom)
            }
        } finally {
            // The converted copy is derived from the WAV; the WAV is what is kept.
            encoded?.delete()
        }
        return keepInApp(wav, named, fellBackFrom)
    }

    private fun transcode(wav: File, format: RecordingFormatOption): File? {
        val output = runCatching {
            workDir.mkdirs()
            File.createTempFile("recording_encoded_", ".${format.extension}", workDir)
        }.getOrNull() ?: return null
        // An empty output is a failure however the transcoder reports it.
        val converted = runCatching { transcoder.transcode(wav, output, format) }.getOrDefault(false) && output.length() > 0L
        if (!converted) {
            output.delete()
            return null
        }
        return output
    }

    private fun writeVerified(sink: RecordingSink, name: String, mimeType: String, source: File): SinkFile? {
        val target = runCatching { sink.createFile(name, mimeType) }.getOrNull() ?: return null
        val expected = source.length()
        val copied = runCatching {
            val output = target.openOutputStream() ?: error("output_stream_not_available")
            output.use { out -> source.inputStream().use { input -> input.copyTo(out) } }
        }.getOrNull()
        val verified = copied == expected && runCatching {
            // A folder that cannot report sizes is trusted on the byte count the stream accepted;
            // one that reports a different size (0 included) holds a partial file.
            val reported = target.reportedSize()
            reported == null || reported == expected
        }.getOrDefault(false)
        if (verified) return target
        // A partial file must not pose as the recording. The audio is still here, untouched.
        runCatching { target.delete() }
        return null
    }

    private fun keepInApp(wav: File, meta: RecordingMeta, fellBackFrom: RecordingFormatOption?): SaveOutcome {
        val kept = runCatching { pending.keep(wav, meta) }.getOrNull() ?: return SaveOutcome.Failed
        return SaveOutcome.KeptPending(kept, fellBackFrom)
    }
}
