package com.johnchourp.learnbyzantinemusic.notes

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object NotesBackupCodec {
    private const val CURRENT_SCHEMA_VERSION = 1

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

    @Throws(IllegalArgumentException::class)
    fun decodeSnapshot(json: String): List<NoteEntity> {
        val root = try {
            JSONObject(json)
        } catch (error: JSONException) {
            throw IllegalArgumentException("invalid_json", error)
        }

        val schemaVersion = root.optInt("schemaVersion", -1)
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw IllegalArgumentException("unsupported_schema_version")
        }

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
