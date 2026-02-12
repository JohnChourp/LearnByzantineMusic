package com.johnchourp.learnbyzantinemusic.editor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.editor.export.PngExporter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ComposerImagesActivity : ComponentActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var adapter: ComposerImagesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_composer_images)

        recyclerView = findViewById(R.id.composer_images_recycler)
        emptyView = findViewById(R.id.composer_images_empty)

        adapter = ComposerImagesAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.composer_images_open_folder_btn).setOnClickListener {
            openImagesFolder()
        }
        findViewById<Button>(R.id.composer_images_refresh_btn).setOnClickListener {
            loadImages()
        }

        loadImages()
    }

    override fun onResume() {
        super.onResume()
        loadImages()
    }

    private fun loadImages() {
        val exportsDir = PngExporter.resolveExportsDirectory(this)
        val files = exportsDir
            .listFiles { file -> file.isFile && file.extension.equals("png", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()

        adapter.submitList(files)
        val hasItems = files.isNotEmpty()
        recyclerView.visibility = if (hasItems) View.VISIBLE else View.GONE
        emptyView.visibility = if (hasItems) View.GONE else View.VISIBLE
    }

    private fun openImagesFolder() {
        val exportsDir = PngExporter.resolveExportsDirectory(this)
        exportsDir.mkdirs()

        val opened = openFolderWithFileManager(exportsDir)
        if (opened) {
            return
        }

        copyPathToClipboard(exportsDir.absolutePath)
        toast(getString(R.string.composer_images_open_folder_failed_path_copied))
    }

    private fun openFolderWithFileManager(folder: File): Boolean {
        val documentUri = buildDocumentUriForFolder(folder) ?: return false
        val openFolderIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(documentUri, DocumentsContract.Document.MIME_TYPE_DIR)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (startIntentSafely(openFolderIntent)) {
            return true
        }

        val openTreeIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            putExtra(EXTRA_INITIAL_URI, documentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        return startIntentSafely(openTreeIntent)
    }

    private fun startIntentSafely(intent: Intent): Boolean {
        if (intent.resolveActivity(packageManager) == null) {
            return false
        }
        return runCatching {
            startActivity(intent)
            true
        }.getOrDefault(false)
    }

    private fun buildDocumentUriForFolder(folder: File): Uri? {
        val appExternalFiles = getExternalFilesDir(null) ?: return null
        val appExternalPath = appExternalFiles.absolutePath
        val folderPath = folder.absolutePath
        if (!folderPath.startsWith(appExternalPath)) {
            return null
        }

        val relativePath = folderPath.removePrefix(appExternalPath).trimStart('/')
        val fullPath = if (relativePath.isEmpty()) {
            "Android/data/$packageName/files"
        } else {
            "Android/data/$packageName/files/$relativePath"
        }
        val docId = "primary:$fullPath"
        return DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY, docId)
    }

    private fun copyPathToClipboard(path: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("composer_exports_path", path)
        clipboard.setPrimaryClip(clip)
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        private const val EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY = "com.android.externalstorage.documents"
        private const val EXTRA_INITIAL_URI = "android.provider.extra.INITIAL_URI"
    }
}

private class ComposerImagesAdapter : RecyclerView.Adapter<ComposerImagesAdapter.ImageViewHolder>() {
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private var items: List<File> = emptyList()

    fun submitList(newItems: List<File>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ImageViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_composer_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val file = items[position]
        holder.fileName.text = file.name
        val dateLabel = dateFormatter.format(Date(file.lastModified()))
        val sizeLabel = formatSize(file.length())
        holder.meta.text = holder.itemView.context.getString(
            R.string.composer_images_item_meta,
            dateLabel,
            sizeLabel
        )
        holder.path.text = file.absolutePath
    }

    override fun getItemCount(): Int = items.size

    private fun formatSize(bytes: Long): String {
        val kb = bytes / 1024.0
        if (kb < 1024.0) {
            return String.format(Locale.US, "%.1f KB", kb)
        }
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.2f MB", mb)
    }

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileName: TextView = view.findViewById(R.id.composer_image_item_name)
        val meta: TextView = view.findViewById(R.id.composer_image_item_meta)
        val path: TextView = view.findViewById(R.id.composer_image_item_path)
    }
}
