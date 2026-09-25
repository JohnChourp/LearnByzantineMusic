package com.johnchourp.learnbyzantinemusic.practice

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * The opt-in daily practice reminder (ClickUp `869f5x2dy`): its two settings, and its schedule.
 *
 * WorkManager wakes [PracticeReminderWorker] near the chosen local time; the worker posts only when
 * [PracticeReminderPolicy.shouldNotify] says so — the reminder is on and today has no completed
 * session — and then schedules the next day. One-time work re-enqueued daily, rather than a 24-hour
 * periodic one, so the reminder keeps its local time across a daylight-saving change.
 *
 * [ensureScheduled] runs on every app start: it re-creates a schedule that a restore from backup or a
 * cleared WorkManager lost, and never moves one that exists. WorkManager itself survives a reboot.
 *
 * **Stored for good:** [WORK_NAME] names the work in WorkManager's own database; never rename it.
 */
object PracticeReminders {
    const val WORK_NAME = "practice_reminder"

    private const val MINUTES_PER_DAY = 24 * 60
    private val DEFAULT_MINUTE = PracticeReminderPolicy.DEFAULT_TIME.hour * 60 + PracticeReminderPolicy.DEFAULT_TIME.minute

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(AppPrefs.PracticeReminderEnabled.name, false)

    fun time(context: Context): LocalTime {
        val minute = prefs(context).getInt(AppPrefs.PracticeReminderMinuteOfDay.name, DEFAULT_MINUTE)
        return if (minute in 0 until MINUTES_PER_DAY) LocalTime.of(minute / 60, minute % 60) else PracticeReminderPolicy.DEFAULT_TIME
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(AppPrefs.PracticeReminderEnabled.name, enabled).apply()
        if (enabled) {
            schedule(context, ExistingWorkPolicy.REPLACE)
        } else {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    fun setTime(context: Context, time: LocalTime) {
        prefs(context).edit().putInt(AppPrefs.PracticeReminderMinuteOfDay.name, time.hour * 60 + time.minute).apply()
        if (isEnabled(context)) schedule(context, ExistingWorkPolicy.REPLACE)
    }

    /** Schedules the reminder when it is on and nothing is scheduled; an existing schedule stays. */
    fun ensureScheduled(context: Context) {
        if (isEnabled(context)) schedule(context, ExistingWorkPolicy.KEEP)
    }

    /** The next reminder, at the chosen time today if it is still ahead, else tomorrow. */
    internal fun schedule(context: Context, policy: ExistingWorkPolicy) {
        val now = ZonedDateTime.now()
        val delay = Duration.between(now, PracticeReminderPolicy.nextReminderAt(now, time(context)))
        val request = OneTimeWorkRequestBuilder<PracticeReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, policy, request)
    }

    private fun prefs(context: Context) = AppPrefs.open(context.applicationContext, AppPrefs.Store.PRACTICE)
}
