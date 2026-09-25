package com.johnchourp.learnbyzantinemusic.notes

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * The notes backup file: the JSON snapshot written into the user's folder on every save, and read
 * back by «Import backup».
 *
 * **Versions.** [CURRENT_SCHEMA_VERSION] is the version of this file's format, and it moves on its own:
 * a NotesDatabase migration does not have to change the file, and a new file format does not have to
 * touch the database. Every file ever written stays importable — [decodeSnapshot] reads each version
 * from 1 to the current one. A file from a **newer** app is refused with its own error,
 * [NewerVersionException], so the user is told to update the app rather than that the file is broken.
 */
object NotesBackupCodec {
    const val CURRENT_SCHEMA_VERSION = 1

    /** The file was written by a newer version of the app, in a format this one cannot read. */
    class NewerVersionException(val version: Int) :
        IllegalArgumentException("backup_from_newer_version: $version > $CURRENT_SCHEMA_VERSION")

    fun encodeSnapshot(notes: List<NoteEntity>, exportedAtEpochMs: Long): String {
        val notesArray = JSONArray()
        notes.forEach { note ->
            val jsonNote = JSONObject()
                .put("id", note.id)
                .put("title", note.title)
                .put("body", note.body)
                .put("createdAtEpochMs", note.createdAtEpochMs)
                .put("updatedAtEpochMs", note.updatedAtEpochMs)
            notesArray.put(jsonNote)
        }

        val payload = JSONObject()
            .put("schemaVersion", CURRENT_SCHEMA_VERSION)
            .put("exportedAtEpochMs", exportedAtEpochMs)
            .put("notes", notesArray)

        return payload.toString(2)
    }

    /**
     * The notes in [json], all or nothing. Throws [NewerVersionException] for a file from a newer app,
     * and [IllegalArgumentException] for anything else that is not a valid backup.
     */
    @Throws(IllegalArgumentException::class)
    fun decodeSnapshot(json: String): List<NoteEntity> {
        val root = try {
            JSONObject(json)
        } catch (error: JSONException) {
            throw IllegalArgumentException("invalid_json", error)
        }

        val schemaVersion = root.optInt("schemaVersion", -1)
        if (schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw NewerVersionException(schemaVersion)
        }
        // One branch per version ever written: a new format adds a branch and keeps the old ones.
        return when (schemaVersion) {
            1 -> decodeVersion1(root)
            else -> throw IllegalArgumentException("unsupported_schema_version")
        }
    }

    /** Version 1, the format written since the first notes release. */
    private fun decodeVersion1(root: JSONObject): List<NoteEntity> {
        val notesArray = root.optJSONArray("notes")
            ?: throw IllegalArgumentException("missing_notes_array")

        val decoded = ArrayList<NoteEntity>(notesArray.length())
        for (index in 0 until notesArray.length()) {
            val noteObject = notesArray.optJSONObject(index)
                ?: throw IllegalArgumentException("invalid_note_item")
            val id = noteObject.optString("id", "").trim()
            if (id.isBlank()) {
                throw IllegalArgumentException("missing_note_id")
            }
            val title = noteObject.optString("title", "")
            val body = noteObject.optString("body", "")
            val createdAtEpochMs = noteObject.optLong("createdAtEpochMs", -1L)
            val updatedAtEpochMs = noteObject.optLong("updatedAtEpochMs", -1L)
            if (createdAtEpochMs <= 0L || updatedAtEpochMs <= 0L) {
                throw IllegalArgumentException("invalid_note_timestamps")
            }
            decoded += NoteEntity(
                id = id,
                title = title,
                body = body,
                createdAtEpochMs = createdAtEpochMs,
                updatedAtEpochMs = updatedAtEpochMs
            )
        }

        return decoded
    }
}
