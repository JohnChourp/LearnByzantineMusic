package com.johnchourp.learnbyzantinemusic.notes

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The notes search box. The search now runs in memory, and [NotesSearch] is the exact function the
 * screen filters the database's list with — so this is the test that matters: no SQL matches
 * anything any more. Before the fix, `LIKE` folded case for ASCII only and a `%` in the query never
 * matched, which is what the queries below were failing on.
 */
class NotesSearchTest {

    private fun note(id: String, title: String, body: String = "") =
        NoteEntity(id, title, body, createdAtEpochMs = 1L, updatedAtEpochMs = 1L)

    private fun find(notes: List<NoteEntity>, query: String): List<String> =
        NotesSearch.filter(NotesSearch.index(notes), query).map { it.id }

    @Test
    fun `Kyrie is found typed exactly, without accents and in capitals`() {
        val notes = listOf(note("k", "Κύριε ἐκέκραξα"), note("d", "Δόξα Πατρί"))

        listOf("Κύριε", "κυριε", "ΚΥΡΙΕ", "κύριε", "ΚΎΡΙΕ", "  κυριε  ").forEach { query ->
            assertEquals("query «$query»", listOf("k"), find(notes, query))
        }
    }

    @Test
    fun `a word in capitals is found with the final sigma everyone types`() {
        val notes = listOf(note("t", "ΤΡΟΧΟΣ"), note("x", "τρόπος"))

        assertEquals(listOf("t"), find(notes, "τροχος"))
        assertEquals(listOf("t"), find(notes, "τροχοσ"))
        assertEquals(listOf("t"), find(notes, "Τροχός"))
    }

    @Test
    fun `percent, underscore and backslash are matched literally`() {
        val notes = listOf(
            note("p", "Ταχύτητα 50%"),
            note("f", "500 ψαλμοί"),
            note("u", "ήχος_α"),
            note("s", "ήχος α"),
            note("b", "Α\\Β")
        )

        assertEquals(listOf("p"), find(notes, "50%"))
        assertEquals(listOf("u"), find(notes, "_"))
        assertEquals(listOf("u"), find(notes, "ηχος_"))
        assertEquals(listOf("b"), find(notes, "\\"))
    }

    @Test
    fun `a blank query keeps every note in the order the database gave`() {
        val notes = listOf(note("c", "Γ"), note("a", "Α"), note("b", "Β"))

        assertEquals(listOf("c", "a", "b"), find(notes, ""))
        assertEquals(listOf("c", "a", "b"), find(notes, "   "))
    }

    @Test
    fun `the body is searched too, and results keep their order`() {
        val notes = listOf(
            note("new", title = "Εσπερινός", body = "Κύριε ἐκέκραξα πρὸς σέ"),
            note("mid", title = "Χωρίς σχέση", body = "τίποτα"),
            note("old", title = "Κύριε ἐλέησον", body = "")
        )

        assertEquals(listOf("new", "old"), find(notes, "κυριε"))
        assertEquals(listOf("new"), find(notes, "προς σε"))
    }

    @Test
    fun `a match never runs from the title into the body`() {
        val notes = listOf(note("n", title = "Εσπερινός", body = "Κύριε ἐκέκραξα"))

        assertEquals(emptyList<String>(), find(notes, "εσπερινος κυριε"))
        assertEquals(emptyList<String>(), find(notes, "οςκυ"))
    }
}
