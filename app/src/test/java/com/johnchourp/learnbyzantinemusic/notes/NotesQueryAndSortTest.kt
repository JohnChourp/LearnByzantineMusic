package com.johnchourp.learnbyzantinemusic.notes

import org.junit.Assert.assertEquals
import org.junit.Test

class NotesQueryAndSortTest {
    @Test
    fun `search pattern escapes wildcards`() {
        val pattern = "  100%_test  ".toNotesSearchPattern()
        assertEquals("%100\\%\\_test%", pattern)
    }

    @Test
    fun `sort by most recent uses updated timestamp descending`() {
        val notes = listOf(
            NoteEntity("x", "", "", createdAtEpochMs = 100L, updatedAtEpochMs = 200L),
            NoteEntity("z", "", "", createdAtEpochMs = 100L, updatedAtEpochMs = 500L),
            NoteEntity("y", "", "", createdAtEpochMs = 100L, updatedAtEpochMs = 300L)
        )

        val sorted = sortNotesByMostRecent(notes)

        assertEquals(listOf("z", "y", "x"), sorted.map { it.id })
    }
}
