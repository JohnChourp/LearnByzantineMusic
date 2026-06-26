package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.ui.components.LessonCard
import com.johnchourp.learnbyzantinemusic.ui.components.LessonChip
import com.johnchourp.learnbyzantinemusic.ui.components.LessonHero
import com.johnchourp.learnbyzantinemusic.ui.components.StaggeredAppear
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrownSoft
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmOutline
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPageBg
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPrimaryContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurface
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurfaceVariant
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary

/** Localized name of a simple descending character. */
@StringRes
internal fun DescentCharacter.nameRes(): Int = when (this) {
    DescentCharacter.APOSTROPHOS -> R.string.apostrophe
    DescentCharacter.ELAFRON -> R.string.slight
    DescentCharacter.YPORROI -> R.string.underflow
    DescentCharacter.CHAMILI -> R.string.low
}

/** The neume drawable for a simple descending character. */
@DrawableRes
internal fun DescentCharacter.diagramRes(): Int = when (this) {
    DescentCharacter.APOSTROPHOS -> R.drawable.apostrophe
    DescentCharacter.ELAFRON -> R.drawable.slight
    DescentCharacter.YPORROI -> R.drawable.underflow
    DescentCharacter.CHAMILI -> R.drawable.low
}

/** Accessibility description for a simple character's neume. */
@StringRes
internal fun DescentCharacter.cdRes(): Int = when (this) {
    DescentCharacter.APOSTROPHOS -> R.string.cd_apostrophe
    DescentCharacter.ELAFRON -> R.string.cd_slight
    DescentCharacter.YPORROI -> R.string.cd_underflow
    DescentCharacter.CHAMILI -> R.string.cd_low
}

/** One-line definition for the characters that have one (Υπορροή); else null. */
@StringRes
internal fun DescentCharacter.definitionRes(): Int? = when (this) {
    DescentCharacter.YPORROI -> R.string.yporroi_definition
    else -> null
}

/** Intrinsic neume size (width × height, dp) from the original layout, kept to preserve scale. */
internal fun DescentCharacter.glyphSize(): Pair<Int, Int> = when (this) {
    DescentCharacter.APOSTROPHOS -> 30 to 18
    DescentCharacter.ELAFRON -> 52 to 18
    // The Υπορροή neume (underflow) is a small, compact mark.
    DescentCharacter.YPORROI -> 19 to 18
    DescentCharacter.CHAMILI -> 58 to 36
}

/** Localized "-N φωνές" label for a descent magnitude 1..12. */
@StringRes
internal fun minusVoicesLabelRes(voices: Int): Int = when (voices) {
    1 -> R.string.minus_one_voice
    2 -> R.string.minus_two_voice
    3 -> R.string.minus_three_voice
    4 -> R.string.minus_four_voice
    5 -> R.string.minus_five_voice
    6 -> R.string.minus_six_voice
    7 -> R.string.minus_seven_voice
    8 -> R.string.minus_eight_voice
    9 -> R.string.minus_nine_voice
    10 -> R.string.minus_ten_voice
    11 -> R.string.minus_eleven_voice
    else -> R.string.minus_twelve_voice
}

/**
 * Redesigned «Κατιόντες» page: an animated hero, a concept card, an interactive ladder that
 * shows how far each character lowers the voice, a reference of the simple descending characters
 * with their neumes and definitions, an explorer of the leaping descents (-2 … -12) with the
 * original neume spellings rendered faithfully, and a practice tip. The whole page mirrors the
 * «Ανιόντες» screen but inverts the metaphor — here the voice *drops*. Back navigation via [onBack].
 */
@Composable
fun DescentsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LbmPageBg)
            .verticalScroll(scroll),
    ) {
        LessonHero(
            title = stringResource(R.string.descents),
            subtitle = stringResource(R.string.descents_subtitle),
            onBack = onBack,
            icon = Icons.AutoMirrored.Filled.TrendingDown,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            StaggeredAppear(delayMillis = 60) { ConceptCard() }
            StaggeredAppear(delayMillis = 140) { VoiceDropCard() }
            StaggeredAppear(delayMillis = 220) { SimpleCharactersCard() }
            StaggeredAppear(delayMillis = 300) { LeapingDescentsCard() }
            StaggeredAppear(delayMillis = 380) { TipCard() }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** What the descending quantity characters are. */
@Composable
private fun ConceptCard() {
    LessonCard(title = stringResource(R.string.descents_concept_title)) {
        Text(
            text = stringResource(R.string.descents_concept_body),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
    }
}

/* ----------------------------- Interactive ladder ----------------------------- */

private val RUNG_HEIGHT = 44.dp
private val MARKER_SIZE = 26.dp

/** Pick a character and watch the voice drop the ladder by that many φωνές. */
@Composable
private fun VoiceDropCard() {
    var selected by remember { mutableStateOf(DescentCharacter.APOSTROPHOS) }
    var replay by remember { mutableIntStateOf(0) }

    LessonCard(title = stringResource(R.string.descents_interactive_title)) {
        Text(
            text = stringResource(R.string.descents_interactive_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DescentCharacter.all.forEach { character ->
                LessonChip(
                    label = stringResource(character.nameRes()),
                    selected = character == selected,
                    onClick = {
                        // Always restart the drop, even when switching between equal-depth
                        // characters (e.g. Ελαφρόν → Υπορροή are both -2 φωνές).
                        selected = character
                        replay++
                    },
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        VoiceDropLadder(target = selected.voices, maxLevel = DescentCharacter.maxVoices, replayKey = replay)
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(
                R.string.descents_interactive_caption,
                stringResource(selected.nameRes()),
                stringResource(minusVoicesLabelRes(selected.voices)),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button) { replay++ }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Replay,
                contentDescription = null,
                tint = LbmBrown,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.descents_replay),
                style = MaterialTheme.typography.labelLarge,
                color = LbmBrown,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * A vertical ladder with rungs 0..[maxLevel] running top-to-bottom; a marker drops from the start
 * (top) down to [target] φωνές each time the selection (or [replayKey]) changes, so the learner
 * sees the voice fall.
 */
@Composable
private fun VoiceDropLadder(target: Int, maxLevel: Int, replayKey: Int) {
    val pos = remember { Animatable(target.toFloat()) }
    LaunchedEffect(target, replayKey) {
        pos.snapTo(0f)
        pos.animateTo(
            targetValue = target.toFloat(),
            animationSpec = tween(durationMillis = 220 * (target + 1), easing = FastOutSlowInEasing),
        )
    }
    val trackHeight = RUNG_HEIGHT * (maxLevel + 1)
    val reached = pos.value

    Row(modifier = Modifier.fillMaxWidth()) {
        // Left rail: guide line, filled path from the top, ticks and the dropping marker.
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(trackHeight),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(LbmOutline),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(4.dp)
                    .height(RUNG_HEIGHT * reached + RUNG_HEIGHT / 2)
                    .clip(RoundedCornerShape(2.dp))
                    .background(LbmBrownSoft),
            )
            for (level in 0..maxLevel) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = RUNG_HEIGHT * level + RUNG_HEIGHT / 2 - 5.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (level <= reached + 0.5f) LbmBrown else LbmOutline),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = RUNG_HEIGHT * reached + RUNG_HEIGHT / 2 - MARKER_SIZE / 2)
                    .size(MARKER_SIZE)
                    .clip(CircleShape)
                    .background(LbmBrown)
                    .border(3.dp, LbmSurface, CircleShape),
            )
        }
        Spacer(Modifier.width(8.dp))
        // Right column: the depth label for each rung, lining up with the rail (start at the top).
        Column(modifier = Modifier.height(trackHeight)) {
            for (level in 0..maxLevel) {
                val isReached = level <= reached + 0.5f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(RUNG_HEIGHT),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = if (level == 0) stringResource(R.string.descents_start)
                        else stringResource(minusVoicesLabelRes(level)),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isReached) LbmTextPrimary else LbmTextSecondary,
                        fontWeight = if (isReached) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

/* ----------------------------- Simple characters ----------------------------- */

/** The four simple descending characters, each with its neume, -N badge and definition. */
@Composable
private fun SimpleCharactersCard() {
    LessonCard(title = stringResource(R.string.descents_simple_title)) {
        Text(
            text = stringResource(R.string.descents_simple_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            DescentCharacter.all.forEach { character -> SimpleCharacterRow(character) }
        }
    }
}

@Composable
private fun SimpleCharacterRow(character: DescentCharacter) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VoiceBadge(voices = character.voices)
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(character.nameRes()),
                style = MaterialTheme.typography.titleMedium,
                color = LbmTextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(minusVoicesLabelRes(character.voices)),
                style = MaterialTheme.typography.titleMedium,
                color = LbmBrown,
                fontWeight = FontWeight.SemiBold,
            )
        }
        character.definitionRes()?.let { defRes ->
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(defRes),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
            )
        }
        Spacer(Modifier.height(10.dp))
        SimpleNeumeFrame(character)
    }
}

private const val SIMPLE_NEUME_SCALE = 2.0f
private val SIMPLE_NEUME_FRAME_HEIGHT = 88.dp

/** A single neume on a white frame, drawn at its true (scaled) size so rows stay compact. */
@Composable
private fun SimpleNeumeFrame(character: DescentCharacter) {
    val (w, h) = character.glyphSize()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, LbmOutline),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(SIMPLE_NEUME_FRAME_HEIGHT),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(character.diagramRes()),
                contentDescription = stringResource(character.cdRes()),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(
                    width = (w * SIMPLE_NEUME_SCALE).dp,
                    height = (h * SIMPLE_NEUME_SCALE).dp,
                ),
            )
        }
    }
}

/** Round badge showing a "-N" descent depth. */
@Composable
private fun VoiceBadge(voices: Int) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(LbmBrown),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "-$voices",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}

/* ----------------------------- Leaping descents ----------------------------- */

/** Explore each descent -2 … -12 and see its authored neume spelling + the falling meter. */
@Composable
private fun LeapingDescentsCard() {
    var selected by remember { mutableIntStateOf(0) }
    val leap = LeapingDescents.all[selected]
    LessonCard(title = stringResource(R.string.transcendent_descents)) {
        Text(
            text = stringResource(R.string.descents_leaping_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.descents_leaping_pick),
            style = MaterialTheme.typography.labelLarge,
            color = LbmTextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LeapingDescents.all.forEachIndexed { index, item ->
                LessonChip(
                    label = "-${item.voices}",
                    selected = index == selected,
                    onClick = { selected = index },
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        VoiceDropMeter(voices = leap.voices)
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.descents_leaping_forms_title),
            style = MaterialTheme.typography.labelLarge,
            color = LbmTextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(10.dp))
        val cd = stringResource(R.string.cd_leaping_descent, stringResource(minusVoicesLabelRes(leap.voices)))
        LeapingDescentForm(form = leap.form, contentDescription = cd)
    }
}

/** The single authored neume spelling of a leaping descent, framed on white. */
@Composable
private fun LeapingDescentForm(form: DescentForm, contentDescription: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LbmSurfaceVariant)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            DescentNeumeStack(form = form, contentDescription = contentDescription)
        }
    }
}

/** A horizontal meter that fills to [voices]/[maxVoices] with a moving fill and "-N" label. */
@Composable
private fun VoiceDropMeter(voices: Int, maxVoices: Int = 12) {
    val fraction = remember { Animatable(0f) }
    LaunchedEffect(voices) {
        fraction.snapTo(0f)
        fraction.animateTo(
            targetValue = voices.toFloat() / maxVoices,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        )
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(LbmOutline),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.value)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(LbmBrown),
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(LbmPrimaryContainer)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = stringResource(minusVoicesLabelRes(voices)),
                style = MaterialTheme.typography.titleMedium,
                color = LbmBrown,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/* ----------------------------- Tip ----------------------------- */

@Composable
private fun TipCard() {
    LessonCard(title = stringResource(R.string.descents_tip_title)) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = LbmBrownSoft,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.descents_tip_body),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
            )
        }
    }
}
