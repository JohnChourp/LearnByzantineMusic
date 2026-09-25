package com.johnchourp.learnbyzantinemusic.modes.ui

import android.content.res.Configuration
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.johnchourp.learnbyzantinemusic.AppLanguage
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.modes.IsonDrone
import com.johnchourp.learnbyzantinemusic.modes.LadderPitchMirror
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
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGreenContent
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmOutline
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPageBg
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPrimaryContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurface
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.material3.Switch
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import com.johnchourp.learnbyzantinemusic.modes.ApichimaSequence
import com.johnchourp.learnbyzantinemusic.music.ModeLadder
import com.johnchourp.learnbyzantinemusic.music.Moria
import com.johnchourp.learnbyzantinemusic.music.Phthong
import java.util.Locale

/**
 * Internal rather than private so `ApichimaInEveryLanguageTest` resolves the απήχημα on the very
 * ladder this screen builds.
 */
internal const val SCALE_OCTAVES = 3

/**
 * The «Μεταφορά βάσης» range the slider offers, in μόρια. Internal so the ison tests sweep exactly
 * this range, whatever it becomes, rather than a copy of today's numbers.
 */
internal const val BASE_SHIFT_MIN = -12
internal const val BASE_SHIFT_MAX = 12

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
    /** Non-null starts (or retunes) the ison drone; null stops it. */
    onDroneChange: (Double?) -> Unit,
    /**
     * Asks the host to start listening (true) or stop (false). The host owns the microphone
     * permission and the capture engine; the screen only says when it wants to hear.
     */
    onListenChange: (Boolean) -> Unit,
    /** The pitch the host last heard, in Hz, or null when nothing usable is coming in. */
    heardFrequencyHz: Double?,
    /** True once the host has been refused the microphone, so the card can say so. */
    micDenied: Boolean,
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
    var droneOn by remember { mutableStateOf(false) }
    // Where «Ίσον σε…» moved the ison; null means the mode's base. Keyed on the mode, so a new ήχος
    // starts on its own base, and never persisted (ClickUp `869f5x251`).
    var isonChoice by remember(selectedModeIndex) { mutableStateOf<Phthong?>(null) }
    var listening by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Switching modes never carries a held tone over.
    LaunchedEffect(selectedModeIndex) {
        activeIndex = -1
        onToneRelease()
    }

    // The drone's pitch is derived, not stored: whenever the mode, its base shift or the chosen
    // φθόγγος changes it is recomputed and the drone retuned in place, so it can never keep sounding
    // the previous mode's note. Switching off, or leaving the composition, stops it — that is what
    // keeps the second AudioTrack from outliving the screen.
    val ison = rememberIson(selectedModeIndex, baseShiftByMode[selectedModeIndex] ?: 0, isonChoice)
    LaunchedEffect(droneOn, ison?.held?.frequencyHz) {
        onDroneChange(if (droneOn) ison?.held?.frequencyHz else null)
    }
    DisposableEffect(Unit) {
        onDispose { onDroneChange(null) }
    }

    // The microphone follows the same rule as the drone: derived from one flag, and released when
    // the screen leaves. A capture thread outliving the composition is the bug this prevents.
    LaunchedEffect(listening) { onListenChange(listening) }
    DisposableEffect(Unit) {
        onDispose { onListenChange(false) }
    }
    // A refusal turns the switch back off, so it cannot sit on while nothing is being heard.
    LaunchedEffect(micDenied) { if (micDenied) listening = false }

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
            StaggeredAppear(delayMillis = 210) {
                IsonDroneCard(
                    enabled = droneOn,
                    ison = ison,
                    onToggle = { droneOn = it },
                    onChoose = { isonChoice = it },
                )
            }
            StaggeredAppear(delayMillis = 225) {
                PitchMirrorCard(
                    listening = listening,
                    micDenied = micDenied,
                    reading = rememberMirrorReading(
                        modeIndex = selectedModeIndex,
                        baseShiftMoria = baseShiftByMode[selectedModeIndex] ?: 0,
                        heardFrequencyHz = if (listening) heardFrequencyHz else null,
                    ),
                    onToggle = { listening = it },
                )
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
                ) { idx ->
                    ModeDetailsCard(
                        modeIndex = idx,
                        baseShiftMoria = baseShiftByMode[idx] ?: 0,
                        onTonePress = onTonePress,
                        onToneRelease = onToneRelease,
                        scope = scope,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/* ----------------------------- Genus legend ----------------------------- */

@OptIn(ExperimentalLayoutApi::class)
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
    // One ladder, built once: φθόγγοι and their pitches together, highest first. Before this, the
    // diagram, the drone and the απήχημα each rebuilt the same thing and could drift apart.
    val ladder = rememberLadder(modeIndex, baseShiftMoria)
    val phthongsTopToBottom = ladder.labels
    val intervalsTopToBottom = remember(modeIndex) {
        scale.repeatedIntervals(SCALE_OCTAVES).reversed()
    }
    // Tonic rungs are found by φθόγγος NAME across every octave — that is what the diagram marks.
    // Matching the typed name, rather than trimming suffixes off a label, is the difference between
    // "the same note in any octave" and "any label that happens to start the same way".
    val tonicName = scale.base.base.name
    val tonicIndices = remember(modeIndex) {
        ladder.steps.indices.filter { i -> ladder.steps[i].phthong.name == tonicName }.toSet()
    }
    val frequencies = ladder.frequencies
    // Tracks the in-flight TalkBack tone pulse so a newer activation cancels the older one's delayed
    // release — otherwise an earlier pulse could stop a note that a later activation is still sounding.
    val pulseJob = remember { mutableStateOf<Job?>(null) }

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
                    pulseJob.value?.cancel()
                    pulseJob.value = scope.launch {
                        onAccessibilityPulse(freq)
                        delay(650)
                        onAccessibilityRelease()
                    }
                }
            },
        )
    }
}

/**
 * The ladder for a mode at a given base shift, memoised.
 *
 * Every pitch on this screen comes from here: the diagram, the ison and the απήχημα. Keeping it in
 * one place is what makes "the drone follows the μεταφορά βάσης" and "the απήχημα plays the notes
 * the diagram shows" structurally true rather than three independent implementations that happen to
 * agree today.
 */
@Composable
private fun rememberLadder(modeIndex: Int, baseShiftMoria: Int): ModeLadder {
    val scale = EIGHT_MODES[modeIndex].scale
    return remember(modeIndex, baseShiftMoria) {
        scale.ladder(octaves = SCALE_OCTAVES, baseShift = Moria(baseShiftMoria))
    }
}

/* ----------------------------- Ισοκράτημα (ison drone) ----------------------------- */

/** What the ison can hold for the current mode, and the rung it holds now. */
private data class IsonState(val choices: IsonDrone.Choices, val held: ModeLadder.Step) {
    val onBase: Boolean get() = held.phthong == choices.base
}

/**
 * Resolves the ison from the same ladder the diagram is drawn from, so the drone and the diagram's
 * key cannot disagree — including after the «Μεταφορά βάσης» slider moves them. [choice] null means
 * the mode's base.
 */
@Composable
private fun rememberIson(modeIndex: Int, baseShiftMoria: Int, choice: Phthong?): IsonState? {
    val mode = EIGHT_MODES[modeIndex].mode
    val ladder = rememberLadder(modeIndex, baseShiftMoria)
    return remember(ladder, mode, choice) {
        val choices = mode?.let { IsonDrone.choices(it, ladder) } ?: return@remember null
        IsonDrone.step(ladder, choice ?: choices.base)?.let { held -> IsonState(choices, held) }
    }
}

/**
 * Toggle for the continuous drone, naming the φθόγγος it holds so the singer knows what they are
 * chanting against, and «Ίσον σε…» to move it (ClickUp `869f5x251`).
 *
 * The move is a menu rather than a tap on the diagram, because a tap there already plays a tone.
 * The mode's δεσπόζοντες come first, then the other φθόγγοι, then the way back to the base.
 */
@Composable
private fun IsonDroneCard(
    enabled: Boolean,
    ison: IsonState?,
    onToggle: (Boolean) -> Unit,
    onChoose: (Phthong?) -> Unit,
) {
    LessonCard(title = stringResource(R.string.eight_modes_ison_card_title)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (enabled && ison != null) {
                        stringResource(R.string.eight_modes_ison_active, ison.held.phthong.label)
                    } else {
                        stringResource(R.string.eight_modes_ison_hint)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) LbmBrown else LbmTextSecondary,
                    fontWeight = if (enabled) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                enabled = ison != null,
            )
        }
        if (ison != null) {
            Spacer(Modifier.height(10.dp))
            IsonNoteSelector(ison = ison, onChoose = onChoose)
        }
    }
}

/** «Ίσον σε…»: the φθόγγος the ison holds, and the menu that moves it. */
@Composable
private fun IsonNoteSelector(ison: IsonState, onChoose: (Phthong?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val heldLabel = ison.held.phthong.label
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LbmPrimaryContainer)
                .border(1.dp, LbmBrown.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.eight_modes_ison_selector_label),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (ison.onBase) {
                    stringResource(R.string.eight_modes_ison_selector_base_value, heldLabel)
                } else {
                    heldLabel
                },
                style = MaterialTheme.typography.titleMedium,
                color = LbmBrown,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = stringResource(R.string.eight_modes_open_selector),
                tint = LbmBrown,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            fun choose(phthong: Phthong?) {
                expanded = false
                onChoose(phthong)
            }
            if (ison.choices.dominants.isNotEmpty()) {
                IsonMenuSection(R.string.eight_modes_ison_selector_dominants)
                ison.choices.dominants.forEach { phthong ->
                    IsonMenuItem(phthong, held = phthong == ison.held.phthong, onClick = { choose(phthong) })
                }
            }
            if (ison.choices.others.isNotEmpty()) {
                IsonMenuSection(R.string.eight_modes_ison_selector_others)
                ison.choices.others.forEach { phthong ->
                    IsonMenuItem(phthong, held = phthong == ison.held.phthong, onClick = { choose(phthong) })
                }
            }
            HorizontalDivider(color = LbmOutline)
            DropdownMenuItem(
                onClick = { choose(null) },
                enabled = !ison.onBase,
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Restore, contentDescription = null)
                },
                text = {
                    Text(stringResource(R.string.eight_modes_ison_reset_to_base, ison.choices.base.label))
                },
            )
        }
    }
}

@Composable
private fun IsonMenuSection(@StringRes titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.labelMedium,
        color = LbmTextSecondary,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .semantics { heading() },
    )
}

@Composable
private fun IsonMenuItem(phthong: Phthong, held: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        onClick = onClick,
        text = {
            Text(
                text = phthong.label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (held) LbmBrown else LbmTextPrimary,
                fontWeight = if (held) FontWeight.Bold else FontWeight.Normal,
            )
        },
    )
}

/* ----------------------------- Ζωντανός καθρέφτης φωνής ----------------------------- */

/**
 * Reads [heardFrequencyHz] against the **same ladder** the diagram, the drone and the απήχημα use,
 * so the indicator cannot disagree with what the screen is showing and sounding.
 *
 * Re-reads on every new frequency; the ladder itself is only rebuilt when the mode or its base
 * shift changes.
 */
@Composable
private fun rememberMirrorReading(
    modeIndex: Int,
    baseShiftMoria: Int,
    heardFrequencyHz: Double?,
): LadderPitchMirror.Reading? {
    val ladder = rememberLadder(modeIndex, baseShiftMoria)
    return remember(ladder, heardFrequencyHz) {
        heardFrequencyHz?.let { LadderPitchMirror.read(ladder, it) }
    }
}

/**
 * «Πού είμαι» — the other half of the ison (ClickUp `869f4tqad`, E1).
 *
 * The drone gives the singer something to chant against; this says whether they are on it, in
 * **μόρια** rather than cents. Deliberately placed straight after [IsonDroneCard]: the two are one
 * exercise, and the research that asked for this named "the student chanting alone does not know
 * they have drifted" as the first obstacle.
 *
 * Everything is on-device — the existing YIN detector, no cloud, no upload.
 */
@Composable
private fun PitchMirrorCard(
    listening: Boolean,
    micDenied: Boolean,
    reading: LadderPitchMirror.Reading?,
    onToggle: (Boolean) -> Unit,
) {
    val message: String = when {
        micDenied -> stringResource(R.string.eight_modes_mirror_denied)
        !listening -> stringResource(R.string.eight_modes_mirror_hint)
        reading == null -> stringResource(R.string.eight_modes_mirror_listening)
        reading.isInTune() -> stringResource(R.string.eight_modes_mirror_on_pitch, reading.label)
        reading.deviationMoria > 0 ->
            stringResource(R.string.eight_modes_mirror_sharp, reading.label, reading.deviationMoria.roundToInt())
        else ->
            stringResource(R.string.eight_modes_mirror_flat, reading.label, -reading.deviationMoria.roundToInt())
    }
    val onPitch = listening && reading != null && reading.isInTune()

    LessonCard(title = stringResource(R.string.eight_modes_mirror_card_title)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (onPitch) AccentGreenContent else if (listening) LbmBrown else LbmTextSecondary,
                fontWeight = if (listening) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = message },
            )
            Spacer(Modifier.width(12.dp))
            Switch(checked = listening, onCheckedChange = onToggle)
        }
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
private fun ModeDetailsCard(
    modeIndex: Int,
    baseShiftMoria: Int,
    onTonePress: (Double) -> Unit,
    onToneRelease: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
) {
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
        ApichimaPlayer(
            modeIndex = modeIndex,
            baseShiftMoria = baseShiftMoria,
            onTonePress = onTonePress,
            onToneRelease = onToneRelease,
            scope = scope,
        )
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

/* ----------------------------- Apichima playback ----------------------------- */

/**
 * «Άκου το απήχημα» — plays the whole intonation formula on one tap, highlighting the syllable
 * currently sounding (ClickUp `869f4tpkv`).
 *
 * The sequence is parsed out of the same teaching string the section below renders, so text and
 * sound cannot disagree. Pitches are looked up in the diagram's own frequency list, which is why the
 * playback transposes with «Μεταφορά βάσης» for free.
 *
 * The pitches are read from the **Greek** copy of that string in every language, and only the
 * syllables from the user's own (ClickUp `869f5x281`). Reading the displayed string made the English
 * page silent: it spells the φθόγγοι in Latin letters, which name no φθόγγος.
 *
 * Stopping is handled in three places on purpose, because each is a real way to leave: the button
 * itself, a mode change (the effect key), and disposal (leaving the screen or backgrounding it).
 */
@Composable
private fun ApichimaPlayer(
    modeIndex: Int,
    baseShiftMoria: Int,
    onTonePress: (Double) -> Unit,
    onToneRelease: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val mode = EIGHT_MODES[modeIndex]
    val greekText = greekStringResource(mode.apichimaSyllablesRes)
    val shownText = stringResource(mode.apichimaSyllablesRes)

    val steps = remember(greekText, shownText) { ApichimaSequence.playable(greekText, shownText) }
    val ladder = rememberLadder(modeIndex, baseShiftMoria)
    val tones = remember(ladder, steps) { ApichimaSequence.frequencies(steps, ladder) }

    var playingIndex by remember { mutableStateOf(-1) }
    val job = remember { mutableStateOf<Job?>(null) }

    fun stop() {
        job.value?.cancel()
        job.value = null
        playingIndex = -1
        onToneRelease()
    }

    fun play(speed: ApichimaSequence.Speed) {
        val frequencies = tones ?: return
        job.value?.cancel()
        job.value = scope.launch {
            try {
                frequencies.forEachIndexed { index, hz ->
                    playingIndex = index
                    onTonePress(hz)
                    delay(speed.millisPerStep)
                }
            } finally {
                // Runs on normal completion AND on cancellation, so a stop mid-phrase never leaves
                // a tone sounding.
                playingIndex = -1
                onToneRelease()
            }
        }
    }

    // A mode change must not leave the previous mode's απήχημα playing.
    LaunchedEffect(modeIndex) { stop() }
    DisposableEffect(Unit) { onDispose { stop() } }

    if (steps.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Both buttons share one `enabled`, and their colours go through the button, never onto
            // the Icon or Text: a colour set there overrides the disabled look, which is how a button
            // that could not play used to look ready to.
            TextButton(
                onClick = { if (playingIndex >= 0) stop() else play(ApichimaSequence.Speed.SHORT) },
                enabled = tones != null,
                colors = ButtonDefaults.textButtonColors(contentColor = LbmBrown),
            ) {
                Icon(
                    imageVector = if (playingIndex >= 0) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = null,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        if (playingIndex >= 0) R.string.eight_modes_apichima_stop
                        else R.string.eight_modes_apichima_play_short
                    ),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.width(4.dp))
            TextButton(
                onClick = { play(ApichimaSequence.Speed.SLOW) },
                enabled = tones != null,
                colors = ButtonDefaults.textButtonColors(contentColor = LbmTextSecondary),
            ) {
                Text(stringResource(R.string.eight_modes_apichima_play_slow))
            }
        }
        // The syllables, with the sounding one lit. Same steps as the sequence, so the highlight
        // cannot point at a different syllable than the one being heard.
        Row(modifier = Modifier.fillMaxWidth()) {
            steps.forEachIndexed { index, step ->
                val active = index == playingIndex
                Text(
                    text = step.syllable,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (active) LbmBrown else LbmTextSecondary,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(end = 10.dp),
                )
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}

/**
 * [id] as the Greek resources spell it, whatever language the UI is in (ClickUp `869f5x281`).
 *
 * For a string the screen computes with rather than only shows — the απήχημα's φθόγγοι, which a
 * translation spells in its own alphabet. Unlike [AppLanguage.wrapContextWithLocale] this leaves the
 * process-wide default locale alone: it reads one string, it does not switch the UI's language.
 */
@Composable
private fun greekStringResource(@StringRes id: Int): String {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(context, configuration, id) {
        val greek = Configuration(configuration)
        greek.setLocale(Locale.forLanguageTag(AppLanguage.languageGreek))
        context.createConfigurationContext(greek).getString(id)
    }
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
