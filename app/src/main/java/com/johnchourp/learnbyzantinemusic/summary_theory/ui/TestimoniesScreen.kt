package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.ui.components.LessonCard
import com.johnchourp.learnbyzantinemusic.ui.components.LessonHero
import com.johnchourp.learnbyzantinemusic.ui.components.StaggeredAppear
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentCrimsonContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentCrimsonContent
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmOutline
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPageBg
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPrimaryContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurface
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary

/**
 * Redesigned «Μαρτυρίες» (Testimonies) page: an animated hero, a card that explains what the
 * martyria signs are (they tell us which mode and note we are on), a highlighted «δεν ψέλνονται»
 * (not sung) reminder, and an interactive example row that walks one octave Νη→Νη΄. Each column is
 * a phthong with its martyria; tapping one expands a detail card with the enlarged sign and the
 * mode(s) that use it — every mode quoted verbatim from the app's own theory strings. Nothing on the
 * page produces sound, faithful to «οι μαρτυρίες δεν ψέλνονται». Back navigation via [onBack].
 */
@Composable
fun TestimoniesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LbmPageBg)
            .verticalScroll(scroll),
    ) {
        LessonHero(
            title = stringResource(R.string.testimonials),
            subtitle = stringResource(R.string.testimonies_subtitle),
            onBack = onBack,
            icon = Icons.Filled.Place,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            StaggeredAppear(delayMillis = 60) { DefinitionCard() }
            StaggeredAppear(delayMillis = 120) { NotSungCallout() }
            StaggeredAppear(delayMillis = 180) { ExamplesCard() }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/* ----------------------------- Definition ----------------------------- */

@Composable
private fun DefinitionCard() {
    LessonCard(title = stringResource(R.string.testimonies_definition_title)) {
        Text(
            text = stringResource(R.string.testimonials_definition),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
    }
}

/** Highlighted «δεν ψέλνονται» reminder — a muted/music-off badge, never a play control. */
@Composable
private fun NotSungCallout() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = LbmSurface, contentColor = LbmTextPrimary),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentCrimsonContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicOff,
                    contentDescription = null,
                    tint = AccentCrimsonContent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.testimonies_not_sung_note),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/* ----------------------------- Examples (the octave row) ----------------------------- */

@Composable
private fun ExamplesCard() {
    LessonCard(title = stringResource(R.string.testimonies_examples_title)) {
        Text(
            text = stringResource(R.string.matching_testimonials_definition),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.testimonies_table_caption),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(14.dp))

        var selected by remember { mutableIntStateOf(-1) }
        // Keep the last-shown column so its content stays put while the card shrinks away on collapse.
        var shown by remember { mutableStateOf<MartyriaUiModel?>(null) }
        if (selected in TESTIMONY_ROW.indices) shown = TESTIMONY_ROW[selected]

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            TESTIMONY_ROW.forEachIndexed { index, item ->
                // Deal the columns out left→right, continuing the section stagger (240, 300 … 660).
                StaggeredAppear(delayMillis = 240 + index * 60) {
                    MartyriaColumn(
                        item = item,
                        isOctaveClose = index == TESTIMONY_ROW.lastIndex,
                        selected = selected == index,
                        onClick = { selected = if (selected == index) -1 else index },
                    )
                }
            }
        }

        // Expanding detail card for the tapped column.
        AnimatedVisibility(
            visible = selected in TESTIMONY_ROW.indices,
            enter = fadeIn(tween(durationMillis = 280, easing = FastOutSlowInEasing)) +
                expandVertically(tween(durationMillis = 280, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(durationMillis = 200, easing = FastOutSlowInEasing)) +
                shrinkVertically(tween(durationMillis = 200, easing = FastOutSlowInEasing)),
        ) {
            Column {
                Spacer(Modifier.height(14.dp))
                shown?.let { MartyriaDetailCard(it) }
            }
        }

        Spacer(Modifier.height(14.dp))
        OctaveNote()
    }
}

/** One column: phthong name over the martyria sign on a white tile; tappable to expand details. */
@Composable
private fun MartyriaColumn(
    item: MartyriaUiModel,
    isOctaveClose: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // A single, subtle settle on the closing octave Νη once the row has finished dealing out.
    val settle = remember { Animatable(if (isOctaveClose) 0.8f else 1f) }
    if (isOctaveClose) {
        LaunchedEffect(Unit) {
            settle.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 420, delayMillis = 760, easing = FastOutSlowInEasing),
            )
        }
    }
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .selectable(selected = selected, role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(item.phthongRes),
            style = MaterialTheme.typography.titleMedium,
            color = LbmTextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        MartyriaTile(
            item = item,
            selected = selected,
            modifier = Modifier
                .size(width = 60.dp, height = 74.dp)
                .graphicsLayer { scaleX = settle.value; scaleY = settle.value },
        )
    }
}

/** White, rounded tile holding a single martyria drawable; brown frame when selected. */
@Composable
private fun MartyriaTile(
    item: MartyriaUiModel,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) LbmPrimaryContainer else Color.White,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) LbmBrown else LbmOutline),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(item.drawable),
                contentDescription = stringResource(item.contentDescRes),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

/** Detail card for the tapped column: enlarged sign + the mode(s) that use it (verbatim app strings). */
@Composable
private fun MartyriaDetailCard(item: MartyriaUiModel) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = LbmSurface, contentColor = LbmTextPrimary),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            MartyriaTile(
                item = item,
                selected = false,
                modifier = Modifier.size(width = 88.dp, height = 104.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(item.phthongRes),
                    style = MaterialTheme.typography.titleLarge,
                    color = LbmTextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))
                if (item.modes.isEmpty()) {
                    Text(
                        text = stringResource(item.contentDescRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LbmTextSecondary,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.testimonies_modes_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = LbmTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    item.modes.forEach { mode ->
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ModeChip(text = stringResource(mode.nameRes))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(mode.descRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = LbmTextSecondary,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Small brown-on-cream pill naming a mode (ήχος). */
@Composable
private fun ModeChip(text: String) {
    Box(
        modifier = Modifier
            .heightIn(min = 28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(LbmPrimaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = LbmBrown,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

/** Grounded octave-wrap note: the row begins and ends on Νη because it spans one octave (διαπασών). */
@Composable
private fun OctaveNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LbmPrimaryContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Place,
            contentDescription = null,
            tint = LbmBrown,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.testimonies_octave_note),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}
