package com.johnchourp.learnbyzantinemusic.recordings.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.recordings.ManagerEntryType
import com.johnchourp.learnbyzantinemusic.recordings.ManagerListItem
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentBlueContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentBlueContent
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGoldContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGoldContent
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmMeasureBar
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmSurface
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary

/**
 * Redesigned manager row: an [ElevatedCard] with a type-aware leading badge (folder vs audio), the
 * entry name + type label, and a kebab overflow menu (Open / Rename / Move / Delete). Tapping the
 * row opens the entry (or enters the folder); a press-scale gives tactile feedback. Pure renderer.
 */
@Composable
fun ManagerListItemRow(
    item: ManagerListItem,
    onOpen: (ManagerListItem) -> Unit,
    onRename: (ManagerListItem) -> Unit,
    onMove: (ManagerListItem) -> Unit,
    onDelete: (ManagerListItem) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = tween(120), label = "managerRowScale")

    val isFolder = item.type == ManagerEntryType.FOLDER
    val badgeColor = if (isFolder) AccentBlueContainer else AccentGoldContainer
    val badgeTint = if (isFolder) AccentBlueContent else AccentGoldContent

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null) { onOpen(item) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = LbmSurface, contentColor = LbmTextPrimary),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isFolder) Icons.Filled.Folder else Icons.Filled.GraphicEq,
                    contentDescription = null,
                    tint = badgeTint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = LbmTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (isFolder) {
                        stringResource(R.string.recordings_manage_item_folder_label)
                    } else {
                        stringResource(R.string.recordings_manage_item_audio_label)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = LbmTextSecondary,
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.recordings_action_menu),
                        tint = LbmTextSecondary,
                    )
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.recordings_action_open_short)) },
                        leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onOpen(item)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.recordings_action_rename_short)) },
                        leadingIcon = { Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onRename(item)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.recordings_action_move_short)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onMove(item)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.recordings_action_delete_short), color = LbmMeasureBar) },
                        leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = LbmMeasureBar) },
                        onClick = {
                            menuExpanded = false
                            onDelete(item)
                        },
                    )
                }
            }
        }
    }
}
