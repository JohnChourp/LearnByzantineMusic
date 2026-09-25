package com.johnchourp.learnbyzantinemusic.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.johnchourp.learnbyzantinemusic.AppLanguage
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs

/**
 * Every notification the app shows goes through here: its channel, its id, and the Android 13+
 * permission to post it (ClickUp `869f5x273`).
 *
 * The app has no server and sends nothing: these are local notifications. The first is the
 * «Ηχογράφηση…» of a recording in progress (`RecordingService`); the second the ison playing in the
 * background (`IsonPlaybackService`, ClickUp `869f5x2dq`). The reminder is meant to add a [Channel]
 * and an id here too, not a second helper.
 *
 * **Stored for good by the system:** a channel's id — the user's settings for that channel hang on
 * it — so an id is never renamed. Names and descriptions follow the app's language each time a
 * channel is ensured.
 *
 * **The permission** is asked at most once per install ([shouldAskPermission], [markPermissionAsked];
 * key `notifications_permission_asked`). A «no» changes only visibility: a foreground service still
 * runs, its notification is just not shown.
 */
object AppNotifications {

    /** One id per notification the app can show; they only have to differ. */
    const val RECORDING_NOTIFICATION_ID = 1
    const val ISON_NOTIFICATION_ID = 2

    enum class Channel(
        val id: String,
        @StringRes val nameRes: Int,
        @StringRes val descriptionRes: Int,
        val importance: Int,
    ) {
        /** Low importance and silent: it sits in the shade while recording and never interrupts. */
        RECORDING(
            id = "recording",
            nameRes = R.string.recordings_notification_channel_name,
            descriptionRes = R.string.recordings_notification_channel_description,
            importance = NotificationManagerCompat.IMPORTANCE_LOW,
        ),

        /** Low and silent as well: it must never sound over the ison it controls. */
        ISON(
            id = "ison",
            nameRes = R.string.eight_modes_ison_notification_channel,
            descriptionRes = R.string.eight_modes_ison_notification_channel_description,
            importance = NotificationManagerCompat.IMPORTANCE_LOW,
        ),
    }

    /** Creates [channel], or refreshes its name and description in the app's language. No sound, no badge. */
    fun ensureChannel(context: Context, channel: Channel) {
        val localized = AppLanguage.wrapContextWithLocale(context) ?: context
        NotificationManagerCompat.from(context).createNotificationChannel(
            NotificationChannelCompat.Builder(channel.id, channel.importance)
                .setName(localized.getString(channel.nameRes))
                .setDescription(localized.getString(channel.descriptionRes))
                .setSound(null, null)
                .setVibrationEnabled(false)
                .setShowBadge(false)
                .build()
        )
    }

    /** Before Android 13 there is no such permission, so it counts as granted. */
    fun isPermissionGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Whether a notification posted now would be seen: permission granted and notifications on. */
    fun canPost(context: Context): Boolean =
        isPermissionGranted(context) && NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Whether to show the notifications prompt now — see [NotificationPermissionPolicy]. */
    fun shouldAskPermission(context: Context): Boolean =
        NotificationPermissionPolicy.shouldAsk(
            sdkInt = Build.VERSION.SDK_INT,
            granted = isPermissionGranted(context),
            alreadyAsked = prefs(context).getBoolean(AppPrefs.NotificationsPermissionAsked.name, false),
        )

    /** Written before the prompt shows, so a process killed during it does not ask again. */
    fun markPermissionAsked(context: Context) {
        prefs(context).edit().putBoolean(AppPrefs.NotificationsPermissionAsked.name, true).apply()
    }

    private fun prefs(context: Context) = AppPrefs.open(context, AppPrefs.Store.SETTINGS)
}
