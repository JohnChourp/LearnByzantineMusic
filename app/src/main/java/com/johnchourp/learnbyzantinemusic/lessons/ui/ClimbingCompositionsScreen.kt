package com.johnchourp.learnbyzantinemusic.lessons.ui

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stairs
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
import com.johnchourp.learnbyzantinemusic.summary_theory.ui.Neume
import com.johnchourp.learnbyzantinemusic.summary_theory.ui.NeumeForm
import com.johnchourp.learnbyzantinemusic.summary_theory.ui.NeumeStack
import com.johnchourp.learnbyzantinemusic.summary_theory.ui.drawableRes
import com.johnchourp.learnbyzantinemusic.ui.components.LessonCard
import com.johnchourp.learnbyzantinemusic.ui.components.LessonHero
import com.johnchourp.learnbyzantinemusic.ui.components.StaggeredAppear
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
 * Redesigned «Συνθέσεις ανάβασης» page: an animated hero, a concept card that frames the idea, a
 * legend of every symbol, and three sections (base compositions, the γοργόν, the δίγοργον) where
 * each written character visibly "breaks apart" into the simpler neumes it is read as, with a
 * plain-Greek reading for every one. The neume diagrams are rendered faithfully from the original
 * layout via the shared [NeumeStack]. Back navigation via [onBack].
 */
@Composable
fun ClimbingCompositionsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LbmPageBg)
            .verticalScroll(scroll),
    ) {
        LessonHero(
            title = stringResource(R.string.climbing_compositions),
            subtitle = stringResource(R.string.climbing_subtitle),
            onBack = onBack,
            icon = Icons.Filled.Stairs,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            StaggeredAppear(delayMillis = 60) { ConceptCard() }
            StaggeredAppear(delayMillis = 140) { LegendCard() }
            StaggeredAppear(delayMillis = 220) { SimpleCompositionsCard() }
            StaggeredAppear(delayMillis = 300) { GorgoCompositionsCard() }
            StaggeredAppear(delayMillis = 380) { DigorgoCompositionsCard() }
            StaggeredAppear(delayMillis = 460) { TipCard() }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/* ----------------------------- Concept ----------------------------- */

@Composable
private fun ConceptCard() {
    LessonCard(title = stringResource(R.string.climbing_concept_title)) {
        Text(
            text = stringResource(R.string.climbing_concept_body),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LbmPrimaryContainer)
                .padding(12.dp),
        ) {
            Text(
                text = stringResource(R.string.climbing_how_to_read),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextPrimary,
            )
        }
    }
}

/* ----------------------------- Legend ----------------------------- */

private data class LegendItem(
    val neume: Neume,
    @StringRes val nameRes: Int,
    @StringRes val meaningRes: Int,
    @StringRes val cdRes: Int,
)

private val LEGEND = listOf(
    LegendItem(Neume.OLIGON, R.string.oligon, R.string.climbing_legend_oligon, R.string.cd_oligon),
    LegendItem(Neume.ISON, R.string.ison, R.string.climbing_legend_ison, R.string.cd_ison),
    LegendItem(Neume.APOSTROPHE, R.string.apostrophe, R.string.climbing_legend_apostrophe, R.string.cd_apostrophe),
    LegendItem(Neume.EMBROIDERIES, R.string.embroideries, R.string.climbing_legend_embroideries, R.string.cd_embroideries),
    LegendItem(Neume.UNDERFLOW, R.string.underflow, R.string.climbing_legend_underflow, R.string.cd_underflow),
    LegendItem(Neume.GORGO, R.string.gorgo, R.string.climbing_legend_gorgo, R.string.cd_gorgo),
    LegendItem(Neume.DIGORGO, R.string.digorgo, R.string.climbing_legend_digorgo, R.string.cd_digorgo),
    LegendItem(Neume.SIMPLE_DOT, R.string.name_simple_dot, R.string.climbing_legend_simple_dot, R.string.cd_simple_dot),
    LegendItem(Neume.FRACTION, R.string.fraction, R.string.climbing_legend_fraction, R.string.cd_fraction),
)

@Composable
private fun LegendCard() {
    LessonCard(title = stringResource(R.string.climbing_legend_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            LEGEND.forEach { GlyphLegendRow(it) }
        }
    }
}

@Composable
private fun GlyphLegendRow(item: LegendItem) {
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

/* ----------------------------- Sections ----------------------------- */

@Composable
private fun SimpleCompositionsCard() {
    LessonCard(title = stringResource(R.string.climbing_compositions)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ClimbingCompositions.simple.forEach { DecompositionRow(it, sectionReplay = 0) }
        }
    }
}

@Composable
private fun GorgoCompositionsCard() {
    var replay by remember { mutableIntStateOf(0) }
    LessonCard(title = stringResource(R.string.gorgo_in_climbing_compositions)) {
        SectionIntro(
            introRes = R.string.gorgo_section_intro,
            badgeRes = R.string.climbing_gorgo_badge,
            sectionReplay = replay,
        )
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ClimbingCompositions.gorgo.forEach { DecompositionRow(it, sectionReplay = replay) }
        }
        Spacer(Modifier.height(8.dp))
        ReplayControl { replay++ }
    }
}

@Composable
private fun DigorgoCompositionsCard() {
    var replay by remember { mutableIntStateOf(0) }
    LessonCard(title = stringResource(R.string.digorgo_in_climbing_compositions)) {
        SectionIntro(
            introRes = R.string.digorgo_section_intro,
            badgeRes = R.string.climbing_digorgo_badge,
            sectionReplay = replay,
        )
        Spacer(Modifier.height(8.dp))
        ClimbingCompositions.digorgo.forEach { DecompositionRow(it, sectionReplay = replay) }
        Spacer(Modifier.height(14.dp))
        TimingStrip()
        Spacer(Modifier.height(8.dp))
        ReplayControl { replay++ }
    }
}

@Composable
private fun SectionIntro(@StringRes introRes: Int, @StringRes badgeRes: Int, sectionReplay: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(introRes),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(10.dp))
        EmphasisBadge(badgeRes, sectionReplay)
    }
}

/** A crimson timing badge that gives a single attention pulse on reveal / replay. */
@Composable
private fun EmphasisBadge(@StringRes textRes: Int, sectionReplay: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(visible, sectionReplay) {
        if (visible) {
            pulse.snapTo(1f)
            pulse.animateTo(1.16f, tween(220, easing = FastOutSlowInEasing))
            pulse.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
        }
    }
    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }
            .clip(RoundedCornerShape(10.dp))
            .background(LbmMeasureBar)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}

/* ----------------------------- Decomposition row ----------------------------- */

private val TILE_HEIGHT = 82.dp

/**
 * One equation: the compound character(s) on the left, an animated «=», then the simpler neumes it
 * decomposes into — which fade + slide in one-by-one — and the plain-Greek reading below. Tapping
 * the row (or the section replay) re-runs the reveal. The whole diagram is marked decorative for
 * TalkBack because the reading text fully describes it.
 */
@Composable
private fun DecompositionRow(composition: Composition, sectionReplay: Int) {
    var localReplay by remember { mutableIntStateOf(0) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(visible, sectionReplay, localReplay) {
        if (visible) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 260 + composition.parts.size * 170,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }
    val reading = stringResource(composition.readingRes)
    val replayLabel = stringResource(R.string.climbing_replay)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClickLabel = replayLabel) { localReplay++ }
            .padding(vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .clearAndSetSemantics { },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            composition.combined.forEachIndexed { i, f ->
                if (i > 0) DotSeparator()
                NeumeTile(form = f)
            }
            EqualsConnector(progress = progress.value)
            val n = composition.parts.size
            composition.parts.forEachIndexed { i, f ->
                if (i > 0) DotSeparator()
                val pp = ((progress.value * n) - i).coerceIn(0f, 1f)
                NeumeTile(
                    form = f,
                    modifier = Modifier.graphicsLayer {
                        alpha = pp
                        translationX = (1f - pp) * -22.dp.toPx()
                    },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = reading,
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
    }
}

/** A single neume form on a white tile, sized to its widest glyph; vertically centred. */
@Composable
private fun NeumeTile(form: NeumeForm, modifier: Modifier = Modifier) {
    val width = (form.glyphs.maxOf { it.w } + 32).coerceIn(80, 134).dp
    Surface(
        modifier = modifier
            .width(width)
            .height(TILE_HEIGHT),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, LbmOutline),
    ) {
        Box(contentAlignment = Alignment.Center) {
            NeumeStack(
                form = form,
                contentDescription = "",
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

/* ----------------------------- Timing strip ----------------------------- */

@Composable
private fun TimingStrip() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LbmSurfaceVariant)
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.climbing_timing_title),
                style = MaterialTheme.typography.labelLarge,
                color = LbmTextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            TimingRow(
                labelRes = R.string.climbing_timing_combined_label,
                values = listOf(R.string.time_1_by_2, R.string.time_1_by_2),
            )
            TimingRow(
                labelRes = R.string.climbing_timing_parts_label,
                values = listOf(R.string.time_1_by_2, R.string.time_1_by_4, R.string.time_1_by_4),
            )
            Text(
                text = stringResource(R.string.climbing_timing_caption),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
            )
        }
    }
}

@Composable
private fun TimingRow(@StringRes labelRes: Int, values: List<Int>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
            modifier = Modifier.width(86.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { TimeChip(it) }
        }
    }
}

@Composable
private fun TimeChip(@StringRes valueRes: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(LbmPrimaryContainer)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = stringResource(valueRes),
            style = MaterialTheme.typography.titleLarge,
            color = LbmBrown,
            fontWeight = FontWeight.Bold,
        )
    }
}

/* ----------------------------- Replay + tip ----------------------------- */

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
            text = stringResource(R.string.climbing_replay),
            style = MaterialTheme.typography.labelLarge,
            color = LbmBrown,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun TipCard() {
    LessonCard(title = stringResource(R.string.climbing_tip_title)) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = LbmBrownSoft,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.climbing_tip_body),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
            )
        }
    }
}
