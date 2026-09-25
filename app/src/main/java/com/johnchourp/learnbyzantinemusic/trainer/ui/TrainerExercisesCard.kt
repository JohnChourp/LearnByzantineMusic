package com.johnchourp.learnbyzantinemusic.trainer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.modes.ModeResources
import com.johnchourp.learnbyzantinemusic.trainer.ExerciseBook
import com.johnchourp.learnbyzantinemusic.ui.components.LessonCard
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmBrown
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextPrimary
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTextSecondary

/**
 * «Οι ασκήσεις μου» — save the melody under a name, and open, rename or delete a saved one
 * (ClickUp `869f5x261`, F6).
 *
 * A pure renderer of [TrainerExercisesUi]: the list's rules live in `ExerciseBook` and the Activity
 * applies them, so a refused name comes back as the dialog's [ExerciseNameError] rather than being
 * checked here. Deleting always asks first, and so does opening while a melody is being written,
 * because opening replaces it.
 */
@Composable
internal fun TrainerExercisesCard(
    exercises: TrainerExercisesUi,
    onRequestSave: () -> Unit,
    onSave: (String) -> Unit,
    onRequestOpen: (String) -> Unit,
    onOpen: (String) -> Unit,
    onRequestRename: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onRequestDelete: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismissDialog: () -> Unit,
) {
    LessonCard(title = stringResource(R.string.melody_trainer_exercises_title)) {
        if (exercises.newerFormat) {
            Text(
                text = stringResource(R.string.melody_trainer_exercises_newer),
                style = MaterialTheme.typography.bodyMedium,
                color = LbmBrown,
            )
        } else {
            Text(
                text = stringResource(R.string.melody_trainer_exercises_hint),
                style = MaterialTheme.typography.bodySmall,
                color = LbmTextSecondary,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onRequestSave, enabled = exercises.saveEnabled) {
                Icon(imageVector = Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.melody_trainer_exercise_save_as))
            }
            Spacer(Modifier.height(8.dp))
            if (exercises.items.isEmpty()) {
                Text(
                    text = stringResource(R.string.melody_trainer_exercises_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = LbmTextSecondary,
                )
            } else {
                exercises.items.forEach { item ->
                    ExerciseRow(
                        item = item,
                        enabled = exercises.enabled,
                        onOpen = onRequestOpen,
                        onRename = onRequestRename,
                        onDelete = onRequestDelete,
                    )
                }
            }
        }
    }

    when (val dialog = exercises.dialog) {
        is ExerciseDialogUi.SaveAs -> NameDialog(
            title = stringResource(R.string.melody_trainer_exercise_save_title),
            initial = "",
            error = dialog.error,
            confirmLabel = stringResource(R.string.melody_trainer_exercise_save),
            onConfirm = onSave,
            onDismiss = onDismissDialog,
        )
        is ExerciseDialogUi.Rename -> NameDialog(
            title = stringResource(R.string.melody_trainer_exercise_rename_title),
            initial = dialog.name,
            error = dialog.error,
            confirmLabel = stringResource(R.string.melody_trainer_exercise_rename),
            onConfirm = { onRename(dialog.name, it) },
            onDismiss = onDismissDialog,
        )
        is ExerciseDialogUi.ConfirmOpen -> ConfirmDialog(
            text = stringResource(R.string.melody_trainer_exercise_open_confirm, dialog.name),
            confirmLabel = stringResource(R.string.melody_trainer_exercise_open),
            onConfirm = { onOpen(dialog.name) },
            onDismiss = onDismissDialog,
        )
        is ExerciseDialogUi.ConfirmDelete -> ConfirmDialog(
            text = stringResource(R.string.melody_trainer_exercise_delete_confirm, dialog.name),
            confirmLabel = stringResource(R.string.melody_trainer_exercise_delete),
            onConfirm = { onDelete(dialog.name) },
            onDismiss = onDismissDialog,
        )
        null -> Unit
    }
}

@Composable
private fun ExerciseRow(
    item: ExerciseItemUi,
    enabled: Boolean,
    onOpen: (String) -> Unit,
    onRename: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val scaleName = item.mode?.let { stringResource(ModeResources.nameRes(it)) }
        ?: stringResource(R.string.melody_trainer_scale_default)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                color = LbmTextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.melody_trainer_exercise_summary, item.noteCount, scaleName, item.bpm),
                style = MaterialTheme.typography.bodySmall,
                color = LbmTextSecondary,
            )
        }
        IconButton(onClick = { onOpen(item.name) }, enabled = enabled) {
            Icon(
                imageVector = Icons.Filled.FolderOpen,
                contentDescription = stringResource(R.string.melody_trainer_exercise_open_cd, item.name),
                tint = LbmBrown,
            )
        }
        IconButton(onClick = { onRename(item.name) }, enabled = enabled) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = stringResource(R.string.melody_trainer_exercise_rename_cd, item.name),
                tint = LbmBrown,
            )
        }
        IconButton(onClick = { onDelete(item.name) }, enabled = enabled) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.melody_trainer_exercise_delete_cd, item.name),
                tint = LbmBrown,
            )
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    error: ExerciseNameError?,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(ExerciseBook.MAX_NAME_LENGTH) },
                label = { Text(stringResource(R.string.melody_trainer_exercise_name_label)) },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { reason -> { Text(errorText(reason)) } },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.melody_trainer_exercise_cancel)) }
        },
    )
}

@Composable
private fun ConfirmDialog(text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.melody_trainer_exercise_cancel)) }
        },
    )
}

@Composable
private fun errorText(reason: ExerciseNameError): String = when (reason) {
    ExerciseNameError.BLANK -> stringResource(R.string.melody_trainer_exercise_name_blank)
    ExerciseNameError.TOO_LONG ->
        stringResource(R.string.melody_trainer_exercise_name_too_long, ExerciseBook.MAX_NAME_LENGTH)
    ExerciseNameError.TAKEN -> stringResource(R.string.melody_trainer_exercise_name_taken)
    ExerciseNameError.FULL -> stringResource(R.string.melody_trainer_exercise_full, ExerciseBook.MAX_EXERCISES)
    ExerciseNameError.NEWER_FORMAT -> stringResource(R.string.melody_trainer_exercises_newer)
}
