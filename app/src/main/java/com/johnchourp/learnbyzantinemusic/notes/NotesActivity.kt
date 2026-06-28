package com.johnchourp.learnbyzantinemusic.notes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.notes.ui.NotesScreen
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme

class NotesActivity : BaseActivity() {
    private lateinit var notesPrefs: NotesPrefs
    private val notesRepository by lazy { NotesRepository.getInstance(applicationContext) }

    private val viewModel: NotesViewModel by viewModels {
        NotesViewModelFactory(
            repository = notesRepository,
            prefs = NotesPrefs(this)
        )
    }

    private var folderRequiredFromOnboarding: Boolean = false

    private val folderPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) {
                if (folderRequiredFromOnboarding) {
                    Toast.makeText(this, R.string.notes_folder_required_hint, Toast.LENGTH_SHORT).show()
                    launchFolderPicker()
                }
                return@registerForActivityResult
            }

            val permissionFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            val permissionGranted = runCatching {
                contentResolver.takePersistableUriPermission(uri, permissionFlags)
            }.isSuccess

            if (!permissionGranted) {
                Toast.makeText(this, R.string.notes_status_folder_permission_error, Toast.LENGTH_SHORT).show()
                if (folderRequiredFromOnboarding) {
                    launchFolderPicker()
                }
                return@registerForActivityResult
            }

            folderRequiredFromOnboarding = false
            viewModel.onFolderConfigured(uri)
        }

    private val importBackupLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) {
                return@registerForActivityResult
            }
            viewModel.importReplace(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notesPrefs = NotesPrefs(this)

        // Flush unsaved editor edits on the way out (header Back button and system back both route
        // through here), so edits made within the 1.2s auto-save debounce aren't lost. Scoped to a
        // real exit — NOT onStop — so it never fires while a SAF picker (folder/import) is open.
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    viewModel.flushPendingEdits()
                    finish()
                }
            }
        )

        ensureBackupFolderConfigured()

        setContent {
            LbmTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val statusText = resolveStatusText(uiState.statusMessage)

                NotesScreen(
                    uiState = uiState,
                    statusText = statusText,
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onCreateNote = { viewModel.createNewNote() },
                    onDeleteSelected = { viewModel.deleteSelectedNote() },
                    onSaveNow = { viewModel.saveNow() },
                    onExportNow = { viewModel.exportNow() },
                    onImport = {
                        importBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    },
                    onSelectFolder = { launchFolderPicker() },
                    onResyncPending = { viewModel.syncPendingNow() },
                    onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                    onSelectNote = { viewModel.onSelectNote(it) },
                    onEditorTitleChanged = { viewModel.onEditorTitleChanged(it) },
                    onEditorBodyChanged = { viewModel.onEditorBodyChanged(it) }
                )
            }
        }
    }

    private fun ensureBackupFolderConfigured() {
        val storedUri = notesPrefs.getFolderUri()
        if (storedUri != null && notesPrefs.hasPersistedReadWriteAccess(contentResolver, storedUri)) {
            viewModel.refreshSyncState()
            return
        }

        folderRequiredFromOnboarding = true
        launchFolderPicker()
    }

    private fun launchFolderPicker() {
        val initialUri = notesPrefs.getFolderUri()
        folderPickerLauncher.launch(initialUri)
    }

    private fun resolveStatusText(statusKey: String): String {
        return when (statusKey) {
            "notes_status_created" -> getString(R.string.notes_status_created)
            "notes_status_saved" -> getString(R.string.notes_status_saved)
            "notes_status_auto_saved" -> getString(R.string.notes_status_auto_saved)
            "notes_status_deleted" -> getString(R.string.notes_status_deleted)
            "notes_status_exported" -> getString(R.string.notes_status_exported)
            "notes_status_imported" -> getString(R.string.notes_status_imported)
            "notes_status_resync_success" -> getString(R.string.notes_status_resync_success)
            "notes_status_resync_nothing" -> getString(R.string.notes_status_resync_nothing)
            "notes_status_resync_failed" -> getString(R.string.notes_status_resync_failed)
            "notes_status_sync_failed_local_saved" -> getString(R.string.notes_status_sync_failed_local_saved)
            "notes_status_sync_partial_pending" -> getString(R.string.notes_status_sync_partial_pending)
            "notes_status_import_invalid_json" -> getString(R.string.notes_status_import_invalid_json)
            "notes_status_import_read_failed" -> getString(R.string.notes_status_import_read_failed)
            "notes_status_folder_selected" -> getString(R.string.notes_status_folder_selected)
            else -> statusKey
        }
    }
}
