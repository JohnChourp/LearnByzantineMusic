package com.johnchourp.learnbyzantinemusic.recordings

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.Locale

class RecordingExternalOpener(
    private val activity: Activity
) {
    suspend fun openRecordingWithChooser(
        sourceUri: Uri,
        fileName: String,
        mimeType: String?,
        chooserTitle: String,
        onFailure: (Throwable?) -> Unit
    ): Boolean {
        val cachedUri = runCatching {
            withContext(Dispatchers.IO) {
                val cachedFile = copyToOpenCache(sourceUri, fileName)
                FileProvider.getUriForFile(
                    activity,
                    "${activity.packageName}.fileprovider",
                    cachedFile
                )
            }
        }.getOrNull()

        val mimeCandidates = buildMimeCandidates(fileName, mimeType)
        val targetUris = listOfNotNull(
            sourceUri,
            cachedUri
        ).distinct()

        targetUris.forEach { targetUri ->
            mimeCandidates.forEach { candidateMime ->
                if (tryOpenChooser(targetUri, candidateMime, chooserTitle)) {
                    return true
                }
            }
        }

        onFailure(null)
        return false
    }

    private fun buildMimeCandidates(fileName: String, mimeType: String?): List<String> {
        return linkedSetOf<String>().apply {
            val resolvedMimeType = resolveMimeType(fileName, mimeType)
            if (!resolvedMimeType.isNullOrBlank()) {
                add(resolvedMimeType)
            }
            add("audio/*")
        }.toList()
    }

    private fun tryOpenChooser(targetUri: Uri, mimeType: String, chooserTitle: String): Boolean {
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(targetUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            clipData = ClipData.newRawUri("recording", targetUri)
        }

        val resolveInfoList = runCatching {
            activity.packageManager.queryIntentActivities(viewIntent, 0)
        }.getOrDefault(emptyList())

        resolveInfoList.forEach { resolveInfo ->
            runCatching {
                activity.grantUriPermission(
                    resolveInfo.activityInfo.packageName,
                    targetUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }

        return runCatching {
            val chooserIntent = Intent.createChooser(viewIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                clipData = ClipData.newRawUri("recording", targetUri)
            }
            activity.startActivity(chooserIntent)
        }.isSuccess
    }

    private fun copyToOpenCache(sourceUri: Uri, fileName: String): File {
        val targetDir = File(activity.cacheDir, OPEN_CACHE_DIRECTORY).apply {
            if (!exists() && !mkdirs()) {
                throw IOException("Could not create open cache directory.")
            }
        }
        cleanupOpenCache(targetDir)

        val targetFile = File(targetDir, buildCacheFileName(fileName))
        val sourceStream = activity.contentResolver.openInputStream(sourceUri)
            ?: throw IOException("Could not open source recording stream.")
        sourceStream.use { inputStream ->
            targetFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
                outputStream.flush()
            }
        }
        return targetFile
    }

    private fun resolveMimeType(fileName: String, mimeType: String?): String? {
        if (!mimeType.isNullOrBlank() && mimeType != "application/octet-stream") {
            return mimeType
        }
        return RecordingFormatOption.resolveMimeTypeByFileName(fileName)
    }

    private fun buildCacheFileName(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").trim().lowercase(Locale.ROOT)
        val baseName = fileName.substringBeforeLast('.', fileName)
            .trim()
            .ifBlank { "recording" }
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(48)
            .ifBlank { "recording" }
        val uniqueSuffix = System.currentTimeMillis()
        return if (extension.isBlank()) {
            "${baseName}_$uniqueSuffix"
        } else {
            "${baseName}_$uniqueSuffix.$extension"
        }
    }

    private fun cleanupOpenCache(targetDir: File) {
        val cachedFiles = targetDir.listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?: return
        cachedFiles.drop(MAX_CACHE_FILES).forEach { staleFile ->
            runCatching { staleFile.delete() }
        }
    }

    companion object {
        private const val OPEN_CACHE_DIRECTORY = "byz_recordings_open"
        private const val MAX_CACHE_FILES = 32
    }
}
