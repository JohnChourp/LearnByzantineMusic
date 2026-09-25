package com.johnchourp.learnbyzantinemusic.practice

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.johnchourp.learnbyzantinemusic.AppLanguage
import com.johnchourp.learnbyzantinemusic.MainActivity
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.notifications.AppNotifications

/**
 * The daily practice reminder itself (ClickUp `869f5x2dy`), through the app's one notifications
 * helper, [AppNotifications]: its channel, its id, and whether posting is allowed at all. A tap opens
 * «Πεντάλεπτο της ημέρας» with the home screen under it, so Back lands in the app, not outside it.
 */
object PracticeReminderNotification {

    // Posting is checked by AppNotifications.canPost first; a permission revoked in between is caught.
    @SuppressLint("MissingPermission")
    fun post(context: Context, streak: Int) {
        if (!AppNotifications.canPost(context)) return
        val channel = AppNotifications.Channel.PRACTICE_REMINDER
        AppNotifications.ensureChannel(context, channel)
        val localized = AppLanguage.wrapContextWithLocale(context) ?: context
        val open = TaskStackBuilder.create(context)
            .addNextIntent(Intent(context, MainActivity::class.java))
            .addNextIntent(Intent(context, DailyPracticeActivity::class.java))
            .getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val text = if (streak > 0) {
            localized.getString(R.string.practice_reminder_notification_text_streak, streak)
        } else {
            localized.getString(R.string.practice_reminder_notification_text)
        }
        val notification = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(R.drawable.ic_notification_practice)
            .setContentTitle(localized.getString(R.string.practice_daily_title))
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(AppNotifications.PRACTICE_REMINDER_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // The permission went away between the check and the post: there is simply nothing to show.
        }
    }
}
