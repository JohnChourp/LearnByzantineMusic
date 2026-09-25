package com.johnchourp.learnbyzantinemusic.practice

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * Runs near the reminder's time each day (ClickUp `869f5x2dy`). It notifies only when
 * [PracticeReminderPolicy.shouldNotify] says so — the reminder is on and the user has not completed a
 * practice session today — and a refused notifications permission only hides the notification.
 * Then it schedules tomorrow's run, last, because re-enqueuing the same unique work replaces this run.
 */
class PracticeReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val context = applicationContext
        val enabled = PracticeReminders.isEnabled(context)
        val summary = PracticeLogStore(context).summary()
        if (PracticeReminderPolicy.shouldNotify(enabled, summary.practisedToday)) {
            PracticeReminderNotification.post(context, summary.streak)
        }
        if (enabled) PracticeReminders.schedule(context, ExistingWorkPolicy.REPLACE)
        return Result.success()
    }
}
