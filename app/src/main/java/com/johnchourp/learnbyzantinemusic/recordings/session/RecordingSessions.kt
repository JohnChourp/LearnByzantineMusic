package com.johnchourp.learnbyzantinemusic.recordings.session

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Toast
import com.johnchourp.learnbyzantinemusic.AppLanguage
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.recordings.AudioTranscoder
import com.johnchourp.learnbyzantinemusic.recordings.RecordingsPrefs
import com.johnchourp.learnbyzantinemusic.recordings.index.RecordingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/**
 * The process's one [RecordingSession], wired to the real microphone ([AudioRecordPcmSource]),
 * FFmpeg ([AudioTranscoder]), the picked SAF folder ([SafRecordingSink]) and the «πρόσφατες» list.
 *
 * **Storage.** `filesDir/recordings_capture/` holds the recording in progress and
 * `filesDir/recordings_pending/` what could not reach the folder — both app-private, neither in the
 * cache, so the system never clears them; both are kept out of the cloud backup (raw audio, and
 * larger than its quota). The sweep of both — and of the old screen's cache temp files — runs once,
 * when the first screen asks for the session.
 *
 * **The toast** is shown from here, with the application context in the app's language, so a save
 * that finishes after the user has left the screen still says how it ended.
 */
object RecordingSessions {
    const val CAPTURE_DIR = "recordings_capture"
    const val PENDING_DIR = "recordings_pending"

    // Process lifetime, like NotesViewModel.flushScope: a save must outlive every screen.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainThread by lazy { Handler(Looper.getMainLooper()) }

    @Volatile
    private var instance: RecordingSession? = null

    fun get(context: Context): RecordingSession =
        instance ?: synchronized(this) {
            instance ?: create(context.applicationContext).also { instance = it }
        }

    private fun create(app: Context): RecordingSession {
        val pending = PendingRecordings(File(app.filesDir, PENDING_DIR))
        val repository = RecordingsRepository.getInstance(app)
        val session = RecordingSession(
            scope = scope,
            captureDir = File(app.filesDir, CAPTURE_DIR),
            pending = pending,
            saver = RecordingSaver(
                transcoder = { source, output, format -> AudioTranscoder.transcode(source, output, format).isSuccess },
                pending = pending,
                workDir = app.cacheDir,
            ),
            openMicrophone = { AudioRecordPcmSource.open() },
            openSink = { meta -> SafRecordingSink.open(app, repository, meta.target) },
            selectedFormat = { RecordingsPrefs(app).getSelectedFormat() },
            elapsedClock = SystemClock::elapsedRealtime,
            wallClock = System::currentTimeMillis,
            onEvent = { event -> announce(app, event) },
        )
        session.recoverOrphans(legacyCacheDir = app.cacheDir)
        return session
    }

    private fun announce(app: Context, event: RecordingEvent) {
        val text = RecordingEventText.toast(AppLanguage.wrapContextWithLocale(app) ?: app, event) ?: return
        val plainSuccess = event is RecordingEvent.Saved && event.fellBackFrom == null
        mainThread.post {
            Toast.makeText(app, text, if (plainSuccess) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
        }
    }
}

/** How a [RecordingEvent] reads, on the status line and in the toast. */
object RecordingEventText {
    fun status(context: Context, event: RecordingEvent): String = when (event) {
        is RecordingEvent.Saved -> event.fellBackFrom
            ?.let { context.getString(R.string.recordings_saved_as_wav_template, it.name, event.fileName) }
            ?: context.getString(R.string.recordings_status_saved_template, event.fileName)
        is RecordingEvent.KeptInApp -> context.getString(R.string.recordings_kept_in_app)
        RecordingEvent.SaveFailed -> context.getString(R.string.recordings_error_save)
        RecordingEvent.StartFailed -> context.getString(R.string.recordings_error_start)
    }

    /** Null for events that only change the status line (a failed start never had a toast). */
    fun toast(context: Context, event: RecordingEvent): String? = when (event) {
        is RecordingEvent.Saved ->
            if (event.fellBackFrom == null) context.getString(R.string.recordings_saved_ok) else status(context, event)
        is RecordingEvent.KeptInApp -> context.getString(R.string.recordings_kept_in_app)
        RecordingEvent.SaveFailed -> context.getString(R.string.recordings_error_save)
        RecordingEvent.StartFailed -> null
    }
}
