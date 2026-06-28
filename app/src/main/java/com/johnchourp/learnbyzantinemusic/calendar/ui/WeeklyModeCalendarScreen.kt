package com.johnchourp.learnbyzantinemusic.calendar.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.calendar.CalendarCelebrationType
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

/**
 * Redesigned «Ημερολόγιο Ήχου Εβδομάδας» screen. A pure renderer of [WeeklyModeCalendarUiState] +
 * callbacks: a hero, the month-navigation + animated day grid, a legend that explains every marker,
 * the weekly tone, the day's εορτολόγιο with type badges, and the day's αναγνώσματα as tappable rows.
 * All date/tone/celebration logic stays in the host Activity.
 */
@Composable
fun WeeklyModeCalendarScreen(
    state: WeeklyModeCalendarUiState,
    onBack: () -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onPickMonth: (year: Int, month: Int) -> Unit,
    onOpenReading: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LbmPageBg)
            .verticalScroll(scroll),
    ) {
        LessonHero(
            title = stringResource(R.string.title_activity_weekly_mode_calendar),
            subtitle = stringResource(R.string.weekly_mode_calendar_subtitle),
            onBack = onBack,
            icon = Icons.Filled.CalendarMonth,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            StaggeredAppear(delayMillis = 60) {
                CalendarCard(
                    state = state,
                    onPrevMonth = onPrevMonth,
                    onNextMonth = onNextMonth,
                    onToday = onToday,
                    onSelectDay = onSelectDay,
                    onPickMonth = onPickMonth,
                )
            }
            StaggeredAppear(delayMillis = 120) { LegendCard() }
            StaggeredAppear(delayMillis = 180) { ToneCard(state) }
            StaggeredAppear(delayMillis = 240) { CelebrationsCard(state) }
            StaggeredAppear(delayMillis = 300) { ReadingsCard(state, onOpenReading) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/* ----------------------------------------------------------------------------------------------- */
/* Calendar card: month navigation + animated day grid                                             */
/* ----------------------------------------------------------------------------------------------- */

@Composable
private fun CalendarCard(
    state: WeeklyModeCalendarUiState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onPickMonth: (Int, Int) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = LbmSurface, contentColor = LbmTextPrimary),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            MonthNavRow(
                monthLabel = state.monthLabel,
                monthContentDescription = state.monthContentDescription,
                onPrevMonth = onPrevMonth,
                onNextMonth = onNextMonth,
                onToday = onToday,
                onOpenPicker = { showPicker = true },
            )
            Spacer(Modifier.height(14.dp))
            WeekdayHeader()
            Spacer(Modifier.height(8.dp))
            AnimatedContent(
                targetState = state.monthGrid,
                transitionSpec = {
                    val forward = targetState.monthKey >= initialState.monthKey
                    val dir = if (forward) 1 else -1
                    (slideInHorizontally(tween(320)) { w -> dir * w } + fadeIn(tween(320))) togetherWith
                        (slideOutHorizontally(tween(320)) { w -> -dir * w } + fadeOut(tween(320)))
                },
                label = "monthGrid",
            ) { grid ->
                // Only the in-month (steady / incoming) grid shows the selection; the outgoing
                // month frame passes -1 so it doesn't flash the new month's selected day mid-slide.
                val gridSelectedDay = if (grid.monthKey == state.monthGrid.monthKey) state.selectedDay else -1
                CalendarGrid(grid = grid, selectedDay = gridSelectedDay, onSelectDay = onSelectDay)
            }
        }
    }

    if (showPicker) {
        MonthYearPickerDialog(
            initialYear = state.visibleYear,
            initialMonth = state.visibleMonthValue,
            minYear = state.minYear,
            maxYear = state.maxYear,
            monthNames = state.monthNames,
            onConfirm = { year, month ->
                onPickMonth(year, month)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun MonthNavRow(
    monthLabel: String,
    monthContentDescription: String,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onOpenPicker: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onPrevMonth) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = stringResource(R.string.weekly_mode_calendar_prev_month),
                tint = LbmBrown,
            )
        }
        Surface(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenPicker)
                .semantics { contentDescription = monthContentDescription },
            color = LbmPrimaryContainer,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, LbmOutline),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = LbmTextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = LbmTextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = stringResource(R.string.weekly_mode_calendar_next_month),
                tint = LbmBrown,
            )
        }
        TodayPill(onToday)
    }
}

@Composable
private fun TodayPill(onToday: () -> Unit) {
    Surface(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToday),
        color = LbmBrown,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Today,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.weekly_mode_calendar_today_action),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun WeekdayHeader() {
    val labels = listOf(
        R.string.weekly_mode_calendar_weekday_sun,
        R.string.weekly_mode_calendar_weekday_mon,
        R.string.weekly_mode_calendar_weekday_tue,
        R.string.weekly_mode_calendar_weekday_wed,
        R.string.weekly_mode_calendar_weekday_thu,
        R.string.weekly_mode_calendar_weekday_fri,
        R.string.weekly_mode_calendar_weekday_sat,
    )
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEach { res ->
            Text(
                text = stringResource(res),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                color = LbmTextSecondary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    grid: MonthGridUi,
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        grid.weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                week.forEach { cell ->
                    if (cell == null) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        DayCell(
                            cell = cell,
                            selected = cell.day == selectedDay,
                            onClick = { onSelectDay(cell.day) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    cell: DayCellUi,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedColor = colorResource(R.color.mode_hard_chromatic_blue)
    val todayBg = colorResource(R.color.weekly_calendar_today_background)
    val todayText = colorResource(R.color.weekly_calendar_today_text)

    val background by animateColorAsState(
        targetValue = when {
            selected -> selectedColor
            cell.isToday -> todayBg
            else -> LbmSurface
        },
        animationSpec = tween(220),
        label = "dayBg",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> selectedColor
            cell.isToday -> selectedColor
            else -> LbmOutline
        },
        animationSpec = tween(220),
        label = "dayBorder",
    )
    val textColor by animateColorAsState(
        targetValue = when {
            selected -> Color.White
            cell.isToday -> todayText
            else -> LbmTextPrimary
        },
        animationSpec = tween(220),
        label = "dayText",
    )
    val pop by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "dayPop",
    )

    Box(
        modifier = modifier
            .heightIn(min = 46.dp)
            .scale(pop)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = cell.contentDescription }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = cell.day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = if (selected || cell.isToday) FontWeight.Bold else FontWeight.Normal,
            )
            Spacer(Modifier.height(3.dp))
            val dotColor = markerColor(cell.markerType)
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            cell.markerType == null -> Color.Transparent
                            selected -> Color.White
                            else -> dotColor
                        },
                    ),
            )
        }
    }
}

@Composable
private fun markerColor(type: CalendarCelebrationType?): Color = when (type) {
    CalendarCelebrationType.PUBLIC_HOLIDAY -> colorResource(R.color.weekly_calendar_dot_public)
    CalendarCelebrationType.HALF_HOLIDAY -> colorResource(R.color.weekly_calendar_dot_half)
    CalendarCelebrationType.RELIGIOUS_OBSERVANCE -> colorResource(R.color.weekly_calendar_dot_religious)
    else -> Color.Transparent
}

/* ----------------------------------------------------------------------------------------------- */
/* Legend card — explains every marker on the calendar                                             */
/* ----------------------------------------------------------------------------------------------- */

@Composable
private fun LegendCard() {
    LessonCard(title = stringResource(R.string.weekly_mode_calendar_legend_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.weekly_mode_calendar_legend_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
            )
            LegendDotRow(colorResource(R.color.weekly_calendar_dot_public), R.string.weekly_mode_calendar_type_public_holiday)
            LegendDotRow(colorResource(R.color.weekly_calendar_dot_half), R.string.weekly_mode_calendar_type_half_holiday)
            LegendDotRow(colorResource(R.color.weekly_calendar_dot_religious), R.string.weekly_mode_calendar_type_religious_observance)
            LegendSwatchRow(
                fill = colorResource(R.color.mode_hard_chromatic_blue),
                border = colorResource(R.color.mode_hard_chromatic_blue),
                labelRes = R.string.weekly_mode_calendar_legend_selected,
            )
            LegendSwatchRow(
                fill = colorResource(R.color.weekly_calendar_today_background),
                border = colorResource(R.color.mode_hard_chromatic_blue),
                labelRes = R.string.weekly_mode_calendar_legend_today,
            )
        }
    }
}

@Composable
private fun LegendDotRow(dotColor: Color, labelRes: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextPrimary,
        )
    }
}

@Composable
private fun LegendSwatchRow(fill: Color, border: Color, labelRes: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(fill)
                .border(1.5.dp, border, RoundedCornerShape(6.dp)),
        )
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyMedium,
            color = LbmTextPrimary,
        )
    }
}

/* ----------------------------------------------------------------------------------------------- */
/* Tone of the week                                                                                */
/* ----------------------------------------------------------------------------------------------- */

@Composable
private fun ToneCard(state: WeeklyModeCalendarUiState) {
    LessonCard(title = stringResource(R.string.weekly_mode_calendar_tone_section_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(LbmPrimaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = LbmBrown,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column {
                    Crossfade(targetState = state.toneNameRes, label = "toneName") { res ->
                        Text(
                            text = if (res != 0) stringResource(res) else "",
                            style = MaterialTheme.typography.titleLarge,
                            color = LbmTextPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (state.weekRangeLabel.isNotEmpty()) {
                        Text(
                            text = state.weekRangeLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LbmTextSecondary,
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.weekly_mode_calendar_tone_explainer),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
            )
        }
    }
}

/* ----------------------------------------------------------------------------------------------- */
/* Εορτολόγιο ημέρας (celebrations)                                                                */
/* ----------------------------------------------------------------------------------------------- */

@Composable
private fun CelebrationsCard(state: WeeklyModeCalendarUiState) {
    LessonCard(title = stringResource(R.string.weekly_mode_calendar_celebration_section_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.selectedDateLabel.isNotEmpty()) {
                Text(
                    text = state.selectedDateLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = LbmTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            CelebrationBlock(state.primaryCelebration)

            if (state.extraCelebrations.isNotEmpty()) {
                var expanded by remember(state.selectedDateLabel) { mutableStateOf(false) }
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = LbmBrown,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (expanded) {
                            stringResource(R.string.weekly_mode_calendar_more_entries_hide)
                        } else {
                            stringResource(R.string.weekly_mode_calendar_more_entries_show, state.extraCelebrations.size)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = LbmBrown,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Column(
                    modifier = Modifier.animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (expanded) {
                        state.extraCelebrations.forEach { CelebrationBlock(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CelebrationBlock(celebration: CelebrationUi) {
    val bg = celebrationBackground(celebration.type)
    val border = celebrationBorder(celebration.type)
    val accent = celebrationAccent(celebration.type)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TypeBadge(typeLabelRes(celebration.type), accent)
            if (celebration.title.isNotEmpty()) {
                Text(
                    text = celebration.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = LbmTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (celebration.description.isNotEmpty()) {
                Text(
                    text = celebration.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LbmTextSecondary,
                )
            }
        }
    }
}

@Composable
private fun TypeBadge(labelRes: Int, accent: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelLarge,
            color = accent,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/* ----------------------------------------------------------------------------------------------- */
/* Αναγνώσματα ημέρας (readings)                                                                   */
/* ----------------------------------------------------------------------------------------------- */

@Composable
private fun ReadingsCard(state: WeeklyModeCalendarUiState, onOpenReading: (String) -> Unit) {
    LessonCard(title = stringResource(R.string.weekly_mode_calendar_readings_section_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ReadingSubsection(
                labelRes = R.string.weekly_mode_calendar_apostle_label,
                readings = state.apostleReadings,
                onOpenReading = onOpenReading,
            )
            ReadingSubsection(
                labelRes = R.string.weekly_mode_calendar_gospel_label,
                readings = state.gospelReadings,
                onOpenReading = onOpenReading,
            )
            if (state.readingsEmpty) {
                Text(
                    text = stringResource(R.string.weekly_mode_calendar_readings_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LbmTextSecondary,
                )
            }
        }
    }
}

@Composable
private fun ReadingSubsection(
    labelRes: Int,
    readings: List<ReadingRefUi>,
    onOpenReading: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.titleMedium,
            color = LbmTextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        if (readings.isEmpty()) {
            Text(
                text = stringResource(R.string.weekly_mode_calendar_readings_none_for_kind),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
            )
        } else {
            readings.forEach { reading ->
                ReadingRow(reading = reading, onClick = { onOpenReading(reading.id) })
            }
        }
    }
}

@Composable
private fun ReadingRow(reading: ReadingRefUi, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "readingPress",
    )
    val cd = stringResource(R.string.weekly_mode_calendar_reading_open_cd, reading.reference)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(pressScale)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .clearAndSetSemantics { contentDescription = cd },
        color = LbmPrimaryContainer,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LbmOutline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = LbmBrown,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = reading.reference,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = LbmTextSecondary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/* ----------------------------------------------------------------------------------------------- */
/* Month / year picker dialog                                                                      */
/* ----------------------------------------------------------------------------------------------- */

@Composable
private fun MonthYearPickerDialog(
    initialYear: Int,
    initialMonth: Int,
    minYear: Int,
    maxYear: Int,
    monthNames: List<String>,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var year by remember { mutableStateOf(initialYear.coerceIn(minYear, maxYear)) }
    var month by remember { mutableStateOf(initialMonth.coerceIn(1, 12)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(year, month) }) {
                Text(stringResource(R.string.weekly_mode_calendar_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.weekly_mode_calendar_picker_cancel))
            }
        },
        title = { Text(stringResource(R.string.weekly_mode_calendar_picker_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Year stepper
                Text(
                    text = stringResource(R.string.weekly_mode_calendar_picker_year_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = LbmTextSecondary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(
                        onClick = { if (year > minYear) year -= 1 },
                        enabled = year > minYear,
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.weekly_mode_calendar_picker_year_decrement), tint = LbmBrown)
                    }
                    Text(
                        text = year.toString(),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        color = LbmTextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(
                        onClick = { if (year < maxYear) year += 1 },
                        enabled = year < maxYear,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.weekly_mode_calendar_picker_year_increment), tint = LbmBrown)
                    }
                }
                // Month grid (4 rows × 3 columns)
                Text(
                    text = stringResource(R.string.weekly_mode_calendar_picker_month_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = LbmTextSecondary,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            for (col in 0 until 3) {
                                val index = row * 3 + col
                                val label = monthNames.getOrElse(index) { (index + 1).toString() }
                                LessonChip(
                                    label = label,
                                    selected = month == index + 1,
                                    onClick = { month = index + 1 },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}

/* ----------------------------------------------------------------------------------------------- */
/* Type → label / colour helpers                                                                   */
/* ----------------------------------------------------------------------------------------------- */

private fun typeLabelRes(type: CalendarCelebrationType): Int = when (type) {
    CalendarCelebrationType.PUBLIC_HOLIDAY -> R.string.weekly_mode_calendar_type_public_holiday
    CalendarCelebrationType.HALF_HOLIDAY -> R.string.weekly_mode_calendar_type_half_holiday
    CalendarCelebrationType.RELIGIOUS_OBSERVANCE -> R.string.weekly_mode_calendar_type_religious_observance
    CalendarCelebrationType.NORMAL_DAY -> R.string.weekly_mode_calendar_type_normal_day
}

@Composable
private fun celebrationBackground(type: CalendarCelebrationType): Color = when (type) {
    CalendarCelebrationType.PUBLIC_HOLIDAY -> colorResource(R.color.weekly_calendar_celebration_public_bg)
    CalendarCelebrationType.HALF_HOLIDAY -> colorResource(R.color.weekly_calendar_celebration_half_bg)
    CalendarCelebrationType.RELIGIOUS_OBSERVANCE -> colorResource(R.color.weekly_calendar_celebration_religious_bg)
    CalendarCelebrationType.NORMAL_DAY -> colorResource(R.color.weekly_calendar_celebration_normal_bg)
}

@Composable
private fun celebrationBorder(type: CalendarCelebrationType): Color = when (type) {
    CalendarCelebrationType.PUBLIC_HOLIDAY -> colorResource(R.color.weekly_calendar_celebration_public_border)
    CalendarCelebrationType.HALF_HOLIDAY -> colorResource(R.color.weekly_calendar_celebration_half_border)
    CalendarCelebrationType.RELIGIOUS_OBSERVANCE -> colorResource(R.color.weekly_calendar_celebration_religious_border)
    CalendarCelebrationType.NORMAL_DAY -> colorResource(R.color.weekly_calendar_celebration_normal_border)
}

@Composable
private fun celebrationAccent(type: CalendarCelebrationType): Color = when (type) {
    CalendarCelebrationType.PUBLIC_HOLIDAY -> colorResource(R.color.weekly_calendar_dot_public)
    CalendarCelebrationType.HALF_HOLIDAY -> colorResource(R.color.weekly_calendar_dot_half)
    CalendarCelebrationType.RELIGIOUS_OBSERVANCE -> colorResource(R.color.weekly_calendar_dot_religious)
    CalendarCelebrationType.NORMAL_DAY -> LbmTextSecondary
}
