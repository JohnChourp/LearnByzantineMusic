package com.johnchourp.learnbyzantinemusic.recordings

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

data class OwnedRecordingEntry(
    val rootUri: String,
    val uri: String,
    val name: String,
    val mimeType: String?,
    val parentUri: String,
    val relativePath: String,
    val parentRelativePath: String,
    val createdTs: Long,
    val updatedTs: Long
)

fun OwnedRecordingEntry.toRecordingListItem(): RecordingListItem {
    return RecordingListItem(
        name = name,
        uri = Uri.parse(uri),
        mimeType = mimeType,
        relativePath = relativePath,
        parentRelativePath = parentRelativePath,
        parentUri = Uri.parse(parentUri),
        createdTimestamp = createdTs,
        updatedTimestamp = updatedTs
    )
}

class OwnedRecordingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val entriesFlow = MutableStateFlow(loadEntries())
    private val lock = Any()

    fun observeRecent(rootUri: Uri, limit: Int): Flow<List<OwnedRecordingEntry>> {
        val rootUriRaw = rootUri.toString()
        return entriesFlow.asStateFlow().map { entries ->
            entries
                .asSequence()
                .filter { it.rootUri == rootUriRaw }
                .sortedByDescending { it.createdTs }
                .take(limit.coerceAtLeast(1))
                .toList()
        }
    }

    fun register(entry: OwnedRecordingEntry) {
        synchronized(lock) {
            mutateEntries { entries ->
                val filtered = entries.filterNot { it.uri == entry.uri }
                (listOf(entry) + filtered)
                    .sortedByDescending { it.createdTs }
                    .take(MAX_ENTRIES)
            }
        }
    }

    fun remove(uri: Uri) {
        val rawUri = uri.toString()
        synchronized(lock) {
            mutateEntries { entries ->
                entries.filterNot { it.uri == rawUri }
            }
        }
    }

    fun updateIfTracked(
        sourceUri: Uri,
        updatedUri: Uri,
        updatedName: String,
        updatedMimeType: String?,
        updatedParentUri: Uri,
        updatedRelativePath: String,
        updatedTimestamp: Long
    ) {
        val sourceRaw = sourceUri.toString()
        val updatedRaw = updatedUri.toString()
        val updatedParentRaw = updatedParentUri.toString()
        val parentRelativePath = updatedRelativePath.substringBeforeLast('/', "")
        val parentRelativePathDisplay = if (parentRelativePath.isBlank()) "/" else "/$parentRelativePath"
        synchronized(lock) {
            mutateEntries { entries ->
                val currentEntry = entries.firstOrNull { it.uri == sourceRaw } ?: return@mutateEntries entries
                val replacement = currentEntry.copy(
                    uri = updatedRaw,
                    name = updatedName,
                    mimeType = updatedMimeType,
                    parentUri = updatedParentRaw,
                    relativePath = updatedRelativePath,
                    parentRelativePath = parentRelativePathDisplay,
                    updatedTs = updatedTimestamp
                )

                if (currentEntry == replacement) {
                    return@mutateEntries entries
                }

                val filtered = entries.filterNot { it.uri == sourceRaw || it.uri == replacement.uri }
                (listOf(replacement) + filtered)
                    .sortedByDescending { it.createdTs }
                    .take(MAX_ENTRIES)
            }
        }
    }

    private fun mutateEntries(transform: (List<OwnedRecordingEntry>) -> List<OwnedRecordingEntry>) {
        val current = entriesFlow.value
        val updated = transform(current)
        if (updated == current) {
            return
        }
        entriesFlow.value = updated
        saveEntries(updated)
    }

    private fun loadEntries(): List<OwnedRecordingEntry> {
        val raw = prefs.getString(KEY_OWNED_RECORDINGS, null) ?: return emptyList()
        return runCatching {
            val jsonArray = JSONArray(raw)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(index) ?: continue
                    add(item.toOwnedRecordingEntry() ?: continue)
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun saveEntries(entries: List<OwnedRecordingEntry>) {
        val jsonArray = JSONArray()
        entries.forEach { entry ->
            jsonArray.put(
                JSONObject().apply {
                    put("rootUri", entry.rootUri)
                    put("uri", entry.uri)
                    put("name", entry.name)
                    put("mimeType", entry.mimeType)
                    put("parentUri", entry.parentUri)
                    put("relativePath", entry.relativePath)
                    put("parentRelativePath", entry.parentRelativePath)
                    put("createdTs", entry.createdTs)
                    put("updatedTs", entry.updatedTs)
                }
            )
        }
        prefs.edit().putString(KEY_OWNED_RECORDINGS, jsonArray.toString()).apply()
    }

    private fun JSONObject.toOwnedRecordingEntry(): OwnedRecordingEntry? {
        val rootUri = optString("rootUri", "")
        val uri = optString("uri", "")
        val name = optString("name", "")
        val parentUri = optString("parentUri", "")
        val relativePath = optString("relativePath", "")
        if (rootUri.isBlank() || uri.isBlank() || name.isBlank() || parentUri.isBlank() || relativePath.isBlank()) {
            return null
        }

        return OwnedRecordingEntry(
            rootUri = rootUri,
            uri = uri,
            name = name,
            mimeType = optString("mimeType", "").ifBlank { null },
            parentUri = parentUri,
            relativePath = relativePath,
            parentRelativePath = optString("parentRelativePath", ""),
            createdTs = optLong("createdTs", 0L),
            updatedTs = optLong("updatedTs", 0L)
        )
    }

    companion object {
        private const val PREFS_NAME = "learn_byzantine_music_owned_recordings"
        private const val KEY_OWNED_RECORDINGS = "owned_recordings"
        private const val MAX_ENTRIES = 300
    }
}
