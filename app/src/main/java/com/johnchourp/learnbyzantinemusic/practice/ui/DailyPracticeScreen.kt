package com.johnchourp.learnbyzantinemusic.practice.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.modes.ModeResources
import com.johnchourp.learnbyzantinemusic.practice.PracticeStep
import com.johnchourp.learnbyzantinemusic.practice.PracticeSummary
import com.johnchourp.learnbyzantinemusic.ui.components.LessonCard
import com.johnchourp.learnbyzantinemusic.ui.components.LessonHero
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmOutline
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPageBg
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary

/**
 * «Πεντάλεπτο της ημέρας»: the current step — what to do, and a button that opens its screen — with
 * «Επόμενο» and «Τέλος» under it, or, once the session is completed, the result. Pure renderer of the
 * state `DailyPracticeActivity` holds; the rules are in `DailySession`.
 */
@Composable
fun DailyPracticeScreen(
    steps: List<PracticeStep>,
    stepIndex: Int,
    result: PracticeSummary?,
    @StringRes lessonTitleRes: Int?,
    onBack: () -> Unit,
    onOpenStep: () -> Unit,
    onNext: () -> Unit,
    onEnd: () -> Unit,
    onOpenHistory: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LbmPageBg)
            .verticalScroll(rememberScrollState()),
    ) {
        LessonHero(
            title = stringResource(R.string.practice_daily_title),
            subtitle = stringResource(R.string.practice_daily_subtitle),
            onBack = onBack,
            icon = Icons.Filled.SelfImprovement,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (result != null) {
                ResultCard(result = result, onOpenHistory = onOpenHistory, onClose = onClose)
            } else if (steps.isNotEmpty()) {
                val step = steps[stepIndex]
                val isLast = stepIndex == steps.lastIndex
                Text(
                    text = stringResource(R.string.practice_step_counter, stepIndex + 1, steps.size, step.minutes),
                    style = MaterialTheme.typography.labelLarge,
                    color = LbmTextSecondary,
                )
                LinearProgressIndicator(
                    progress = { (stepIndex + 1).toFloat() / steps.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = LbmBrown,
                    trackColor = LbmOutline,
                )
                StepCard(step = step, lessonTitleRes = lessonTitleRes, onOpenStep = onOpenStep)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onEnd, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.practice_end))
                    }
                    Button(
                        onClick = onNext,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = LbmBrown),
                    ) {
                        Text(stringResource(R.string.practice_next))
                    }
                }
                Text(
                    text = stringResource(
                        if (isLast) R.string.practice_last_step_hint else R.string.practice_counts_at_end_hint,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LbmTextSecondary,
                )
                TextButton(onClick = onOpenHistory) {
                    Text(stringResource(R.string.practice_open_history))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StepCard(step: PracticeStep, @StringRes lessonTitleRes: Int?, onOpenStep: () -> Unit) {
    val title: Int
    val body: String
    val action: Int
    var noToneNote = false
    when (step) {
        is PracticeStep.Apichima -> {
            title = R.string.practice_step_apichima_title
            body = stringResource(R.string.practice_step_apichima_body, stringResource(ModeResources.nameRes(step.mode)))
            action = R.string.home_weekly_tone_open_eight_modes
            noToneNote = step.noToneThisWeek
        }
        is PracticeStep.Voice -> {
            title = R.string.practice_step_voice_title
            body = stringResource(R.string.practice_step_voice_body, stringResource(ModeResources.nameRes(step.mode)))
            action = R.string.practice_step_voice_action
            noToneNote = step.noToneThisWeek
        }
        PracticeStep.Rhythm -> {
            title = R.string.practice_step_rhythm_title
            body = stringResource(R.string.practice_step_rhythm_body)
            action = R.string.practice_step_rhythm_action
        }
        is PracticeStep.NextLesson -> {
            title = R.string.practice_step_lesson_title
            body = stringResource(
                R.string.practice_step_lesson_body,
                lessonTitleRes?.let { stringResource(it) }.orEmpty(),
            )
            action = R.string.practice_step_lesson_action
        }
    }
    LessonCard(title = stringResource(title)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = body, style = MaterialTheme.typography.bodyLarge, color = LbmTextPrimary)
            if (noToneNote) {
                Text(
                    text = stringResource(R.string.practice_no_tone_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LbmTextSecondary,
                )
            }
            Button(onClick = onOpenStep, colors = ButtonDefaults.buttonColors(containerColor = LbmBrown)) {
                Text(stringResource(action))
            }
        }
    }
}

@Composable
private fun ResultCard(result: PracticeSummary, onOpenHistory: () -> Unit, onClose: () -> Unit) {
    LessonCard(title = stringResource(R.string.practice_done_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.practice_streak_days, result.streak),
                style = MaterialTheme.typography.titleLarge,
                color = LbmTextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.practice_close))
                }
                Button(
                    onClick = onOpenHistory,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = LbmBrown),
                ) {
                    Text(stringResource(R.string.practice_open_history))
                }
            }
        }
    }
}
