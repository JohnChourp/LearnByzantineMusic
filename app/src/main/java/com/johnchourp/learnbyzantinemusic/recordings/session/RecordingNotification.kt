package com.johnchourp.learnbyzantinemusic.recordings.session

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.notifications.AppNotifications
import com.johnchourp.learnbyzantinemusic.recordings.RecordingStateUi
import com.johnchourp.learnbyzantinemusic.recordings.RecordingsActivity

/**
 * The «Ηχογράφηση…» notification of a recording in progress (ClickUp `869f5x273`): what it says in
 * each phase, its buttons ([RecordingNotificationAction.buttonsFor]), and a tap that opens the
 * «Ηχογραφήσεις» page, which shows the recording as it is.
 *
 * Silent and ongoing, on the low-importance «Ηχογράφηση» channel; while recording the system runs its
 * clock. The buttons go to `RecordingService`, which applies them to the session.
 */
object RecordingNotification {

    fun build(context: Context, state: RecordingSessionState): Notification {
        val title = context.getString(
            when (state.phase) {
                RecordingStateUi.PAUSED -> R.string.recordings_notification_paused
                RecordingStateUi.SAVING -> R.string.recordings_status_saving
                else -> R.string.recordings_notification_recording
            }
        )
        val builder = NotificationCompat.Builder(context, AppNotifications.Channel.RECORDING.id)
            .setSmallIcon(R.drawable.ic_notification_recording)
            .setContentTitle(title)
            .setContentText(state.target?.label?.let { context.getString(R.string.recordings_target_template, it) })
            .setContentIntent(openScreen(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (state.phase == RecordingStateUi.RECORDING) {
            // The system ticks this clock; nothing here re-posts per second.
            builder.setUsesChronometer(true)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis() - state.elapsedAt(SystemClock.elapsedRealtime()))
        } else {
            builder.setShowWhen(false)
        }
        RecordingNotificationAction.buttonsFor(state.phase).forEach { action ->
            builder.addAction(0, context.getString(labelOf(action)), buttonIntent(context, action))
        }
        return builder.build()
    }

    /** Re-posts for [state]. Without the Android 13+ permission it would not be shown: nothing to do. */
    @SuppressLint("MissingPermission") // AppNotifications.canPost checks POST_NOTIFICATIONS first.
    fun show(context: Context, state: RecordingSessionState) {
        if (!AppNotifications.canPost(context)) return
        NotificationManagerCompat.from(context).notify(AppNotifications.RECORDING_NOTIFICATION_ID, build(context, state))
    }

    private fun labelOf(action: RecordingNotificationAction): Int = when (action) {
        RecordingNotificationAction.PAUSE -> R.string.recordings_pause
        RecordingNotificationAction.RESUME -> R.string.recordings_resume
        RecordingNotificationAction.STOP -> R.string.recordings_notification_stop
    }

    private fun buttonIntent(context: Context, action: RecordingNotificationAction): PendingIntent =
        PendingIntent.getService(
            context,
            action.ordinal + 1,
            Intent(context, RecordingService::class.java).setAction(action.intentAction),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun openScreen(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, RecordingsActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
