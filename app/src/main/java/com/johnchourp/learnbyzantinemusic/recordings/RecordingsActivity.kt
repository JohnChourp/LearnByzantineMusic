package com.johnchourp.learnbyzantinemusic.recordings

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.recordings.index.RecordingsRepository
import com.johnchourp.learnbyzantinemusic.recordings.analysis.AnalysisSettingsStore
import com.johnchourp.learnbyzantinemusic.recordings.analysis.RecordingAnalysisActivity
import com.johnchourp.learnbyzantinemusic.recordings.session.PendingRecording
import com.johnchourp.learnbyzantinemusic.recordings.session.RecordingEventText
import com.johnchourp.learnbyzantinemusic.recordings.session.RecordingSessionState
import com.johnchourp.learnbyzantinemusic.recordings.session.RecordingSessions
import com.johnchourp.learnbyzantinemusic.recordings.session.RecordingTarget
import com.johnchourp.learnbyzantinemusic.recordings.ui.RecordingsScreen
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme
import kotlinx.coroutines.launch

/**
 * The «Ηχογραφήσεις» screen: record from the microphone into a folder the user owns.
 *
 * **Flow.** The recording belongs to the process, not to this screen: a
 * [com.johnchourp.learnbyzantinemusic.recordings.session.RecordingSession] captures raw PCM into an
 * app-private WAV and saves it in the order transcode → create the file in the chosen SAF folder →
 * copy → verify → only then delete the WAV. This screen shows the session's state and forwards taps,
 * so re-creating it — the device's dark theme switching on a schedule, split-screen, a fold, a
 * language or font-size change — neither stops nor deletes a recording, and Back during
 * «Αποθήκευση…» simply leaves while the save finishes (ClickUp `869f5x26x`).
 *
 * **A failed transcode produces no file in the chosen format** — nothing empty or half-written is
 * left in the folder; the WAV is saved there instead, with a message saying so
 * (`RecordingSaverTranscodeFailureTest`). When the folder cannot take the recording at all, it is
 * kept inside the app and listed here under «Δεν αποθηκεύτηκαν ακόμη», with «Αποθήκευση» and
 * «Διαγραφή».
 *
 * **Why this list is "the last 10 of mine".** It is served from [OwnedRecordingsStore], a local
 * history of what *this app* produced — not from a scan of the folder. That is deliberate: the main
 * page must open instantly even when the folder holds thousands of files, and a full SAF walk cannot
 * promise that. Browsing everything is [RecordingsManagerActivity]'s job.
 *
 * **Opening a file** goes through [RecordingExternalOpener], which copies into cache and hands out a
 * `FileProvider` URI, because many external players cannot read a foreign SAF URI directly.
 *
 * **Requires** `RECORD_AUDIO` and a persisted SAF tree grant; changing the folder is confirmed first,
 * so a mis-tap cannot silently orphan the current one.
 *
 * **Touches:** `recordings_folder_tree_uri`, `recordings_output_format`, `owned_recordings`, and —
 * through the session — `filesDir/recordings_capture/` and `filesDir/recordings_pending/`.
 */
class RecordingsActivity : BaseActivity() {
    private lateinit var recordingsPrefs: RecordingsPrefs
    private val recordingsRepository by lazy { RecordingsRepository.getInstance(applicationContext) }
    private val recordingExternalOpener by lazy { RecordingExternalOpener(this) }

    private val viewModel: RecordingsViewModel by viewModels {
        RecordingsViewModelFactory(
            repository = recordingsRepository,
            prefs = RecordingsPrefs(this)
        )
    }

    // The process's recording: it outlives this screen (see the class KDoc).
    private val session by lazy { RecordingSessions.get(applicationContext) }
    private var sessionShown = false

    private var selectedFolderUri: Uri? = null
    // Optional sub-folder of the picked folder to save into (e.g. a hymn's folder, see EXTRA_TARGET_FOLDER_PATH).
    private var targetFolderSegments: List<String> = emptyList()
    private var targetFolderMatchPrefix: String? = null
    private var intentTargetLabel: String? = null
    private var hasAttemptedInitialFolderRequest = false
    private var folderPickMode: FolderPickMode = FolderPickMode.INITIAL_REQUIRED
    private var pendingFolderBeforeChange: Uri? = null

    private val requestAudioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startRecordingSession()
            } else {
                setStatus(getString(R.string.recordings_microphone_permission_required))
                Toast.makeText(this, R.string.recordings_microphone_permission_required, Toast.LENGTH_SHORT).show()
                viewModel.setRecordingState(RecordingStateUi.ERROR)
            }
        }

    private val pickFolderLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) {
                when (folderPickMode) {
                    FolderPickMode.INITIAL_REQUIRED -> {
                        setStatus(getString(R.string.recordings_folder_required))
                        Toast.makeText(this, R.string.recordings_folder_required, Toast.LENGTH_SHORT).show()
                        viewModel.clearRootFolder(getString(R.string.recordings_folder_required))
                    }

                    FolderPickMode.CHANGE_REQUEST -> {
                        val fallback = pendingFolderBeforeChange
                        if (fallback != null) {
                            applySelectedFolder(fallback)
                        }
                        setStatus(getString(R.string.recordings_folder_change_canceled))
                    }
                }
                pendingFolderBeforeChange = null
                return@registerForActivityResult
            }

            val permissionFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            val permissionTaken = runCatching {
                contentResolver.takePersistableUriPermission(uri, permissionFlags)
            }.isSuccess

            if (!permissionTaken) {
                setStatus(getString(R.string.recordings_folder_permission_error))
                Toast.makeText(this, R.string.recordings_folder_permission_error, Toast.LENGTH_SHORT).show()
                val fallback = pendingFolderBeforeChange
                if (fallback != null) {
                    applySelectedFolder(fallback)
                }
                pendingFolderBeforeChange = null
                return@registerForActivityResult
            }

            recordingsPrefs.setFolderUri(uri)
            applySelectedFolder(uri)
            setStatus(getString(R.string.recordings_folder_selected))
            if (viewModel.uiState.value.recordingState == RecordingStateUi.ERROR) {
                viewModel.setRecordingState(RecordingStateUi.IDLE)
            }
            pendingFolderBeforeChange = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recordingsPrefs = RecordingsPrefs(this)
        targetFolderSegments = intent.getStringArrayListExtra(EXTRA_TARGET_FOLDER_PATH)
            ?.filter { it.isNotBlank() }
            .orEmpty()
        targetFolderMatchPrefix = intent.getStringExtra(EXTRA_TARGET_FOLDER_MATCH_PREFIX)?.takeIf { it.isNotBlank() }
        intentTargetLabel = intent.getStringExtra(EXTRA_TARGET_LABEL)?.takeIf { targetFolderSegments.isNotEmpty() }
        viewModel.setRecordingTarget(intentTargetLabel)

        restoreSavedFolder()
        // The recording state comes from the session, never from a fresh screen's assumption: a
        // re-created screen must show a recording that is still running as running.
        lifecycleScope.launch { session.state.collect { renderSession(it) } }
        lifecycleScope.launch { session.pendingState.collect { viewModel.setPending(it) } }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBackRequested()
                }
            }
        )

        setContent {
            LbmTheme(palette = currentPalette()) {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val recentItems = viewModel.recentItemsFlow.collectAsLazyPagingItems()

                RecordingsScreen(
                    uiState = uiState,
                    recentItems = recentItems,
                    onBack = { handleBackRequested() },
                    onChangeFolder = { showChangeFolderConfirmationDialog() },
                    onOpenFolder = { openFolderInExternalExplorer() },
                    onOpenManager = { openManagerPage() },
                    onStartRecording = { ensureMicrophonePermissionThenStart() },
                    onPauseResume = { togglePauseResume() },
                    onStopRecording = { stopAndPersistRecording() },
                    onSavePending = { savePendingRecording(it) },
                    onDeletePending = { showDeletePendingDialog(it) },
                    onFormatChanged = { viewModel.setSelectedFormat(it) },
                    onOpenRecording = { openRecordingInExternalPlayer(it) },
                    onShareRecording = { shareRecording(it) },
                    onRenameRecording = { showRenameRecordingDialog(it) },
                    onDeleteRecording = { showDeleteRecordingDialog(it) },
                    onAnalyzeRecording = { item ->
                        startActivity(
                            RecordingAnalysisActivity.intent(
                                context = this,
                                uri = item.uri,
                                name = item.name,
                                contextKey = AnalysisSettingsStore.recordingKey(item.uri.toString()),
                            )
                        )
                    }
                )
            }
        }
    }

    /**
     * Mirrors the session onto this screen; the first call comes from [onCreate], right after
     * [restoreSavedFolder] has written its «ready» line. A recording in progress always shows its
     * phase — over that line, on a re-created screen too. An error always shows what went wrong. A
     * finished save's line shows only when this screen saw it happen, so an old «saved» does not greet
     * the next visit (its toast was shown either way).
     *
     * The label says what the recording is *for*: while one is in progress that is the session's —
     * this page may have been opened without a hymn (the launcher shortcut) while a hymn's recording runs.
     */
    private fun renderSession(state: RecordingSessionState) {
        val status = when (state.phase) {
            RecordingStateUi.RECORDING -> getString(R.string.recordings_status_recording)
            RecordingStateUi.PAUSED -> getString(R.string.recordings_status_paused)
            RecordingStateUi.SAVING -> getString(R.string.recordings_status_saving)
            RecordingStateUi.ERROR -> state.lastEvent?.let { RecordingEventText.status(this, it) }
            RecordingStateUi.IDLE -> state.lastEvent?.takeIf { sessionShown }?.let { RecordingEventText.status(this, it) }
        }
        sessionShown = true
        val label = if (state.isActive) state.target?.label else intentTargetLabel
        viewModel.applySession(state, status, label)
    }

    private fun restoreSavedFolder() {
        val storedUri = recordingsPrefs.getFolderUri()
        if (storedUri != null && recordingsPrefs.hasPersistedReadWriteAccess(contentResolver, storedUri)) {
            applySelectedFolder(storedUri)
            setStatus(getString(R.string.recordings_ready))
            return
        }

        setStatus(getString(R.string.recordings_select_folder_first))
        viewModel.clearRootFolder(getString(R.string.recordings_select_folder_first))
        if (!hasAttemptedInitialFolderRequest) {
            hasAttemptedInitialFolderRequest = true
            launchFolderPicker(FolderPickMode.INITIAL_REQUIRED, storedUri)
        }
    }

    private fun applySelectedFolder(folderUri: Uri) {
        selectedFolderUri = folderUri
        val folderName = currentFolderDocument()?.name ?: getString(R.string.recordings_manage_root_label)
        viewModel.setRootFolder(folderUri, folderName)
    }

    private fun launchFolderPicker(mode: FolderPickMode, initialUri: Uri?) {
        folderPickMode = mode
        pendingFolderBeforeChange = if (mode == FolderPickMode.CHANGE_REQUEST) selectedFolderUri else null
        pickFolderLauncher.launch(initialUri)
    }

    private fun showChangeFolderConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.recordings_change_folder_confirm_title))
            .setMessage(getString(R.string.recordings_change_folder_confirm_message))
            .setPositiveButton(getString(R.string.recordings_change_folder_continue)) { _, _ ->
                launchFolderPicker(FolderPickMode.CHANGE_REQUEST, selectedFolderUri)
            }
            .setNegativeButton(getString(R.string.recordings_change_folder_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun currentFolderDocument(): DocumentFile? {
        val uri = selectedFolderUri ?: return null
        val folder = DocumentFile.fromTreeUri(this, uri) ?: return null
        return if (folder.exists() && folder.isDirectory && folder.canRead() && folder.canWrite()) {
            folder
        } else {
            null
        }
    }

    private fun openFolderInExternalExplorer() {
        val uri = selectedFolderUri
        if (uri == null) {
            setStatus(getString(R.string.recordings_select_folder_first))
            Toast.makeText(this, R.string.recordings_select_folder_first, Toast.LENGTH_SHORT).show()
            return
        }

        val folderDocumentUri = runCatching {
            DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
        }.getOrElse {
            uri
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(folderDocumentUri, DocumentsContract.Document.MIME_TYPE_DIR)
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, folderDocumentUri)
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
        }
        val chooser = Intent.createChooser(intent, getString(R.string.recordings_open_folder_with))
        runCatching {
            startActivity(chooser)
        }.onFailure {
            val fallbackIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, folderDocumentUri)
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                )
            }
            runCatching {
                startActivity(fallbackIntent)
            }.onFailure {
                setStatus(getString(R.string.recordings_open_folder_no_app))
                Toast.makeText(this, R.string.recordings_open_folder_no_app, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openManagerPage() {
        if (currentFolderDocument() == null) {
            setStatus(getString(R.string.recordings_select_folder_first))
            Toast.makeText(this, R.string.recordings_select_folder_first, Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(this, RecordingsManagerActivity::class.java))
    }

    private fun showDeleteRecordingDialog(item: RecordingListItem) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.recordings_delete_confirm_title))
            .setMessage(getString(R.string.recordings_delete_confirm_message, item.name))
            .setPositiveButton(getString(R.string.recordings_delete_confirm_button)) { _, _ ->
                viewModel.deleteItem(item) { result ->
                    when (result) {
                        DeleteOutcome.SUCCESS -> {
                            setStatus(getString(R.string.recordings_status_deleted_template, item.name))
                            Toast.makeText(this, R.string.recordings_deleted_ok, Toast.LENGTH_SHORT).show()
                        }

                        DeleteOutcome.REMOVED -> {
                            handleRemovedRecording(item)
                        }

                        DeleteOutcome.FAILED -> {
                            setStatus(getString(R.string.recordings_error_delete))
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

    private fun showRenameRecordingDialog(item: RecordingListItem) {
        val parsedName = RecordingDocumentOps.parseFileName(item.name)
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
                    setStatus(getString(R.string.recordings_rename_invalid_name))
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
                            setStatus(getString(R.string.recordings_status_renamed_template, targetName))
                            Toast.makeText(this, R.string.recordings_renamed_ok, Toast.LENGTH_SHORT).show()
                        }

                        RenameOutcome.NAME_EXISTS -> {
                            setStatus(getString(R.string.recordings_error_rename_name_exists))
                            Toast.makeText(this, R.string.recordings_error_rename_name_exists, Toast.LENGTH_SHORT).show()
                        }

                        RenameOutcome.REMOVED -> {
                            handleRemovedRecording(item)
                        }

                        RenameOutcome.FAILED -> {
                            setStatus(getString(R.string.recordings_error_rename))
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

    /**
     * Shares one recording through the system share sheet (ClickUp `869f4tpmq`).
     *
     * Checks the document still exists first, for the same reason opening does: a file moved or
     * deleted outside the app would otherwise produce an opaque failure instead of the «καταργήθηκε»
     * handling the rest of the screen uses.
     */
    private fun shareRecording(item: RecordingListItem) {
        lifecycleScope.launch {
            if (!recordingsRepository.checkRecordingExists(item.uri)) {
                handleRemovedRecording(item)
                return@launch
            }
            recordingExternalOpener.shareRecording(
                sourceUri = item.uri,
                fileName = item.name,
                mimeType = resolvePlaybackMimeType(item),
                chooserTitle = getString(R.string.recordings_share_chooser_title),
                onFailure = {
                    setStatus(getString(R.string.recordings_share_failed))
                    Toast.makeText(
                        this@RecordingsActivity,
                        R.string.recordings_share_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                },
            )
        }
    }

    private fun openRecordingInExternalPlayer(item: RecordingListItem) {
        lifecycleScope.launch {
            val exists = recordingsRepository.checkRecordingExists(item.uri)
            if (!exists) {
                handleRemovedRecording(item)
                return@launch
            }

            setStatus(getString(R.string.recordings_status_opening_template, item.name))
            val resolvedMimeType = resolvePlaybackMimeType(item)
            recordingExternalOpener.openRecordingWithChooser(
                sourceUri = item.uri,
                fileName = item.name,
                mimeType = resolvedMimeType,
                chooserTitle = getString(R.string.recordings_open_recording_with),
                onFailure = {
                    lifecycleScope.launch {
                        val stillExists = recordingsRepository.checkRecordingExists(item.uri)
                        if (!stillExists) {
                            handleRemovedRecording(item)
                        } else {
                            setStatus(getString(R.string.recordings_open_recording_no_app))
                            Toast.makeText(
                                this@RecordingsActivity,
                                R.string.recordings_open_recording_no_app,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
        }
    }

    private fun resolvePlaybackMimeType(item: RecordingListItem): String {
        val directType = item.mimeType
        if (!directType.isNullOrBlank() && directType != "application/octet-stream") {
            return directType
        }

        return RecordingFormatOption.resolveMimeTypeByFileName(item.name) ?: "audio/*"
    }

    private fun ensureMicrophonePermissionThenStart() {
        if (currentFolderDocument() == null) {
            setStatus(getString(R.string.recordings_select_folder_first))
            Toast.makeText(this, R.string.recordings_select_folder_first, Toast.LENGTH_SHORT).show()
            launchFolderPicker(FolderPickMode.INITIAL_REQUIRED, selectedFolderUri)
            return
        }

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            startRecordingSession()
        } else {
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startRecordingSession() {
        // The session refuses while a recording runs or saves; the result — recording, or
        // «Αποτυχία εκκίνησης» — arrives through its state.
        session.start(
            RecordingTarget(
                folderSegments = targetFolderSegments,
                folderMatchPrefix = targetFolderMatchPrefix,
                label = intentTargetLabel,
            )
        )
    }

    private fun togglePauseResume() {
        when (session.state.value.phase) {
            RecordingStateUi.RECORDING -> session.pause()
            RecordingStateUi.PAUSED -> session.resume()
            else -> Unit
        }
    }

    private fun stopAndPersistRecording() {
        session.stop()
    }

    /** «Αποθήκευση» on a recording kept in the app: into the folder picked now — or pick one first. */
    private fun savePendingRecording(item: PendingRecording) {
        if (currentFolderDocument() == null) {
            setStatus(getString(R.string.recordings_select_folder_first))
            Toast.makeText(this, R.string.recordings_select_folder_first, Toast.LENGTH_SHORT).show()
            launchFolderPicker(FolderPickMode.INITIAL_REQUIRED, selectedFolderUri)
            return
        }
        session.savePending(item)
    }

    /** A kept recording exists nowhere else, so deleting it is confirmed — and the dialog says so. */
    private fun showDeletePendingDialog(item: PendingRecording) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.recordings_delete_confirm_title))
            .setMessage(getString(R.string.recordings_pending_delete_confirm_message, item.baseName))
            .setPositiveButton(getString(R.string.recordings_delete_confirm_button)) { _, _ ->
                session.deletePending(item)
            }
            .setNegativeButton(getString(R.string.recordings_delete_cancel_button)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun handleRemovedRecording(item: RecordingListItem) {
        setStatus(getString(R.string.recordings_error_removed))
        Toast.makeText(this, R.string.recordings_error_removed, Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            recordingsRepository.removeOwnedRecording(item.uri)
        }
    }

    /**
     * Back handling (hero arrow + system back). While RECORDING/PAUSED we confirm: stop & save,
     * discard & exit (the one way a recording in progress is thrown away), or cancel. Otherwise —
     * SAVING included — the screen just closes: the save belongs to the session and finishes on its
     * own, with its toast and its place in «πρόσφατες», whether or not this screen is still here.
     */
    private fun handleBackRequested() {
        val state = session.state.value.phase
        if (state == RecordingStateUi.RECORDING || state == RecordingStateUi.PAUSED) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.recordings_exit_recording_title))
                .setMessage(getString(R.string.recordings_exit_recording_message))
                .setPositiveButton(getString(R.string.recordings_exit_stop_save)) { _, _ ->
                    stopAndPersistRecording()
                }
                .setNegativeButton(getString(R.string.recordings_exit_discard)) { _, _ ->
                    session.discard()
                    finish()
                }
                .setNeutralButton(getString(R.string.recordings_delete_cancel_button)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } else {
            finish()
        }
    }

    private fun setStatus(message: String) {
        viewModel.setStatusMessage(message)
    }

    private enum class FolderPickMode {
        INITIAL_REQUIRED,
        CHANGE_REQUEST
    }

    companion object {
        /** ArrayList<String>: sub-folders of the picked recordings folder to save into, created if missing. */
        const val EXTRA_TARGET_FOLDER_PATH = "com.johnchourp.learnbyzantinemusic.recordings.EXTRA_TARGET_FOLDER_PATH"

        /** String: an existing last folder whose name starts with this prefix is reused (e.g. "03 "). */
        const val EXTRA_TARGET_FOLDER_MATCH_PREFIX = "com.johnchourp.learnbyzantinemusic.recordings.EXTRA_TARGET_FOLDER_MATCH_PREFIX"

        /** String: what the recording is for, shown on the record card. */
        const val EXTRA_TARGET_LABEL = "com.johnchourp.learnbyzantinemusic.recordings.EXTRA_TARGET_LABEL"
    }
}
