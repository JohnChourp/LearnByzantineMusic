package com.johnchourp.learnbyzantinemusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmOutline
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurface
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary

/**
 * A pill-shaped, single-select chip with an animated brown fill when [selected]. Shared between
 * the lesson screens (the «Ανιόντες» / «Κατιόντες» character & interval pickers) so the selection
 * UI stays identical — promoted out of the individual screens alongside [LessonHero]/[LessonCard]
 * to keep the pages DRY. Honors a 48dp minimum touch target.
 */
@Composable
fun LessonChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container by animateColorAsState(
        targetValue = if (selected) LbmBrown else LbmSurface,
        label = "chipBg",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else LbmTextPrimary,
        label = "chipText",
    )
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .border(1.dp, if (selected) LbmBrown else LbmOutline, RoundedCornerShape(12.dp))
            .selectable(selected = selected, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}
