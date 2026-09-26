package com.johnchourp.learnbyzantinemusic.voice.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurface
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary
import com.johnchourp.learnbyzantinemusic.voice.VoiceRangeTest
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

/**
 * «Βρες τη φωνή σου» (ClickUp `869f5x2dd`, J4): about thirty seconds — the lowest comfortable note,
 * the highest, and a suggested global shift that is applied only when the singer accepts it.
 *
 * Offered once by the 8 Ήχοι page the first time it opens, and always from the «Φωνή» card in
 * Settings. The microphone stays the host's: this asks for it through [onListen] only from «Ξεκίνα»
 * on, reads back [heardFrequencyHz], and releases it whenever it closes. The steps and the arithmetic
 * are [VoiceRangeTest] and its advisor, tested on the JVM.
 */
@Composable
fun VoiceRangeDialog(
    /** The pitch the host last heard, in Hz, or null when nothing usable is coming in. */
    heardFrequencyHz: Double?,
    /** True once the host has been refused the microphone. */
    micDenied: Boolean,
    /** The global shift in force now, so an unchanged suggestion can say so. */
    currentGlobalShiftMoria: Int,
    onListen: (Boolean) -> Unit,
    onApply: (Int) -> Unit,
    onClose: () -> Unit,
    /** A background ison sounds, and the host will stop it on «Ξεκίνα»: the test must hear only the singer. */
    isonWillStop: Boolean = false,
) {
    var test by remember { mutableStateOf(VoiceRangeTest()) }
    val latestHz by rememberUpdatedState(heardFrequencyHz)

    LaunchedEffect(test.listening) { onListen(test.listening) }
    DisposableEffect(Unit) { onDispose { onListen(false) } }
    // Takes whatever the host last heard at a steady rate while a step listens: a held note comes in
    // as the same few values over and over, and a change-driven collector would miss most of them.
    LaunchedEffect(test.step) {
        while (test.listening) {
            test = test.hear(latestHz)
            delay(SAMPLE_EVERY_MS)
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.voice_test_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                when (test.step) {
                    VoiceRangeTest.Step.INTRO -> {
                        Body(stringResource(R.string.voice_test_intro))
                        if (isonWillStop) {
                            Spacer(Modifier.height(10.dp))
                            Hint(stringResource(R.string.voice_test_ison_will_stop))
                        }
                    }
                    VoiceRangeTest.Step.LOWEST, VoiceRangeTest.Step.HIGHEST -> {
                        Body(
                            stringResource(
                                if (test.step == VoiceRangeTest.Step.LOWEST) R.string.voice_test_lowest
                                else R.string.voice_test_highest
                            )
                        )
                        Spacer(Modifier.height(10.dp))
                        Hint(
                            when {
                                micDenied -> stringResource(R.string.voice_test_mic_denied)
                                latestHz != null ->
                                    stringResource(R.string.voice_test_hearing, latestHz!!.roundToInt())
                                else -> stringResource(R.string.voice_test_waiting)
                            }
                        )
                    }
                    VoiceRangeTest.Step.RESULT -> {
                        val advice = test.advice!!
                        Body(
                            stringResource(
                                R.string.voice_test_result_range,
                                advice.lowestHz.roundToInt(),
                                advice.highestHz.roundToInt(),
                                String.format(Locale.getDefault(), "%.1f", advice.octaves),
                            )
                        )
                        Spacer(Modifier.height(10.dp))
                        Body(
                            if (advice.globalShiftMoria == currentGlobalShiftMoria) {
                                stringResource(R.string.voice_test_result_same, advice.globalShiftMoria)
                            } else {
                                stringResource(R.string.voice_test_result_shift, advice.globalShiftMoria)
                            }
                        )
                    }
                    VoiceRangeTest.Step.RETRY -> Body(stringResource(R.string.voice_test_retry_text))
                }
            }
        },
        confirmButton = {
            when (test.step) {
                VoiceRangeTest.Step.INTRO, VoiceRangeTest.Step.RETRY ->
                    Action(
                        if (test.step == VoiceRangeTest.Step.INTRO) R.string.voice_test_start else R.string.voice_test_retry,
                        onClick = { test = test.start() },
                    )
                VoiceRangeTest.Step.LOWEST, VoiceRangeTest.Step.HIGHEST ->
                    Action(R.string.voice_test_done_step, enabled = test.canFinishStep, onClick = { test = test.finishStep() })
                VoiceRangeTest.Step.RESULT -> {
                    val shift = test.advice!!.globalShiftMoria
                    if (shift == currentGlobalShiftMoria) {
                        Action(R.string.voice_test_close, onClick = onClose)
                    } else {
                        Action(R.string.voice_test_apply, onClick = {
                            onApply(shift)
                            onClose()
                        })
                    }
                }
            }
        },
        dismissButton = {
            val label = when (test.step) {
                VoiceRangeTest.Step.INTRO -> R.string.voice_test_not_now
                VoiceRangeTest.Step.RESULT -> R.string.voice_test_decline
                else -> R.string.voice_test_cancel
            }
            if (!(test.step == VoiceRangeTest.Step.RESULT && test.advice?.globalShiftMoria == currentGlobalShiftMoria)) {
                Action(label, primary = false, onClick = onClose)
            }
        },
        containerColor = LbmSurface,
        titleContentColor = LbmTextPrimary,
        textContentColor = LbmTextSecondary,
        shape = RoundedCornerShape(20.dp),
    )
}

/** How often the dialog takes the latest pitch the host heard. */
private const val SAMPLE_EVERY_MS = 50L

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = LbmTextPrimary)
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = LbmTextSecondary)
}

/** Its colour goes through the button, so a disabled «Έτοιμο» looks disabled (the lesson of ClickUp `869f5x281`). */
@Composable
private fun Action(label: Int, enabled: Boolean = true, primary: Boolean = true, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = if (primary) LbmBrown else LbmTextSecondary),
    ) {
        Text(stringResource(label), fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Normal)
    }
}
