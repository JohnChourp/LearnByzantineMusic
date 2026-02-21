package com.johnchourp.learnbyzantinemusic.recordings

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.recordings.index.RecordingsRepository
import com.johnchourp.learnbyzantinemusic.recordings.ui.RecordingsManagerScreen
import kotlinx.coroutines.launch

class RecordingsManagerActivity : BaseActivity() {
    private lateinit var recordingsPrefs: RecordingsPrefs
    private val recordingsRepository by lazy { RecordingsRepository.getInstance(applicationContext) }
    private val recordingExternalOpener by lazy { RecordingExternalOpener(this) }

    private val viewModel: RecordingsManagerViewModel by viewModels {
        RecordingsManagerViewModelFactory(
            repository = recordingsRepository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recordingsPrefs = RecordingsPrefs(this)

        val rootUri = recordingsPrefs.getFolderUri()
        if (rootUri == null || !recordingsPrefs.hasPersistedReadWriteAccess(contentResolver, rootUri)) {
            Toast.makeText(this, R.string.recordings_select_folder_first, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val rootFolderName = DocumentFile.fromTreeUri(this, rootUri)?.name
            ?: getString(R.string.recordings_manage_root_label)
        viewModel.setRootFolder(rootUri, rootFolderName)

        setupBackHandling()

        setContent {
            MaterialTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val entries = viewModel.entriesFlow.collectAsLazyPagingItems()

                RecordingsManagerScreen(
                    uiState = uiState,
                    entries = entries,
                    onNavigateUp = {
                        viewModel.navigateUp { moved ->
                            if (!moved) {
                                finish()
                            }
                        }
                    },
                    onCreateFolder = { showCreateFolderDialog() },
                    onSearchChanged = { viewModel.setSearchQuery(it) },
                    onSortChanged = { viewModel.setSortOption(it) },
                    onFilterChanged = { viewModel.setFilter(it) },
                    onOpenEntry = { entry ->
                        if (entry.type == ManagerEntryType.FOLDER) {
                            viewModel.enterFolder(entry)
                        } else {
                            openRecordingInExternalPlayer(entry)
                        }
                    },
                    onRenameEntry = { showRenameEntryDialog(it) },
                    onDeleteEntry = { showDeleteEntryDialog(it) },
                    onMoveEntry = { item, target ->
                        showMoveEntryConfirmationDialog(item, target)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.requestReindex(force = false)
    }

    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    viewModel.navigateUp { moved ->
                        if (!moved) {
                            finish()
                        }
                    }
                }
            }
        )
    }

    private fun showCreateFolderDialog() {
        val inputField = EditText(this).apply {
            setSingleLine(true)
            hint = getString(R.string.recordings_manage_create_folder_hint)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.recordings_manage_create_folder_title))
            .setView(inputField)
            .setPositiveButton(getString(R.string.recordings_manage_create_folder_confirm)) { _, _ ->
                val folderName = RecordingDocumentOps.sanitizeName(inputField.text?.toString().orEmpty())
                if (folderName.isBlank()) {
                    Toast.makeText(this, R.string.recordings_rename_invalid_name, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewModel.createFolder(folderName) { created ->
                    if (created) {
                        Toast.makeText(
                            this,
                            getString(R.string.recordings_manage_folder_created_template, folderName),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(this, R.string.recordings_manage_error_create_folder, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.recordings_rename_cancel_button)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showRenameEntryDialog(item: ManagerListItem) {
        val parsedName = if (item.type == ManagerEntryType.AUDIO_FILE) {
            RecordingDocumentOps.parseFileName(item.name)
        } else {
            ParsedFileName(item.name, null)
        }

        val inputField = EditText(this).apply {
            setSingleLine(true)
            setText(parsedName.baseName)
            setSelection(text.length)
            hint = getString(R.string.recordings_rename_input_hint)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.recordings_rename_confirm_title))
            .setMessage(getString(R.string.recordings_rename_confirm_message, item.name))
            .setView(inputField)
            .setPositiveButton(getString(R.string.recordings_rename_confirm_button)) { _, _ ->
                val sanitizedBaseName = RecordingDocumentOps.sanitizeName(inputField.text?.toString().orEmpty())
                if (sanitizedBaseName.isBlank()) {
                    Toast.makeText(this, R.string.recordings_rename_invalid_name, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val targetName = parsedName.extension?.let { extension ->
                    "$sanitizedBaseName.$extension"
                } ?: sanitizedBaseName

                if (targetName == item.name) {
                    return@setPositiveButton
                }

                viewModel.renameItem(item, targetName) { result ->
                    when (result) {
                        RenameOutcome.SUCCESS -> {
                            Toast.makeText(this, R.string.recordings_renamed_ok, Toast.LENGTH_SHORT).show()
                        }

                        RenameOutcome.NAME_EXISTS -> {
                            Toast.makeText(this, R.string.recordings_error_rename_name_exists, Toast.LENGTH_SHORT).show()
                        }

                        RenameOutcome.REMOVED -> {
                            Toast.makeText(this, R.string.recordings_manage_error_entry_removed, Toast.LENGTH_SHORT).show()
                        }

                        RenameOutcome.FAILED -> {
                            Toast.makeText(this, R.string.recordings_error_rename, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton(getString(R.string.recordings_rename_cancel_button)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showDeleteEntryDialog(item: ManagerListItem) {
        val titleRes = if (item.type == ManagerEntryType.FOLDER) {
            R.string.recordings_manage_delete_folder_confirm_title
        } else {
            R.string.recordings_delete_confirm_title
        }

        val messageRes = if (item.type == ManagerEntryType.FOLDER) {
            R.string.recordings_manage_delete_folder_confirm_message
        } else {
            R.string.recordings_delete_confirm_message
        }

        AlertDialog.Builder(this)
            .setTitle(getString(titleRes))
            .setMessage(getString(messageRes, item.name))
            .setPositiveButton(getString(R.string.recordings_delete_confirm_button)) { _, _ ->
                viewModel.deleteItem(item) { result ->
                    when (result) {
                        DeleteOutcome.SUCCESS -> {
                            Toast.makeText(this, R.string.recordings_deleted_ok, Toast.LENGTH_SHORT).show()
                        }

                        DeleteOutcome.REMOVED -> {
                            Toast.makeText(this, R.string.recordings_manage_error_entry_removed, Toast.LENGTH_SHORT).show()
                        }

                        DeleteOutcome.FAILED -> {
                            Toast.makeText(this, R.string.recordings_error_delete, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton(getString(R.string.recordings_delete_cancel_button)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showMoveEntryConfirmationDialog(item: ManagerListItem, target: MoveTargetFolder) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.recordings_manage_move_confirm_title))
            .setMessage(
                getString(
                    R.string.recordings_manage_move_confirm_message,
                    toDisplayPath(item.relativePath),
                    target.displayPath
                )
            )
            .setPositiveButton(getString(R.string.recordings_manage_move_confirm_button)) { _, _ ->
                viewModel.moveItem(item, target) { outcome ->
                    when (outcome) {
                        MoveOutcome.SUCCESS -> {
                            Toast.makeText(this, R.string.recordings_manage_move_success, Toast.LENGTH_SHORT).show()
                        }

                        MoveOutcome.SAME_PARENT -> {
                            Toast.makeText(this, R.string.recordings_manage_move_same_parent, Toast.LENGTH_SHORT).show()
                        }

                        MoveOutcome.BLOCKED_SELF_OR_DESCENDANT -> {
                            Toast.makeText(this, R.string.recordings_manage_move_invalid_target, Toast.LENGTH_SHORT).show()
                        }

                        MoveOutcome.FAILED -> {
                            Toast.makeText(this, R.string.recordings_manage_error_move, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton(getString(R.string.recordings_delete_cancel_button)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun openRecordingInExternalPlayer(item: ManagerListItem) {
        val mimeType = if (!item.mimeType.isNullOrBlank() && item.mimeType != "application/octet-stream") {
            item.mimeType
        } else {
            RecordingFormatOption.resolveMimeTypeByFileName(item.name) ?: "audio/*"
        }

        lifecycleScope.launch {
            val exists = recordingsRepository.checkRecordingExists(item.uri)
            if (!exists) {
                if (item.type == ManagerEntryType.AUDIO_FILE) {
                    recordingsRepository.removeOwnedRecording(item.uri)
                }
                viewModel.requestReindex(force = true)
                Toast.makeText(
                    this@RecordingsManagerActivity,
                    R.string.recordings_manage_error_entry_removed,
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            recordingExternalOpener.openRecordingWithChooser(
                sourceUri = item.uri,
                fileName = item.name,
                mimeType = mimeType,
                chooserTitle = getString(R.string.recordings_open_recording_with),
                onFailure = {
                    lifecycleScope.launch {
                        val stillExists = recordingsRepository.checkRecordingExists(item.uri)
                        if (!stillExists) {
                            if (item.type == ManagerEntryType.AUDIO_FILE) {
                                recordingsRepository.removeOwnedRecording(item.uri)
                            }
                            viewModel.requestReindex(force = true)
                            Toast.makeText(
                                this@RecordingsManagerActivity,
                                R.string.recordings_manage_error_entry_removed,
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@RecordingsManagerActivity,
                                R.string.recordings_open_recording_no_app,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
        }
    }

    private fun toDisplayPath(relativePath: String): String {
        return if (relativePath.isBlank()) {
            "/"
        } else {
            "/$relativePath"
        }
    }
}
