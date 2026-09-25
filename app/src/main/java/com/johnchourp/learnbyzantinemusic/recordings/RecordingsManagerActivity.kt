package com.johnchourp.learnbyzantinemusic.recordings

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.recordings.index.RecordingsRepository
import com.johnchourp.learnbyzantinemusic.recordings.player.InAppPlayerViewModel
import com.johnchourp.learnbyzantinemusic.recordings.player.PlayerItem
import com.johnchourp.learnbyzantinemusic.recordings.player.cardActions
import com.johnchourp.learnbyzantinemusic.recordings.ui.RecordingsManagerScreen
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme
import kotlinx.coroutines.launch

/**
 * The «Διαχείριση ηχογραφήσεων» screen: browse, search, sort, rename, move and delete inside the
 * recordings folder.
 *
 * **Flow.** Reads from the Room index rather than from SAF directly, paged with Paging3, so a folder
 * with thousands of files scrolls smoothly. A BFS indexer fills that index in the background through
 * WorkManager; the linear progress at the top is that reindex, not a page load.
 *
 * **Guards worth knowing about** (each exists because it was hit for real):
 * - tree vs document URI normalisation, so a folder holding only sub-folders is not reported empty;
 * - a move into the folder itself, into one of its own descendants, or into its current parent is
 *   refused before it starts;
 * - rename falls back to copy+delete when a provider's direct `renameTo` fails;
 * - a document that has disappeared underneath the app resolves to «καταργήθηκε» and is dropped from
 *   the list instead of lingering as a dead row.
 *
 * **Listening:** a recording opens in the in-app player under the title (loop, slower at the same
 * pitch, shift in μόρια — ClickUp `869f5x268`); another app is one tap away on the player card. The
 * player is released in `onStop`, and closed when the file it holds is renamed, moved or deleted.
 *
 * **Inputs:** none — it opens on the folder recorded in preferences.
 * **Touches:** `recordings_folder_tree_uri` and the `recordings_index.db` Room index.
 */
class RecordingsManagerActivity : BaseActivity() {
    private lateinit var recordingsPrefs: RecordingsPrefs
    private val recordingsRepository by lazy { RecordingsRepository.getInstance(applicationContext) }
    private val recordingExternalOpener by lazy { RecordingExternalOpener(this) }
    private val player: InAppPlayerViewModel by viewModels()

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
            LbmTheme(palette = currentPalette()) {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val entries = viewModel.entriesFlow.collectAsLazyPagingItems()
                val playerState by player.state.collectAsStateWithLifecycle()

                RecordingsManagerScreen(
                    uiState = uiState,
                    entries = entries,
                    player = playerState,
                    playerActions = remember { player.cardActions(this@RecordingsManagerActivity, ::openRecordingExternally) },
                    onBack = {
                        viewModel.navigateUp { moved ->
                            if (!moved) {
                                finish()
                            }
                        }
                    },
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
                            playRecording(entry)
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

    override fun onStop() {
        super.onStop()
        // No sound in the background: released here, prepared again on the next play.
        player.onScreenStopped()
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
                    if (result == RenameOutcome.SUCCESS || result == RenameOutcome.REMOVED) closePlayerIfHolding(item.uri)
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
                    if (result != DeleteOutcome.FAILED) closePlayerIfHolding(item.uri)
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
                    if (outcome == MoveOutcome.SUCCESS) closePlayerIfHolding(item.uri)
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

    /** A recording opens in the in-app player; one that has disappeared is dropped from the list. */
    private fun playRecording(item: ManagerListItem) {
        lifecycleScope.launch {
            if (!recordingsRepository.checkRecordingExists(item.uri)) {
                recordingsRepository.removeOwnedRecording(item.uri)
                viewModel.requestReindex(force = true)
                Toast.makeText(
                    this@RecordingsManagerActivity,
                    R.string.recordings_manage_error_entry_removed,
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }
            player.open(PlayerItem(item.uri, item.name, item.mimeType))
        }
    }

    /** «Άνοιγμα σε άλλη εφαρμογή» on the player card — the only way out when the device cannot play the file. */
    private fun openRecordingExternally() {
        val recording = player.current ?: return
        openRecordingInExternalPlayer(recording.uri, recording.name, recording.mimeType)
    }

    private fun closePlayerIfHolding(uri: Uri) {
        if (player.current?.uri == uri) player.close()
    }

    private fun openRecordingInExternalPlayer(uri: Uri, name: String, itemMimeType: String?) {
        val mimeType = if (!itemMimeType.isNullOrBlank() && itemMimeType != "application/octet-stream") {
            itemMimeType
        } else {
            RecordingFormatOption.resolveMimeTypeByFileName(name) ?: "audio/*"
        }

        lifecycleScope.launch {
            val exists = recordingsRepository.checkRecordingExists(uri)
            if (!exists) {
                recordingsRepository.removeOwnedRecording(uri)
                viewModel.requestReindex(force = true)
                Toast.makeText(
                    this@RecordingsManagerActivity,
                    R.string.recordings_manage_error_entry_removed,
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            recordingExternalOpener.openRecordingWithChooser(
                sourceUri = uri,
                fileName = name,
                mimeType = mimeType,
                chooserTitle = getString(R.string.recordings_open_recording_with),
                onFailure = {
                    lifecycleScope.launch {
                        val stillExists = recordingsRepository.checkRecordingExists(uri)
                        if (!stillExists) {
                            recordingsRepository.removeOwnedRecording(uri)
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
