package com.johnchourp.learnbyzantinemusic.home

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentBlueContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentBlueContent
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentBrownContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentBrownContent
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGoldContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGoldContent
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGreenContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGreenContent
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentOrangeContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentOrangeContent
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentPurpleContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentPurpleContent
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrownSoft
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmHeroEnd
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmHeroStart
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPrimaryContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurface
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary

/** Resolves a [TileAccent] to its (container, content) color pair. */
private fun accentColors(accent: TileAccent): Pair<Color, Color> = when (accent) {
    TileAccent.Gold -> AccentGoldContainer to AccentGoldContent
    TileAccent.Blue -> AccentBlueContainer to AccentBlueContent
    TileAccent.Purple -> AccentPurpleContainer to AccentPurpleContent
    TileAccent.Orange -> AccentOrangeContainer to AccentOrangeContent
    TileAccent.Green -> AccentGreenContainer to AccentGreenContent
    TileAccent.Brown -> AccentBrownContainer to AccentBrownContent
}

/** Gradient hero header with an animated music-note badge, app title and subtitle. */
@Composable
fun HomeHeroHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        shadowElevation = 3.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(LbmHeroStart, LbmHeroEnd)))
                .padding(horizontal = 20.dp, vertical = 26.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                        .size(66.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(CircleShape)
                        .background(LbmBrown),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
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

/** Section title with a small accent bar and optional subtitle. */
@Composable
fun SectionHeader(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 10.dp, bottom = 2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(LbmBrownSoft),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = LbmTextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

/** Tappable navigation card: accent icon badge, title, subtitle, chevron, with press-scale feedback. */
@Composable
fun NavTile(tile: HomeTile, modifier: Modifier = Modifier) {
    val (container, content) = accentColors(tile.accent)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "press")
    val title = stringResource(tile.titleRes)
    val subtitle = tile.subtitleRes?.let { stringResource(it) }
    ElevatedCard(
        onClick = tile.onClick,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = LbmSurface,
            contentColor = LbmTextPrimary,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp, pressedElevation = 6.dp),
        interactionSource = interaction,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(container),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = tile.icon,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = LbmTextPrimary,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LbmTextSecondary,
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = LbmTextSecondary,
            )
        }
    }
}

/** Expandable explainer card for the quantity characters (0 / +1 / -1 voice). */
@Composable
fun QuantityVoicesInfoCard(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(true) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = LbmPrimaryContainer,
            contentColor = LbmTextPrimary,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button) { expanded = !expanded }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = LbmBrown)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.quantity_characters),
                    style = MaterialTheme.typography.titleMedium,
                    color = LbmTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = LbmBrown,
                    modifier = Modifier.graphicsLayer { rotationZ = rotation },
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = stringResource(R.string.quantity_characters_definition),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LbmTextSecondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    VoiceRow(R.string.zero_voice, R.string.zero_voice_definition)
                    VoiceRow(R.string.add_one_voice, R.string.one_voice_definition)
                    VoiceRow(R.string.minus_one_voice, R.string.minus_one_voice_definition)
                }
            }
        }
    }
}

@Composable
private fun VoiceRow(@StringRes labelRes: Int, @StringRes valueRes: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = LbmSurface,
            modifier = Modifier.width(88.dp),
        ) {
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelLarge,
                color = LbmBrown,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(valueRes),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextPrimary,
        )
    }
}

/** Footer line: "powered by ... v.X". */
@Composable
fun PoweredByFooter(version: String, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.powered_by_version, version),
        style = MaterialTheme.typography.bodyMedium,
        color = LbmTextSecondary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
    )
}
