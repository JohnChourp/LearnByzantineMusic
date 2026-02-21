package com.johnchourp.learnbyzantinemusic.recordings

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

object RecordingDocumentOps {
    private val recordingNameFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.US)
    private val recordingNameRegex = Regex("^recording_(\\d{8}_\\d{6})\\.[^.]+$")

    fun parseFileName(fullName: String): ParsedFileName {
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

    fun sanitizeName(rawName: String): String = rawName.trim().replace("/", "_")

    fun resolveCreationLikeTimestamp(fileName: String): Long? {
        val match = recordingNameRegex.matchEntire(fileName) ?: return null
        val token = match.groupValues.getOrNull(1) ?: return null
        return try {
            LocalDateTime.parse(token, recordingNameFormatter)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    fun renameFileWithFallback(
        context: Context,
        sourceUri: Uri,
        parentFolderUri: Uri,
        targetName: String
    ): RenameOutcome {
        val parentFolder = DocumentFile.fromTreeUri(context, parentFolderUri)
            ?: DocumentFile.fromSingleUri(context, parentFolderUri)
            ?: return RenameOutcome.FAILED
        if (!parentFolder.exists() || !parentFolder.isDirectory) {
            return RenameOutcome.FAILED
        }
        val conflict = parentFolder.findFile(targetName)
        if (conflict != null) {
            return RenameOutcome.NAME_EXISTS
        }

        val sourceDocument = DocumentFile.fromSingleUri(context, sourceUri) ?: return RenameOutcome.FAILED
        if (!sourceDocument.exists() || !sourceDocument.isFile) {
            return RenameOutcome.REMOVED
        }

        val directRenameSuccess = runCatching { sourceDocument.renameTo(targetName) }.getOrDefault(false)
        if (directRenameSuccess) {
            return RenameOutcome.SUCCESS
        }

        val sourceMimeType = sourceDocument.type ?: RecordingFormatOption.resolveMimeTypeByFileName(sourceDocument.name)
        val targetDocument = parentFolder.createFile(sourceMimeType ?: "application/octet-stream", targetName)
            ?: return RenameOutcome.FAILED

        val copied = runCatching {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: error("source_input_stream_not_available")
            inputStream.use { source ->
                val outputStream = context.contentResolver.openOutputStream(targetDocument.uri, "w")
                    ?: error("target_output_stream_not_available")
                outputStream.use { target ->
                    source.copyTo(target)
                }
            }
        }.isSuccess

        if (!copied) {
            runCatching { targetDocument.delete() }
            return RenameOutcome.FAILED
        }

        val deletedSource = runCatching { sourceDocument.delete() }.getOrDefault(false)
        if (!deletedSource) {
            runCatching { targetDocument.delete() }
            return RenameOutcome.FAILED
        }

        return RenameOutcome.SUCCESS
    }
}

data class ParsedFileName(
    val baseName: String,
    val extension: String?
)
