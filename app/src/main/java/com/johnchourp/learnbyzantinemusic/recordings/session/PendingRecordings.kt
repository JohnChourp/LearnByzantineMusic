package com.johnchourp.learnbyzantinemusic.recordings.session

import java.io.File

/** A recording that is safe on this device but not yet in the user's folder. */
data class PendingRecording(
    val id: String,
    val wav: File,
    val meta: RecordingMeta,
    val durationMs: Long,
    val recordedAtMillis: Long,
) {
    /** The name it gets in the folder, without extension. */
    val baseName: String get() = meta.baseName ?: RecordingFiles.baseNameFor(recordedAtMillis)
}

/**
 * `filesDir/recordings_pending/`: recordings the app could not write into the user's folder — the
 * folder's permission was lost, the folder was deleted, the storage was full. They are app-private
 * and **not** in the cache, so the system never clears them; they stay listed on the «Ηχογραφήσεις»
 * page, with «Αποθήκευση» and «Διαγραφή», until the user does one or the other. Nothing here expires.
 *
 * **The sweep ([recoverOrphans]).** A capture file only outlives its process when that process died
 * mid-recording or mid-save. At the start of the next process nothing is capturing, so every such
 * file is an orphan: its header is rewritten from its length and it joins this list, instead of
 * sitting unseen on the device. That includes the `cacheDir/recording_<digits>.wav` temp files the
 * screen wrote before ClickUp `869f5x26x`, which is where a failed conversion's audio used to be
 * stranded.
 *
 * Pure `java.io`, so it is tested on the JVM (`PendingRecordingsSweepTest`).
 */
class PendingRecordings(private val dir: File) {

    fun list(): List<PendingRecording> =
        dir.listFiles { file -> file.isFile && file.extension == "wav" }
            .orEmpty()
            .map(::entryFor)
            .sortedByDescending { it.recordedAtMillis }

    fun find(id: String): PendingRecording? = list().firstOrNull { it.id == id }

    /**
     * Moves [wav] (with [meta]) into the pending folder, or just refreshes its metadata when it is
     * already there. Null when even that failed — the audio is then still where it was, untouched.
     */
    fun keep(wav: File, meta: RecordingMeta): PendingRecording? {
        if (!wav.isFile) return null
        if (!dir.isDirectory && !dir.mkdirs()) return null
        if (isInside(wav)) {
            RecordingFiles.writeMeta(wav, meta)
            return entryFor(wav)
        }
        val destination = freeFileFor(wav.nameWithoutExtension)
        // Metadata first: a process killed mid-move then loses neither the hymn nor the name.
        RecordingFiles.writeMeta(destination, meta)
        if (!move(wav, destination)) {
            RecordingFiles.sidecarOf(destination).delete()
            return null
        }
        RecordingFiles.sidecarOf(wav).delete()
        return entryFor(destination)
    }

    /** The user's explicit «Διαγραφή» — the only way a pending recording ever disappears unsaved. */
    fun delete(recording: PendingRecording): Boolean = RecordingFiles.deleteBundle(recording.wav)

    /**
     * Adopts every capture file no live recording is using. [isBusy] answers for the capture or
     * save in progress in this process, which is never touched. Returns how many were recovered.
     */
    fun recoverOrphans(captureDir: File, legacyCacheDir: File?, isBusy: (File) -> Boolean): Int {
        var recovered = 0
        captureDir.listFiles().orEmpty().filter { it.isFile }.forEach { file ->
            when (file.extension) {
                "wav" -> if (!isBusy(file) && adopt(file)) recovered++
                "json" -> {
                    // Metadata whose audio is gone (a save that completed, a discard) describes nothing.
                    val audio = File(file.parentFile, file.nameWithoutExtension + ".wav")
                    if (!audio.exists() && !isBusy(audio)) file.delete()
                }
            }
        }
        legacyCacheDir
            ?.listFiles { file -> file.isFile && LEGACY_CAPTURE.matches(file.name) }
            .orEmpty()
            .forEach { if (adopt(it)) recovered++ }
        return recovered
    }

    private fun adopt(orphan: File): Boolean {
        if (WavFormat.pcmBytesOf(orphan.length()) == 0L) {
            // Stopped before a single sample arrived: there is no audio to keep.
            RecordingFiles.deleteBundle(orphan)
            return false
        }
        // Name it by when it was last written, before the header rewrite touches the file.
        val meta = (RecordingFiles.readMeta(orphan) ?: RecordingMeta()).namedAt(orphan.lastModified())
        runCatching { WavFormat.finalizeHeader(orphan) }
        return keep(orphan, meta) != null
    }

    private fun entryFor(wav: File): PendingRecording = PendingRecording(
        id = wav.nameWithoutExtension,
        wav = wav,
        meta = RecordingFiles.readMeta(wav) ?: RecordingMeta(),
        durationMs = WavFormat.durationMs(WavFormat.pcmBytesOf(wav.length())),
        recordedAtMillis = wav.lastModified(),
    )

    private fun isInside(file: File): Boolean =
        runCatching { file.parentFile?.canonicalFile == dir.canonicalFile }.getOrDefault(false)

    private fun freeFileFor(id: String): File {
        var candidate = File(dir, "$id.wav")
        var suffix = 1
        while (candidate.exists() || RecordingFiles.sidecarOf(candidate).exists()) {
            candidate = File(dir, "${id}_$suffix.wav")
            suffix++
        }
        return candidate
    }

    /** Same volume: a rename, instant and needing no free space. Otherwise copy, check, then delete. */
    private fun move(from: File, to: File): Boolean {
        if (from.renameTo(to)) return true
        val copied = runCatching {
            from.inputStream().use { input -> to.outputStream().use { output -> input.copyTo(output) } } == from.length()
        }.getOrDefault(false)
        if (!copied) {
            to.delete()
            return false
        }
        to.setLastModified(from.lastModified())
        // Should this delete fail, the audio exists twice — never zero times.
        from.delete()
        return true
    }

    companion object {
        /** `File.createTempFile("recording_", ".wav")` names; user-facing names have `_` inside the digits. */
        private val LEGACY_CAPTURE = Regex("""^recording_-?\d+\.wav$""")
    }
}
