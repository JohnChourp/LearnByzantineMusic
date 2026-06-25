package com.johnchourp.learnbyzantinemusic.lessons.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.ui.components.StaggeredAppear
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmHeroEnd
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmHeroStart
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmOutline
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPageBg
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPrimaryContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurface
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary

/** Localized display name for a phthong (Greek: Νη Πα …, English: Ni Pa …). */
@StringRes
internal fun Phthong.nameRes(): Int = when (this) {
    Phthong.NI -> R.string.phthong_ni
    Phthong.PA -> R.string.phthong_pa
    Phthong.VOU -> R.string.phthong_bou
    Phthong.GA -> R.string.phthong_ga
    Phthong.DI -> R.string.phthong_di
    Phthong.KE -> R.string.phthong_ke
    Phthong.ZO -> R.string.phthong_zo
}

/**
 * Redesigned "Ονόματα Φθόγγων" (Names of Phthongs) lesson: an animated hero, the
 * letter→name origin, an interactive octave strip and the climbing/descending staircase,
 * followed by the first exercise. Pure UI — back navigation is supplied via [onBack].
 */
@Composable
fun PhthongsNamesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LbmPageBg)
            .verticalScroll(scroll),
    ) {
        LessonHero(
            title = stringResource(R.string.phthongs_names),
            subtitle = stringResource(R.string.phthongs_names_subtitle),
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            StaggeredAppear(delayMillis = 60) { OriginCard() }
            StaggeredAppear(delayMillis = 140) { OctaveStripCard() }
            StaggeredAppear(delayMillis = 220) { StaircaseCard() }
            StaggeredAppear(delayMillis = 300) { ExerciseCard() }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LessonHero(title: String, subtitle: String, onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        shadowElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(LbmHeroStart, LbmHeroEnd)))
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp, bottom = 24.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = LbmBrown,
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val infinite = rememberInfiniteTransition(label = "hero")
                val scale by infinite.animateFloat(
                    initialValue = 0.96f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "heroScale",
                )
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(CircleShape)
                        .background(LbmBrown),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = LbmTextPrimary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LbmTextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Card body wrapper: a titled [ElevatedCard] used by every lesson section. */
@Composable
private fun LessonCard(title: String, content: @Composable () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = LbmSurface,
            contentColor = LbmTextPrimary,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(LbmBrown),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = LbmTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/** "Where the names come from": each Greek letter maps to the phthong it names. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OriginCard() {
    LessonCard(title = stringResource(R.string.phthongs_origin_title)) {
        Text(
            text = stringResource(R.string.phthongs_origin_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(14.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PhthongScale.byAlphabet.forEach { phthong ->
                LetterMappingChip(letter = phthong.sourceLetter, nameRes = phthong.nameRes())
            }
        }
    }
}

@Composable
private fun LetterMappingChip(letter: Char, @StringRes nameRes: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(LbmPrimaryContainer)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(LbmBrown),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = letter.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = LbmTextSecondary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(nameRes),
            style = MaterialTheme.typography.titleMedium,
            color = LbmTextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Interactive octave row: tap a phthong to highlight it; the row repeats endlessly. */
@Composable
private fun OctaveStripCard() {
    var selected by remember { mutableIntStateOf(0) }
    LessonCard(title = stringResource(R.string.phthongs_octave_title)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Ellipsis()
            PhthongScale.octave.forEachIndexed { index, phthong ->
                Spacer(Modifier.width(6.dp))
                OctaveChip(
                    nameRes = phthong.nameRes(),
                    selected = index == selected,
                    onClick = { selected = index },
                )
                Spacer(Modifier.width(6.dp))
            }
            // The wrap-around: Νη appears again, one octave up, as its own selectable step.
            OctaveChip(
                nameRes = PhthongScale.phthongAt(PhthongScale.size).nameRes(),
                selected = selected == PhthongScale.size,
                onClick = { selected = PhthongScale.size },
            )
            Spacer(Modifier.width(6.dp))
            Ellipsis()
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.phthongs_octave_explainer),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
    }
}

@Composable
private fun Ellipsis() {
    // Decorative continuation marker (endless octaves) — the explainer text carries the
    // meaning, so hide it from the accessibility tree to avoid TalkBack reading "…".
    Text(
        text = "…",
        style = MaterialTheme.typography.titleLarge,
        color = LbmTextSecondary,
        modifier = Modifier.clearAndSetSemantics {},
    )
}

@Composable
private fun OctaveChip(@StringRes nameRes: Int, selected: Boolean, onClick: () -> Unit) {
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
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .border(1.dp, if (selected) LbmBrown else LbmOutline, RoundedCornerShape(12.dp))
            .selectable(selected = selected, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = stringResource(nameRes),
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** The interactive climbing/descending staircase plus the original diagram for reference. */
@Composable
private fun StaircaseCard() {
    LessonCard(title = stringResource(R.string.phthongs_staircase_title)) {
        Text(
            text = stringResource(R.string.phthongs_staircase_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(14.dp))
        PhthongStaircase()
        Spacer(Modifier.height(16.dp))
        FramedImage(
            res = R.drawable.phthongs_staircase,
            contentDescription = stringResource(R.string.cd_phthongs_staircase),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.phthongs_staircase_image_caption),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ExerciseCard() {
    LessonCard(title = stringResource(R.string.exercise_1)) {
        Text(
            text = stringResource(R.string.phthongs_exercise_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(14.dp))
        FramedImage(
            res = R.drawable.lesson1_exercise_1,
            contentDescription = stringResource(R.string.cd_exercise_1),
        )
    }
}

/** White, rounded frame for the original lesson diagrams so they sit cleanly on the page. */
@Composable
private fun FramedImage(@DrawableRes res: Int, contentDescription: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, LbmOutline),
    ) {
        Image(
            painter = painterResource(res),
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            contentScale = ContentScale.FillWidth,
        )
    }
}
