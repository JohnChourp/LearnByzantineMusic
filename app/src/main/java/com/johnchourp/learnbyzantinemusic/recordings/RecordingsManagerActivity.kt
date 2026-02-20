package com.johnchourp.learnbyzantinemusic.recordings

import android.app.AlertDialog
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R
import kotlinx.coroutines.launch

class RecordingsManagerActivity : BaseActivity() {
    private lateinit var breadcrumbTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var dropZoneTextView: TextView
    private lateinit var listView: ListView
    private lateinit var emptyTextView: TextView
    private lateinit var backButton: Button
    private lateinit var createFolderButton: Button

    private lateinit var recordingsPrefs: RecordingsPrefs
    private lateinit var entriesAdapter: BaseAdapter
    private val recordingExternalOpener by lazy { RecordingExternalOpener(this) }

    private val entries = mutableListOf<ManagerEntry>()
    private val currentPathSegments = mutableListOf<String>()

    private var rootFolderUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_recordings_manager)

        recordingsPrefs = RecordingsPrefs(this)

        breadcrumbTextView = findViewById(R.id.recordings_manager_breadcrumb_text_view)
        statusTextView = findViewById(R.id.recordings_manager_status_text_view)
        dropZoneTextView = findViewById(R.id.recordings_manager_drop_zone_text_view)
        listView = findViewById(R.id.recordings_manager_list_view)
        emptyTextView = findViewById(R.id.recordings_manager_empty_text_view)
        backButton = findViewById(R.id.recordings_manager_back_button)
        createFolderButton = findViewById(R.id.recordings_manager_create_folder_button)

        setupList()
        setupActions()
        initRootFolder()
        setupBackHandling()
    }

    override fun onResume() {
        super.onResume()
        refreshEntries()
    }

    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!navigateUpFolder()) {
                        finish()
                    }
                }
            }
        )
    }

    private fun setupList() {
        entriesAdapter = ManagerEntriesAdapter()
        listView.adapter = entriesAdapter
        listView.emptyView = emptyTextView
    }

    private fun setupActions() {
        backButton.setOnClickListener {
            if (!navigateUpFolder()) {
                finish()
            }
        }

        createFolderButton.setOnClickListener {
            showCreateFolderDialog()
        }

        dropZoneTextView.setOnDragListener { view, event ->
            handleDropTargetDrag(
                targetView = view,
                event = event,
                targetParentUri = currentFolderDocument()?.uri,
                targetRelativePath = currentRelativePath()
            )
        }
    }

    private fun initRootFolder() {
        val storedUri = recordingsPrefs.getFolderUri()
        if (storedUri == null || !recordingsPrefs.hasPersistedReadWriteAccess(contentResolver, storedUri)) {
            Toast.makeText(this, R.string.recordings_select_folder_first, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        rootFolderUri = storedUri
        refreshEntries()
        showStatus(getString(R.string.recordings_manage_ready))
    }

    private fun getRootFolderDocument(): DocumentFile? {
        val uri = rootFolderUri ?: return null
        val folder = DocumentFile.fromTreeUri(this, uri) ?: return null
        return if (folder.exists() && folder.isDirectory && folder.canRead() && folder.canWrite()) {
            folder
        } else {
            null
        }
    }

    private fun currentFolderDocument(): DocumentFile? {
        var currentFolder = getRootFolderDocument() ?: return null
        for (segment in currentPathSegments) {
            val nextFolder = currentFolder.findFile(segment)
            if (nextFolder == null || !nextFolder.exists() || !nextFolder.isDirectory) {
                return null
            }
            currentFolder = nextFolder
        }
        return currentFolder
    }

    private fun currentRelativePath(): String = currentPathSegments.joinToString("/")

    private fun refreshEntries() {
        val folder = currentFolderDocument()
        if (folder == null) {
            if (currentPathSegments.isNotEmpty()) {
                currentPathSegments.clear()
                refreshEntries()
                return
            }
            entries.clear()
            entriesAdapter.notifyDataSetChanged()
            updatePathUi()
            return
        }

        val children = runCatching { folder.listFiles().toList() }.getOrElse { emptyList() }
        val folders = children
            .filter { it.isDirectory }
            .sortedByDescending { it.lastModified() }
        val audioFiles = children
            .filter { it.isFile && RecordingFormatOption.supportsFileName(it.name) }
            .sortedByDescending { it.lastModified() }

        val parentRelativePath = currentRelativePath()
        entries.clear()

        entries.addAll(
            folders.mapNotNull { doc ->
                val name = doc.name ?: return@mapNotNull null
                val relativePath = if (parentRelativePath.isBlank()) {
                    name
                } else {
                    "$parentRelativePath/$name"
                }
                ManagerEntry(
                    type = ManagerEntryType.FOLDER,
                    name = name,
                    uri = doc.uri,
                    parentUri = folder.uri,
                    mimeType = doc.type,
                    relativePath = relativePath,
                    lastModified = doc.lastModified()
                )
            }
        )

        entries.addAll(
            audioFiles.mapNotNull { doc ->
                val name = doc.name ?: return@mapNotNull null
                val relativePath = if (parentRelativePath.isBlank()) {
                    name
                } else {
                    "$parentRelativePath/$name"
                }
                ManagerEntry(
                    type = ManagerEntryType.AUDIO_FILE,
                    name = name,
                    uri = doc.uri,
                    parentUri = folder.uri,
                    mimeType = doc.type,
                    relativePath = relativePath,
                    lastModified = doc.lastModified()
                )
            }
        )

        entriesAdapter.notifyDataSetChanged()
        updatePathUi()
    }

    private fun updatePathUi() {
        val rootName = getRootFolderDocument()?.name ?: getString(R.string.recordings_manage_root_label)
        val currentPath = currentRelativePath()
        val displayPath = toDisplayPath(currentPath)

        breadcrumbTextView.text = getString(R.string.recordings_manage_breadcrumb_template, rootName, displayPath)
        dropZoneTextView.text = getString(R.string.recordings_manage_drop_here_template, displayPath)
        backButton.isEnabled = currentPathSegments.isNotEmpty()
    }

    private fun navigateUpFolder(): Boolean {
        if (currentPathSegments.isEmpty()) {
            return false
        }
        currentPathSegments.removeLastOrNull()
        refreshEntries()
        return true
    }

    private fun enterFolder(folderEntry: ManagerEntry) {
        if (folderEntry.type != ManagerEntryType.FOLDER) {
            return
        }
        currentPathSegments.clear()
        currentPathSegments.addAll(folderEntry.relativePath.split('/').filter { it.isNotBlank() })
        refreshEntries()
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
                    showStatus(getString(R.string.recordings_rename_invalid_name))
                    Toast.makeText(this, R.string.recordings_rename_invalid_name, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val parentFolder = currentFolderDocument()
                if (parentFolder == null) {
                    showStatus(getString(R.string.recordings_manage_error_create_folder))
                    Toast.makeText(this, R.string.recordings_manage_error_create_folder, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (parentFolder.findFile(folderName) != null) {
                    showStatus(getString(R.string.recordings_manage_error_name_exists))
                    Toast.makeText(this, R.string.recordings_manage_error_name_exists, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val created = runCatching {
                    parentFolder.createDirectory(folderName)
                }.getOrNull()

                if (created != null) {
                    showStatus(getString(R.string.recordings_manage_folder_created_template, folderName))
                    refreshEntries()
                } else {
                    showStatus(getString(R.string.recordings_manage_error_create_folder))
                    Toast.makeText(this, R.string.recordings_manage_error_create_folder, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.recordings_rename_cancel_button)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showRenameEntryDialog(entry: ManagerEntry) {
        val parsedName = if (entry.type == ManagerEntryType.AUDIO_FILE) {
            RecordingDocumentOps.parseFileName(entry.name)
        } else {
            ParsedFileName(entry.name, null)
        }

        val inputField = EditText(this).apply {
            setSingleLine(true)
            setText(parsedName.baseName)
            setSelection(text.length)
            hint = getString(R.string.recordings_rename_input_hint)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.recordings_rename_confirm_title))
            .setMessage(getString(R.string.recordings_rename_confirm_message, entry.name))
            .setView(inputField)
            .setPositiveButton(getString(R.string.recordings_rename_confirm_button)) { _, _ ->
                val sanitizedBaseName = RecordingDocumentOps.sanitizeName(inputField.text?.toString().orEmpty())
                if (sanitizedBaseName.isBlank()) {
                    showStatus(getString(R.string.recordings_rename_invalid_name))
                    Toast.makeText(this, R.string.recordings_rename_invalid_name, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val targetName = parsedName.extension?.let { extension ->
                    "$sanitizedBaseName.$extension"
                } ?: sanitizedBaseName

                if (targetName == entry.name) {
                    return@setPositiveButton
                }

                when (renameEntry(entry, targetName)) {
                    RenameOutcome.SUCCESS -> {
                        showStatus(getString(R.string.recordings_status_renamed_template, targetName))
                        Toast.makeText(this, R.string.recordings_renamed_ok, Toast.LENGTH_SHORT).show()
                        refreshEntries()
                    }

                    RenameOutcome.NAME_EXISTS -> {
                        showStatus(getString(R.string.recordings_error_rename_name_exists))
                        Toast.makeText(this, R.string.recordings_error_rename_name_exists, Toast.LENGTH_SHORT).show()
                    }

                    RenameOutcome.FAILED -> {
                        showStatus(getString(R.string.recordings_error_rename))
                        Toast.makeText(this, R.string.recordings_error_rename, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.recordings_rename_cancel_button)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun renameEntry(entry: ManagerEntry, targetName: String): RenameOutcome {
        val parentFolder = DocumentFile.fromTreeUri(this, entry.parentUri) ?: return RenameOutcome.FAILED
        if (parentFolder.findFile(targetName) != null) {
            return RenameOutcome.NAME_EXISTS
        }

        return if (entry.type == ManagerEntryType.AUDIO_FILE) {
            RecordingDocumentOps.renameFileWithFallback(
                context = this,
                sourceUri = entry.uri,
                parentFolderUri = entry.parentUri,
                targetName = targetName
            )
        } else {
            val renamed = runCatching {
                val document = DocumentFile.fromSingleUri(this, entry.uri)
                document != null && document.exists() && document.renameTo(targetName)
            }.getOrDefault(false)
            if (renamed) RenameOutcome.SUCCESS else RenameOutcome.FAILED
        }
    }

    private fun showDeleteEntryDialog(entry: ManagerEntry) {
        val titleRes = if (entry.type == ManagerEntryType.FOLDER) {
            R.string.recordings_manage_delete_folder_confirm_title
        } else {
            R.string.recordings_delete_confirm_title
        }

        val messageRes = if (entry.type == ManagerEntryType.FOLDER) {
            R.string.recordings_manage_delete_folder_confirm_message
        } else {
            R.string.recordings_delete_confirm_message
        }

        AlertDialog.Builder(this)
            .setTitle(getString(titleRes))
            .setMessage(getString(messageRes, entry.name))
            .setPositiveButton(getString(R.string.recordings_delete_confirm_button)) { _, _ ->
                val deleted = runCatching {
                    val document = DocumentFile.fromSingleUri(this, entry.uri)
                    document != null && document.exists() && document.delete()
                }.getOrDefault(false)

                if (deleted) {
                    showStatus(getString(R.string.recordings_status_deleted_template, entry.name))
                    Toast.makeText(this, R.string.recordings_deleted_ok, Toast.LENGTH_SHORT).show()
                    refreshEntries()
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

    private fun handleDropTargetDrag(
        targetView: View,
        event: DragEvent,
        targetParentUri: Uri?,
        targetRelativePath: String
    ): Boolean {
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                return event.localState is MoveRequestPayload
            }

            DragEvent.ACTION_DRAG_ENTERED -> {
                targetView.alpha = 0.65f
                return true
            }

            DragEvent.ACTION_DRAG_EXITED -> {
                targetView.alpha = 1f
                return true
            }

            DragEvent.ACTION_DROP -> {
                targetView.alpha = 1f
                val payload = event.localState as? MoveRequestPayload ?: return false
                if (targetParentUri == null) {
                    showStatus(getString(R.string.recordings_manage_error_move))
                    Toast.makeText(this, R.string.recordings_manage_error_move, Toast.LENGTH_SHORT).show()
                    return true
                }
                handleDrop(payload, targetParentUri, targetRelativePath)
                return true
            }

            DragEvent.ACTION_DRAG_ENDED -> {
                targetView.alpha = 1f
                return true
            }
        }
        return false
    }

    private fun handleDrop(payload: MoveRequestPayload, targetParentUri: Uri, targetRelativePath: String) {
        if (payload.sourceParentUri == targetParentUri) {
            showStatus(getString(R.string.recordings_manage_move_same_parent))
            Toast.makeText(this, R.string.recordings_manage_move_same_parent, Toast.LENGTH_SHORT).show()
            return
        }

        if (
            payload.sourceType == ManagerEntryType.FOLDER &&
            (targetRelativePath == payload.sourceRelativePath || targetRelativePath.startsWith("${payload.sourceRelativePath}/"))
        ) {
            showStatus(getString(R.string.recordings_manage_move_invalid_target))
            Toast.makeText(this, R.string.recordings_manage_move_invalid_target, Toast.LENGTH_SHORT).show()
            return
        }

        val sourcePathDisplay = toDisplayPath(payload.sourceRelativePath)
        val targetPathDisplay = toDisplayPath(targetRelativePath)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.recordings_manage_move_confirm_title))
            .setMessage(getString(R.string.recordings_manage_move_confirm_message, sourcePathDisplay, targetPathDisplay))
            .setPositiveButton(getString(R.string.recordings_manage_move_confirm_button)) { _, _ ->
                val moved = runCatching {
                    DocumentsContract.moveDocument(
                        contentResolver,
                        payload.sourceUri,
                        payload.sourceParentUri,
                        targetParentUri
                    )
                }.getOrNull() != null

                if (moved) {
                    showStatus(getString(R.string.recordings_manage_move_success_template, payload.sourceName, targetPathDisplay))
                    Toast.makeText(this, R.string.recordings_manage_move_success, Toast.LENGTH_SHORT).show()
                    refreshEntries()
                } else {
                    showStatus(getString(R.string.recordings_manage_error_move))
                    Toast.makeText(this, R.string.recordings_manage_error_move, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.recordings_delete_cancel_button)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun openRecordingInExternalPlayer(entry: ManagerEntry) {
        showStatus(getString(R.string.recordings_status_opening_template, entry.name))
        val mimeType = if (!entry.mimeType.isNullOrBlank() && entry.mimeType != "application/octet-stream") {
            entry.mimeType
        } else {
            RecordingFormatOption.resolveMimeTypeByFileName(entry.name) ?: "audio/*"
        }
        lifecycleScope.launch {
            recordingExternalOpener.openRecordingWithChooser(
                sourceUri = entry.uri,
                fileName = entry.name,
                mimeType = mimeType,
                chooserTitle = getString(R.string.recordings_open_recording_with),
                onFailure = {
                    showStatus(getString(R.string.recordings_open_recording_no_app))
                    Toast.makeText(
                        this@RecordingsManagerActivity,
                        R.string.recordings_open_recording_no_app,
                        Toast.LENGTH_SHORT
                    ).show()
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

    private fun showStatus(message: String) {
        statusTextView.text = message
    }

    private data class ManagerRowViewHolder(
        val rootView: View,
        val nameTextView: TextView,
        val subtitleTextView: TextView,
        val renameButton: ImageButton,
        val deleteButton: ImageButton,
        val dragButton: ImageButton
    )

    private inner class ManagerEntriesAdapter : BaseAdapter() {
        private val inflater = LayoutInflater.from(this@RecordingsManagerActivity)

        override fun getCount(): Int = entries.size

        override fun getItem(position: Int): Any = entries[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val rowView: View
            val holder: ManagerRowViewHolder

            if (convertView == null) {
                rowView = inflater.inflate(R.layout.list_item_manager_entry, parent, false)
                holder = ManagerRowViewHolder(
                    rootView = rowView.findViewById(R.id.manager_item_container),
                    nameTextView = rowView.findViewById(R.id.manager_item_name_text_view),
                    subtitleTextView = rowView.findViewById(R.id.manager_item_subtitle_text_view),
                    renameButton = rowView.findViewById(R.id.manager_item_rename_button),
                    deleteButton = rowView.findViewById(R.id.manager_item_delete_button),
                    dragButton = rowView.findViewById(R.id.manager_item_drag_button)
                )
                rowView.tag = holder
            } else {
                rowView = convertView
                holder = rowView.tag as ManagerRowViewHolder
            }

            val entry = entries[position]
            val subtitle = if (entry.type == ManagerEntryType.FOLDER) {
                getString(R.string.recordings_manage_item_folder_label)
            } else {
                getString(R.string.recordings_manage_item_audio_label)
            }

            holder.nameTextView.text = entry.name
            holder.subtitleTextView.text = subtitle
            holder.rootView.setOnClickListener {
                if (entry.type == ManagerEntryType.FOLDER) {
                    enterFolder(entry)
                } else {
                    openRecordingInExternalPlayer(entry)
                }
            }
            holder.renameButton.setOnClickListener { showRenameEntryDialog(entry) }
            holder.deleteButton.setOnClickListener { showDeleteEntryDialog(entry) }
            holder.dragButton.setOnTouchListener { dragView, motionEvent ->
                if (motionEvent.actionMasked != MotionEvent.ACTION_DOWN) {
                    return@setOnTouchListener false
                }

                val payload = MoveRequestPayload(
                    sourceUri = entry.uri,
                    sourceParentUri = entry.parentUri,
                    sourceType = entry.type,
                    sourceRelativePath = entry.relativePath,
                    sourceName = entry.name
                )

                dragView.startDragAndDrop(
                    ClipData.newPlainText("recording_move", entry.name),
                    View.DragShadowBuilder(dragView),
                    payload,
                    0
                )
                true
            }

            if (entry.type == ManagerEntryType.FOLDER) {
                holder.rootView.setOnDragListener { targetView, event ->
                    handleDropTargetDrag(
                        targetView = targetView,
                        event = event,
                        targetParentUri = entry.uri,
                        targetRelativePath = entry.relativePath
                    )
                }
            } else {
                holder.rootView.setOnDragListener(null)
            }

            return rowView
        }
    }
}
