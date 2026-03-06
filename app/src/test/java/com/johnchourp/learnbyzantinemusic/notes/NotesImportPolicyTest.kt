package com.johnchourp.learnbyzantinemusic.notes

import org.junit.Assert.assertEquals
import org.junit.Test

class NotesImportPolicyTest {
    @Test
    fun `replace import keeps only imported notes sorted by recent`() {
        val imported = listOf(
            NoteEntity("a", "A", "", createdAtEpochMs = 10L, updatedAtEpochMs = 20L),
            NoteEntity("b", "B", "", createdAtEpochMs = 30L, updatedAtEpochMs = 40L),
            NoteEntity("c", "C", "", createdAtEpochMs = 50L, updatedAtEpochMs = 60L)
        )

        val replaced = replaceAllNotesWithImport(imported)

        assertEquals(listOf("c", "b", "a"), replaced.map { it.id })
    }
}
