package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * «Οι ασκήσεις μου» (ClickUp `869f5x261`, F6): the rules for saving, renaming and deleting, and what
 * happens to stored entries this version cannot read — they are kept, never silently dropped.
 */
class ExerciseBookTest {

    private fun melody(vararg phthongs: PhthongName) = TrainerMelody(notes = phthongs.map { TrainerNote(it) })

    private val first = melody(PhthongName.NI, PhthongName.PA)
    private val second = TrainerMelody(notes = listOf(TrainerNote(PhthongName.DI)), bpm = 120, scale = TrainerScale(Mode.SECOND, -4))

    private fun ExerciseChange.book(): ExerciseBook = (this as ExerciseChange.Done).book

    private fun bookOf(vararg names: String): ExerciseBook =
        names.foldIndexed(ExerciseBook.EMPTY) { index, book, name -> book.saveAs(name, first, index.toLong()).book() }

    @Test
    fun savedExercisesAreListedNewestFirst() {
        val book = ExerciseBook.EMPTY.saveAs("Α", first, 1L).book().saveAs("Β", second, 2L).book()
        assertEquals(listOf("Β", "Α"), book.exercises.map { it.name })
        assertEquals(second, book.find("Β")!!.melody)
    }

    @Test
    fun aNameIsTrimmedAndUniqueIgnoringCase() {
        val book = ExerciseBook.EMPTY.saveAs("  Ψαλμός 140  ", first, 1L).book()
        assertEquals("Ψαλμός 140", book.exercises.single().name)
        assertEquals(ExerciseChange.NameTaken, book.saveAs("ψαλμός 140", second, 2L))
        assertEquals("the taken name is not overwritten", first, book.find("Ψαλμός 140")!!.melody)
    }

    @Test
    fun aBlankOrTooLongNameIsRefused() {
        assertEquals(ExerciseChange.NameBlank, ExerciseBook.EMPTY.saveAs("   ", first, 1L))
        val tooLong = "α".repeat(ExerciseBook.MAX_NAME_LENGTH + 1)
        assertEquals(ExerciseChange.NameTooLong, ExerciseBook.EMPTY.saveAs(tooLong, first, 1L))
        val longest = "α".repeat(ExerciseBook.MAX_NAME_LENGTH)
        assertTrue(ExerciseBook.EMPTY.saveAs(longest, first, 1L) is ExerciseChange.Done)
    }

    @Test
    fun theListIsCapped() {
        val full = bookOf(*Array(ExerciseBook.MAX_EXERCISES) { "Άσκηση $it" })
        assertEquals(ExerciseBook.MAX_EXERCISES, full.exercises.size)
        assertEquals(ExerciseChange.Full, full.saveAs("Μία ακόμα", first, 999L))
        // Deleting one makes room again.
        assertTrue(full.delete("Άσκηση 0").book().saveAs("Μία ακόμα", first, 999L) is ExerciseChange.Done)
    }

    @Test
    fun renameRefusesATakenNameButAllowsItsOwnInAnotherCase() {
        val book = bookOf("αρχή", "τέλος")
        assertEquals(ExerciseChange.NameTaken, book.rename("αρχή", "ΤΈΛΟΣ"))
        assertEquals(ExerciseChange.NameBlank, book.rename("αρχή", " "))
        val renamed = book.rename("αρχή", "Αρχή").book()
        assertEquals(setOf("Αρχή", "τέλος"), renamed.exercises.map { it.name }.toSet())
        assertEquals("the melody moves with the name", first, renamed.find("Αρχή")!!.melody)
        assertEquals("its place in the list does not change", book.exercises.map { it.savedAtMillis }, renamed.exercises.map { it.savedAtMillis })
    }

    @Test
    fun deleteRemovesThatExerciseOnly() {
        val book = bookOf("α", "β", "γ").delete("Β").book()
        assertEquals(listOf("γ", "α"), book.exercises.map { it.name })
        assertEquals(ExerciseChange.NotFound, book.delete("β"))
        assertEquals(ExerciseChange.NotFound, book.rename("β", "δ"))
    }

    @Test
    fun theStoredFormReadsBackTheSameList() {
        val book = ExerciseBook.EMPTY.saveAs("Α", first, 10L).book().saveAs("Β", second, 20L).book()
        val again = ExerciseBook.decode(book.encode())
        assertEquals(book.exercises, again.exercises)
        assertEquals(1, JSONObject(book.encode()).get("schemaVersion"))
    }

    /**
     * Written by a newer app, or damaged: an entry this version cannot read is left out of the list —
     * and written back unchanged, so saving here does not destroy it.
     */
    @Test
    fun anEntryThatCannotBeReadIsKeptOnTheNextWrite() {
        val readable = JSONObject(ExerciseBook.EMPTY.saveAs("Καλή", first, 1L).book().encode()).getJSONArray("exercises").getJSONObject(0)
        val newerMelody = JSONObject(readable.toString()).apply {
            put("name", "Από νεότερη")
            getJSONObject("melody").put("schemaVersion", 2)
        }
        val unknownPhthong = JSONObject(readable.toString()).apply {
            put("name", "Άγνωστος φθόγγος")
            getJSONObject("melody").getJSONArray("notes").getJSONObject(0).put("phthong", "XX")
        }
        val stored = JSONObject()
            .put("schemaVersion", 1)
            .put("exercises", JSONArray().put(readable).put(newerMelody).put(unknownPhthong))
            .toString()

        val book = ExerciseBook.decode(stored)
        assertEquals("only the readable one is listed", listOf("Καλή"), book.exercises.map { it.name })
        assertEquals("but all three still count", 3, book.storedCount)

        val written = JSONObject(book.saveAs("Νέα", second, 5L).book().encode()).getJSONArray("exercises")
        val kept = (0 until written.length()).map { written.getJSONObject(it) }
        assertEquals(4, kept.size)
        val newerKept = kept.single { it.getString("name") == "Από νεότερη" }
        assertEquals("the newer entry is written back as it was", newerMelody.toString(), newerKept.toString())
        val unknownKept = kept.single { it.getString("name") == "Άγνωστος φθόγγος" }
        assertEquals("so is the one with the unknown φθόγγος", unknownPhthong.toString(), unknownKept.toString())
    }

    @Test
    fun aListWrittenInANewerFormatIsReadOnly() {
        val newer = JSONObject().put("schemaVersion", 2).put("folders", JSONArray()).toString()
        val book = ExerciseBook.decode(newer)
        assertTrue(book.isNewerFormat)
        assertTrue(book.exercises.isEmpty())
        assertEquals(ExerciseChange.NewerFormat, book.saveAs("Α", first, 1L))
        assertEquals(ExerciseChange.NewerFormat, book.rename("Α", "Β"))
        assertEquals(ExerciseChange.NewerFormat, book.delete("Α"))
        assertThrows(IllegalStateException::class.java) { book.encode() }
    }

    @Test
    fun aDuplicateNameOnReadKeepsTheFirstAndPreservesTheOther() {
        val entry = JSONObject(ExerciseBook.EMPTY.saveAs("Ίδιο", first, 1L).book().encode()).getJSONArray("exercises").getJSONObject(0)
        val twin = JSONObject(entry.toString()).put("name", "ΊΔΙΟ").put("savedAt", 2L)
        val stored = JSONObject().put("schemaVersion", 1).put("exercises", JSONArray().put(entry).put(twin)).toString()
        val book = ExerciseBook.decode(stored)
        assertEquals(listOf("Ίδιο"), book.exercises.map { it.name })
        assertEquals(2, book.storedCount)
    }

    @Test
    fun nothingOrDamagedTextIsAnEmptyListThatCanBeWritten() {
        listOf(null, "", "not json", "[1, 2]").forEach { raw ->
            val book = ExerciseBook.decode(raw)
            assertTrue("«$raw»", book.exercises.isEmpty() && !book.isNewerFormat)
            assertTrue(book.saveAs("Α", first, 1L) is ExerciseChange.Done)
        }
        assertNull(ExerciseBook.EMPTY.find("Α"))
    }
}
