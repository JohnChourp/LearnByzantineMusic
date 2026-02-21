package com.johnchourp.learnbyzantinemusic.recordings.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.recordings.ManagerEntryType
import com.johnchourp.learnbyzantinemusic.recordings.ManagerListItem

@Composable
fun ManagerListItemRow(
    item: ManagerListItem,
    onOpen: (ManagerListItem) -> Unit,
    onRename: (ManagerListItem) -> Unit,
    onMove: (ManagerListItem) -> Unit,
    onDelete: (ManagerListItem) -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onOpen(item) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (item.type == ManagerEntryType.FOLDER) {
                    stringResource(R.string.recordings_manage_item_folder_label)
                } else {
                    stringResource(R.string.recordings_manage_item_audio_label)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { isMenuExpanded = true }) {
                    Text(text = stringResource(R.string.recordings_action_menu))
                }
                DropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.recordings_action_open_short)) },
                        onClick = {
                            isMenuExpanded = false
                            onOpen(item)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.recordings_action_rename_short)) },
                        onClick = {
                            isMenuExpanded = false
                            onRename(item)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.recordings_action_move_short)) },
                        onClick = {
                            isMenuExpanded = false
                            onMove(item)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.recordings_action_delete_short)) },
                        onClick = {
                            isMenuExpanded = false
                            onDelete(item)
                        }
                    )
                }
            }
        }
    }
}
