package com.johnchourp.learnbyzantinemusic.practice

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.notifications.AppNotifications
import com.johnchourp.learnbyzantinemusic.practice.ui.PracticeBarUi
import com.johnchourp.learnbyzantinemusic.practice.ui.PracticeHistoryScreen
import com.johnchourp.learnbyzantinemusic.practice.ui.PracticeHistoryUi
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * «Ιστορικό εξάσκησης» (ClickUp `869f5x2dy`): the streak of completed «Πεντάλεπτο της ημέρας»
 * sessions, the best streak, the last seven days as a bar chart of minutes, and the opt-in daily
 * reminder. Called «Ιστορικό» and not «ημερολόγιο» so it is never confused with the Calendar tile.
 *
 * **The reminder.** Switching it on asks for the Android 13+ notifications permission — only then,
 * and at most once per install, through [AppNotifications]. A «no» breaks nothing: the reminder stays
 * on, its notification just cannot show, and the page says so with a way to the system settings.
 *
 * Everything is re-read in `onResume`, since a session completed on another page, or a permission
 * changed in the system settings, must show when the user comes back.
 *
 * **Inputs:** none. **Opens:** the system notification settings. **Touches:** `practice_log` (read),
 * `practice_reminder_enabled` and `practice_reminder_minute_of_day` (read + write),
 * `notifications_permission_asked` (through [AppNotifications]).
 */
class PracticeHistoryActivity : BaseActivity() {

    private lateinit var store: PracticeLogStore
    private lateinit var appLocale: Locale
    private var ui by mutableStateOf(PracticeHistoryUi())

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = PracticeLogStore(this)
        appLocale = resources.configuration.locales.let { if (it.isEmpty) Locale.getDefault() else it[0] }
        refresh()
        setContent {
            LbmTheme(palette = currentPalette()) {
                PracticeHistoryScreen(
                    ui = ui,
                    onBack = ::finish,
                    onReminderChange = ::setReminder,
                    onPickTime = ::pickTime,
                    onOpenNotificationSettings = ::openNotificationSettings,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val summary = store.summary()
        val time = PracticeReminders.time(this)
        ui = PracticeHistoryUi(
            streak = summary.streak,
            bestStreak = summary.bestStreak,
            practisedToday = summary.practisedToday,
            bars = summary.week.map { bar ->
                PracticeBarUi(
                    label = bar.date.dayOfWeek.getDisplayName(TextStyle.SHORT, appLocale),
                    contentDescription = getString(
                        R.string.practice_history_bar_cd,
                        bar.date.dayOfWeek.getDisplayName(TextStyle.FULL, appLocale),
                        bar.minutes,
                    ),
                    minutes = bar.minutes,
                )
            },
            weekDays = PracticeWeek.daysPractised(summary.week),
            weekMinutes = PracticeWeek.totalMinutes(summary.week),
            reminderEnabled = PracticeReminders.isEnabled(this),
            reminderTimeLabel = formatTime(time),
            notificationsBlocked = !AppNotifications.canPost(this),
        )
    }

    private fun setReminder(enabled: Boolean) {
        if (enabled && AppNotifications.shouldAskPermission(this)) {
            // Marked before the prompt shows, so a process killed during it does not ask again.
            AppNotifications.markPermissionAsked(this)
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        PracticeReminders.setEnabled(this, enabled)
        refresh()
    }

    private fun pickTime() {
        val current = PracticeReminders.time(this)
        TimePickerDialog(
            this,
            { _, hour, minute ->
                PracticeReminders.setTime(this, LocalTime.of(hour, minute))
                refresh()
            },
            current.hour,
            current.minute,
            DateFormat.is24HourFormat(this),
        ).show()
    }

    private fun openNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        }
        runCatching { startActivity(intent) }
    }

    private fun formatTime(time: LocalTime): String {
        val pattern = if (DateFormat.is24HourFormat(this)) "HH:mm" else "h:mm a"
        return time.format(DateTimeFormatter.ofPattern(pattern, appLocale))
    }
}
