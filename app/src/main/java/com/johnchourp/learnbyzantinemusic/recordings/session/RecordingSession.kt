package com.johnchourp.learnbyzantinemusic.recordings.session

import com.johnchourp.learnbyzantinemusic.recordings.RecordingFormatOption
import com.johnchourp.learnbyzantinemusic.recordings.RecordingStateUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream

/** The microphone, reduced to what capture needs: `AudioRecord` in the app, a fake in tests. */
interface PcmSource {
    /** Bytes asked for per read — the capture buffer. */
    val bufferSize: Int

    /** Blocks for the next PCM16 mono 44.1 kHz bytes; 0 or less when none arrived. */
    fun read(buffer: ByteArray): Int

    /** Stops delivering, so that a blocked [read] returns. */
    fun stop()

    /** Gives the microphone back, once the capture thread has finished. */
    fun release()
}

/** What happened to the last recording: the status line and the toast say it. */
sealed interface RecordingEvent {
    /** [fellBackFrom] non-null: that conversion failed and the WAV was saved instead. */
    data class Saved(val fileName: String, val fellBackFrom: RecordingFormatOption?) : RecordingEvent

    /** The folder could not take it; it waits on the «Ηχογραφήσεις» page. */
    data class KeptInApp(val name: String) : RecordingEvent

    data object SaveFailed : RecordingEvent

    data object StartFailed : RecordingEvent
}

data class RecordingSessionState(
    val phase: RecordingStateUi = RecordingStateUi.IDLE,
    /** What the recording in progress is for; null when nothing is being recorded or saved. */
    val target: RecordingTarget? = null,
    /** Recorded time before the current stretch — the timer's value while paused or saving. */
    val elapsedBeforeMs: Long = 0L,
    /** The clock when the current RECORDING stretch began; null when not recording. */
    val recordingSince: Long? = null,
    val lastEvent: RecordingEvent? = null,
) {
    val isActive: Boolean
        get() = phase == RecordingStateUi.RECORDING || phase == RecordingStateUi.PAUSED || phase == RecordingStateUi.SAVING

    fun elapsedAt(now: Long): Long =
        elapsedBeforeMs + (recordingSince?.let { (now - it).coerceAtLeast(0L) } ?: 0L)
}

data class PendingState(
    val recordings: List<PendingRecording> = emptyList(),
    /** Recordings with a save or delete in flight; their buttons wait. */
    val busyIds: Set<String> = emptySet(),
)

/**
 * The one recording of the process: capture, pause, stop-and-save — owned by the process, not by a
 * screen (ClickUp `869f5x26x`).
 *
 * **Why.** Capture used to live in `RecordingsActivity`, whose `onDestroy` stopped it and deleted the
 * temporary WAV. Anything that re-creates the screen — the device switching to dark theme on a
 * schedule, split-screen, a fold, a language or font-size change — therefore threw away the recording
 * in progress, without a word; so did Back while «Αποθήκευση…» was still running. Here a screen only
 * *shows* this state and forwards taps: a screen going away stops nothing and deletes nothing, and a
 * new one finds the recording exactly where it is.
 *
 * **What deletes audio — the complete list.** A verified save ([RecordingSaver]); the user's explicit
 * «Απόρριψη» ([discard]) or «Διαγραφή» of a pending recording ([deletePending]); a capture file that
 * never received a single sample. Nothing else, and never while it is being recorded or saved.
 *
 * **Threads.** Taps arrive on the main thread; capture runs on its own thread, as it always has;
 * saving runs in [scope], which outlives every screen, one save at a time. `RecordingSessions` holds
 * the process's instance and wires the real microphone, FFmpeg and the SAF folder; the tests wire
 * fakes. A future foreground service (G3) can own this object as it is.
 */
class RecordingSession(
    private val scope: CoroutineScope,
    private val captureDir: File,
    private val pending: PendingRecordings,
    private val saver: RecordingSaver,
    private val openMicrophone: () -> PcmSource,
    /** Resolves the user's folder *at save time*; null when it is not usable right now. */
    private val openSink: (RecordingMeta) -> RecordingSink?,
    private val selectedFormat: () -> RecordingFormatOption,
    /** Monotonic, for the timer (`SystemClock.elapsedRealtime` in the app). */
    private val elapsedClock: () -> Long,
    /** Wall clock, for file names. */
    private val wallClock: () -> Long,
    /** A save finished, whether or not any screen is left to show it (the toast). */
    private val onEvent: (RecordingEvent) -> Unit = {},
) {
    private val lock = Any()

    /** One transcode and one folder write at a time. */
    private val saveLane = Mutex()
    private val refreshLane = Mutex()

    /** Files a capture, save or delete is using right now: the sweep never touches them. */
    private val busy = mutableSetOf<File>()
    private var capture: Capture? = null

    private val _state = MutableStateFlow(RecordingSessionState())
    val state: StateFlow<RecordingSessionState> = _state.asStateFlow()

    private val _pending = MutableStateFlow(PendingState())
    val pendingState: StateFlow<PendingState> = _pending.asStateFlow()

    /** Starts capturing for [target]. False when a recording is already in progress or the microphone failed. */
    fun start(target: RecordingTarget): Boolean {
        synchronized(lock) {
            if (capture != null || _state.value.isActive) return false
            val wav = freeCaptureFile()
            // Busy before it exists, so the sweep can never mistake it for an orphan.
            busy += wav
            var output: FileOutputStream? = null
            var source: PcmSource? = null
            try {
                captureDir.mkdirs()
                output = FileOutputStream(wav).apply {
                    write(ByteArray(WavFormat.HEADER_SIZE))
                    flush()
                }
                val meta = RecordingMeta(target)
                RecordingFiles.writeMeta(wav, meta)
                source = openMicrophone()
                val since = elapsedClock()
                val started = Capture(wav, meta, source, output)
                started.begin()
                capture = started
                _state.value = RecordingSessionState(
                    phase = RecordingStateUi.RECORDING,
                    target = target,
                    recordingSince = since,
                )
                return true
            } catch (_: Throwable) {
                source?.let {
                    runCatching { it.stop() }
                    runCatching { it.release() }
                }
                output?.let { runCatching { it.close() } }
                // Only the empty header was written: there is no audio to lose.
                RecordingFiles.deleteBundle(wav)
                busy -= wav
                _state.value = RecordingSessionState(
                    phase = RecordingStateUi.ERROR,
                    lastEvent = RecordingEvent.StartFailed,
                )
                return false
            }
        }
    }

    fun pause() {
        synchronized(lock) {
            val current = capture ?: return
            val state = _state.value
            if (state.phase != RecordingStateUi.RECORDING) return
            current.paused = true
            _state.value = state.copy(
                phase = RecordingStateUi.PAUSED,
                elapsedBeforeMs = state.elapsedAt(elapsedClock()),
                recordingSince = null,
            )
        }
    }

    fun resume() {
        synchronized(lock) {
            val current = capture ?: return
            val state = _state.value
            if (state.phase != RecordingStateUi.PAUSED) return
            current.paused = false
            _state.value = state.copy(phase = RecordingStateUi.RECORDING, recordingSince = elapsedClock())
        }
    }

    /**
     * Stops and saves. Returns at once; the save runs in [scope] and ends in IDLE (saved) or ERROR
     * (kept in the app, or failed), with [RecordingSessionState.lastEvent] and [onEvent] saying which.
     */
    fun stop(): Boolean {
        val stopped: Capture
        synchronized(lock) {
            val state = _state.value
            if (state.phase != RecordingStateUi.RECORDING && state.phase != RecordingStateUi.PAUSED) return false
            stopped = capture ?: return false
            capture = null
            _state.value = state.copy(
                phase = RecordingStateUi.SAVING,
                elapsedBeforeMs = state.elapsedAt(elapsedClock()),
                recordingSince = null,
            )
        }
        scope.launch {
            val outcome = runCatching {
                stopped.halt()
                // Named when it stops, as it always was; written down before the slow part, so a
                // process killed mid-save leaves a recording the sweep can still name and place.
                val meta = stopped.meta.namedAt(wallClock())
                RecordingFiles.writeMeta(stopped.wav, meta)
                saveLane.withLock { saveInto(stopped.wav, meta) }
            }.getOrElse { SaveOutcome.Failed }
            val event = outcome.toEvent()
            synchronized(lock) {
                busy -= stopped.wav
                _state.value = RecordingSessionState(
                    phase = if (outcome is SaveOutcome.Saved) RecordingStateUi.IDLE else RecordingStateUi.ERROR,
                    lastEvent = event,
                )
            }
            publishPending()
            onEvent(event)
        }
        return true
    }

    /** The user's explicit «Απόρριψη»: the one way a recording in progress is thrown away. */
    fun discard(): Boolean {
        val dropped: Capture
        synchronized(lock) {
            val state = _state.value
            if (state.phase != RecordingStateUi.RECORDING && state.phase != RecordingStateUi.PAUSED) return false
            dropped = capture ?: return false
            capture = null
            // Synchronously, as the screen always did: the microphone is free before this returns.
            dropped.halt()
            _state.value = RecordingSessionState()
        }
        scope.launch {
            RecordingFiles.deleteBundle(dropped.wav)
            synchronized(lock) { busy -= dropped.wav }
        }
        return true
    }

    /** «Αποθήκευση» on a pending recording: into the folder picked *now*, in the format selected *now*. */
    fun savePending(recording: PendingRecording): Boolean {
        if (!claim(recording)) return false
        scope.launch {
            val outcome = runCatching {
                saveLane.withLock { saveInto(recording.wav, recording.meta.namedAt(recording.recordedAtMillis)) }
            }.getOrElse { SaveOutcome.Failed }
            val event = outcome.toEvent()
            synchronized(lock) {
                busy -= recording.wav
                _state.update { state ->
                    // A recording in progress keeps its phase; otherwise the phase follows the latest
                    // outcome, so a saved kept recording clears the error it was kept with.
                    if (state.isActive) {
                        state.copy(lastEvent = event)
                    } else {
                        state.copy(
                            phase = if (outcome is SaveOutcome.Saved) RecordingStateUi.IDLE else RecordingStateUi.ERROR,
                            lastEvent = event,
                        )
                    }
                }
            }
            publishPending()
            onEvent(event)
        }
        return true
    }

    /** «Διαγραφή» on a pending recording, after the screen asked the user to confirm. */
    fun deletePending(recording: PendingRecording): Boolean {
        if (!claim(recording)) return false
        scope.launch {
            runCatching { pending.delete(recording) }
            synchronized(lock) { busy -= recording.wav }
            publishPending()
        }
        return true
    }

    /**
     * The sweep, once per process before anything records: capture files left by a process that
     * died, and the old screen's cache temp files, join the pending list (see [PendingRecordings]).
     */
    fun recoverOrphans(legacyCacheDir: File?): Job = scope.launch {
        runCatching {
            pending.recoverOrphans(captureDir, legacyCacheDir) { file -> synchronized(lock) { file in busy } }
        }
        publishPending()
    }

    private suspend fun saveInto(wav: File, meta: RecordingMeta): SaveOutcome {
        val sink = runCatching { openSink(meta) }.getOrNull()
        val outcome = saver.save(wav, meta, selectedFormat(), sink)
        if (outcome is SaveOutcome.Saved && sink != null) {
            // The «πρόσφατες» list. A failure here cannot undo a verified file, so it is not one.
            runCatching { sink.register(outcome.file) }
        }
        return outcome
    }

    private fun SaveOutcome.toEvent(): RecordingEvent = when (this) {
        is SaveOutcome.Saved -> RecordingEvent.Saved(
            fileName = runCatching { file.name }.getOrNull().orEmpty(),
            fellBackFrom = fellBackFrom,
        )
        is SaveOutcome.KeptPending -> RecordingEvent.KeptInApp(recording.baseName)
        SaveOutcome.Failed -> RecordingEvent.SaveFailed
    }

    private fun claim(recording: PendingRecording): Boolean {
        synchronized(lock) {
            if (recording.wav in busy) return false
            busy += recording.wav
        }
        _pending.update { it.copy(busyIds = it.busyIds + recording.id) }
        return true
    }

    private suspend fun publishPending() {
        refreshLane.withLock {
            val recordings = runCatching { pending.list() }.getOrDefault(emptyList())
            val busyIds = synchronized(lock) { recordings.filter { it.wav in busy }.map { it.id }.toSet() }
            _pending.value = PendingState(recordings, busyIds)
        }
    }

    private fun freeCaptureFile(): File {
        var stamp = wallClock()
        var file = File(captureDir, "capture_$stamp.wav")
        while (file.exists() || file in busy) {
            stamp++
            file = File(captureDir, "capture_$stamp.wav")
        }
        return file
    }

    /** One capture: the microphone, its thread, and the WAV it writes into. */
    private inner class Capture(
        val wav: File,
        val meta: RecordingMeta,
        private val source: PcmSource,
        output: FileOutputStream,
    ) {
        @Volatile
        var paused = false

        @Volatile
        private var running = true

        private val ioLock = Any()
        private var output: FileOutputStream? = output
        private val thread = Thread(::loop, "lbm-recording")

        fun begin() = thread.start()

        private fun loop() {
            val buffer = ByteArray(source.bufferSize)
            while (running) {
                val read = source.read(buffer)
                if (read <= 0 || paused) continue
                val written = synchronized(ioLock) {
                    val stream = output ?: return@synchronized true
                    runCatching { stream.write(buffer, 0, read) }.isSuccess
                }
                if (!written) {
                    // Storage full or gone. Save what was captured rather than record on into
                    // nothing — the old loop let this exception kill the app.
                    running = false
                    scope.launch { stopAfterWriteFailure(this@Capture) }
                    return
                }
            }
        }

        /** Stops the microphone and closes the file. Never deletes anything. */
        fun halt() {
            running = false
            runCatching { source.stop() }
            runCatching { thread.join(1200) }
            runCatching { source.release() }
            synchronized(ioLock) {
                runCatching { output?.flush() }
                runCatching { output?.close() }
                output = null
            }
        }
    }

    private fun stopAfterWriteFailure(failed: Capture) {
        if (synchronized(lock) { capture === failed }) stop()
    }
}
