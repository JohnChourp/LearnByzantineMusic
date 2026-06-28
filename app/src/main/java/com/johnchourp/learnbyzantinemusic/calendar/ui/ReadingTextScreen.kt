package com.johnchourp.learnbyzantinemusic.calendar.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.ui.components.LessonCard
import com.johnchourp.learnbyzantinemusic.ui.components.LessonChip
import com.johnchourp.learnbyzantinemusic.ui.components.LessonHero
import com.johnchourp.learnbyzantinemusic.ui.components.StaggeredAppear
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPageBg
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary

/** Immutable snapshot of the reading the screen renders (resolved once from the launching Intent). */
@Immutable
data class ReadingTextUiState(
    val hasReading: Boolean = false,
    val reference: String = "",
    val textAncient: String = "",
    val textModern: String = "",
)

/**
 * Redesigned «Κείμενο Αναγνώσματος» screen, reached by tapping a reading on the calendar. A hero, the
 * reference card, an Αρχαία / Νεοελληνικά toggle and the passage with an animated swap on mode change.
 */
@Composable
fun ReadingTextScreen(
    state: ReadingTextUiState,
    ancientSelected: Boolean,
    onBack: () -> Unit,
    onSelectAncient: () -> Unit,
    onSelectModern: () -> Unit,
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
            title = stringResource(R.string.reading_text_page_title),
            subtitle = stringResource(R.string.reading_text_subtitle),
            onBack = onBack,
            icon = Icons.AutoMirrored.Filled.MenuBook,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(2.dp))

            StaggeredAppear(delayMillis = 60) {
                LessonCard(title = stringResource(R.string.reading_text_reference_label)) {
                    Text(
                        text = if (state.hasReading) {
                            state.reference
                        } else {
                            stringResource(R.string.reading_text_missing_reference)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = LbmTextPrimary,
                    )
                }
            }

            if (state.hasReading) {
                StaggeredAppear(delayMillis = 120) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LessonChip(
                            label = stringResource(R.string.reading_text_mode_ancient),
                            selected = ancientSelected,
                            onClick = onSelectAncient,
                            modifier = Modifier.weight(1f),
                        )
                        LessonChip(
                            label = stringResource(R.string.reading_text_mode_modern),
                            selected = !ancientSelected,
                            onClick = onSelectModern,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            StaggeredAppear(delayMillis = 180) {
                LessonCard(title = stringResource(R.string.reading_text_content_label)) {
                    if (state.hasReading) {
                        Crossfade(targetState = ancientSelected, label = "readingMode") { ancient ->
                            Text(
                                text = if (ancient) state.textAncient else state.textModern,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp),
                                color = LbmTextPrimary,
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.reading_text_missing_content),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LbmTextPrimary,
                            textAlign = TextAlign.Start,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
