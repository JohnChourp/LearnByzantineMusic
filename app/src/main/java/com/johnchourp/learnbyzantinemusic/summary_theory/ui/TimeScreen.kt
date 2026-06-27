package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.ui.components.LessonCard
import com.johnchourp.learnbyzantinemusic.ui.components.LessonHero
import com.johnchourp.learnbyzantinemusic.ui.components.StaggeredAppear
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmOutline
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPageBg
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPrimaryContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurface
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary
import kotlin.math.abs

/**
 * Redesigned «Χαρακτήρες Χρόνου» (Time Characters) page: an animated hero, a lead card that frames
 * the time characters and lists the κλάσμα/κουκίδες that lengthen a note, a βαρεία rest card, then
 * one card per member of the γοργό / δίγοργο / τρίγοργο / αργό time-division family. Each diagram is
 * a worked [TimeEquation] — the written character, an animated «=», and the simpler notes it is read
 * as, every note tagged with its beat value (½, ¼, ⅓ …) and the time character tinted crimson so it
 * separates from the plain black neumes. Back navigation via [onBack].
 */
@Composable
fun TimeScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LbmPageBg)
            .verticalScroll(scroll),
    ) {
        LessonHero(
            title = stringResource(R.string.time_characters),
            subtitle = stringResource(R.string.time_characters_subtitle),
            onBack = onBack,
            icon = Icons.Filled.Schedule,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            StaggeredAppear(delayMillis = 60) { IntroCard() }
            StaggeredAppear(delayMillis = 120) {
                SymbolListCard(R.string.pause, listOf(R.string.heavy_definition_used), TimeCharacters.pauses)
            }
            StaggeredAppear(delayMillis = 180) {
                EquationCard(R.string.gorgo, listOf(R.string.gorgo_definition_1, R.string.gorgo_definition_2), listOf(TimeCharacters.gorgo))
            }
            StaggeredAppear(delayMillis = 240) {
                EquationCard(R.string.presented_gorgo, listOf(R.string.presented_gorgo_definition), TimeCharacters.presentedGorgo)
            }
            StaggeredAppear(delayMillis = 300) {
                EquationCard(R.string.digorgo, emptyList(), listOf(TimeCharacters.digorgo))
            }
            StaggeredAppear(delayMillis = 360) {
                EquationCard(R.string.presented_digorgo, emptyList(), TimeCharacters.presentedDigorgo)
            }
            StaggeredAppear(delayMillis = 420) {
                EquationCard(R.string.trigorgo, emptyList(), listOf(TimeCharacters.trigorgo))
            }
            StaggeredAppear(delayMillis = 480) {
                EquationCard(R.string.argo, emptyList(), listOf(TimeCharacters.argo))
            }
            StaggeredAppear(delayMillis = 540) {
                EquationCard(R.string.diargo, emptyList(), listOf(TimeCharacters.diargo))
            }
            StaggeredAppear(delayMillis = 600) {
                EquationCard(R.string.triargo, emptyList(), listOf(TimeCharacters.triargo))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/* ----------------------------- Intro + examples ----------------------------- */

@Composable
private fun IntroCard() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = LbmSurface, contentColor = LbmTextPrimary),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.time_characters_definition),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
            )
            Spacer(Modifier.height(14.dp))
            // «1 χρόνο → προσθέτει 1 χρόνο στο σύμβολο» — the headline rule, as its own callout.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(LbmPrimaryContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BeatChip(text = stringResource(R.string.time_1), emphasized = true)
                Spacer(Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = LbmBrown,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.add_time_1),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LbmTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.time_characters_examples_definition),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
            )
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                TimeCharacters.examples.forEach { SymbolRow(it) }
            }
        }
    }
}

/* ----------------------------- Symbol → meaning rows ----------------------------- */

@Composable
private fun SymbolListCard(titleRes: Int, bodyRes: List<Int>, rows: List<TimeSymbolRow>) {
    LessonCard(title = stringResource(titleRes)) {
        bodyRes.forEachIndexed { i, res ->
            if (i > 0) Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(res),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
            )
        }
        if (bodyRes.isNotEmpty()) Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            rows.forEach { SymbolRow(it) }
        }
    }
}

@Composable
private fun SymbolRow(row: TimeSymbolRow) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (row.nameRes != 0) {
            Text(
                text = stringResource(row.nameRes),
                style = MaterialTheme.typography.titleSmall,
                color = LbmTextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(76.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        GlyphTile(form = row.form, contentDescription = stringResource(if (row.nameRes != 0) row.nameRes else row.meaningRes))
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(row.meaningRes),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
            modifier = Modifier.weight(1f),
        )
    }
}

/** A single glyph form on a white tile, sized to fit the symbol-row layout. */
@Composable
private fun GlyphTile(form: NeumeForm, contentDescription: String) {
    Surface(
        // 92dp ≥ widest example glyph (triple_dots is 70dp) + 2×8dp padding, so nothing clips.
        modifier = Modifier.size(width = 92.dp, height = 58.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, LbmOutline),
    ) {
        Box(contentAlignment = Alignment.Center) {
            NeumeStack(
                form = form,
                contentDescription = contentDescription,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

/* ----------------------------- Equation card ----------------------------- */

@Composable
private fun EquationCard(titleRes: Int, bodyRes: List<Int>, equations: List<TimeEquation>) {
    LessonCard(title = stringResource(titleRes)) {
        bodyRes.forEachIndexed { i, res ->
            if (i > 0) Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(res),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
            )
        }
        equations.forEachIndexed { i, eq ->
            if (i > 0 || bodyRes.isNotEmpty()) Spacer(Modifier.height(14.dp))
            EquationRow(eq)
        }
    }
}

/**
 * Renders one [TimeEquation] left→right inside a horizontally-scrollable row. The written character(s)
 * before the «=» are static; the «=» and the notes it is read as fade/slide in on first composition,
 * so the diagram visibly assembles into its reading.
 */
@Composable
private fun EquationRow(eq: TimeEquation) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val progress = remember { Animatable(0f) }
    val eqIndex = eq.terms.indexOfFirst { it.isEquals }
    val afterCount = (eq.terms.size - eqIndex - 1).coerceAtLeast(1)
    LaunchedEffect(visible) {
        if (visible) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 260 + afterCount * 150, easing = FastOutSlowInEasing),
            )
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        eq.terms.forEachIndexed { i, term ->
            val revealModifier = when {
                eqIndex < 0 || i < eqIndex -> Modifier // written character(s): always shown
                i == eqIndex -> Modifier.graphicsLayer {
                    val s = (progress.value * 3f).coerceIn(0f, 1f)
                    alpha = s
                    scaleX = 0.6f + 0.4f * s
                    scaleY = 0.6f + 0.4f * s
                }
                else -> {
                    val k = i - eqIndex - 1
                    Modifier.graphicsLayer {
                        val pp = ((progress.value * afterCount) - k).coerceIn(0f, 1f)
                        alpha = pp
                        translationX = (1f - pp) * -22.dp.toPx()
                    }
                }
            }
            EquationTermTile(term = term, highlight = eq.highlight, modifier = revealModifier)
        }
    }
}

@Composable
private fun EquationTermTile(term: TimeTerm, highlight: Set<Neume>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.height(64.dp), contentAlignment = Alignment.Center) {
            if (term.isEquals) {
                Text(
                    text = stringResource(R.string.equal),
                    style = MaterialTheme.typography.headlineMedium,
                    color = LbmBrown,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            } else {
                val form = term.form!!
                val span = form.glyphs.maxOf { abs(it.dx) * 2 + it.w }
                val tileWidth = (span + 22).coerceIn(56, 150).dp
                Surface(
                    // The glyph diagram is decorative; the beat chips + «=» below carry the reading
                    // for TalkBack (e.g. «½ ½ = 1 χρόνο»), so skip the empty per-glyph nodes.
                    modifier = Modifier
                        .size(width = tileWidth, height = 58.dp)
                        .clearAndSetSemantics { },
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
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier.height(30.dp).widthIn(min = 28.dp, max = 120.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (term.labelRes != 0) BeatChip(text = stringResource(term.labelRes))
        }
    }
}

/* ----------------------------- Beat chip ----------------------------- */

/** A small pill showing a beat value (½, ¼, ⅓, «1 χρόνο» …). [emphasized] inverts it for the headline rule. */
@Composable
private fun BeatChip(text: String, emphasized: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (emphasized) LbmBrown else LbmPrimaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (emphasized) Color.White else LbmBrown,
            fontWeight = FontWeight.Bold,
        )
    }
}
