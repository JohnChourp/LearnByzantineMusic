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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingsActivity : BaseActivity() {
    private lateinit var folderTextView: TextView
    private lateinit var recordingsListView: ListView
    private lateinit var recordingsEmptyTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var startRecordingButton: Button
    private lateinit var pauseResumeButton: Button
    private lateinit var stopRecordingButton: Button
    private lateinit var formatSpinner: Spinner
    private lateinit var formatDescriptionTextView: TextView
    private lateinit var changeFolderButton: Button
    private lateinit var openFolderButton: Button

    private lateinit var recordingsPrefs: RecordingsPrefs
    private lateinit var recordingsAdapter: BaseAdapter

    private val recordingItems = mutableListOf<RecordingListItem>()
    private val ioLock = Any()

    private var selectedFolderUri: Uri? = null
    private var selectedFormat: RecordingFormatOption = RecordingFormatOption.FLAC
    private var recordingState: RecordingState = RecordingState.IDLE

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var tempWavFile: File? = null
    private var tempOutputStream: FileOutputStream? = null
    private var pcmBytesWritten: Long = 0L

    @Volatile
    private var shouldRecord = false

    @Volatile
    private var isPaused = false

    private var hasAttemptedInitialFolderRequest = false
    private var folderPickMode: FolderPickMode = FolderPickMode.INITIAL_REQUIRED
    private var pendingFolderBeforeChange: Uri? = null

    private val requestAudioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startRecordingSession()
            } else {
                showStatus(getString(R.string.recordings_microphone_permission_required))
                Toast.makeText(this, R.string.recordings_microphone_permission_required, Toast.LENGTH_SHORT).show()
                setRecordingState(RecordingState.ERROR)
            }
        }

    private val pickFolderLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) {
                when (folderPickMode) {
                    FolderPickMode.INITIAL_REQUIRED -> {
                        showStatus(getString(R.string.recordings_folder_required))
                        Toast.makeText(this, R.string.recordings_folder_required, Toast.LENGTH_SHORT).show()
                        updateFolderState(null)
                        refreshRecordingsList()
                    }

                    FolderPickMode.CHANGE_REQUEST -> {
                        updateFolderState(pendingFolderBeforeChange)
                        refreshRecordingsList()
                        showStatus(getString(R.string.recordings_folder_change_canceled))
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
                showStatus(getString(R.string.recordings_folder_permission_error))
                Toast.makeText(this, R.string.recordings_folder_permission_error, Toast.LENGTH_SHORT).show()
                if (folderPickMode == FolderPickMode.CHANGE_REQUEST) {
                    updateFolderState(pendingFolderBeforeChange)
                    refreshRecordingsList()
                } else {
                    updateFolderState(null)
                    refreshRecordingsList()
                }
                pendingFolderBeforeChange = null
                return@registerForActivityResult
            }

            recordingsPrefs.setFolderUri(uri)
            updateFolderState(uri)
            refreshRecordingsList()
            showStatus(getString(R.string.recordings_folder_selected))
            pendingFolderBeforeChange = null
            if (recordingState == RecordingState.ERROR) {
                setRecordingState(RecordingState.IDLE)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_recordings)

        recordingsPrefs = RecordingsPrefs(this)

        folderTextView = findViewById(R.id.recordings_folder_text_view)
        recordingsListView = findViewById(R.id.recordings_list_view)
        recordingsEmptyTextView = findViewById(R.id.recordings_empty_text_view)
        statusTextView = findViewById(R.id.recordings_status_text_view)
        startRecordingButton = findViewById(R.id.recordings_start_button)
        pauseResumeButton = findViewById(R.id.recordings_pause_resume_button)
        stopRecordingButton = findViewById(R.id.recordings_stop_button)
        formatSpinner = findViewById(R.id.recordings_format_spinner)
        formatDescriptionTextView = findViewById(R.id.recordings_format_description_text_view)
        changeFolderButton = findViewById(R.id.recordings_change_folder_button)
        openFolderButton = findViewById(R.id.recordings_open_folder_button)

        setupRecordingsList()
        setupFormatSelector()
        setupActions()
        restoreSavedFolder()
        setRecordingState(RecordingState.IDLE)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCaptureInfrastructure()
        cleanupTempFiles()
    }

    private fun setupRecordingsList() {
        recordingsAdapter = RecordingListAdapter()
        recordingsListView.adapter = recordingsAdapter
        recordingsListView.emptyView = recordingsEmptyTextView
        recordingsListView.setOnItemClickListener { _, _, position, _ ->
            val item = recordingItems.getOrNull(position) ?: return@setOnItemClickListener
            openRecordingInExternalPlayer(item)
        }
    }

    private fun setupFormatSelector() {
        selectedFormat = recordingsPrefs.getSelectedFormat()
        val labels = RecordingFormatOption.entries.map { formatOption -> getString(formatOption.labelResId) }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        formatSpinner.adapter = adapter

        val selectedIndex = RecordingFormatOption.entries.indexOf(selectedFormat).coerceAtLeast(0)
        formatSpinner.setSelection(selectedIndex, false)
        updateFormatDescription(selectedFormat)

        formatSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedFormat = RecordingFormatOption.entries.getOrNull(position) ?: RecordingFormatOption.FLAC
                recordingsPrefs.setSelectedFormat(selectedFormat)
                updateFormatDescription(selectedFormat)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
    }

    private fun setupActions() {
        changeFolderButton.setOnClickListener {
            showChangeFolderConfirmationDialog()
        }

        openFolderButton.setOnClickListener {
            openFolderInExternalExplorer()
        }

        startRecordingButton.setOnClickListener {
            ensureMicrophonePermissionThenStart()
        }

        pauseResumeButton.setOnClickListener {
            togglePauseResume()
        }

        stopRecordingButton.setOnClickListener {
            stopAndPersistRecording()
        }
    }

    private fun restoreSavedFolder() {
        val storedUri = recordingsPrefs.getFolderUri()
        if (storedUri != null && recordingsPrefs.hasPersistedReadWriteAccess(contentResolver, storedUri)) {
            updateFolderState(storedUri)
            refreshRecordingsList()
            showStatus(getString(R.string.recordings_ready))
            return
        }

        updateFolderState(null)
        showStatus(getString(R.string.recordings_select_folder_first))
        if (!hasAttemptedInitialFolderRequest) {
            hasAttemptedInitialFolderRequest = true
            launchFolderPicker(FolderPickMode.INITIAL_REQUIRED, storedUri)
        }
    }

    private fun launchFolderPicker(mode: FolderPickMode, initialUri: Uri?) {
        folderPickMode = mode
        pendingFolderBeforeChange = if (mode == FolderPickMode.CHANGE_REQUEST) {
            selectedFolderUri
        } else {
            null
        }
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

    private fun updateFolderState(folderUri: Uri?) {
        selectedFolderUri = folderUri
        val folderName = currentFolderDocument()?.name
        folderTextView.text = if (folderUri == null || folderName.isNullOrBlank()) {
            getString(R.string.recordings_folder_not_selected)
        } else {
            getString(R.string.recordings_folder_selected_template, folderName)
        }
        val hasFolder = currentFolderDocument() != null
        startRecordingButton.isEnabled = hasFolder
        openFolderButton.isEnabled = hasFolder
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

    private fun refreshRecordingsList() {
        val files = currentFolderDocument()
            ?.listFiles()
            ?.filter { doc -> doc.isFile && RecordingFormatOption.supportsFileName(doc.name) }
            ?.sortedByDescending { doc -> doc.lastModified() }
            ?: emptyList()

        recordingItems.clear()
        recordingItems.addAll(files.map { file ->
            RecordingListItem(
                name = file.name ?: getString(R.string.recordings_unnamed_file),
                uri = file.uri,
                mimeType = file.type
            )
        })

        recordingsAdapter.notifyDataSetChanged()
    }

    private fun openFolderInExternalExplorer() {
        val uri = selectedFolderUri
        if (uri == null) {
            showStatus(getString(R.string.recordings_select_folder_first))
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
                showStatus(getString(R.string.recordings_open_folder_no_app))
                Toast.makeText(this, R.string.recordings_open_folder_no_app, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteRecordingDialog(item: RecordingListItem) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.recordings_delete_confirm_title))
            .setMessage(getString(R.string.recordings_delete_confirm_message, item.name))
            .setPositiveButton(getString(R.string.recordings_delete_confirm_button)) { _, _ ->
                val deleted = runCatching {
                    val document = DocumentFile.fromSingleUri(this, item.uri)
                    document != null && document.exists() && document.delete()
                }.getOrDefault(false)

                if (deleted) {
                    showStatus(getString(R.string.recordings_status_deleted_template, item.name))
                    Toast.makeText(this, R.string.recordings_deleted_ok, Toast.LENGTH_SHORT).show()
                    refreshRecordingsList()
                } else {
                    showStatus(getString(R.string.recordings_error_delete))
                    Toast.makeText(this, R.string.recordings_error_delete, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.recordings_delete_cancel_button)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showRenameRecordingDialog(item: RecordingListItem) {
        val parsedName = parseFileName(item.name)
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
                val userInput = inputField.text?.toString()?.trim().orEmpty()
                val sanitizedBaseName = userInput.replace("/", "_")
                if (sanitizedBaseName.isBlank()) {
                    showStatus(getString(R.string.recordings_rename_invalid_name))
                    Toast.makeText(this, R.string.recordings_rename_invalid_name, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val targetName = parsedName.extension?.let { extension ->
                    "$sanitizedBaseName.$extension"
                } ?: sanitizedBaseName

                if (targetName == item.name) {
                    return@setPositiveButton
                }

                val renamed = runCatching {
                    val document = DocumentFile.fromSingleUri(this, item.uri)
                    document != null && document.exists() && document.renameTo(targetName)
                }.getOrDefault(false)

                if (renamed) {
                    showStatus(getString(R.string.recordings_status_renamed_template, targetName))
                    Toast.makeText(this, R.string.recordings_renamed_ok, Toast.LENGTH_SHORT).show()
                    refreshRecordingsList()
                } else {
                    showStatus(getString(R.string.recordings_error_rename))
                    Toast.makeText(this, R.string.recordings_error_rename, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.recordings_rename_cancel_button)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun parseFileName(fullName: String): ParsedFileName {
        val dotIndex = fullName.lastIndexOf('.')
        return if (dotIndex <= 0 || dotIndex >= fullName.lastIndex) {
            ParsedFileName(baseName = fullName, extension = null)
        } else {
            ParsedFileName(
                baseName = fullName.substring(0, dotIndex),
                extension = fullName.substring(dotIndex + 1)
            )
        }
    }

    private fun openRecordingInExternalPlayer(item: RecordingListItem) {
        val resolvedMimeType = resolvePlaybackMimeType(item)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(item.uri, resolvedMimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            startActivity(intent)
        }.onFailure {
            showStatus(getString(R.string.recordings_open_recording_no_app))
            Toast.makeText(this, R.string.recordings_open_recording_no_app, Toast.LENGTH_SHORT).show()
        }
    }

    private fun resolvePlaybackMimeType(item: RecordingListItem): String {
        val directType = item.mimeType
        if (!directType.isNullOrBlank() && directType != "application/octet-stream") {
            return directType
        }

        return when (item.name.substringAfterLast('.', "").lowercase(Locale.US)) {
            "flac" -> "audio/flac"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "aac" -> "audio/aac"
            "m4a" -> "audio/mp4"
            "opus" -> "audio/ogg"
            else -> "audio/*"
        }
    }

    private fun ensureMicrophonePermissionThenStart() {
        if (currentFolderDocument() == null) {
            showStatus(getString(R.string.recordings_select_folder_first))
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
        if (recordingState == RecordingState.RECORDING || recordingState == RecordingState.PAUSED || recordingState == RecordingState.SAVING) {
            return
        }

        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_ENCODING)
        if (minBuffer <= 0) {
            showStatus(getString(R.string.recordings_error_start))
            setRecordingState(RecordingState.ERROR)
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

            setRecordingState(RecordingState.RECORDING)
            showStatus(getString(R.string.recordings_status_recording))
        } catch (error: Throwable) {
            stopCaptureInfrastructure()
            cleanupTempFiles()
            showStatus(getString(R.string.recordings_error_start))
            setRecordingState(RecordingState.ERROR)
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
        when (recordingState) {
            RecordingState.RECORDING -> {
                isPaused = true
                setRecordingState(RecordingState.PAUSED)
                showStatus(getString(R.string.recordings_status_paused))
            }

            RecordingState.PAUSED -> {
                isPaused = false
                setRecordingState(RecordingState.RECORDING)
                showStatus(getString(R.string.recordings_status_recording))
            }

            else -> Unit
        }
    }

    private fun stopAndPersistRecording() {
        if (recordingState != RecordingState.RECORDING && recordingState != RecordingState.PAUSED) {
            return
        }

        setRecordingState(RecordingState.SAVING)
        showStatus(getString(R.string.recordings_status_saving))

        lifecycleScope.launch {
            val saveResult = withContext(Dispatchers.IO) { persistRecording() }
            saveResult.onSuccess { savedName ->
                showStatus(getString(R.string.recordings_status_saved_template, savedName))
                Toast.makeText(this@RecordingsActivity, R.string.recordings_saved_ok, Toast.LENGTH_SHORT).show()
                refreshRecordingsList()
                setRecordingState(RecordingState.IDLE)
            }.onFailure {
                showStatus(getString(R.string.recordings_error_save))
                Toast.makeText(this@RecordingsActivity, R.string.recordings_error_save, Toast.LENGTH_SHORT).show()
                setRecordingState(RecordingState.ERROR)
            }
        }
    }

    private fun persistRecording(): Result<String> {
        return runCatching {
            stopCaptureInfrastructure()

            val wavSource = tempWavFile ?: error("missing_temp_wav")
            writeWavHeader(wavSource, pcmBytesWritten)

            val folder = currentFolderDocument() ?: error("folder_not_available")
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

            targetDocument.name ?: targetFileName
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

    private fun updateFormatDescription(format: RecordingFormatOption) {
        formatDescriptionTextView.text = getString(format.descriptionResId)
    }

    private fun setRecordingState(newState: RecordingState) {
        recordingState = newState
        when (newState) {
            RecordingState.IDLE, RecordingState.ERROR -> {
                startRecordingButton.visibility = View.VISIBLE
                startRecordingButton.isEnabled = currentFolderDocument() != null
                pauseResumeButton.visibility = View.GONE
                stopRecordingButton.visibility = View.GONE
                pauseResumeButton.text = getString(R.string.recordings_pause)
            }

            RecordingState.RECORDING -> {
                startRecordingButton.visibility = View.GONE
                pauseResumeButton.visibility = View.VISIBLE
                stopRecordingButton.visibility = View.VISIBLE
                pauseResumeButton.isEnabled = true
                stopRecordingButton.isEnabled = true
                pauseResumeButton.text = getString(R.string.recordings_pause)
            }

            RecordingState.PAUSED -> {
                startRecordingButton.visibility = View.GONE
                pauseResumeButton.visibility = View.VISIBLE
                stopRecordingButton.visibility = View.VISIBLE
                pauseResumeButton.isEnabled = true
                stopRecordingButton.isEnabled = true
                pauseResumeButton.text = getString(R.string.recordings_resume)
            }

            RecordingState.SAVING -> {
                startRecordingButton.visibility = View.GONE
                pauseResumeButton.visibility = View.VISIBLE
                stopRecordingButton.visibility = View.VISIBLE
                pauseResumeButton.isEnabled = false
                stopRecordingButton.isEnabled = false
            }
        }
    }

    private fun showStatus(message: String) {
        statusTextView.text = message
    }

    private enum class RecordingState {
        IDLE,
        RECORDING,
        PAUSED,
        SAVING,
        ERROR
    }

    private enum class FolderPickMode {
        INITIAL_REQUIRED,
        CHANGE_REQUEST
    }

    private data class RecordingListItem(
        val name: String,
        val uri: Uri,
        val mimeType: String?
    )

    private data class ParsedFileName(
        val baseName: String,
        val extension: String?
    )

    private data class RecordingRowViewHolder(
        val nameTextView: TextView,
        val renameButton: ImageButton,
        val deleteButton: ImageButton
    )

    private inner class RecordingListAdapter : BaseAdapter() {
        private val layoutInflater = LayoutInflater.from(this@RecordingsActivity)

        override fun getCount(): Int = recordingItems.size

        override fun getItem(position: Int): Any = recordingItems[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val rowView: View
            val holder: RecordingRowViewHolder

            if (convertView == null) {
                rowView = layoutInflater.inflate(R.layout.list_item_recording, parent, false)
                holder = RecordingRowViewHolder(
                    nameTextView = rowView.findViewById(R.id.recording_item_name_text_view),
                    renameButton = rowView.findViewById(R.id.recording_item_rename_button),
                    deleteButton = rowView.findViewById(R.id.recording_item_delete_button)
                )
                rowView.tag = holder
            } else {
                rowView = convertView
                holder = rowView.tag as RecordingRowViewHolder
            }

            val item = recordingItems[position]
            holder.nameTextView.text = item.name
            holder.renameButton.setOnClickListener { showRenameRecordingDialog(item) }
            holder.deleteButton.setOnClickListener { showDeleteRecordingDialog(item) }
            return rowView
        }
    }

    companion object {
        private const val SAMPLE_RATE = 44_100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val WAV_HEADER_SIZE = 44
    }
}
