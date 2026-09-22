package com.johnchourp.learnbyzantinemusic.recordings.saf

import android.database.Cursor
import android.database.MatrixCursor
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import java.io.File
import java.io.FileNotFoundException

/**
 * A real `DocumentsProvider` over a temp directory, so the SAF edge-case tests run through the
 * actual Storage Access Framework rather than around it (ClickUp `869f4tpt9`, B5).
 *
 * ## Why not `DocumentFile.fromFile`
 *
 * A file-backed `DocumentFile` never touches a provider: `renameTo` becomes `File.renameTo`, which
 * essentially always succeeds. The fallback path in `RecordingDocumentOps` exists precisely because
 * real providers refuse `renameTo`, so a file-backed test would exercise the one branch that has
 * never produced a bug and skip every branch that has.
 *
 * ## What it can be told to do
 *
 * [refuseRename] and [refuseDelete] make the provider behave like the awkward ones in the wild:
 * a provider that has no rename at all, and one that reports a delete it did not perform. They are
 * static because the framework instantiates the provider itself.
 */
class FakeSafProvider : DocumentsProvider() {

    override fun onCreate(): Boolean = true

    /**
     * Required for tree URIs, and the default returns **false**.
     *
     * `DocumentsProvider` enforces the tree on every access through a `content://…/tree/<root>/
     * document/<id>` URI: unless the document is declared a descendant of the tree's root, the call
     * is refused. With the default in place only the root itself answered — `listFiles()` returned
     * children whose every column read back null, and `findFile` never matched (measured
     * 2026-09-22). Nothing in the failure named the tree check, which is why this comment does.
     */
    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean =
        documentId == parentDocumentId || documentId.startsWith("$parentDocumentId/")

    override fun queryRoots(projection: Array<out String>?): Cursor =
        MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION).apply {
            newRow()
                .add(Root.COLUMN_ROOT_ID, ROOT_ID)
                .add(Root.COLUMN_DOCUMENT_ID, ROOT_ID)
                .add(Root.COLUMN_TITLE, "Fake SAF")
                .add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE)
        }

    /**
     * Throws for a document that is not there, the way a real provider does.
     *
     * Returning a row for a missing file makes `DocumentFile.exists()` answer **true**, which sent
     * `renameFileWithFallback` straight past its "the recording is gone" branch and into the copy
     * fallback, where it failed on the missing input stream and reported FAILED instead of REMOVED
     * (measured 2026-09-22). A fake that is wrong about existence cannot test a guard about
     * existence.
     */
    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        val file = fileFor(documentId)
        if (!file.exists()) throw FileNotFoundException("no such document: $documentId")
        return MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION).apply { addRow(this, file) }
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val parent = fileFor(parentDocumentId)
        if (!parent.isDirectory) throw FileNotFoundException("not a folder: $parentDocumentId")
        return MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION).apply {
            parent.listFiles()?.forEach { addRow(this, it) }
        }
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: android.os.CancellationSignal?,
    ): ParcelFileDescriptor = ParcelFileDescriptor.open(fileFor(documentId), parcelModeOf(mode))

    override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String {
        val target = File(fileFor(parentDocumentId), displayName)
        if (mimeType == Document.MIME_TYPE_DIR) target.mkdirs() else target.createNewFile()
        return documentIdOf(target)
    }

    override fun deleteDocument(documentId: String) {
        if (refuseDelete) throw UnsupportedOperationException("delete refused by the test provider")
        fileFor(documentId).delete()
    }

    override fun renameDocument(documentId: String, displayName: String): String? {
        if (refuseRename) throw UnsupportedOperationException("rename refused by the test provider")
        val source = fileFor(documentId)
        val target = File(source.parentFile, displayName)
        if (!source.renameTo(target)) throw IllegalStateException("rename failed")
        return documentIdOf(target)
    }

    /**
     * Fills one row, using **only the columns the caller asked for**.
     *
     * `MatrixCursor.RowBuilder.add(name, value)` throws on a column that is not in the cursor's
     * projection, and `DocumentFile` queries with narrow projections — `listFiles()` asks for the
     * document id alone. Adding every column unconditionally therefore threw inside the query, the
     * framework swallowed it, and every child came back with a null name (measured 2026-09-22).
     */
    private fun addRow(cursor: MatrixCursor, file: File) {
        val flags = when {
            file.isDirectory -> Document.FLAG_DIR_SUPPORTS_CREATE or Document.FLAG_SUPPORTS_DELETE
            else -> Document.FLAG_SUPPORTS_WRITE or Document.FLAG_SUPPORTS_DELETE or
                (if (refuseRename) 0 else Document.FLAG_SUPPORTS_RENAME)
        }
        val values = mapOf(
            Document.COLUMN_DOCUMENT_ID to documentIdOf(file),
            Document.COLUMN_DISPLAY_NAME to file.name,
            Document.COLUMN_SIZE to file.length(),
            Document.COLUMN_LAST_MODIFIED to file.lastModified(),
            Document.COLUMN_FLAGS to flags,
            Document.COLUMN_MIME_TYPE to
                if (file.isDirectory) Document.MIME_TYPE_DIR else "audio/mp4",
        )
        val row = cursor.newRow()
        cursor.columnNames.forEach { column -> row.add(column, values[column]) }
    }

    private fun fileFor(documentId: String): File =
        if (documentId == ROOT_ID) requireNotNull(root) { "FakeSafProvider.root was never set" }
        else File(requireNotNull(root), documentId.removePrefix("$ROOT_ID/"))

    private fun documentIdOf(file: File): String {
        val base = requireNotNull(root)
        if (file.absolutePath == base.absolutePath) return ROOT_ID
        return "$ROOT_ID/${file.absolutePath.removePrefix(base.absolutePath + "/")}"
    }

    private fun parcelModeOf(mode: String): Int = when {
        mode.contains("w") -> ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_TRUNCATE
        else -> ParcelFileDescriptor.MODE_READ_ONLY
    }

    companion object {
        const val AUTHORITY = "com.johnchourp.learnbyzantinemusic.test.saf"
        const val ROOT_ID = "root"

        /** The directory the provider serves. Set by each test before it touches a URI. */
        @JvmStatic
        var root: File? = null

        /** Makes the provider behave like one with no rename support. */
        @JvmStatic
        var refuseRename: Boolean = false

        /** Makes delete throw, so "the source could not be removed" can be exercised. */
        @JvmStatic
        var refuseDelete: Boolean = false

        fun reset(newRoot: File) {
            root = newRoot
            refuseRename = false
            refuseDelete = false
        }

        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            Root.COLUMN_ROOT_ID, Root.COLUMN_DOCUMENT_ID, Root.COLUMN_TITLE, Root.COLUMN_FLAGS,
        )
        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_SIZE,
            Document.COLUMN_LAST_MODIFIED, Document.COLUMN_FLAGS, Document.COLUMN_MIME_TYPE,
        )
    }
}
