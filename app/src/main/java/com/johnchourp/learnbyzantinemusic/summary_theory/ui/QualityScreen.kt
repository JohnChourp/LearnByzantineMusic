package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.ui.components.FramedImage
import com.johnchourp.learnbyzantinemusic.ui.components.LessonCard
import com.johnchourp.learnbyzantinemusic.ui.components.LessonHero
import com.johnchourp.learnbyzantinemusic.ui.components.StaggeredAppear
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGreenContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGreenContent
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrownSoft
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmMeasureBar
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmOutline
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPageBg
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPrimaryContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurfaceVariant
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary

/**
 * Redesigned «Ποιότητος» (Quality) page: an animated hero, an intro card that frames «σημάδια
 * ποιότητος» with an at-a-glance taxonomy, a legend of the base neumes, then one card per quality
 * sign — each with its glyph highlighted, a «what it changes» tag, the original definitions, and
 * worked examples that animate the written character into the simpler neumes it is read as (with the
 * quality sign tinted crimson throughout). Do/don't cases and a nasal hint for the Ενδόφωνο make the
 * harder signs concrete. Back navigation via [onBack].
 */
@Composable
fun QualityScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LbmPageBg)
            .verticalScroll(scroll),
    ) {
        LessonHero(
            title = stringResource(R.string.quality),
            subtitle = stringResource(R.string.quality_subtitle),
            onBack = onBack,
            icon = Icons.Filled.AutoAwesome,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            StaggeredAppear(delayMillis = 60) { IntroCard() }
            StaggeredAppear(delayMillis = 120) { LegendCard() }
            QualitySigns.all.forEachIndexed { i, sign ->
                StaggeredAppear(delayMillis = 180 + i * 60) { SignCard(sign) }
            }
            StaggeredAppear(delayMillis = 660) { TipCard() }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/* ----------------------------- Intro + taxonomy ----------------------------- */

@Composable
private fun IntroCard() {
    LessonCard(title = stringResource(R.string.quality_intro_title)) {
        Text(
            text = stringResource(R.string.quality_intro_body),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.quality_taxonomy_hint),
            style = MaterialTheme.typography.labelLarge,
            color = LbmTextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            QualitySigns.all.forEachIndexed { i, sign -> TaxonomyRow(i + 1, sign) }
        }
    }
}

@Composable
private fun TaxonomyRow(number: Int, sign: QualitySign) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(LbmPrimaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = LbmBrown,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(sign.titleRes),
            style = MaterialTheme.typography.titleSmall,
            color = LbmTextPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(116.dp),
        )
        Text(
            text = stringResource(sign.tagRes),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
            modifier = Modifier.weight(1f),
        )
    }
}

/* ----------------------------- Legend ----------------------------- */

@Composable
private fun LegendCard() {
    LessonCard(title = stringResource(R.string.quality_legend_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            QualitySigns.legend.forEach { GlyphLegendRow(it) }
        }
    }
}

@Composable
private fun GlyphLegendRow(item: QualityLegendItem) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(width = 66.dp, height = 46.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, LbmOutline),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(item.neume.drawableRes()),
                    contentDescription = stringResource(item.cdRes),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(item.nameRes),
                style = MaterialTheme.typography.titleMedium,
                color = LbmTextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(item.meaningRes),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
            )
        }
    }
}

/* ----------------------------- Sign card ----------------------------- */

@Composable
private fun SignCard(sign: QualitySign) {
    var replay by remember { mutableIntStateOf(0) }
    LessonCard(title = stringResource(sign.titleRes)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HighlightedGlyph(sign)
            Spacer(Modifier.width(14.dp))
            TagBadge(stringResource(sign.tagRes))
        }
        Spacer(Modifier.height(12.dp))
        sign.definitionRes.forEachIndexed { i, res ->
            if (i > 0) Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(res),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
            )
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            sign.examples.forEach { ExampleRow(sign, it, sectionReplay = replay) }
        }
        if (sign.counterImageRes != 0) {
            Spacer(Modifier.height(12.dp))
            CounterExample(sign)
        }
        if (sign.id == "intercom") {
            Spacer(Modifier.height(12.dp))
            NasalHint()
        }
        Spacer(Modifier.height(6.dp))
        ReplayControl { replay++ }
    }
}

/** The sign's own glyph on a white tile, tinted crimson and pulsing gently to draw the eye. */
@Composable
private fun HighlightedGlyph(sign: QualitySign) {
    val infinite = rememberInfiniteTransition(label = "glyph")
    val scale by infinite.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glyphScale",
    )
    Surface(
        modifier = Modifier.size(width = 96.dp, height = 60.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, LbmOutline),
    ) {
        Box(contentAlignment = Alignment.Center) {
            NeumeStack(
                form = sign.glyph,
                contentDescription = stringResource(sign.glyphCdRes),
                highlight = sign.highlight,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale },
            )
        }
    }
}

@Composable
private fun TagBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(LbmPrimaryContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = LbmBrown,
            fontWeight = FontWeight.Bold,
        )
    }
}

/* ----------------------------- Example row ----------------------------- */

private val TILE_MIN_W = 64
private val TILE_MAX_W = 150

/**
 * One worked example: an optional tone pill, the written character(s), an animated «=» + the simpler
 * neumes (when the example is a decomposition), and the plain-Greek reading below. Tapping the row
 * (or the card's replay) re-runs the reveal. The diagram is decorative for TalkBack — the reading
 * fully describes it.
 */
@Composable
private fun ExampleRow(sign: QualitySign, example: QualityExample, sectionReplay: Int) {
    var localReplay by remember { mutableIntStateOf(0) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val progress = remember { Animatable(0f) }
    val partCount = example.parts.size
    LaunchedEffect(visible, sectionReplay, localReplay) {
        if (visible) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 240 + partCount * 170,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }
    val replayLabel = stringResource(R.string.quality_replay)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClickLabel = replayLabel) { localReplay++ }
            .padding(vertical = 8.dp),
    ) {
        if (example.tone != ExampleTone.NEUTRAL) {
            TonePill(example.tone)
            Spacer(Modifier.height(6.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .clearAndSetSemantics { },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            example.combined.forEachIndexed { i, form ->
                if (i > 0) DotSeparator()
                NeumeTile(form = form, highlight = sign.highlight)
            }
            if (partCount > 0) {
                EqualsConnector(progress = progress.value)
                example.parts.forEachIndexed { i, form ->
                    if (i > 0) DotSeparator()
                    val pp = ((progress.value * partCount) - i).coerceIn(0f, 1f)
                    NeumeTile(
                        form = form,
                        highlight = sign.highlight,
                        modifier = Modifier.graphicsLayer {
                            alpha = pp
                            translationX = (1f - pp) * -22.dp.toPx()
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(example.readingRes),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
    }
}

@Composable
private fun TonePill(tone: ExampleTone) {
    val isDo = tone == ExampleTone.DO
    val container = if (isDo) AccentGreenContainer else Color(0xFFF7E0E8)
    val content = if (isDo) AccentGreenContent else LbmMeasureBar
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isDo) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(if (isDo) R.string.quality_do_label else R.string.quality_dont_label),
            style = MaterialTheme.typography.labelMedium,
            color = content,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** A single neume form on a white tile, sized to its widest glyph; vertically centred. */
@Composable
private fun NeumeTile(form: NeumeForm, highlight: Set<Neume>, modifier: Modifier = Modifier) {
    val width = (form.glyphs.maxOf { it.w } + 28).coerceIn(TILE_MIN_W, TILE_MAX_W).dp
    val height = (form.frameHeight + 34).coerceIn(58, 96).dp
    Surface(
        modifier = modifier
            .width(width)
            .height(height),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, LbmOutline),
    ) {
        Box(contentAlignment = Alignment.Center) {
            NeumeStack(
                form = form,
                contentDescription = "",
                highlight = highlight,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

@Composable
private fun EqualsConnector(progress: Float) {
    val s = (progress * 3f).coerceIn(0f, 1f)
    Text(
        text = stringResource(R.string.equal),
        style = MaterialTheme.typography.headlineMedium,
        color = LbmBrown,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .graphicsLayer {
                alpha = s
                scaleX = 0.6f + 0.4f * s
                scaleY = 0.6f + 0.4f * s
            },
    )
}

@Composable
private fun DotSeparator() {
    Text(
        text = "·",
        style = MaterialTheme.typography.headlineSmall,
        color = LbmTextSecondary,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

/* ----------------------------- Counter example + nasal hint + tip ----------------------------- */

@Composable
private fun CounterExample(sign: QualitySign) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF7E0E8))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = LbmMeasureBar,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.quality_dont_label),
                style = MaterialTheme.typography.labelLarge,
                color = LbmMeasureBar,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))
        FramedImage(
            res = sign.counterImageRes,
            contentDescription = stringResource(sign.counterCdRes),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(sign.counterTextRes),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
    }
}

@Composable
private fun NasalHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LbmSurfaceVariant)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Filled.RecordVoiceOver,
                contentDescription = null,
                tint = LbmBrown,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.quality_intercom_hint_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = LbmTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.quality_intercom_hint_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LbmTextSecondary,
                )
            }
        }
    }
}

@Composable
private fun ReplayControl(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
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
            text = stringResource(R.string.quality_replay),
            style = MaterialTheme.typography.labelLarge,
            color = LbmBrown,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun TipCard() {
    LessonCard(title = stringResource(R.string.quality_tip_title)) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = LbmBrownSoft,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.quality_tip_body),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
            )
        }
    }
}
