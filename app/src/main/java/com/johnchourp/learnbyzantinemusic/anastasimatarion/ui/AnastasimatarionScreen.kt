package com.johnchourp.learnbyzantinemusic.anastasimatarion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.anastasimatarion.AnastasimatarionLabels
import com.johnchourp.learnbyzantinemusic.anastasimatarion.AnastasimatarionUiState
import com.johnchourp.learnbyzantinemusic.anastasimatarion.Hymn
import com.johnchourp.learnbyzantinemusic.anastasimatarion.HymnService
import com.johnchourp.learnbyzantinemusic.ui.components.LessonCard
import com.johnchourp.learnbyzantinemusic.ui.components.LessonChip
import com.johnchourp.learnbyzantinemusic.ui.components.LessonHero
import com.johnchourp.learnbyzantinemusic.ui.components.StaggeredAppear
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentOrangeContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentOrangeContent
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPageBg
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary

/**
 * Renders [AnastasimatarionUiState]: hero, mode picker (the week's mode is named under it),
 * then one card per service with its hymn groups. A hymn row shows its code, incipit, catalog
 * note and — when the user has recordings for it — a recordings badge.
 */
@Composable
fun AnastasimatarionScreen(
    uiState: AnastasimatarionUiState,
    onBack: () -> Unit,
    onSelectMode: (String) -> Unit,
    onOpenHymn: (modeKey: String, hymn: Hymn) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mode = uiState.catalog?.mode(uiState.selectedModeKey)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LbmPageBg),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "hero") {
            LessonHero(
                title = stringResource(R.string.anastasimatarion_title),
                subtitle = stringResource(R.string.anastasimatarion_subtitle),
                onBack = onBack,
                icon = Icons.AutoMirrored.Filled.MenuBook,
            )
        }

        item(key = "modes") {
            StaggeredAppear(delayMillis = 60, modifier = Modifier.padding(horizontal = 16.dp)) {
                ModePicker(
                    selectedModeKey = uiState.selectedModeKey,
                    weekModeKey = uiState.weekModeKey,
                    onSelectMode = onSelectMode,
                )
            }
        }

        when {
            mode != null -> {
                mode.services.forEachIndexed { index, service ->
                    item(key = "service-${service.key}") {
                        StaggeredAppear(delayMillis = 120 + index * 60, modifier = Modifier.padding(horizontal = 16.dp)) {
                            ServiceCard(
                                service = service,
                                counts = uiState.counts,
                                onOpenHymn = { hymn -> onOpenHymn(mode.key, hymn) },
                            )
                        }
                    }
                }
                if (!uiState.hasRecordingsFolder) {
                    item(key = "no-folder") {
                        HintText(stringResource(R.string.anastasimatarion_no_folder_hint))
                    }
                }
            }

            uiState.loadFailed -> item(key = "error") { HintText(stringResource(R.string.anastasimatarion_load_failed)) }
        }

    }
}

@Composable
private fun ModePicker(
    selectedModeKey: String,
    weekModeKey: String?,
    onSelectMode: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AnastasimatarionLabels.MODE_ORDER.forEach { modeKey ->
                LessonChip(
                    label = stringResource(AnastasimatarionLabels.modeName(modeKey)),
                    selected = modeKey == selectedModeKey,
                    onClick = { onSelectMode(modeKey) },
                )
            }
        }
        if (weekModeKey != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.anastasimatarion_week_mode_template,
                    stringResource(AnastasimatarionLabels.modeName(weekModeKey)),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmTextSecondary,
            )
        }
    }
}

@Composable
private fun ServiceCard(
    service: HymnService,
    counts: Map<String, Int>,
    onOpenHymn: (Hymn) -> Unit,
) {
    LessonCard(title = stringResource(AnastasimatarionLabels.serviceName(service.key))) {
        Column(modifier = Modifier.fillMaxWidth()) {
            service.groups.forEachIndexed { index, group ->
                if (index > 0) Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(AnastasimatarionLabels.groupName(group.key)),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = LbmBrown,
                )
                group.hymns.forEach { hymn ->
                    HymnRow(hymn = hymn, recordings = counts[hymn.code] ?: 0, onClick = { onOpenHymn(hymn) })
                }
            }
        }
    }
}

@Composable
private fun HymnRow(hymn: Hymn, recordings: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = hymn.code,
            style = MaterialTheme.typography.labelMedium,
            color = LbmTextSecondary,
            modifier = Modifier.width(28.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = hymn.incipit,
                style = MaterialTheme.typography.bodyLarge,
                color = LbmTextPrimary,
            )
            hymn.note?.let { note ->
                Text(
                    text = noteText(note),
                    style = MaterialTheme.typography.bodySmall,
                    color = LbmTextSecondary,
                )
            }
        }
        if (recordings > 0) {
            Spacer(Modifier.width(8.dp))
            RecordingsBadge(count = recordings)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = LbmTextSecondary,
        )
    }
}

@Composable
private fun RecordingsBadge(count: Int) {
    val description = stringResource(R.string.anastasimatarion_recordings_count_cd, count)
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = AccentOrangeContainer,
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = null,
                tint = AccentOrangeContent,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = AccentOrangeContent,
            )
        }
    }
}

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = LbmTextSecondary,
        modifier = Modifier.padding(horizontal = 24.dp),
    )
}

/** The catalog note («Θεοτοκίο», «Ψαλμός 140», «Ωδή α΄») in the app language. */
@Composable
internal fun noteText(note: String): String {
    val label = AnastasimatarionLabels.note(note)
    return when {
        label.res != null && label.argument != null -> stringResource(label.res, label.argument)
        label.res != null -> stringResource(label.res)
        else -> label.argument.orEmpty()
    }
}
