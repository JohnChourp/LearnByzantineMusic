package com.johnchourp.learnbyzantinemusic.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The notes editor against its own autosave. [NotesViewModel] needs the Android main dispatcher, so
 * these drive [NotesEditorSync] — which makes every decision the ViewModel acts on — through the same
 * sequences the screen goes through: typing, a save starting, the database sending its list back, the
 * save finishing.
 */
class NotesEditorSyncTest {

    private fun note(id: String, title: String = "", body: String = "", updated: Long = 1L) =
        NoteEntity(id, title, body, createdAtEpochMs = 1L, updatedAtEpochMs = updated)

    // A screen with a backup folder (saves need one) that has just received these notes.
    private fun screenWith(vararg notes: NoteEntity): NotesUiState =
        NotesEditorSync.onNotesChanged(NotesUiState(hasConfiguredFolder = true), notes.toList())

    @Test
    fun `a save stores exactly what was typed`() {
        val existing = note("a", title = "Ήχος", body = "κείμενο")
        val request = NoteSaveRequest("a", title = "  Ήχος πλάγιος ", body = "\nπρώτη γραμμή \n\n")

        val stored = NotesEditorSync.noteToStore(request, existing, nowEpochMs = 9L)

        assertEquals("  Ήχος πλάγιος ", stored.title)
        assertEquals("\nπρώτη γραμμή \n\n", stored.body)
        assertEquals(1L, stored.createdAtEpochMs)
        assertEquals(9L, stored.updatedAtEpochMs)
    }

    @Test
    fun `a trailing space and new line survive their own autosave`() {
        val a = note("a", title = "Ήχος", body = "Πρώτη γραμμή")
        var state = screenWith(a)
        state = NotesEditorSync.onTitleEdited(state, "Ήχος ")
        state = NotesEditorSync.onBodyEdited(state, "Πρώτη γραμμή\n")

        val request = NotesEditorSync.saveRequest(state)!!
        assertEquals(NoteSaveRequest("a", "Ήχος ", "Πρώτη γραμμή\n"), request)
        state = NotesEditorSync.onSaveStarted(state, request)
        val echo = NotesEditorSync.noteToStore(request, existing = a, nowEpochMs = 2L)
        assertEquals("Ήχος ", echo.title)
        state = NotesEditorSync.onNotesChanged(state, listOf(echo))
        state = NotesEditorSync.onSaveFinished(state, request)

        assertEquals("Ήχος ", state.editorTitle)
        assertEquals("Πρώτη γραμμή\n", state.editorBody)
        assertNull("nothing left to save, so no second backup file", NotesEditorSync.saveRequest(state))
    }

    @Test
    fun `letters typed while a save runs are kept, and saved next`() {
        val a = note("a", title = "Εσπερινός", body = "Κύριε")
        var state = screenWith(a)
        state = NotesEditorSync.onBodyEdited(state, "Κύριε ἐκέκραξα")
        val first = NotesEditorSync.saveRequest(state)!!
        state = NotesEditorSync.onSaveStarted(state, first)

        state = NotesEditorSync.onBodyEdited(state, "Κύριε ἐκέκραξα πρὸς σέ")
        // The database sends back what the save wrote: older than the editor by now.
        state = NotesEditorSync.onNotesChanged(state, listOf(NotesEditorSync.noteToStore(first, a, 2L)))
        assertEquals("Κύριε ἐκέκραξα πρὸς σέ", state.editorBody)

        state = NotesEditorSync.onSaveFinished(state, first)
        assertEquals("Κύριε ἐκέκραξα πρὸς σέ", state.editorBody)
        assertEquals(
            NoteSaveRequest("a", "Εσπερινός", "Κύριε ἐκέκραξα πρὸς σέ"),
            NotesEditorSync.saveRequest(state)
        )
    }

    @Test
    fun `text already being saved is not saved twice, yet the exit flush still covers it`() {
        var state = screenWith(note("a", title = "Α", body = "α"))
        state = NotesEditorSync.onBodyEdited(state, "αβ")
        val running = NotesEditorSync.saveRequest(state)!!
        state = NotesEditorSync.onSaveStarted(state, running)

        // Leaving the note, or «Νέα σημείωση», while that save runs: nothing more to write.
        assertNull(NotesEditorSync.saveRequest(state))
        // Leaving the screen cancels the screen's own save, so the exit flush writes the text itself.
        assertEquals(running, NotesEditorSync.exitSaveRequest(state))
        // Undoing the edit mid-save is due: the running save is about to store «αβ».
        state = NotesEditorSync.onBodyEdited(state, "α")
        assertEquals(NoteSaveRequest("a", "Α", "α"), NotesEditorSync.saveRequest(state))
    }

    @Test
    fun `new note opens once the list containing it arrives, also after a search`() {
        val a = note("a", title = "Κύριε ἐκέκραξα", updated = 2L)
        val b = note("b", title = "Ἀπολυτίκιον", updated = 1L)
        // A search for «Κύριε» lists only A, which is open with unsaved text.
        var state = screenWith(a)
        state = NotesEditorSync.onBodyEdited(state, "στίχοι")

        // Created before the full list (the ViewModel clears the search) has reached the editor.
        state = NotesEditorSync.onNoteCreated(state, "new")
        assertEquals("a", state.selectedNoteId)
        assertEquals("στίχοι", state.editorBody)

        state = NotesEditorSync.onNotesChanged(state, listOf(note("new", updated = 3L), a, b))
        assertEquals("new", state.selectedNoteId)
        assertEquals("", state.editorTitle)
        assertEquals("", state.editorBody)
        assertNull(NotesEditorSync.saveRequest(state))
    }

    @Test
    fun `new note opens at once when its list is already there`() {
        val a = note("a", title = "Α", body = "α", updated = 2L)
        var state = screenWith(a)
        // The echo of the creation arrives first, and alone does not move the editor off A.
        state = NotesEditorSync.onNotesChanged(state, listOf(note("new", updated = 3L), a))
        assertEquals("a", state.selectedNoteId)

        state = NotesEditorSync.onNoteCreated(state, "new")
        assertEquals("new", state.selectedNoteId)
        assertEquals("", state.editorTitle)
        assertEquals("", state.editorBody)
    }

    @Test
    fun `opening another note loads it, and tapping the open note keeps what is typed`() {
        val a = note("a", title = "Α", body = "α")
        val b = note("b", title = "Β", body = "β")
        var state = screenWith(a, b)
        state = NotesEditorSync.onBodyEdited(state, "α και νέο")

        state = NotesEditorSync.open(state, "a")
        assertEquals("α και νέο", state.editorBody)

        state = NotesEditorSync.open(state, "b")
        assertEquals("b", state.selectedNoteId)
        assertEquals("Β", state.editorTitle)
        assertEquals("β", state.editorBody)
        assertNull(NotesEditorSync.saveRequest(state))
    }

    @Test
    fun `when the open note leaves the list the first remaining one opens`() {
        val a = note("a", title = "Α", body = "α", updated = 2L)
        val b = note("b", title = "Β", body = "β", updated = 1L)
        var state = screenWith(a, b)

        state = NotesEditorSync.onNotesChanged(state, listOf(b))
        assertEquals("b", state.selectedNoteId)
        assertEquals("β", state.editorBody)

        state = NotesEditorSync.onNotesChanged(state, emptyList())
        assertNull(state.selectedNoteId)
        assertEquals("", state.editorTitle)
        assertEquals("", state.editorBody)
    }

    @Test
    fun `an import reloads the editor even though the note id is unchanged`() {
        var state = screenWith(note("a", title = "Πριν", body = "παλιό κείμενο"))
        val imported = note("a", title = "Από το backup", body = "κείμενο του backup", updated = 5L)
        // The imported list reaches the screen before the import is reported finished.
        state = NotesEditorSync.onNotesChanged(state, listOf(imported))

        state = NotesEditorSync.onImportFinished(state)

        assertEquals("Από το backup", state.editorTitle)
        assertEquals("κείμενο του backup", state.editorBody)
        assertNull("autosave must not write the old text back", NotesEditorSync.saveRequest(state))
    }

    @Test
    fun `an import reported before its list arrives still ends on the imported text`() {
        var state = screenWith(note("a", title = "Πριν", body = "παλιό κείμενο"))
        state = NotesEditorSync.onImportFinished(state)

        val imported = note("a", title = "Από το backup", body = "κείμενο του backup", updated = 5L)
        state = NotesEditorSync.onNotesChanged(state, listOf(imported))
        assertEquals("Από το backup", state.editorTitle)
        assertEquals("κείμενο του backup", state.editorBody)
        assertNull(NotesEditorSync.saveRequest(state))

        // From the first keystroke on, the user's text wins over any list again.
        state = NotesEditorSync.onBodyEdited(state, "κείμενο του backup, διορθωμένο")
        state = NotesEditorSync.onNotesChanged(state, listOf(imported))
        assertEquals("κείμενο του backup, διορθωμένο", state.editorBody)
    }

    @Test
    fun `a new search saves the open note first, so a search that hides it loses nothing`() {
        val a = note("a", title = "Κύριε ἐκέκραξα", body = "στίχοι", updated = 2L)
        val b = note("b", title = "Δόξα", body = "δοξαστικό", updated = 1L)
        assertNull("nothing typed, nothing saved", NotesEditorSync.onSearchChanged(screenWith(a, b), "Δ").save)
        var state = screenWith(a, b)
        // Typed less than 1.2 s ago: the autosave is still pending.
        state = NotesEditorSync.onBodyEdited(state, "στίχοι και ψαλμοί")

        val step = NotesEditorSync.onSearchChanged(state, "Δόξα")
        assertEquals("Δόξα", step.state.searchQuery)
        assertEquals(NoteSaveRequest("a", "Κύριε ἐκέκραξα", "στίχοι και ψαλμοί"), step.save)
        // The next letter in the search box, while that save runs, does not repeat it.
        assertNull(NotesEditorSync.onSearchChanged(step.state, "Δόξα Π").save)

        // The search hides A: the editor moves to B, and A's text is already on its way to the database.
        state = NotesEditorSync.onNotesChanged(step.state, listOf(b))
        assertEquals("b", state.selectedNoteId)
        assertNull(NotesEditorSync.onSearchChanged(state, "Δόξα Πατρί").save)
    }
}
