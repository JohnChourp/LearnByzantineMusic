package com.johnchourp.learnbyzantinemusic.recordings

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.recordings.index.RecordingsRepository
import com.johnchourp.learnbyzantinemusic.recordings.ui.RecordingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    private var selectedFolderUri: Uri? = null
    private var hasAttemptedInitialFolderRequest = false
    private var folderPickMode: FolderPickMode = FolderPickMode.INITIAL_REQUIRED
    private var pendingFolderBeforeChange: Uri? = null

    private val ioLock = Any()
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var tempWavFile: File? = null
    private var tempOutputStream: FileOutputStream? = null
    private var pcmBytesWritten: Long = 0L

    @Volatile
    private var shouldRecord = false

    @Volatile
    private var isPaused = false

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

        restoreSavedFolder()
        viewModel.setRecordingState(RecordingStateUi.IDLE)

        setContent {
            MaterialTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val recentItems = viewModel.recentItemsFlow.collectAsLazyPagingItems()

                RecordingsScreen(
                    uiState = uiState,
                    recentItems = recentItems,
                    onChangeFolder = { showChangeFolderConfirmationDialog() },
                    onOpenFolder = { openFolderInExternalExplorer() },
                    onOpenManager = { openManagerPage() },
                    onStartRecording = { ensureMicrophonePermissionThenStart() },
                    onPauseResume = { togglePauseResume() },
                    onStopRecording = { stopAndPersistRecording() },
                    onFormatChanged = { viewModel.setSelectedFormat(it) },
                    onOpenRecording = { openRecordingInExternalPlayer(it) },
                    onRenameRecording = { showRenameRecordingDialog(it) },
                    onDeleteRecording = { showDeleteRecordingDialog(it) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.requestReindex(force = false)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCaptureInfrastructure()
        cleanupTempFiles()
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
        val recordingState = viewModel.uiState.value.recordingState
        if (recordingState == RecordingStateUi.RECORDING || recordingState == RecordingStateUi.PAUSED || recordingState == RecordingStateUi.SAVING) {
            return
        }

        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_ENCODING)
        if (minBuffer <= 0) {
            setStatus(getString(R.string.recordings_error_start))
            viewModel.setRecordingState(RecordingStateUi.ERROR)
            return
        }

        val bufferSize = (minBuffer * 2).coerceAtLeast(8192)
        val tempFile = File.createTempFile("recording_", ".wav", cacheDir)

        try {
            tempWavFile = tempFile
            tempOutputStream = FileOutputStream(tempFile).apply {
                write(ByteArray(WAV_HEADER_SIZE))
                flush()
            }
            pcmBytesWritten = 0L
            shouldRecord = true
            isPaused = false

            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_ENCODING,
                bufferSize
            )

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                throw IllegalStateException("audio_record_not_initialized")
            }

            audioRecord = recorder
            recorder.startRecording()
            recordingThread = Thread {
                runCaptureLoop(bufferSize)
            }.apply { start() }

            viewModel.setRecordingState(RecordingStateUi.RECORDING)
            setStatus(getString(R.string.recordings_status_recording))
        } catch (_: Throwable) {
            stopCaptureInfrastructure()
            cleanupTempFiles()
            setStatus(getString(R.string.recordings_error_start))
            viewModel.setRecordingState(RecordingStateUi.ERROR)
        }
    }

    private fun runCaptureLoop(bufferSize: Int) {
        val recorder = audioRecord ?: return
        val localBuffer = ByteArray(bufferSize)

        while (shouldRecord) {
            val readBytes = recorder.read(localBuffer, 0, localBuffer.size)
            if (readBytes <= 0) {
                continue
            }
            if (isPaused) {
                continue
            }

            synchronized(ioLock) {
                tempOutputStream?.write(localBuffer, 0, readBytes)
                pcmBytesWritten += readBytes
            }
        }
    }

    private fun togglePauseResume() {
        when (viewModel.uiState.value.recordingState) {
            RecordingStateUi.RECORDING -> {
                isPaused = true
                viewModel.setRecordingState(RecordingStateUi.PAUSED)
                setStatus(getString(R.string.recordings_status_paused))
            }

            RecordingStateUi.PAUSED -> {
                isPaused = false
                viewModel.setRecordingState(RecordingStateUi.RECORDING)
                setStatus(getString(R.string.recordings_status_recording))
            }

            else -> Unit
        }
    }

    private fun stopAndPersistRecording() {
        if (viewModel.uiState.value.recordingState != RecordingStateUi.RECORDING && viewModel.uiState.value.recordingState != RecordingStateUi.PAUSED) {
            return
        }

        viewModel.setRecordingState(RecordingStateUi.SAVING)
        setStatus(getString(R.string.recordings_status_saving))

        lifecycleScope.launch {
            val saveResult = withContext(Dispatchers.IO) { persistRecording() }
            saveResult.onSuccess { savedItem ->
                val rootUri = selectedFolderUri
                if (rootUri != null) {
                    lifecycleScope.launch {
                        recordingsRepository.registerOwnedRecording(rootUri, savedItem)
                    }
                }

                setStatus(getString(R.string.recordings_status_saved_template, savedItem.name))
                Toast.makeText(this@RecordingsActivity, R.string.recordings_saved_ok, Toast.LENGTH_SHORT).show()
                viewModel.setRecordingState(RecordingStateUi.IDLE)
                viewModel.requestReindex(force = true)
            }.onFailure {
                setStatus(getString(R.string.recordings_error_save))
                Toast.makeText(this@RecordingsActivity, R.string.recordings_error_save, Toast.LENGTH_SHORT).show()
                viewModel.setRecordingState(RecordingStateUi.ERROR)
            }
        }
    }

    private fun persistRecording(): Result<RecordingListItem> {
        return runCatching {
            stopCaptureInfrastructure()

            val wavSource = tempWavFile ?: error("missing_temp_wav")
            writeWavHeader(wavSource, pcmBytesWritten)

            val folder = currentFolderDocument() ?: error("folder_not_available")
            val selectedFormat = viewModel.uiState.value.selectedFormat
            val targetFileName = generateTargetFileName(selectedFormat)
            val targetDocument = folder.createFile(selectedFormat.mimeType, targetFileName)
                ?: error("create_target_failed")

            val sourceForCopy = if (selectedFormat == RecordingFormatOption.WAV) {
                wavSource
            } else {
                val transcodedFile = File.createTempFile("recording_encoded_", ".${selectedFormat.extension}", cacheDir)
                val transcodeResult = AudioTranscoder.transcode(
                    sourceWav = wavSource,
                    outputFile = transcodedFile,
                    format = selectedFormat
                )
                if (!transcodeResult.isSuccess) {
                    transcodedFile.delete()
                    error(transcodeResult.details)
                }
                transcodedFile
            }

            try {
                contentResolver.openOutputStream(targetDocument.uri, "w")?.use { outputStream ->
                    sourceForCopy.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: error("output_stream_not_available")
            } finally {
                if (sourceForCopy != wavSource) {
                    sourceForCopy.delete()
                }
                cleanupTempFiles()
            }

            val resolvedName = targetDocument.name ?: targetFileName
            val updatedTimestamp = runCatching { targetDocument.lastModified() }
                .getOrDefault(System.currentTimeMillis())
                .takeIf { it > 0L }
                ?: System.currentTimeMillis()
            val createdTimestamp = RecordingDocumentOps.resolveCreationLikeTimestamp(resolvedName) ?: updatedTimestamp

            RecordingListItem(
                name = resolvedName,
                uri = targetDocument.uri,
                mimeType = targetDocument.type ?: selectedFormat.mimeType,
                relativePath = resolvedName,
                parentRelativePath = "/",
                parentUri = folder.uri,
                createdTimestamp = createdTimestamp,
                updatedTimestamp = updatedTimestamp
            )
        }
    }

    private fun stopCaptureInfrastructure() {
        shouldRecord = false

        val recorder = audioRecord
        runCatching {
            recorder?.stop()
        }
        runCatching {
            recordingThread?.join(1200)
        }
        recordingThread = null
        runCatching {
            recorder?.release()
        }
        audioRecord = null

        synchronized(ioLock) {
            runCatching { tempOutputStream?.flush() }
            runCatching { tempOutputStream?.close() }
            tempOutputStream = null
        }
    }

    private fun cleanupTempFiles() {
        tempWavFile?.delete()
        tempWavFile = null
        pcmBytesWritten = 0L
        isPaused = false
    }

    private fun writeWavHeader(file: File, pcmBytes: Long) {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = SAMPLE_RATE * channels * (bitsPerSample / 8)
        val blockAlign = channels * (bitsPerSample / 8)
        val totalDataLen = pcmBytes + 36

        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(0)
            raf.writeBytes("RIFF")
            raf.writeInt(Integer.reverseBytes(totalDataLen.toInt()))
            raf.writeBytes("WAVE")
            raf.writeBytes("fmt ")
            raf.writeInt(Integer.reverseBytes(16))
            raf.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt())
            raf.writeShort(java.lang.Short.reverseBytes(channels.toShort()).toInt())
            raf.writeInt(Integer.reverseBytes(SAMPLE_RATE))
            raf.writeInt(Integer.reverseBytes(byteRate))
            raf.writeShort(java.lang.Short.reverseBytes(blockAlign.toShort()).toInt())
            raf.writeShort(java.lang.Short.reverseBytes(bitsPerSample.toShort()).toInt())
            raf.writeBytes("data")
            raf.writeInt(Integer.reverseBytes(pcmBytes.toInt()))
        }
    }

    private fun generateTargetFileName(format: RecordingFormatOption): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "recording_$timestamp.${format.extension}"
    }

    private fun handleRemovedRecording(item: RecordingListItem) {
        setStatus(getString(R.string.recordings_error_removed))
        Toast.makeText(this, R.string.recordings_error_removed, Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            recordingsRepository.removeOwnedRecording(item.uri)
            viewModel.requestReindex(force = true)
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
        private const val SAMPLE_RATE = 44_100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val WAV_HEADER_SIZE = 44
    }
}
