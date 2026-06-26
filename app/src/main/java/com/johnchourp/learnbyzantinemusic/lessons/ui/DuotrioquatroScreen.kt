package com.johnchourp.learnbyzantinemusic.lessons.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary

/** Localized name of a grouping (Δίσημος / Τρίσημος / Τετράσημος). */
@StringRes
internal fun BeatGrouping.nameRes(): Int = when (this) {
    BeatGrouping.DISIMOS -> R.string.disimos
    BeatGrouping.TRISIMOS -> R.string.trisimos
    BeatGrouping.TETRASIMOS -> R.string.tetrasimos
}

/** Localized one-line definition of a grouping. */
@StringRes
internal fun BeatGrouping.definitionRes(): Int = when (this) {
    BeatGrouping.DISIMOS -> R.string.disimos_definition
    BeatGrouping.TRISIMOS -> R.string.trisimos_definition
    BeatGrouping.TETRASIMOS -> R.string.tetrasimos_definition
}

/** The original neume diagram for a grouping. */
@DrawableRes
internal fun BeatGrouping.diagramRes(): Int = when (this) {
    BeatGrouping.DISIMOS -> R.drawable.disimos
    BeatGrouping.TRISIMOS -> R.drawable.trisimos
    BeatGrouping.TETRASIMOS -> R.drawable.tetrasimos
}

/** Accessibility description for a grouping's neume diagram. */
@StringRes
internal fun BeatGrouping.diagramCdRes(): Int = when (this) {
    BeatGrouping.DISIMOS -> R.string.cd_disimos
    BeatGrouping.TRISIMOS -> R.string.cd_trisimos
    BeatGrouping.TETRASIMOS -> R.string.cd_tetrasimos
}

/**
 * Redesigned "Δίσημος, Τρίσημος, Τετράσημος" lesson: an animated hero, a concept card with a
 * 2·3·4 legend, an interactive metronome that pulses each grouping, the three corrected
 * definitions framed beside their neumes, and a practice tip. Back navigation via [onBack].
 */
@Composable
fun DuotrioquatroScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LbmPageBg)
            .verticalScroll(scroll),
    ) {
        LessonHero(
            title = stringResource(R.string.duotrioquatro),
            subtitle = stringResource(R.string.duotrio_subtitle),
            onBack = onBack,
            icon = Icons.Filled.Timer,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            StaggeredAppear(delayMillis = 60) { ConceptCard() }
            StaggeredAppear(delayMillis = 140) { InteractiveBeatCard() }
            StaggeredAppear(delayMillis = 220) { GroupingsReferenceCard() }
            StaggeredAppear(delayMillis = 300) { TipCard() }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** What the groupings are, plus a 2·3·4 legend tying each name to its beat/phthong count. */
@Composable
private fun ConceptCard() {
    LessonCard(title = stringResource(R.string.duotrio_concept_title)) {
        Text(
            text = stringResource(R.string.duotrio_concept_body),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BeatGrouping.all.forEach { grouping ->
                LegendBadge(grouping = grouping, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LegendBadge(grouping: BeatGrouping, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(LbmPrimaryContainer)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NumberBadge(number = grouping.beats)
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(grouping.nameRes()),
            style = MaterialTheme.typography.labelLarge,
            color = LbmTextPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.duotrio_phthongs_count, grouping.beats),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NumberBadge(number: Int) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(LbmBrown),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Pick a grouping and watch it pulse on the animated metronome, beside its real neumes. */
@Composable
private fun InteractiveBeatCard() {
    var selected by remember { mutableStateOf(BeatGrouping.DISIMOS) }
    LessonCard(title = stringResource(R.string.duotrio_interactive_title)) {
        Text(
            text = stringResource(R.string.duotrio_interactive_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(14.dp))
        GroupingSelector(selected = selected, onSelect = { selected = it })
        Spacer(Modifier.height(20.dp))
        BeatPulse(grouping = selected)
        Spacer(Modifier.height(20.dp))
        FramedImage(
            res = selected.diagramRes(),
            contentDescription = stringResource(selected.diagramCdRes()),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(
                R.string.duotrio_interactive_caption,
                stringResource(selected.nameRes()),
                selected.beats,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun GroupingSelector(selected: BeatGrouping, onSelect: (BeatGrouping) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BeatGrouping.all.forEach { grouping ->
            SelectorChip(
                label = stringResource(grouping.nameRes()),
                selected = grouping == selected,
                onClick = { onSelect(grouping) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RowScope.SelectorChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container by animateColorAsState(
        targetValue = if (selected) LbmBrown else LbmSurface,
        label = "selectorBg",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else LbmTextPrimary,
        label = "selectorText",
    )
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .border(1.dp, if (selected) LbmBrown else LbmOutline, RoundedCornerShape(12.dp))
            .selectable(selected = selected, role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

/** The three groupings with their corrected definitions framed beside the original neumes. */
@Composable
private fun GroupingsReferenceCard() {
    LessonCard(title = stringResource(R.string.duotrio_groupings_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            BeatGrouping.all.forEach { grouping ->
                GroupingReferenceRow(grouping)
            }
        }
    }
}

@Composable
private fun GroupingReferenceRow(grouping: BeatGrouping) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NumberBadge(number = grouping.beats)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(grouping.nameRes()),
                    style = MaterialTheme.typography.titleMedium,
                    color = LbmTextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(grouping.definitionRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LbmTextSecondary,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        FramedImage(
            res = grouping.diagramRes(),
            contentDescription = stringResource(grouping.diagramCdRes()),
        )
    }
}

/** A short, friendly practice tip. */
@Composable
private fun TipCard() {
    LessonCard(title = stringResource(R.string.duotrio_tip_title)) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = LbmBrownSoft,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.duotrio_tip_body),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
            )
        }
    }
}
