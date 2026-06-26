package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.ui.components.FramedImage
import com.johnchourp.learnbyzantinemusic.ui.components.LessonCard
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

/** Localized name of a simple ascending character. */
@StringRes
internal fun AscentCharacter.nameRes(): Int = when (this) {
    AscentCharacter.ISON -> R.string.ison
    AscentCharacter.OLIGON -> R.string.oligon
    AscentCharacter.PETASTI -> R.string.flyer
    AscentCharacter.KENTIMATA -> R.string.embroideries
    AscentCharacter.KENTIMA -> R.string.embroidery
    AscentCharacter.YPSILI -> R.string.high
}

/** The neume drawable for a simple ascending character. */
@DrawableRes
internal fun AscentCharacter.diagramRes(): Int = when (this) {
    AscentCharacter.ISON -> R.drawable.ison
    AscentCharacter.OLIGON -> R.drawable.oligon
    AscentCharacter.PETASTI -> R.drawable.flyer
    AscentCharacter.KENTIMATA -> R.drawable.embroideries
    AscentCharacter.KENTIMA -> R.drawable.embroidery
    AscentCharacter.YPSILI -> R.drawable.high
}

/** Accessibility description for a simple character's neume. */
@StringRes
internal fun AscentCharacter.cdRes(): Int = when (this) {
    AscentCharacter.ISON -> R.string.cd_ison
    AscentCharacter.OLIGON -> R.string.cd_oligon
    AscentCharacter.PETASTI -> R.string.cd_flyer
    AscentCharacter.KENTIMATA -> R.string.cd_embroideries
    AscentCharacter.KENTIMA -> R.string.cd_embroidery
    AscentCharacter.YPSILI -> R.string.cd_high
}

/** One-line definition for the characters that have one (Πεταστή, Κεντήματα); else null. */
@StringRes
internal fun AscentCharacter.definitionRes(): Int? = when (this) {
    AscentCharacter.PETASTI -> R.string.flyer_definition
    AscentCharacter.KENTIMATA -> R.string.embroideries_definition
    else -> null
}

/** Localized "+N φωνές" / "0 φωνή" label for a voice count 0..14. */
@StringRes
internal fun voicesLabelRes(voices: Int): Int = when (voices) {
    0 -> R.string.zero_voice
    1 -> R.string.add_one_voice
    2 -> R.string.add_two_voice
    3 -> R.string.add_three_voice
    4 -> R.string.add_four_voice
    5 -> R.string.add_five_voice
    6 -> R.string.add_six_voice
    7 -> R.string.add_seven_voice
    8 -> R.string.add_eight_voice
    9 -> R.string.add_nine_voice
    10 -> R.string.add_ten_voice
    11 -> R.string.add_eleven_voice
    12 -> R.string.add_twelve_voice
    13 -> R.string.add_thirteen_voice
    else -> R.string.add_fourteen_voice
}

/** Whether a form is written on the emphatic Πεταστή base (else the plain Ολίγον base). */
internal fun NeumeForm.baseLabelRes(): Int =
    if (glyphs.any { it.neume == Neume.FLYER }) R.string.ascents_base_petasti
    else R.string.ascents_base_oligon

/**
 * Redesigned «Ανιόντες» page: an animated hero, a concept card, an interactive ladder that
 * shows how far each character lifts the voice, a reference of the simple ascending characters
 * with their neumes and definitions, an explorer of the leaping ascents (+2 … +14) with the
 * original neume spellings rendered faithfully, and a practice tip. Back navigation via [onBack].
 */
@Composable
fun AscentsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LbmPageBg)
            .verticalScroll(scroll),
    ) {
        LessonHero(
            title = stringResource(R.string.ascents),
            subtitle = stringResource(R.string.ascents_subtitle),
            onBack = onBack,
            icon = Icons.AutoMirrored.Filled.TrendingUp,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            StaggeredAppear(delayMillis = 60) { ConceptCard() }
            StaggeredAppear(delayMillis = 140) { VoiceRiseCard() }
            StaggeredAppear(delayMillis = 220) { SimpleCharactersCard() }
            StaggeredAppear(delayMillis = 300) { LeapingAscentsCard() }
            StaggeredAppear(delayMillis = 380) { TipCard() }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** What the ascending quantity characters are. */
@Composable
private fun ConceptCard() {
    LessonCard(title = stringResource(R.string.ascents_concept_title)) {
        Text(
            text = stringResource(R.string.ascents_concept_body),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
    }
}

/* ----------------------------- Interactive ladder ----------------------------- */

private val RUNG_HEIGHT = 44.dp
private val MARKER_SIZE = 26.dp

/** Pick a character and watch the voice climb the ladder by that many φωνές. */
@Composable
private fun VoiceRiseCard() {
    var selected by remember { mutableStateOf(AscentCharacter.OLIGON) }
    var replay by remember { mutableIntStateOf(0) }

    LessonCard(title = stringResource(R.string.ascents_interactive_title)) {
        Text(
            text = stringResource(R.string.ascents_interactive_hint),
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
            AscentCharacter.all.forEach { character ->
                LabelChip(
                    label = stringResource(character.nameRes()),
                    selected = character == selected,
                    onClick = {
                        if (character == selected) replay++ else selected = character
                    },
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        VoiceRiseLadder(target = selected.voices, maxLevel = AscentCharacter.maxVoices, replayKey = replay)
        Spacer(Modifier.height(16.dp))
        val captionRes =
            if (selected.voices == 0) R.string.ascents_interactive_caption_zero
            else R.string.ascents_interactive_caption
        Text(
            text = stringResource(
                captionRes,
                stringResource(selected.nameRes()),
                stringResource(voicesLabelRes(selected.voices)),
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
                text = stringResource(R.string.ascents_replay),
                style = MaterialTheme.typography.labelLarge,
                color = LbmBrown,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * A vertical ladder with rungs 0..[maxLevel]; a marker climbs from 0 up to [target] φωνές each
 * time the selection (or [replayKey]) changes, so the learner sees the voice rise.
 */
@Composable
private fun VoiceRiseLadder(target: Int, maxLevel: Int, replayKey: Int) {
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
        // Left rail: guide line, filled path, ticks and the climbing marker.
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
                    .align(Alignment.BottomCenter)
                    .width(4.dp)
                    .height(RUNG_HEIGHT * reached + RUNG_HEIGHT / 2)
                    .clip(RoundedCornerShape(2.dp))
                    .background(LbmBrownSoft),
            )
            for (level in 0..maxLevel) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = -(RUNG_HEIGHT * level + RUNG_HEIGHT / 2 - 5.dp))
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (level <= reached + 0.5f) LbmBrown else LbmOutline),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = -(RUNG_HEIGHT * reached + RUNG_HEIGHT / 2 - MARKER_SIZE / 2))
                    .size(MARKER_SIZE)
                    .clip(CircleShape)
                    .background(LbmBrown)
                    .border(3.dp, LbmSurface, CircleShape),
            )
        }
        Spacer(Modifier.width(8.dp))
        // Right column: the voice-count label for each rung, lining up with the rail.
        Column(modifier = Modifier.height(trackHeight)) {
            for (level in maxLevel downTo 0) {
                val isReached = level <= reached + 0.5f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(RUNG_HEIGHT),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = stringResource(voicesLabelRes(level)),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isReached) LbmTextPrimary else LbmTextSecondary,
                        fontWeight = if (isReached) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun LabelChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        targetValue = if (selected) LbmBrown else LbmSurface,
        label = "chipBg",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else LbmTextPrimary,
        label = "chipText",
    )
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .border(1.dp, if (selected) LbmBrown else LbmOutline, RoundedCornerShape(12.dp))
            .selectable(selected = selected, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/* ----------------------------- Simple characters ----------------------------- */

/** The six simple ascending characters, each with its neume, +N badge and definition. */
@Composable
private fun SimpleCharactersCard() {
    LessonCard(title = stringResource(R.string.ascents_simple_title)) {
        Text(
            text = stringResource(R.string.ascents_simple_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            AscentCharacter.all.forEach { character -> SimpleCharacterRow(character) }
        }
    }
}

@Composable
private fun SimpleCharacterRow(character: AscentCharacter) {
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
                text = stringResource(voicesLabelRes(character.voices)),
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
        FramedImage(
            res = character.diagramRes(),
            contentDescription = stringResource(character.cdRes()),
        )
    }
}

/** Round badge showing a "+N" voice count (or "0" for Ίσον). */
@Composable
private fun VoiceBadge(voices: Int) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (voices == 0) LbmBrownSoft else LbmBrown),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (voices == 0) "0" else "+$voices",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}

/* ----------------------------- Leaping ascents ----------------------------- */

/** Explore each leap +2 … +14 and see its authored neume spellings + the rising meter. */
@Composable
private fun LeapingAscentsCard() {
    var selected by remember { mutableIntStateOf(0) }
    val leap = LeapingAscents.all[selected]
    LessonCard(title = stringResource(R.string.transcendent_ascents)) {
        Text(
            text = stringResource(R.string.ascents_leaping_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.ascents_leaping_pick),
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
            LeapingAscents.all.forEachIndexed { index, item ->
                LabelChip(
                    label = "+${item.voices}",
                    selected = index == selected,
                    onClick = { selected = index },
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        VoiceLeapMeter(voices = leap.voices)
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.ascents_leaping_forms_title),
            style = MaterialTheme.typography.labelLarge,
            color = LbmTextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(10.dp))
        val cd = stringResource(R.string.cd_leaping_neume, stringResource(voicesLabelRes(leap.voices)))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            leap.forms.forEach { form ->
                LeapingForm(form = form, contentDescription = cd, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LeapingForm(form: NeumeForm, contentDescription: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(LbmSurfaceVariant)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            NeumeStack(form = form, contentDescription = contentDescription)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(form.baseLabelRes()),
            style = MaterialTheme.typography.labelLarge,
            color = LbmTextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

/** A horizontal meter that fills to [voices]/[maxVoices] with a moving marker and "+N" label. */
@Composable
private fun VoiceLeapMeter(voices: Int, maxVoices: Int = 14) {
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
                text = stringResource(voicesLabelRes(voices)),
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
    LessonCard(title = stringResource(R.string.ascents_tip_title)) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = LbmBrownSoft,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.ascents_tip_body),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
            )
        }
    }
}
