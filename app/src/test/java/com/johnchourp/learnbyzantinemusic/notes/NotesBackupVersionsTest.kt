package com.johnchourp.learnbyzantinemusic.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every backup file the app has ever written stays importable, and a file from a newer app is refused
 * with an error of its own (ClickUp `869f5x2a2`). Before, the codec rejected every version but the
 * current one: the first format change would have made every existing backup unreadable, and a file
 * from a newer app was reported as simply invalid.
 */
class NotesBackupVersionsTest {

    /** A notes backup as the app has written it since the first notes release: format version 1. */
    private val version1File = """
        {
          "schemaVersion": 1,
          "exportedAtEpochMs": 1758800000000,
          "notes": [
            {
              "id": "0b7f7f5e-2c55-4a39-9a8e-4f1f4b8d2c61",
              "title": "Εσπερινός ",
              "body": "Κύριε ἐκέκραξα\nπρὸς σέ",
              "createdAtEpochMs": 1758700000000,
              "updatedAtEpochMs": 1758790000000
            },
            {
              "id": "9d3e2a10-7b4c-4c1e-8f7a-2b6d0e5f9a34",
              "title": "",
              "body": "",
              "createdAtEpochMs": 1758600000000,
              "updatedAtEpochMs": 1758600000000
            }
          ]
        }
    """.trimIndent()

    /** One real file per format version. A new format adds its file here; the old ones stay. */
    private val fileOfVersion = mapOf(1 to version1File)

    private fun errorOf(json: String): Throwable? = runCatching { NotesBackupCodec.decodeSnapshot(json) }.exceptionOrNull()

    @Test
    fun everyFormatVersionUpToTheCurrentOneHasAFileThatImports() {
        assertEquals((1..NotesBackupCodec.CURRENT_SCHEMA_VERSION).toSet(), fileOfVersion.keys)
        fileOfVersion.forEach { (version, json) ->
            assertTrue("version $version", NotesBackupCodec.decodeSnapshot(json).isNotEmpty())
        }
    }

    @Test
    fun aVersion1BackupStillImportsExactly() {
        assertEquals(
            listOf(
                NoteEntity(
                    id = "0b7f7f5e-2c55-4a39-9a8e-4f1f4b8d2c61",
                    title = "Εσπερινός ",
                    body = "Κύριε ἐκέκραξα\nπρὸς σέ",
                    createdAtEpochMs = 1758700000000L,
                    updatedAtEpochMs = 1758790000000L
                ),
                NoteEntity(
                    id = "9d3e2a10-7b4c-4c1e-8f7a-2b6d0e5f9a34",
                    title = "",
                    body = "",
                    createdAtEpochMs = 1758600000000L,
                    updatedAtEpochMs = 1758600000000L
                )
            ),
            NotesBackupCodec.decodeSnapshot(version1File)
        )
    }

    @Test
    fun aBackupFromANewerAppIsRefusedWithItsOwnError() {
        val fromNewerApp = version1File.replace("\"schemaVersion\": 1", "\"schemaVersion\": 2")

        val error = errorOf(fromNewerApp)

        assertTrue("expected NewerVersionException, got $error", error is NotesBackupCodec.NewerVersionException)
        assertEquals(2, (error as NotesBackupCodec.NewerVersionException).version)
    }

    @Test
    fun aBrokenFileIsRefusedAsInvalidNotAsNewer() {
        val broken = listOf(
            version1File.replace("\"schemaVersion\": 1,", ""),
            version1File.replace("\"schemaVersion\": 1", "\"schemaVersion\": 0"),
            "{ not json",
            """{ "schemaVersion": 1 }"""
        )
        broken.forEach { json ->
            val error = errorOf(json)
            assertTrue("refused: $json", error is IllegalArgumentException)
            assertFalse("not «newer»: $json", error is NotesBackupCodec.NewerVersionException)
        }
    }
}
