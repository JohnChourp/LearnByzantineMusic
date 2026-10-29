package com.johnchourp.learnbyzantinemusic.eartrainer.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import com.johnchourp.learnbyzantinemusic.eartrainer.EarTrainerLevel
import com.johnchourp.learnbyzantinemusic.eartrainer.EarTrainerPhrase
import com.johnchourp.learnbyzantinemusic.eartrainer.EarTrainerScores
import com.johnchourp.learnbyzantinemusic.modes.IsonDrone
import com.johnchourp.learnbyzantinemusic.modes.ModeResources
import com.johnchourp.learnbyzantinemusic.modes.ui.EIGHT_MODES
import com.johnchourp.learnbyzantinemusic.music.Genus
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.ui.components.LessonCard
import com.johnchourp.learnbyzantinemusic.ui.components.LessonHero
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentCrimsonContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentCrimsonContent
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGreenContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGreenContent
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPageBg
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary

/**
 * «Μάντεψε τον ήχο»: the level, the question with its answers, what the answer means, and the score
 * of each ήχος. Pure renderer of the state `EarTrainerActivity` holds.
 *
 * The φθόγγοι are written in Greek in every language, as on every screen that plays a note
 * (`PhthongName`); only the words around them are translated.
 */
@Composable
fun EarTrainerScreen(
    level: EarTrainerLevel,
    asked: Mode,
    chosen: Mode?,
    heard: Boolean,
    sounding: Mode?,
    scores: EarTrainerScores,
    onBack: () -> Unit,
    onLevel: (EarTrainerLevel) -> Unit,
    onPlay: (Mode) -> Unit,
    onStop: () -> Unit,
    onAnswer: (Mode) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LbmPageBg)
            .verticalScroll(rememberScrollState()),
    ) {
        LessonHero(
            title = stringResource(R.string.ear_trainer_title),
            subtitle = stringResource(R.string.ear_trainer_subtitle),
            onBack = onBack,
            icon = Icons.Filled.Hearing,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LevelCard(level = level, onLevel = onLevel)
            QuestionCard(
                level = level,
                asked = asked,
                chosen = chosen,
                heard = heard,
                playing = sounding == asked,
                onPlay = { onPlay(asked) },
                onStop = onStop,
                onAnswer = onAnswer,
            )
            if (chosen != null) {
                AnswerCard(asked = asked, chosen = chosen, sounding = sounding, onPlay = onPlay, onStop = onStop, onNext = onNext)
            }
            ScoresCard(level = level, scores = scores)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LevelCard(level: EarTrainerLevel, onLevel: (EarTrainerLevel) -> Unit) {
    LessonCard(title = stringResource(R.string.ear_trainer_level_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            EarTrainerLevel.entries.forEach { option ->
                FilterChip(
                    selected = option == level,
                    onClick = { onLevel(option) },
                    label = { Text(stringResource(levelLabelRes(option))) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                )
            }
        }
    }
}

@Composable
private fun QuestionCard(
    level: EarTrainerLevel,
    asked: Mode,
    chosen: Mode?,
    heard: Boolean,
    playing: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onAnswer: (Mode) -> Unit,
) {
    LessonCard(title = stringResource(R.string.ear_trainer_question_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(
                    if (level.withIson) R.string.ear_trainer_question_hint else R.string.ear_trainer_question_hint_without_ison,
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = LbmTextPrimary,
            )
            Button(
                onClick = if (playing) onStop else onPlay,
                colors = ButtonDefaults.buttonColors(containerColor = LbmBrown),
            ) {
                Icon(if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (playing) R.string.ear_trainer_stop else R.string.ear_trainer_play))
            }
            if (!heard) {
                Text(
                    text = stringResource(R.string.ear_trainer_listen_first),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LbmTextSecondary,
                )
            }
            level.modes.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { mode ->
                        AnswerButton(
                            mode = mode,
                            isAsked = mode == asked,
                            isChosen = mode == chosen,
                            answered = chosen != null,
                            enabled = heard && chosen == null,
                            onClick = { onAnswer(mode) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * One ήχος to answer with. Once answered, the right one turns green and a wrong choice red; the
 * colours go through the button's own disabled colours, so the disabled look cannot override them.
 */
@Composable
private fun AnswerButton(
    mode: Mode,
    isAsked: Boolean,
    isChosen: Boolean,
    answered: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = when {
        answered && isAsked -> ButtonDefaults.outlinedButtonColors(
            disabledContainerColor = AccentGreenContainer,
            disabledContentColor = AccentGreenContent,
        )
        answered && isChosen -> ButtonDefaults.outlinedButtonColors(
            disabledContainerColor = AccentCrimsonContainer,
            disabledContentColor = AccentCrimsonContent,
        )
        else -> ButtonDefaults.outlinedButtonColors()
    }
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        colors = colors,
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Text(stringResource(shortNameRes(mode)))
    }
}

@Composable
private fun AnswerCard(
    asked: Mode,
    chosen: Mode,
    sounding: Mode?,
    onPlay: (Mode) -> Unit,
    onStop: () -> Unit,
    onNext: () -> Unit,
) {
    val right = chosen == asked
    val askedName = stringResource(ModeResources.nameRes(asked))
    LessonCard(
        title = if (right) {
            stringResource(R.string.ear_trainer_right, askedName)
        } else {
            stringResource(R.string.ear_trainer_wrong, askedName, stringResource(ModeResources.nameRes(chosen)))
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeFacts(mode = asked, playing = sounding == asked, onPlay = { onPlay(asked) }, onStop = onStop)
            if (!right) {
                ModeFacts(mode = chosen, playing = sounding == chosen, onPlay = { onPlay(chosen) }, onStop = onStop)
            }
            Button(onClick = onNext, colors = ButtonDefaults.buttonColors(containerColor = LbmBrown)) {
                Text(stringResource(R.string.ear_trainer_next))
            }
        }
    }
}

/** Why a ήχος sounds as it does: its γένος and base, the window around the base, its δεσπόζοντες. */
@Composable
private fun ModeFacts(mode: Mode, playing: Boolean, onPlay: () -> Unit, onStop: () -> Unit) {
    val name = stringResource(ModeResources.nameRes(mode))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = LbmTextPrimary)
        Text(
            text = stringResource(R.string.ear_trainer_genus_and_base, stringResource(genusRes(mode)), IsonDrone.base(mode).label),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextPrimary,
        )
        Text(
            text = stringResource(R.string.ear_trainer_moria_from_base, signedMoria(EarTrainerPhrase.moriaFromBase(mode))),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextPrimary,
        )
        Text(
            text = stringResource(R.string.ear_trainer_dominants, IsonDrone.dominants(mode).joinToString(", ") { it.label }),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
        TextButton(onClick = if (playing) onStop else onPlay) {
            Text(if (playing) stringResource(R.string.ear_trainer_stop) else stringResource(R.string.ear_trainer_play_mode, name))
        }
    }
}

@Composable
private fun ScoresCard(level: EarTrainerLevel, scores: EarTrainerScores) {
    LessonCard(title = stringResource(R.string.ear_trainer_scores_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            level.modes.forEach { mode ->
                val score = scores.of(mode)
                Text(
                    text = if (score.answered == 0) {
                        stringResource(R.string.ear_trainer_score_none, stringResource(ModeResources.nameRes(mode)))
                    } else {
                        stringResource(R.string.ear_trainer_score, stringResource(ModeResources.nameRes(mode)), score.correct, score.answered)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = LbmTextPrimary,
                )
            }
        }
    }
}

/** μόρια with a true minus sign: «−20 −12 0 10 18 30». */
private fun signedMoria(values: List<Int>): String =
    values.joinToString(" ") { if (it < 0) "−${-it}" else "$it" }

@StringRes
private fun levelLabelRes(level: EarTrainerLevel): Int = when (level) {
    EarTrainerLevel.KYRIOI -> R.string.ear_trainer_level_kyrioi
    EarTrainerLevel.ALL -> R.string.ear_trainer_level_all
    EarTrainerLevel.ALL_WITHOUT_ISON -> R.string.ear_trainer_level_all_without_ison
}

/** The 8 Ήχοι selector's short name of [mode], which fits a button two to a row. */
@StringRes
private fun shortNameRes(mode: Mode): Int = EIGHT_MODES.first { it.mode == mode }.selectorNameRes

/**
 * The bare name of [mode]'s γένος, as the 8 Ήχοι legend writes it. Not the selector's label, which
 * adds the intervals from Πα and would read as a second, different set of numbers next to the ones
 * from the base.
 */
@StringRes
private fun genusRes(mode: Mode): Int = when (mode.scale.genus) {
    Genus.DIATONIC -> R.string.eight_modes_genus_diatonic
    Genus.SOFT_CHROMATIC -> R.string.eight_modes_genus_soft_chromatic
    Genus.HARD_CHROMATIC -> R.string.eight_modes_genus_hard_chromatic
    Genus.ENHARMONIC -> R.string.eight_modes_genus_enharmonic
}
