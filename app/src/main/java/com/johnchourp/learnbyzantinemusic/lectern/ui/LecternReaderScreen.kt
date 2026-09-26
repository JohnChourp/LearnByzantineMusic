package com.johnchourp.learnbyzantinemusic.lectern.ui

import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.lectern.LecternIson
import com.johnchourp.learnbyzantinemusic.lectern.LecternNightTint
import com.johnchourp.learnbyzantinemusic.lectern.LecternOpenFailure
import com.johnchourp.learnbyzantinemusic.lectern.LecternPageTurns
import com.johnchourp.learnbyzantinemusic.lectern.LecternReaderViewModel
import com.johnchourp.learnbyzantinemusic.lectern.PageTurn
import com.johnchourp.learnbyzantinemusic.modes.ApichimaSequence
import com.johnchourp.learnbyzantinemusic.modes.IsonDrone
import com.johnchourp.learnbyzantinemusic.modes.ModeResources
import com.johnchourp.learnbyzantinemusic.modes.ui.EIGHT_MODES
import com.johnchourp.learnbyzantinemusic.modes.ui.greekStringResource
import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.Phthong
import com.johnchourp.learnbyzantinemusic.ui.components.LessonChip
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmOutline
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPageBg
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPalette
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPrimaryContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurface
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme
import com.johnchourp.learnbyzantinemusic.ui.theme.LocalLbmPalette
import kotlin.math.roundToInt

/** What the reader screen can ask of its host. */
class LecternReaderActions(
    val renderPage: suspend (pageIndex: Int, widthPx: Int) -> Bitmap?,
    val prefetchPage: (pageIndex: Int, widthPx: Int) -> Unit,
    val onTurn: (PageTurn) -> Unit,
    val onGoTo: (pageIndex: Int) -> Unit,
    val onNightChange: (Boolean) -> Unit,
    val onIsonChange: (Boolean) -> Unit,
    val onChooseMode: (Mode) -> Unit,
    val onChooseShift: (Int) -> Unit,
    val onChooseIson: (Phthong?) -> Unit,
    val onClearPage: () -> Unit,
    val onUsePageSetting: () -> Unit,
    val onPlayApichima: (List<ApichimaSequence.Step>) -> Unit,
    val onStopApichima: () -> Unit,
    val onRemoveFromLibrary: () -> Unit,
    val onBack: () -> Unit,
)

/** A drag at least this long turns the page; shorter ones only scroll. */
private val SWIPE_THRESHOLD = 56.dp

private val nightFilter = ColorFilter.colorMatrix(ColorMatrix(LecternNightTint.MATRIX))

/**
 * The lectern's reader (ClickUp `869f5x2e7`): the page as wide as the screen, a top bar (back, title,
 * ‹ page ›, night tint) and the ison bar under the page. A tap in the middle of the page hides both
 * bars and shows them again; the sides turn the page, as do a swipe, the volume keys and a pedal (the
 * activity reads the keys). A page taller than the screen scrolls, and starts at its top.
 *
 * The ison bar names the ήχος that holds on the page, the φθόγγος of its ison and its shift, and where
 * the setting comes from. A tap on it opens the page's setting — ήχος, «Μεταφορά βάσης», «Ίσον σε…» —
 * which applies from this page on. With the night tint on, the bars go dark too.
 */
@Composable
fun LecternReaderScreen(
    state: LecternReaderViewModel.State,
    title: String,
    actions: LecternReaderActions,
    modifier: Modifier = Modifier,
) {
    val base = LocalLbmPalette.current
    // The night tint darkens the whole reader, bars included, whatever the app's theme.
    LbmTheme(palette = if (state.night && !base.isDark) LbmPalette.dark else base) {
        var barsVisible by rememberSaveable { mutableStateOf(true) }
        var editing by rememberSaveable { mutableStateOf(false) }
        var pickingPage by rememberSaveable { mutableStateOf(false) }
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(LbmPageBg),
        ) {
            if (barsVisible || state.phase != LecternReaderViewModel.Phase.READY) {
                ReaderTopBar(
                    title = title,
                    state = state,
                    actions = actions,
                    onPickPage = { pickingPage = true },
                )
            }
            when (state.phase) {
                LecternReaderViewModel.Phase.OPENING -> Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = LbmBrown)
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.lectern_opening), color = LbmTextSecondary)
                    }
                }
                LecternReaderViewModel.Phase.FAILED -> FailureMessage(
                    failure = state.failure ?: LecternOpenFailure.UNREADABLE,
                    onRemove = actions.onRemoveFromLibrary,
                    modifier = Modifier.weight(1f),
                )
                LecternReaderViewModel.Phase.READY -> {
                    PageView(
                        state = state,
                        actions = actions,
                        onToggleBars = { barsVisible = !barsVisible },
                        modifier = Modifier.weight(1f),
                    )
                    if (barsVisible) {
                        IsonBar(state = state, actions = actions, onEdit = { editing = true })
                    }
                }
            }
        }
        if (editing && state.phase == LecternReaderViewModel.Phase.READY) {
            PageSettingDialog(state = state, actions = actions, onDismiss = { editing = false })
        }
        if (pickingPage && state.pageCount > 1) {
            GoToPageDialog(
                pageIndex = state.pageIndex,
                pageCount = state.pageCount,
                onGoTo = actions.onGoTo,
                onDismiss = { pickingPage = false },
            )
        }
    }
}

/* ----------------------------- The page ----------------------------- */

private data class ShownPage(val pageIndex: Int, val image: ImageBitmap)

@Composable
private fun PageView(
    state: LecternReaderViewModel.State,
    actions: LecternReaderActions,
    onToggleBars: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val turn by rememberUpdatedState(actions.onTurn)
    val toggleBars by rememberUpdatedState(onToggleBars)
    val pageIndex = state.pageIndex
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val widthPx = constraints.maxWidth
        val viewportHeight = maxHeight
        // The page on screen stays until the next one is drawn, so a turn never flashes empty.
        var shown by remember { mutableStateOf<ShownPage?>(null) }
        var failed by remember(pageIndex, widthPx) { mutableStateOf(false) }
        LaunchedEffect(pageIndex, widthPx) {
            val bitmap = actions.renderPage(pageIndex, widthPx)
            if (bitmap != null) {
                shown = ShownPage(pageIndex, bitmap.asImageBitmap())
            } else {
                failed = true
            }
            actions.prefetchPage(pageIndex + 1, widthPx)
            actions.prefetchPage(pageIndex - 1, widthPx)
        }
        val scroll = rememberScrollState()
        LaunchedEffect(pageIndex) { scroll.scrollTo(0) }
        val threshold = with(LocalDensity.current) { SWIPE_THRESHOLD.toPx() }
        val nextLabel = stringResource(R.string.lectern_next_page)
        val previousLabel = stringResource(R.string.lectern_previous_page)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        when (LecternPageTurns.forTap(offset.x, size.width.toFloat())) {
                            LecternPageTurns.TapZone.PREVIOUS -> turn(PageTurn.PREVIOUS)
                            LecternPageTurns.TapZone.NEXT -> turn(PageTurn.NEXT)
                            LecternPageTurns.TapZone.BARS -> toggleBars()
                        }
                    }
                }
                .pointerInput(threshold) {
                    var dragged = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragged = 0f },
                        onDragEnd = { LecternPageTurns.forSwipe(dragged, threshold)?.let(turn) },
                    ) { change, amount ->
                        dragged += amount
                        change.consume()
                    }
                }
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(nextLabel) { turn(PageTurn.NEXT); true },
                        CustomAccessibilityAction(previousLabel) { turn(PageTurn.PREVIOUS); true },
                    )
                }
                .verticalScroll(scroll)
                .heightIn(min = viewportHeight),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val page = shown
            when {
                failed && page?.pageIndex != pageIndex -> Text(
                    text = stringResource(R.string.lectern_page_failed, pageIndex + 1),
                    color = LbmTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
                page == null -> CircularProgressIndicator(color = LbmBrown)
                else -> Image(
                    bitmap = page.image,
                    contentDescription = stringResource(R.string.lectern_page_cd, page.pageIndex + 1, state.pageCount),
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth,
                    colorFilter = if (state.night) nightFilter else null,
                )
            }
        }
    }
}

@Composable
private fun FailureMessage(failure: LecternOpenFailure, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(failureText(failure)),
            style = MaterialTheme.typography.bodyLarge,
            color = LbmTextPrimary,
            textAlign = TextAlign.Center,
        )
        // A grant that is gone, or a file that is: the entry can only be removed and the PDF added again.
        if (failure == LecternOpenFailure.NO_ACCESS || failure == LecternOpenFailure.NOT_FOUND) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRemove, colors = ButtonDefaults.buttonColors(containerColor = LbmBrown)) {
                Text(stringResource(R.string.lectern_remove_from_library))
            }
        }
    }
}

@StringRes
private fun failureText(failure: LecternOpenFailure): Int = when (failure) {
    LecternOpenFailure.NO_ACCESS -> R.string.lectern_failure_no_access
    LecternOpenFailure.NOT_FOUND -> R.string.lectern_failure_not_found
    LecternOpenFailure.PASSWORD_PROTECTED -> R.string.lectern_failure_password
    LecternOpenFailure.DAMAGED -> R.string.lectern_failure_damaged
    LecternOpenFailure.UNREADABLE -> R.string.lectern_failure_unreadable
}

/* ----------------------------- The top bar ----------------------------- */

@Composable
private fun ReaderTopBar(
    title: String,
    state: LecternReaderViewModel.State,
    actions: LecternReaderActions,
    onPickPage: () -> Unit,
) {
    val ready = state.phase == LecternReaderViewModel.Phase.READY
    Surface(color = LbmSurface, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = actions.onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = LbmBrown,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = LbmTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (ready) {
                IconButton(onClick = { actions.onTurn(PageTurn.PREVIOUS) }, enabled = state.pageIndex > 0) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                        contentDescription = stringResource(R.string.lectern_previous_page),
                    )
                }
                TextButton(onClick = onPickPage, enabled = state.pageCount > 1) {
                    Text(
                        text = stringResource(R.string.lectern_page_of, state.pageIndex + 1, state.pageCount),
                        color = LbmBrown,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                IconButton(onClick = { actions.onTurn(PageTurn.NEXT) }, enabled = state.pageIndex < state.pageCount - 1) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                        contentDescription = stringResource(R.string.lectern_next_page),
                    )
                }
                IconButton(onClick = { actions.onNightChange(!state.night) }) {
                    Icon(
                        imageVector = if (state.night) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = stringResource(
                            if (state.night) R.string.lectern_night_off else R.string.lectern_night_on,
                        ),
                        tint = LbmBrown,
                    )
                }
            }
        }
    }
}

@Composable
private fun GoToPageDialog(pageIndex: Int, pageCount: Int, onGoTo: (Int) -> Unit, onDismiss: () -> Unit) {
    var target by remember { mutableFloatStateOf((pageIndex + 1).toFloat()) }
    val page = target.roundToInt()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lectern_go_to_page)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.lectern_page_of, page, pageCount),
                    style = MaterialTheme.typography.titleMedium,
                    color = LbmBrown,
                    fontWeight = FontWeight.Bold,
                )
                Slider(
                    value = target,
                    onValueChange = { target = it },
                    valueRange = 1f..pageCount.toFloat(),
                    colors = SliderDefaults.colors(thumbColor = LbmBrown, activeTrackColor = LbmBrown, inactiveTrackColor = LbmOutline),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onGoTo(page - 1)
                onDismiss()
            }) { Text(stringResource(R.string.lectern_go)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.learning_data_cancel)) } },
    )
}

/* ----------------------------- The ison bar ----------------------------- */

@Composable
private fun IsonBar(state: LecternReaderViewModel.State, actions: LecternReaderActions, onEdit: () -> Unit) {
    val sounding = state.sounding
    Surface(color = LbmSurface, shadowElevation = 6.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            if (state.mapReadOnly) {
                Text(
                    text = stringResource(R.string.lectern_map_newer),
                    style = MaterialTheme.typography.bodySmall,
                    color = LbmTextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
            if (state.moved != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.lectern_playing_background_ison),
                        style = MaterialTheme.typography.bodySmall,
                        color = LbmTextSecondary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                    )
                    // Back to the page's own ison — when the page has one to go back to.
                    if (state.holding != null) {
                        TextButton(onClick = actions.onUsePageSetting) {
                            Text(stringResource(R.string.lectern_use_page_setting), color = LbmBrown)
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledIconToggleButton(
                    checked = state.isonOn,
                    onCheckedChange = actions.onIsonChange,
                    enabled = sounding != null,
                ) {
                    Icon(imageVector = Icons.Filled.GraphicEq, contentDescription = stringResource(R.string.lectern_ison_toggle))
                }
                Spacer(Modifier.width(8.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !state.mapReadOnly, onClick = onEdit)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = sounding?.let { stringResource(ModeResources.nameRes(it.mode)) }
                            ?: stringResource(R.string.lectern_set_mode),
                        style = MaterialTheme.typography.titleSmall,
                        color = LbmTextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = barDetail(state),
                        style = MaterialTheme.typography.bodySmall,
                        color = LbmTextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (sounding != null) {
                    ApichimaButton(request = sounding, state = state, actions = actions)
                }
            }
        }
    }
}

/** «Ίσον σε Κε · +2 μόρια · από τη σελ. 3», or what to do when the page has no ήχος yet. */
@Composable
private fun barDetail(state: LecternReaderViewModel.State): String {
    val sounding = state.sounding ?: return stringResource(R.string.lectern_set_mode_hint)
    val parts = mutableListOf<String>()
    LecternIson.held(sounding)?.let { parts += stringResource(R.string.lectern_bar_ison, it.phthong.label) }
    if (sounding.baseShiftMoria != 0) {
        parts += stringResource(R.string.eight_modes_base_shift_value_template, sounding.baseShiftMoria)
    }
    val holding = state.holding
    if (state.moved == null && holding != null) {
        parts += if (state.setOnThisPage) {
            stringResource(R.string.lectern_bar_set_here)
        } else {
            stringResource(R.string.lectern_bar_from_page, holding.pageIndex + 1)
        }
    }
    return parts.joinToString(" · ")
}

/**
 * «Απήχημα» of the ήχος that sounds, at its shift. Its pitches are read from the **Greek** syllables,
 * whatever the language, exactly as the 8 Ήχοι page reads them (ClickUp `869f5x281`).
 */
@Composable
private fun ApichimaButton(request: IsonDrone.Request, state: LecternReaderViewModel.State, actions: LecternReaderActions) {
    val row = EIGHT_MODES.first { it.mode == request.mode }
    val greekText = greekStringResource(row.apichimaSyllablesRes)
    val shownText = stringResource(row.apichimaSyllablesRes)
    val steps = remember(greekText, shownText) { ApichimaSequence.playable(greekText, shownText) }
    val playable = remember(request, steps) { LecternIson.apichimaFrequencies(request, steps) != null }
    val playing = state.apichimaStep >= 0
    TextButton(
        onClick = { if (playing) actions.onStopApichima() else actions.onPlayApichima(steps) },
        enabled = playable,
        colors = ButtonDefaults.textButtonColors(contentColor = LbmBrown),
    ) {
        Icon(imageVector = if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null)
        Spacer(Modifier.width(4.dp))
        Text(
            text = if (playing) {
                steps.getOrNull(state.apichimaStep)?.syllable ?: stringResource(R.string.eight_modes_apichima_stop)
            } else {
                stringResource(R.string.lectern_apichima)
            },
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/* ----------------------------- The page's setting ----------------------------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PageSettingDialog(state: LecternReaderViewModel.State, actions: LecternReaderActions, onDismiss: () -> Unit) {
    val holding = state.holding
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lectern_setting_title, state.pageIndex + 1)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.lectern_setting_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = LbmTextSecondary,
                )
                SettingLabel(R.string.lectern_setting_mode)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // In the order of the cycle, as a chanter reads the ήχος off the book.
                    Mode.entries.forEach { mode ->
                        LessonChip(
                            label = stringResource(EIGHT_MODES.first { it.mode == mode }.selectorNameRes),
                            selected = holding?.mode == mode,
                            onClick = { if (holding?.mode != mode) actions.onChooseMode(mode) },
                        )
                    }
                }
                if (holding != null) {
                    SettingLabel(R.string.eight_modes_base_shift_title)
                    ShiftSlider(moria = holding.baseShiftMoria, onChange = actions.onChooseShift)
                    SettingLabel(R.string.eight_modes_ison_selector_label)
                    IsonChoiceSelector(request = holding.request, onChoose = actions.onChooseIson)
                    // Only a page's own setting can be removed; the page then follows the pages before it.
                    if (state.setOnThisPage) {
                        TextButton(onClick = actions.onClearPage) {
                            Text(stringResource(R.string.lectern_setting_clear), color = LbmBrown)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.lectern_setting_done)) } },
    )
}

@Composable
private fun SettingLabel(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.labelLarge,
        color = LbmTextPrimary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.semantics { heading() },
    )
}

/** The «Μεταφορά βάσης» of the page's setting, over the range the 8 Ήχοι page offers ([BaseShift]). */
@Composable
private fun ShiftSlider(moria: Int, onChange: (Int) -> Unit) {
    val valueText = if (moria == 0) {
        stringResource(R.string.eight_modes_base_shift_default_value)
    } else {
        stringResource(R.string.eight_modes_base_shift_value_template, moria)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = valueText,
            style = MaterialTheme.typography.labelLarge,
            color = LbmBrown,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { onChange(BaseShift.DEFAULT_MORIA) }, enabled = moria != BaseShift.DEFAULT_MORIA) {
            Icon(imageVector = Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.eight_modes_base_shift_reset))
        }
    }
    Slider(
        value = moria.toFloat(),
        onValueChange = { onChange(it.roundToInt()) },
        modifier = Modifier.semantics { stateDescription = valueText },
        valueRange = BaseShift.MIN_MORIA.toFloat()..BaseShift.MAX_MORIA.toFloat(),
        steps = (BaseShift.MAX_MORIA - BaseShift.MIN_MORIA) - 1,
        colors = SliderDefaults.colors(thumbColor = LbmBrown, activeTrackColor = LbmBrown, inactiveTrackColor = LbmOutline),
    )
}

/** «Ίσον σε…» for the page's ήχος: its δεσπόζοντες, the other φθόγγοι, and back to the base. */
@Composable
private fun IsonChoiceSelector(request: IsonDrone.Request, onChoose: (Phthong?) -> Unit) {
    val choices = remember(request.mode, request.baseShiftMoria) { LecternIson.choices(request) } ?: return
    val held = LecternIson.held(request)?.phthong ?: return
    var expanded by remember { mutableStateOf(false) }
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
                text = if (held == choices.base) {
                    stringResource(R.string.eight_modes_ison_selector_base_value, held.label)
                } else {
                    held.label
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
            if (choices.dominants.isNotEmpty()) {
                MenuSection(R.string.eight_modes_ison_selector_dominants)
                choices.dominants.forEach { phthong -> MenuPhthong(phthong, held = phthong == held) { choose(phthong) } }
            }
            if (choices.others.isNotEmpty()) {
                MenuSection(R.string.eight_modes_ison_selector_others)
                choices.others.forEach { phthong -> MenuPhthong(phthong, held = phthong == held) { choose(phthong) } }
            }
            HorizontalDivider(color = LbmOutline)
            DropdownMenuItem(
                onClick = { choose(null) },
                enabled = held != choices.base,
                leadingIcon = { Icon(imageVector = Icons.Filled.Restore, contentDescription = null) },
                text = { Text(stringResource(R.string.eight_modes_ison_reset_to_base, choices.base.label)) },
            )
        }
    }
}

@Composable
private fun MenuSection(@StringRes title: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.labelMedium,
        color = LbmTextSecondary,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .semantics { heading() },
    )
}

@Composable
private fun MenuPhthong(phthong: Phthong, held: Boolean, onClick: () -> Unit) {
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
