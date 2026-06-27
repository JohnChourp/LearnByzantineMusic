package com.johnchourp.learnbyzantinemusic.modes.ui

import android.widget.TextView
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.modes.ModeScaleFrequencies
import com.johnchourp.learnbyzantinemusic.modes.ModeScaleGenus
import com.johnchourp.learnbyzantinemusic.modes.ModeTheoryCatalog
import com.johnchourp.learnbyzantinemusic.modes.ModeTheoryStyleRow
import com.johnchourp.learnbyzantinemusic.modes.TheoryTopicLinks
import com.johnchourp.learnbyzantinemusic.modes.ToneTimbre
import com.johnchourp.learnbyzantinemusic.ui.components.LessonCard
import com.johnchourp.learnbyzantinemusic.ui.components.LessonChip
import com.johnchourp.learnbyzantinemusic.ui.components.LessonHero
import com.johnchourp.learnbyzantinemusic.ui.components.StaggeredAppear
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmOutline
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPageBg
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPrimaryContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurface
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val SCALE_OCTAVES = 3
private const val BASE_REFERENCE_PHTHONG = "Νη"
private const val BASE_SHIFT_MIN = -12
private const val BASE_SHIFT_MAX = 12

/**
 * Redesigned «Κλίμακες των 8 Ήχων» screen. A hero with a ☰ that opens the catalog pages menu, a genus
 * legend, a colour-coded mode picker, the interactive [ModeScaleDiagram], timbre chips, a base-shift
 * slider and a numbered mode-details card. State (selected mode, timbre, per-mode base shift) is held
 * here and persisted through the callbacks; the host Activity owns the [ModeScaleFrequencies]→tone
 * playback and its lifecycle. Pure logic (scales, theory, audio) is reused unchanged.
 */
@Composable
fun EightModesScreen(
    initialModeIndex: Int,
    initialTimbre: ToneTimbre,
    initialBaseShifts: Map<Int, Int>,
    onSelectMode: (Int) -> Unit,
    onSelectTimbre: (ToneTimbre) -> Unit,
    onBaseShiftChange: (modeIndex: Int, moria: Int) -> Unit,
    onTonePress: (Double) -> Unit,
    onToneRelease: () -> Unit,
    onOpenMenu: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedModeIndex by remember {
        mutableStateOf(initialModeIndex.coerceIn(EIGHT_MODES.indices))
    }
    var timbre by remember { mutableStateOf(initialTimbre) }
    val baseShiftByMode: SnapshotStateMap<Int, Int> = remember {
        mutableStateMapOf<Int, Int>().apply { putAll(initialBaseShifts) }
    }
    var activeIndex by remember { mutableStateOf(-1) }
    val scope = rememberCoroutineScope()

    // Switching modes never carries a held tone over.
    LaunchedEffect(selectedModeIndex) {
        activeIndex = -1
        onToneRelease()
    }

    val currentGenus = EIGHT_MODES[selectedModeIndex].genus
    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LbmPageBg)
            .verticalScroll(scroll),
    ) {
        LessonHero(
            title = stringResource(R.string.eight_modes_page_title),
            subtitle = stringResource(R.string.eight_modes_page_subtitle),
            onBack = onBack,
            icon = Icons.Filled.LibraryMusic,
            onMenuClick = onOpenMenu,
            menuContentDescription = stringResource(
                R.string.eight_modes_navigation_menu_content_description,
            ),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            StaggeredAppear(delayMillis = 60) { GenusLegend(currentGenus) }
            StaggeredAppear(delayMillis = 120) {
                ModePickerCard(
                    selectedIndex = selectedModeIndex,
                    onSelect = { index ->
                        if (index != selectedModeIndex) {
                            selectedModeIndex = index
                            onSelectMode(index)
                        }
                    },
                )
            }
            StaggeredAppear(delayMillis = 180) {
                Crossfade(
                    targetState = selectedModeIndex,
                    animationSpec = tween(280),
                    label = "scaleCrossfade",
                ) { idx ->
                    ScaleCard(
                        modeIndex = idx,
                        baseShiftMoria = baseShiftByMode[idx] ?: 0,
                        activeIndex = activeIndex,
                        onActiveIndexChange = { i -> activeIndex = i },
                        onTonePress = onTonePress,
                        onToneRelease = onToneRelease,
                        onAccessibilityPulse = onTonePress,
                        onAccessibilityRelease = onToneRelease,
                        scope = scope,
                    )
                }
            }
            StaggeredAppear(delayMillis = 240) {
                TimbreCard(
                    selected = timbre,
                    onSelect = { picked ->
                        if (picked != timbre) {
                            timbre = picked
                            activeIndex = -1
                            onSelectTimbre(picked)
                        }
                    },
                )
            }
            StaggeredAppear(delayMillis = 300) {
                BaseShiftCard(
                    moria = baseShiftByMode[selectedModeIndex] ?: 0,
                    onChange = { value ->
                        baseShiftByMode[selectedModeIndex] = value
                        onBaseShiftChange(selectedModeIndex, value)
                    },
                )
            }
            StaggeredAppear(delayMillis = 360) {
                Crossfade(
                    targetState = selectedModeIndex,
                    animationSpec = tween(280),
                    label = "detailsCrossfade",
                ) { idx -> ModeDetailsCard(modeIndex = idx) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/* ----------------------------- Genus legend ----------------------------- */

@Composable
private fun GenusLegend(currentGenus: ModeScaleGenus) {
    LessonCard(title = stringResource(R.string.eight_modes_genus_legend_title)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ModeGenusPalette.ordered.forEach { genus ->
                val accent = ModeGenusPalette.accent(genus)
                val current = genus == currentGenus
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (current) accent.content else accent.container)
                        .border(
                            1.dp,
                            if (current) accent.content else accent.content.copy(alpha = 0.35f),
                            RoundedCornerShape(50),
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (current) Color.White else accent.content),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = stringResource(accent.nameRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (current) Color.White else accent.content,
                        fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

/* ----------------------------- Mode picker ----------------------------- */

@Composable
private fun ModePickerCard(selectedIndex: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val mode = EIGHT_MODES[selectedIndex]
    val accent = ModeGenusPalette.accent(mode.genus)
    LessonCard(title = stringResource(R.string.eight_modes_selector_label)) {
        Text(
            text = stringResource(R.string.eight_modes_mode_selector_hint),
            style = MaterialTheme.typography.bodySmall,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(10.dp))
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.container)
                    .border(1.dp, accent.content.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(mode.selectorNameRes),
                        style = MaterialTheme.typography.titleMedium,
                        color = accent.content,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(mode.selectorGenusRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = LbmTextSecondary,
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = stringResource(R.string.eight_modes_open_selector),
                    tint = accent.content,
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                EIGHT_MODES.forEachIndexed { index, option ->
                    val optionAccent = ModeGenusPalette.accent(option.genus)
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            onSelect(index)
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(optionAccent.content),
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(
                                    R.string.eight_modes_mode_option_template,
                                    stringResource(option.selectorNameRes),
                                    stringResource(option.selectorGenusRes),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = optionAccent.content,
                                fontWeight = if (index == selectedIndex) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}

/* ----------------------------- Scale diagram card ----------------------------- */

@Composable
private fun ScaleCard(
    modeIndex: Int,
    baseShiftMoria: Int,
    activeIndex: Int,
    onActiveIndexChange: (Int) -> Unit,
    onTonePress: (Double) -> Unit,
    onToneRelease: () -> Unit,
    onAccessibilityPulse: (Double) -> Unit,
    onAccessibilityRelease: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val mode = EIGHT_MODES[modeIndex]
    val scale = mode.scale
    val ascendingIntervals = remember(modeIndex) { scale.repeatedIntervals(SCALE_OCTAVES) }
    val ascendingPhthongs = remember(modeIndex) { scale.ascendingPhthongs(SCALE_OCTAVES) }
    val referenceMoria = remember(modeIndex) {
        scale.referenceMoriaFromBottom(BASE_REFERENCE_PHTHONG, SCALE_OCTAVES)
    }
    val phthongsTopToBottom = remember(modeIndex) { ascendingPhthongs.reversed() }
    val intervalsTopToBottom = remember(modeIndex) { ascendingIntervals.reversed() }
    val tonicBare = scale.base.phthong
    val tonicIndices = remember(modeIndex) {
        phthongsTopToBottom.indices.filter { i ->
            phthongsTopToBottom[i].trimEnd('΄', ',') == tonicBare
        }.toSet()
    }
    val frequencies = remember(modeIndex, baseShiftMoria) {
        ModeScaleFrequencies.topToBottom(ascendingIntervals, referenceMoria, baseShiftMoria)
    }

    LessonCard(title = stringResource(R.string.eight_modes_scale_card_title)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.TouchApp,
                contentDescription = null,
                tint = LbmBrown,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.eight_modes_scale_card_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        // Either the μόρια legend, or a live "now playing" readout while a tone sounds.
        val nowPlaying = phthongsTopToBottom.getOrNull(activeIndex)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (nowPlaying != null) LbmPrimaryContainer else LbmSurface)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                text = if (nowPlaying != null) {
                    stringResource(R.string.eight_modes_scale_now_playing, nowPlaying)
                } else {
                    stringResource(R.string.eight_modes_scale_moria_legend)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (nowPlaying != null) LbmBrown else LbmTextSecondary,
                fontWeight = if (nowPlaying != null) FontWeight.Bold else FontWeight.Normal,
            )
        }
        Spacer(Modifier.height(12.dp))
        ModeScaleDiagram(
            phthongsTopToBottom = phthongsTopToBottom,
            intervalsTopToBottom = intervalsTopToBottom,
            genus = scale.genus,
            tonicIndices = tonicIndices,
            activeIndex = activeIndex,
            onActiveIndexChange = { i ->
                onActiveIndexChange(i)
                if (i in frequencies.indices) onTonePress(frequencies[i]) else onToneRelease()
            },
            onAccessibilityPlay = { i ->
                frequencies.getOrNull(i)?.let { freq ->
                    scope.launch {
                        onAccessibilityPulse(freq)
                        delay(650)
                        onAccessibilityRelease()
                    }
                }
            },
        )
    }
}

/* ----------------------------- Timbre chips ----------------------------- */

private val TIMBRE_OPTIONS = listOf(ToneTimbre.CLEAN, ToneTimbre.SOFT, ToneTimbre.CRYSTAL)

@Composable
private fun TimbreCard(selected: ToneTimbre, onSelect: (ToneTimbre) -> Unit) {
    LessonCard(title = stringResource(R.string.eight_modes_timbre_selector_label)) {
        Text(
            text = stringResource(R.string.eight_modes_timbre_selector_hint),
            style = MaterialTheme.typography.bodySmall,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TIMBRE_OPTIONS.forEach { option ->
                LessonChip(
                    label = stringResource(timbreLabelRes(option)),
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun timbreLabelRes(timbre: ToneTimbre): Int = when (timbre) {
    ToneTimbre.CLEAN -> R.string.eight_modes_timbre_clean
    ToneTimbre.SOFT -> R.string.eight_modes_timbre_soft
    ToneTimbre.CRYSTAL -> R.string.eight_modes_timbre_crystal
}

/* ----------------------------- Base-shift slider ----------------------------- */

@Composable
private fun BaseShiftCard(moria: Int, onChange: (Int) -> Unit) {
    val valueText = if (moria == 0) {
        stringResource(R.string.eight_modes_base_shift_default_value)
    } else {
        stringResource(R.string.eight_modes_base_shift_value_template, moria)
    }
    LessonCard(title = stringResource(R.string.eight_modes_base_shift_title)) {
        Text(
            text = stringResource(R.string.eight_modes_base_shift_hint),
            style = MaterialTheme.typography.bodySmall,
            color = LbmTextSecondary,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(LbmPrimaryContainer)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelLarge,
                    color = LbmBrown,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { onChange(0) }) {
                Icon(
                    imageVector = Icons.Filled.Restore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = LbmBrown,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.eight_modes_base_shift_reset),
                    color = LbmBrown,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Slider(
            value = moria.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            modifier = Modifier.semantics { stateDescription = valueText },
            valueRange = BASE_SHIFT_MIN.toFloat()..BASE_SHIFT_MAX.toFloat(),
            steps = (BASE_SHIFT_MAX - BASE_SHIFT_MIN) - 1,
            colors = SliderDefaults.colors(
                thumbColor = LbmBrown,
                activeTrackColor = LbmBrown,
                inactiveTrackColor = LbmOutline,
            ),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.eight_modes_base_shift_lower),
                style = MaterialTheme.typography.bodySmall,
                color = LbmTextSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.eight_modes_base_shift_higher),
                style = MaterialTheme.typography.bodySmall,
                color = LbmTextSecondary,
            )
        }
    }
}

/* ----------------------------- Mode details ----------------------------- */

@Composable
private fun ModeDetailsCard(modeIndex: Int) {
    val mode = EIGHT_MODES[modeIndex]
    val theory = ModeTheoryCatalog.byKey(mode.theoryKey)
    LessonCard(title = stringResource(R.string.eight_modes_current_mode_title)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(mode.nameRes),
                style = MaterialTheme.typography.titleLarge,
                color = LbmTextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            GenusChip(mode.genus)
        }
        Spacer(Modifier.height(8.dp))

        TextSection(1, stringResource(R.string.mode_theory_section_apichima), formatApichima(mode))
        TextSection(
            2,
            stringResource(R.string.mode_theory_section_syllables_phthongs),
            formatApichimaSyllables(mode),
        )
        theory.styleRows.forEachIndexed { index, row ->
            StyleSection(number = index + 3, row = row)
        }
        TextSection(
            6,
            stringResource(R.string.mode_theory_section_modulations),
            stringResource(theory.modulationsRes),
        )
        TextSection(
            7,
            stringResource(R.string.mode_theory_section_attractions),
            stringResource(theory.attractionsRes),
        )
        TextSection(
            8,
            stringResource(R.string.mode_theory_section_phthores),
            stringResource(theory.phthoresRes),
        )
    }
}

@Composable
private fun GenusChip(genus: ModeScaleGenus) {
    val accent = ModeGenusPalette.accent(genus)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(accent.container)
            .border(1.dp, accent.content, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(
            text = stringResource(accent.nameRes),
            style = MaterialTheme.typography.labelLarge,
            color = accent.content,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SectionHeader(number: Int, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(LbmBrown),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = LbmBrown,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TextSection(number: Int, title: String, body: String) {
    Spacer(Modifier.height(12.dp))
    SectionHeader(number, title)
    Spacer(Modifier.height(4.dp))
    LinkedTheoryText(text = body, textSizeSp = 15f)
}

@Composable
private fun StyleSection(number: Int, row: ModeTheoryStyleRow) {
    Spacer(Modifier.height(12.dp))
    SectionHeader(number, stringResource(row.styleNameRes))
    Spacer(Modifier.height(6.dp))
    TheoryRow(stringResource(R.string.mode_theory_field_system), stringResource(row.systemRes))
    TheoryRow(
        stringResource(R.string.mode_theory_field_dominant_phthongs),
        stringResource(row.dominantsCadencesRes),
    )
    TheoryRow(
        stringResource(R.string.mode_theory_field_cadences),
        stringResource(row.dominantsCadencesRes),
    )
    TheoryRow(
        stringResource(R.string.mode_theory_field_final_cadences),
        stringResource(row.dominantsCadencesRes),
    )
}

@Composable
private fun TheoryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = LbmTextSecondary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.43f).padding(end = 8.dp),
        )
        Box(modifier = Modifier.weight(0.57f)) {
            LinkedTheoryText(text = value, textSizeSp = 14f)
        }
    }
}

/**
 * Renders teaching copy through the legacy [TheoryTopicLinks] so the tappable theory cross-links
 * (e.g. «πεντάχορδο», «(2)», «διαπασών») keep working exactly as before, inside Compose.
 */
@Composable
private fun LinkedTheoryText(text: String, textSizeSp: Float, modifier: Modifier = Modifier) {
    val primary = LbmTextPrimary.toArgb()
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(primary)
                textSize = textSizeSp
                val extra = 2f * ctx.resources.displayMetrics.density
                setLineSpacing(extra, 1.0f)
            }
        },
        update = { tv -> TheoryTopicLinks.setLinkedText(tv.context, tv, text) },
    )
}

/* ----------------------------- Apichima formatting ----------------------------- */

@Composable
private fun formatApichima(mode: EightModeUiModel): String =
    listOfNotNull(
        stringResource(mode.apichimaRes),
        mode.apichimaAlternativeRes?.let { stringResource(it) },
    ).joinToString(" / ")

@Composable
private fun formatApichimaSyllables(mode: EightModeUiModel): String {
    val primarySyllables = stringResource(mode.apichimaSyllablesRes)
    val alternativeApichimaRes = mode.apichimaAlternativeRes
    val alternativeSyllablesRes = mode.apichimaAlternativeSyllablesRes
    return if (alternativeApichimaRes != null && alternativeSyllablesRes != null) {
        stringResource(
            R.string.mode_theory_primary_alternative_template,
            stringResource(mode.apichimaRes),
            primarySyllables,
            stringResource(alternativeApichimaRes),
            stringResource(alternativeSyllablesRes),
        )
    } else {
        primarySyllables
    }
}
