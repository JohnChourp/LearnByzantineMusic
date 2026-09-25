package com.johnchourp.learnbyzantinemusic.recordings.session

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.johnchourp.learnbyzantinemusic.AppLanguage
import com.johnchourp.learnbyzantinemusic.notifications.AppNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Keeps a recording going with the screen off (ClickUp `869f5x273`).
 *
 * **Why.** With the display off the app drops to the background, and from Android 9 an app in the
 * background gets silence from the microphone — the rest of a long recording came out mute, with no
 * message. A foreground service of type `microphone`, with its «Ηχογράφηση…» notification, keeps the
 * process in the foreground for as long as a recording is active, saving included.
 *
 * **What it may do.** Show the notification and forward its «Παύση» / «Συνέχεια» / «Στάση» to the
 * process's [RecordingSession] through [RecordingControls], which has no discard: «Στάση» stops and
 * saves. Swiping the app away from recents does the same. It leaves by itself once the session is
 * idle. Every decision lives in [RecordingServiceCore], tested on the JVM.
 *
 * **What it must never do: end a recording badly.** It is started with a plain `startService` from
 * the visible «Ηχογραφήσεις» screen, not `startForegroundService`: a service started that way which
 * then fails to call `startForeground` gets the whole process killed by the system — and the
 * recording with it. Here a refused `startForeground` (app already in the background, a permission,
 * an OEM quirk) is logged and the service steps aside; the recording goes on as before G3, on its
 * screen, which stays on.
 *
 * **Declared** in the manifest `exported="false"`, `foregroundServiceType="microphone"`, with
 * `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE` and `POST_NOTIFICATIONS`.
 */
class RecordingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watching: Job? = null
    private var lastStartId = 0
    private lateinit var core: RecordingServiceCore

    override fun attachBaseContext(newBase: Context?) {
        // The notification speaks the app's language, as every screen does (BaseActivity).
        super.attachBaseContext(AppLanguage.wrapContextWithLocale(newBase))
    }

    override fun onCreate() {
        super.onCreate()
        core = RecordingServiceCore(
            session = RecordingSessions.get(applicationContext),
            promote = { state ->
                AppNotifications.ensureChannel(this, AppNotifications.Channel.RECORDING)
                ServiceCompat.startForeground(
                    this,
                    AppNotifications.RECORDING_NOTIFICATION_ID,
                    RecordingNotification.build(this, state),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
                )
            },
            refresh = { state -> RecordingNotification.show(this, state) },
            leave = {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                // Only if no newer start arrived meanwhile — the next recording's, racing this exit.
                stopSelf(lastStartId)
            },
            log = ::logFailure,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        if (core.onStartCommand(intent?.action) && watching == null) {
            val session = RecordingSessions.get(applicationContext)
            watching = scope.launch { session.state.collect { core.onState(it) } }
        }
        // Not restarted after the process dies: the recording died with it, and the next start's
        // sweep brings its audio back.
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        core.onTaskRemoved()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        // Stops watching; the recording is not the service's to touch.
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "LbmRecordingService"

        /**
         * From the «Ηχογραφήσεις» screen, as a recording starts — while the screen is visible, which a
         * microphone foreground service requires. False when it could not start: the recording is
         * unaffected either way.
         */
        fun start(context: Context): Boolean =
            ForegroundGuard.attempt("start the recording service", ::logFailure) {
                context.startService(Intent(context, RecordingService::class.java))
            }

        private fun logFailure(message: String, failure: Throwable) {
            Log.w(TAG, message, failure)
        }
    }
}
