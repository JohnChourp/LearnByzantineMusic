package com.johnchourp.learnbyzantinemusic.modes

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.johnchourp.learnbyzantinemusic.AppLanguage
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.Phthong
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.notifications.AppNotifications
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * «Συνέχισε στο παρασκήνιο»: the ison playing on with the screen off, or under a book or PDF in
 * another app (ClickUp `869f5x2dq`).
 *
 * **Flow.** With the option on, the 8 Ήχοι page never sounds the ison itself: it hands every change
 * here ([play], [stop]) from the first moment, so leaving the page is not a hand-off — the same
 * stream simply goes on, with nothing to click. A foreground service of type `mediaPlayback` owns one
 * [PhthongTonePlayer] and a media-style notification on [AppNotifications.Channel.ISON]: φθόγγος −/+,
 * «Στάση», the mode's name, and a tap that opens the page. The page follows what plays here through
 * [playing], so a change made from the notification is what it shows when it comes back.
 *
 * **Decisions** are [BackgroundIson]'s: the pitch is the page's own lookup, −/+ walk «Ίσον σε…»'s
 * choices, and the ison stops by itself after an hour or when the headphones come out. A stop from the
 * notification, the hour or the headphones leaves a quiet notification whose «Αναπαραγωγή» reopens the
 * page with the same ison sounding.
 *
 * **Never a trap.** It asks for no audio focus, so the ison can sound under a recording, and it never
 * touches the microphone: «Πού είμαι» stays with the page. Like `RecordingService` it is started with
 * a plain `startService` from the visible page — never `startForegroundService`, whose failure to reach
 * the foreground gets the whole process killed — and «Αναπαραγωγή» after a stop goes through the page
 * for the same reason. A refused `startForeground` is logged, reported through [refused], and the
 * service steps aside; the page then plays the ison itself, as it did before this service existed.
 *
 * **Declared** `exported="false"`, `foregroundServiceType="mediaPlayback"`, with `FOREGROUND_SERVICE`,
 * `FOREGROUND_SERVICE_MEDIA_PLAYBACK` and `POST_NOTIFICATIONS`.
 */
class IsonPlaybackService : Service() {

    private val ison = BackgroundIson()
    private val player = PhthongTonePlayer()
    private val handler = Handler(Looper.getMainLooper())
    private val timeUpCheck = Runnable { checkTimeUp() }

    /** The last thing played, so a stopped notification can offer to play it again. */
    private var last: IsonDrone.Request? = null

    /** Promoted once; after that a change only refreshes the notification. */
    private var inForeground = false

    private var lastStartId = 0

    private val becomingNoisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != AudioManager.ACTION_AUDIO_BECOMING_NOISY) return
            if (ison.onBecomingNoisy() == BackgroundIson.Sound.STOP) pause()
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        // The notification speaks the app's language, as every screen does (BaseActivity).
        super.attachBaseContext(AppLanguage.wrapContextWithLocale(newBase))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(
            this,
            becomingNoisy,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        when (intent?.action) {
            ACTION_PLAY -> {
                val request = intent.readRequest()
                if (request == null) finish() else play(request, intent.readTimbre())
            }
            ACTION_MOVE -> move(intent.getIntExtra(EXTRA_DIRECTION, 0))
            ACTION_PAUSE -> pause()
            ACTION_STOP -> finish()
            else -> finish()
        }
        // A killed service stays down: an ison must never come back by itself.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(timeUpCheck)
        runCatching { unregisterReceiver(becomingNoisy) }
        player.release()
        publish(null)
        active = false
        super.onDestroy()
    }

    private fun play(request: IsonDrone.Request, timbre: ToneTimbre) {
        val now = SystemClock.elapsedRealtime()
        val sound = ison.play(request, timbre, now)
        val hz = ison.frequencyHz
        val promoted = inForeground
        if (hz == null || !(promoted || goForeground(request))) {
            ison.stop()
            if (hz != null) refusals.tryEmit(request)
            finish()
            return
        }
        // Just promoted, startForeground posted it; already in the foreground, refresh its text.
        if (promoted) show(notification(request, playing = true))
        last = request
        when (sound) {
            BackgroundIson.Sound.START -> player.start(hz, timbre)
            BackgroundIson.Sound.RETUNE -> if (!player.retune(hz)) player.start(hz, timbre)
            BackgroundIson.Sound.STOP, BackgroundIson.Sound.NONE -> Unit
        }
        publish(ison.request)
        scheduleTimeUp(now)
    }

    private fun move(direction: Int) {
        val sound = ison.move(direction)
        // A button of a notification that has just stopped: nothing to move, so the service goes.
        val request = ison.request ?: return finish()
        if (sound == BackgroundIson.Sound.RETUNE) {
            ison.frequencyHz?.let { hz -> if (!player.retune(hz)) player.start(hz, ison.timbre) }
        }
        last = request
        show(notification(request, playing = true))
        publish(request)
    }

    /**
     * Stops the sound but leaves a quiet notification that plays it again — after the notification's
     * own «Στάση», the hour, or the headphones. The service itself goes.
     */
    private fun pause() {
        handler.removeCallbacks(timeUpCheck)
        ison.stop()
        player.stop()
        publish(null)
        val request = last ?: return finish()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        inForeground = false
        show(notification(request, playing = false))
        stopSelf(lastStartId)
    }

    /** Silence, no notification, no service. */
    private fun finish() {
        handler.removeCallbacks(timeUpCheck)
        ison.stop()
        player.stop()
        publish(null)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        inForeground = false
        // Only if no newer start arrived meanwhile — the page's next «play», racing this exit.
        stopSelf(lastStartId)
    }

    private fun scheduleTimeUp(nowMillis: Long) {
        handler.removeCallbacks(timeUpCheck)
        ison.millisUntilTimeUp(nowMillis)?.let { handler.postDelayed(timeUpCheck, it) }
    }

    private fun checkTimeUp() {
        val now = SystemClock.elapsedRealtime()
        if (ison.isTimeUp(now)) pause() else scheduleTimeUp(now)
    }

    /** False when the platform refuses the foreground: logged, and the page plays the ison itself. */
    private fun goForeground(request: IsonDrone.Request): Boolean =
        try {
            AppNotifications.ensureChannel(this, AppNotifications.Channel.ISON)
            ServiceCompat.startForeground(
                this,
                AppNotifications.ISON_NOTIFICATION_ID,
                notification(request, playing = true),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
            inForeground = true
            true
        } catch (failure: Exception) {
            Log.w(TAG, "could not put the ison in the foreground — the page plays it instead", failure)
            false
        }

    /** Re-posts. Without the Android 13+ permission it would not be shown: nothing to do. */
    @SuppressLint("MissingPermission") // AppNotifications.canPost checks POST_NOTIFICATIONS first.
    private fun show(notification: Notification) {
        if (!AppNotifications.canPost(this)) return
        getSystemService(NotificationManager::class.java)?.notify(AppNotifications.ISON_NOTIFICATION_ID, notification)
    }

    // ---- the notification -----------------------------------------------------------------------

    private fun notification(request: IsonDrone.Request, playing: Boolean): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, AppNotifications.Channel.ISON.id)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        val held = IsonDrone.held(request)?.phthong?.label.orEmpty()
        builder
            .setSmallIcon(R.drawable.ic_notification_ison)
            .setContentTitle(getString(R.string.eight_modes_ison_notification_title, getString(ModeResources.nameRes(request.mode))))
            .setContentText(
                getString(
                    if (playing) R.string.eight_modes_ison_notification_text else R.string.eight_modes_ison_notification_stopped,
                    held,
                )
            )
            .setContentIntent(openPage(request, startIson = false))
            .setOngoing(playing)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
        if (playing) {
            builder
                .addAction(action(android.R.drawable.ic_media_previous, R.string.eight_modes_ison_notification_lower, moveIntent(-1)))
                .addAction(action(android.R.drawable.ic_media_pause, R.string.eight_modes_ison_notification_stop, pauseIntent()))
                .addAction(action(android.R.drawable.ic_media_next, R.string.eight_modes_ison_notification_higher, moveIntent(+1)))
                .setStyle(Notification.MediaStyle().setShowActionsInCompactView(0, 1, 2))
        } else {
            builder
                .addAction(action(android.R.drawable.ic_media_play, R.string.eight_modes_ison_notification_play, openPage(request, startIson = true)))
                .setStyle(Notification.MediaStyle().setShowActionsInCompactView(0))
        }
        return builder.build()
    }

    private fun action(icon: Int, title: Int, intent: PendingIntent): Notification.Action =
        Notification.Action.Builder(Icon.createWithResource(this, icon), getString(title), intent).build()

    private fun serviceIntent(action: String): Intent =
        Intent(this, IsonPlaybackService::class.java).setAction(action)

    /** While it plays the service is in the foreground, so its own buttons may start it. */
    private fun moveIntent(direction: Int): PendingIntent =
        PendingIntent.getService(
            this,
            if (direction > 0) REQUEST_HIGHER else REQUEST_LOWER,
            serviceIntent(ACTION_MOVE).putExtra(EXTRA_DIRECTION, direction),
            PENDING_FLAGS,
        )

    private fun pauseIntent(): PendingIntent =
        PendingIntent.getService(this, REQUEST_PAUSE, serviceIntent(ACTION_PAUSE), PENDING_FLAGS)

    /**
     * The page, on the mode that is sounding, for this opening only — and with [startIson], sounding
     * that same ison again: after a stop the service is gone, and only a visible page may start it.
     */
    private fun openPage(request: IsonDrone.Request, startIson: Boolean): PendingIntent {
        val intent = if (startIson) {
            EightModesActivity.startIsonIntent(this, request)
        } else {
            EightModesActivity.intent(this, request.mode.key)
        }
        return PendingIntent.getActivity(
            this,
            if (startIson) REQUEST_PLAY else REQUEST_OPEN,
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PENDING_FLAGS,
        )
    }

    companion object {
        private const val TAG = "LbmIsonService"

        private const val ACTION_PLAY = "com.johnchourp.learnbyzantinemusic.modes.ison.PLAY"
        private const val ACTION_MOVE = "com.johnchourp.learnbyzantinemusic.modes.ison.MOVE"
        private const val ACTION_PAUSE = "com.johnchourp.learnbyzantinemusic.modes.ison.PAUSE"
        private const val ACTION_STOP = "com.johnchourp.learnbyzantinemusic.modes.ison.STOP"

        private const val EXTRA_MODE = "mode"
        private const val EXTRA_SHIFT = "base_shift_moria"
        private const val EXTRA_CHOICE = "choice"
        private const val EXTRA_CHOICE_OCTAVE = "choice_octave"
        private const val EXTRA_TIMBRE = "timbre"
        private const val EXTRA_DIRECTION = "direction"

        private const val REQUEST_OPEN = 1
        private const val REQUEST_LOWER = 2
        private const val REQUEST_PAUSE = 3
        private const val REQUEST_HIGHER = 4
        private const val REQUEST_PLAY = 5
        private const val PENDING_FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        private val playingState = MutableStateFlow<IsonDrone.Request?>(null)

        /** What the background ison is sounding now, or null — for the page to follow. */
        val playing: StateFlow<IsonDrone.Request?> = playingState.asStateFlow()

        private val refusals = MutableSharedFlow<IsonDrone.Request>(extraBufferCapacity = 1)

        /** A request the platform would not let the service play: the page plays it itself instead. */
        val refused: SharedFlow<IsonDrone.Request> = refusals.asSharedFlow()

        /** True from a [play] until the service is gone, so [stop] knows there is something to stop. */
        @Volatile
        private var active = false

        private fun publish(request: IsonDrone.Request?) {
            playingState.value = request
        }

        /**
         * From the visible 8 Ήχοι page: starts the background ison, or moves the one that is sounding.
         * False when it could not even be started — the caller then plays the ison itself.
         */
        fun play(context: Context, request: IsonDrone.Request, timbre: ToneTimbre): Boolean =
            try {
                val intent = Intent(context, IsonPlaybackService::class.java)
                    .setAction(ACTION_PLAY)
                    .putRequest(request, timbre)
                context.startService(intent)
                active = true
                true
            } catch (failure: RuntimeException) {
                Log.w(TAG, "could not start the ison service — the page plays it instead", failure)
                false
            }

        /**
         * Silences a sounding background ison, and removes its notification. Once it has stopped by
         * itself — «Στάση», the hour, the headphones — there is nothing to do: its stopped notification
         * stays, so it can be played again.
         */
        fun stop(context: Context) {
            if (!active) return
            // Through the service, in order behind the start it follows.
            runCatching {
                context.startService(Intent(context, IsonPlaybackService::class.java).setAction(ACTION_STOP))
            }
        }

        /** «Συνέχισε στο παρασκήνιο» turned off: stops it, and clears even a stopped notification. */
        fun forget(context: Context) {
            stop(context)
            context.getSystemService(NotificationManager::class.java)?.cancel(AppNotifications.ISON_NOTIFICATION_ID)
        }

        private fun Intent.putRequest(request: IsonDrone.Request, timbre: ToneTimbre): Intent {
            putExtra(EXTRA_MODE, request.mode.key)
            putExtra(EXTRA_SHIFT, request.baseShiftMoria)
            request.choice?.let { choice ->
                putExtra(EXTRA_CHOICE, choice.name.name)
                putExtra(EXTRA_CHOICE_OCTAVE, choice.octave)
            }
            putExtra(EXTRA_TIMBRE, timbre.name)
            return this
        }

        private fun Intent.readRequest(): IsonDrone.Request? {
            val mode = Mode.fromKey(getStringExtra(EXTRA_MODE)) ?: return null
            val choice = getStringExtra(EXTRA_CHOICE)
                ?.let { stored -> PhthongName.entries.firstOrNull { it.name == stored } }
                ?.let { name -> Phthong(name, getIntExtra(EXTRA_CHOICE_OCTAVE, 0)) }
            return IsonDrone.Request(mode, getIntExtra(EXTRA_SHIFT, 0), choice)
        }

        private fun Intent.readTimbre(): ToneTimbre =
            ToneTimbre.entries.firstOrNull { it.name == getStringExtra(EXTRA_TIMBRE) } ?: ToneTimbre.CLEAN
    }
}
