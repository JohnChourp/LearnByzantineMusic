package com.johnchourp.learnbyzantinemusic.practice.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.ui.components.LessonCard
import com.johnchourp.learnbyzantinemusic.ui.components.LessonHero
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmOutline
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPageBg
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary

/** One bar of the weekly chart, with its label and what TalkBack says for it pre-formatted. */
@Immutable
data class PracticeBarUi(val label: String, val contentDescription: String, val minutes: Int)

/** Everything «Ιστορικό εξάσκησης» shows; the host Activity formats the locale-dependent parts. */
@Immutable
data class PracticeHistoryUi(
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val practisedToday: Boolean = false,
    val bars: List<PracticeBarUi> = emptyList(),
    val weekDays: Int = 0,
    val weekMinutes: Int = 0,
    val reminderEnabled: Boolean = false,
    val reminderTimeLabel: String = "",
    val notificationsBlocked: Boolean = false,
)

/** «Ιστορικό εξάσκησης»: streak, the last seven days, and the reminder. A pure renderer of [ui]. */
@Composable
fun PracticeHistoryScreen(
    ui: PracticeHistoryUi,
    onBack: () -> Unit,
    onReminderChange: (Boolean) -> Unit,
    onPickTime: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LbmPageBg)
            .verticalScroll(rememberScrollState()),
    ) {
        LessonHero(
            title = stringResource(R.string.practice_open_history),
            subtitle = stringResource(R.string.practice_history_subtitle),
            onBack = onBack,
            icon = Icons.Filled.Insights,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LessonCard(title = stringResource(R.string.practice_history_streak_title)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.practice_streak_days, ui.streak),
                        style = MaterialTheme.typography.titleLarge,
                        color = LbmTextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(
                            if (ui.practisedToday) R.string.practice_history_today_done else R.string.practice_history_today_pending,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = LbmTextPrimary,
                    )
                    Text(
                        text = stringResource(R.string.practice_history_best, ui.bestStreak),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LbmTextSecondary,
                    )
                }
            }
            LessonCard(title = stringResource(R.string.practice_history_week_title)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    WeekBars(ui.bars)
                    Text(
                        text = stringResource(R.string.practice_history_week_total, ui.weekDays, ui.weekMinutes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LbmTextSecondary,
                    )
                }
            }
            LessonCard(title = stringResource(R.string.practice_reminder_title)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // The row is the switch, so TalkBack reads its label with it — never an unnamed switch.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.toggleable(
                            value = ui.reminderEnabled,
                            onValueChange = onReminderChange,
                            role = Role.Switch,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.practice_reminder_switch),
                            style = MaterialTheme.typography.bodyLarge,
                            color = LbmTextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = ui.reminderEnabled,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(checkedTrackColor = LbmBrown),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.practice_reminder_time, ui.reminderTimeLabel),
                            style = MaterialTheme.typography.bodyLarge,
                            color = LbmTextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onPickTime) {
                            Text(stringResource(R.string.practice_reminder_change_time))
                        }
                    }
                    Text(
                        text = stringResource(R.string.practice_reminder_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LbmTextSecondary,
                    )
                    if (ui.reminderEnabled && ui.notificationsBlocked) {
                        Text(
                            text = stringResource(R.string.practice_reminder_blocked),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LbmTextPrimary,
                        )
                        TextButton(onClick = onOpenNotificationSettings) {
                            Text(stringResource(R.string.practice_reminder_open_settings))
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Seven bars of minutes, oldest first. Empty days keep a thin mark so the week still reads as seven. */
@Composable
private fun WeekBars(bars: List<PracticeBarUi>) {
    val tallest = maxOf(bars.maxOfOrNull { it.minutes } ?: 0, 5)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEach { bar ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics { contentDescription = bar.contentDescription },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((BAR_MAX_HEIGHT_DP * bar.minutes / tallest).coerceAtLeast(3).dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (bar.minutes > 0) LbmBrown else LbmOutline),
                )
                Spacer(Modifier.height(4.dp))
                Text(text = bar.label, style = MaterialTheme.typography.labelMedium, color = LbmTextSecondary)
            }
        }
    }
}

private const val BAR_MAX_HEIGHT_DP = 96
