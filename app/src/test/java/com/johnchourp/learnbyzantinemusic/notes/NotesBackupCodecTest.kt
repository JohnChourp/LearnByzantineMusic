package com.johnchourp.learnbyzantinemusic.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotesBackupCodecTest {
    @Test
    fun `encode then decode returns same notes`() {
        val notes = listOf(
            NoteEntity(
                id = "n1",
                title = "Τίτλος 1",
                body = "Κείμενο 1",
                createdAtEpochMs = 1000L,
                updatedAtEpochMs = 2000L
            ),
            NoteEntity(
                id = "n2",
                title = "Title 2",
                body = "Body 2",
                createdAtEpochMs = 3000L,
                updatedAtEpochMs = 4000L
            )
        )

        val encoded = NotesBackupCodec.encodeSnapshot(notes, exportedAtEpochMs = 5000L)
        val decoded = NotesBackupCodec.decodeSnapshot(encoded)

        assertEquals(notes, decoded)
    }

    @Test
    fun `decode rejects unsupported schema`() {
        val invalidJson = """
            {
              "schemaVersion": 999,
              "exportedAtEpochMs": 1,
              "notes": []
            }
        """.trimIndent()

        val result = runCatching { NotesBackupCodec.decodeSnapshot(invalidJson) }

        assertTrue(result.isFailure)
    }
}
