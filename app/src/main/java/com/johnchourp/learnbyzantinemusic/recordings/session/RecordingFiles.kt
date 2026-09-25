package com.johnchourp.learnbyzantinemusic.recordings.session

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Where a recording goes below the folder the user picked, fixed when capture starts: the hymn
 * sub-folders of `RecordingsActivity.EXTRA_TARGET_FOLDER_PATH`, the prefix that finds a renamed hymn
 * folder, and the label the record card shows. Empty = the picked folder itself.
 */
data class RecordingTarget(
    val folderSegments: List<String> = emptyList(),
    val folderMatchPrefix: String? = null,
    val label: String? = null,
)

/** Everything about a recording that is not audio: its target and, once it has one, its file name. */
data class RecordingMeta(
    val target: RecordingTarget = RecordingTarget(),
    /** `recording_yyyyMMdd_HHmmss`, given at the first save attempt and kept by every retry. */
    val baseName: String? = null,
) {
    fun namedAt(millis: Long): RecordingMeta =
        if (baseName != null) this else copy(baseName = RecordingFiles.baseNameFor(millis))
}

/**
 * A recording on the app's own storage is a *bundle*: `<id>.wav` plus an optional `<id>.json`
 * sidecar holding its [RecordingMeta]. The sidecar is what lets a recording recovered after a crash,
 * or kept because the folder was unreachable, still land in its hymn's folder and keep its name.
 *
 * The sidecar is a stored format: its keys (`version`, `baseName`, `folder`, `matchPrefix`, `label`)
 * are never renamed. A missing or unreadable sidecar is not an error — the audio is what matters,
 * and it then goes to the picked folder under a name taken from the file's time.
 */
object RecordingFiles {
    private const val SIDECAR_VERSION = 1

    fun sidecarOf(wav: File): File = File(wav.parentFile, wav.nameWithoutExtension + ".json")

    /** Same pattern the recording screen has always used, so the manager still reads a creation time from it. */
    fun baseNameFor(millis: Long): String =
        "recording_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(millis))

    /** Best effort: a recording without a sidecar is still a recording. */
    fun writeMeta(wav: File, meta: RecordingMeta): Boolean = runCatching {
        val json = JSONObject()
        json.put("version", SIDECAR_VERSION)
        meta.baseName?.let { json.put("baseName", it) }
        json.put("folder", JSONArray().apply { meta.target.folderSegments.forEach { put(it) } })
        meta.target.folderMatchPrefix?.let { json.put("matchPrefix", it) }
        meta.target.label?.let { json.put("label", it) }
        sidecarOf(wav).writeText(json.toString(), Charsets.UTF_8)
        true
    }.getOrDefault(false)

    /** Null when there is no sidecar or it cannot be read. */
    fun readMeta(wav: File): RecordingMeta? {
        val sidecar = sidecarOf(wav)
        if (!sidecar.isFile) return null
        return runCatching {
            val json = JSONObject(sidecar.readText(Charsets.UTF_8))
            val folder = json.optJSONArray("folder")
            val segments = buildList {
                if (folder != null) {
                    for (index in 0 until folder.length()) {
                        folder.optString(index, "").takeIf { it.isNotBlank() }?.let(::add)
                    }
                }
            }
            RecordingMeta(
                target = RecordingTarget(
                    folderSegments = segments,
                    folderMatchPrefix = json.optString("matchPrefix", "").ifBlank { null },
                    label = json.optString("label", "").ifBlank { null },
                ),
                baseName = json.optString("baseName", "").ifBlank { null },
            )
        }.getOrNull()
    }

    /** Deletes the WAV and its sidecar; true when the audio is gone. Audio that stays keeps its sidecar. */
    fun deleteBundle(wav: File): Boolean {
        val audioGone = !wav.exists() || wav.delete()
        if (audioGone) sidecarOf(wav).delete()
        return audioGone
    }
}
