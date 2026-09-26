package com.johnchourp.learnbyzantinemusic.lectern.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.lectern.LibraryEntry
import com.johnchourp.learnbyzantinemusic.ui.components.LessonCard
import com.johnchourp.learnbyzantinemusic.ui.components.LessonHero
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmOutline
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPageBg
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary

/**
 * The lectern's library (ClickUp `869f5x2e7`): «Άνοιγμα PDF», how the reader turns pages, and the PDFs
 * already opened, newest first. Removing one only forgets it here — the file stays where it is, and its
 * page → ήχος map is kept for when it comes back. Pure UI; the host owns the picker and the grants.
 */
@Composable
fun LecternLibraryScreen(
    entries: List<LibraryEntry>,
    onAdd: () -> Unit,
    onOpen: (LibraryEntry) -> Unit,
    onRemove: (LibraryEntry) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LbmPageBg)
            .verticalScroll(rememberScrollState()),
    ) {
        LessonHero(
            title = stringResource(R.string.lectern_title),
            subtitle = stringResource(R.string.lectern_subtitle),
            onBack = onBack,
            icon = Icons.Filled.AutoStories,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Button(
                onClick = onAdd,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LbmBrown),
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.lectern_open_pdf), fontWeight = FontWeight.SemiBold)
            }
            LessonCard(title = stringResource(R.string.lectern_how_title)) {
                Text(
                    text = stringResource(R.string.lectern_how_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LbmTextSecondary,
                )
            }
            LessonCard(title = stringResource(R.string.lectern_library_title)) {
                if (entries.isEmpty()) {
                    Text(
                        text = stringResource(R.string.lectern_library_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LbmTextSecondary,
                    )
                } else {
                    entries.forEachIndexed { index, entry ->
                        if (index > 0) HorizontalDivider(color = LbmOutline)
                        LibraryRow(entry = entry, onOpen = { onOpen(entry) }, onRemove = { onRemove(entry) })
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.lectern_library_remove_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = LbmTextSecondary,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LibraryRow(entry: LibraryEntry, onOpen: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onOpen)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyLarge,
                color = LbmTextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.lectern_library_last_page, entry.lastPageIndex + 1),
                style = MaterialTheme.typography.bodySmall,
                color = LbmTextSecondary,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.lectern_library_remove, entry.title),
                tint = LbmTextSecondary,
            )
        }
    }
}
