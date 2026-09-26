package com.johnchourp.learnbyzantinemusic.settings

import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.music.TimeSign
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs.Store.SETTINGS
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs.Store.TRAINER
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile.Item
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile.Line
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile.Reason
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile.Result.Accepted
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile.Result.Rejected
import com.johnchourp.learnbyzantinemusic.trainer.ExerciseBook
import com.johnchourp.learnbyzantinemusic.trainer.ExerciseChange
import com.johnchourp.learnbyzantinemusic.trainer.MelodySequence
import com.johnchourp.learnbyzantinemusic.trainer.TrainerExerciseStore
import com.johnchourp.learnbyzantinemusic.trainer.TrainerMelody
import com.johnchourp.learnbyzantinemusic.trainer.TrainerMelodyCodec
import com.johnchourp.learnbyzantinemusic.trainer.TrainerNote
import com.johnchourp.learnbyzantinemusic.trainer.TrainerScale
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Melody Trainer's exercises and last melody in the «Δεδομένα μάθησης» file (ClickUp `869f5x261`
 * with `869f5x25w`): they are the learner's own work, so they go to another phone — but only in the
 * form the Trainer itself writes: never a value it cannot read, and never an empty one.
 */
class TrainerLearningDataTest {

    private val exportedAt = 1_758_800_000_000L

    private val melody = TrainerMelody(
        notes = listOf(
            TrainerNote(PhthongName.DI),
            TrainerNote(PhthongName.KE, octaveShift = 1, baseDurationBeats = 2.5f),
            TrainerNote(PhthongName.ZO, signs = setOf(TimeSign.GORGON)),
        ),
        bpm = 72,
        scale = TrainerScale(Mode.PLAGAL_FOURTH, 5),
    )

    /** The other phone's Trainer store, by registered key, filled by what the import writes. */
    private class Phone : TrainerExerciseStore.Storage {
        val values = mutableMapOf<String, String>()
        override fun read(key: AppPrefs.Key): String? = values[key.name]
        override fun write(key: AppPrefs.Key, value: String) {
            values[key.name] = value
        }

        val importWriter = LearningDataFile.Writer { store, written ->
            if (store == TRAINER) written.forEach { (name, value) -> values[name] = value as String }
            true
        }
    }

    private fun bookOf(vararg names: String): ExerciseBook =
        names.foldIndexed(ExerciseBook.EMPTY) { index, book, name ->
            (book.saveAs(name, melody, nowMillis = exportedAt + index) as ExerciseChange.Done).book
        }

    private fun accepted(file: String): Accepted {
        val result = LearningDataFile.decode(file)
        assertTrue("expected the file to be accepted, got $result", result is Accepted)
        return result as Accepted
    }

    private fun export(trainer: Map<String, Any>): String = LearningDataFile.encode(
        mapOf(SETTINGS to mapOf("app_font_step" to 80), TRAINER to trainer),
        exportedAt,
        "1.17.0",
    )

    /** A version 1 file with one Trainer entry, next to a perfectly good setting. */
    private fun fileWith(key: String, value: String): String = JSONObject()
        .put("schemaVersion", 1)
        .put("exportedAt", exportedAt)
        .put("appVersion", "1.17.0")
        .put(
            "stores",
            JSONObject()
                .put(SETTINGS.fileName, JSONObject().put("app_font_step", JSONObject().put("type", "INT").put("value", 80)))
                .put(TRAINER.fileName, JSONObject().put(key, JSONObject().put("type", "STRING").put("value", value))),
        )
        .toString()

    @Test
    fun `the exercises and the last melody reach the other phone's Trainer as they were`() {
        val book = bookOf("Πλ. Δ΄, αρχή", "Κατάβαση")
        val file = export(
            mapOf(
                "trainer_exercises" to book.encode(),
                "trainer_last_melody" to TrainerMelodyCodec.encodeString(melody),
            )
        )
        val otherPhone = Phone().apply {
            // What the other phone had is replaced as a whole, as every key the file carries.
            values["trainer_exercises"] = bookOf("Μόνο εδώ").encode()
        }

        assertTrue(LearningDataFile.write(accepted(file), otherPhone.importWriter))

        val trainer = TrainerExerciseStore(otherPhone)
        assertEquals(book.exercises, trainer.loadExercises().exercises)
        assertEquals(melody, trainer.loadLastMelody())
    }

    @Test
    fun `the file carries the list as the Trainer would write it back`() {
        val leadingGorgon = melody.copy(notes = listOf(TrainerNote(PhthongName.NI, signs = setOf(TimeSign.GORGON))) + melody.notes)
        // A φθόγγος this version does not know: unreadable here, and kept as it is.
        val unknownPhthong = JSONObject().put("schemaVersion", 1).put("bpm", 80)
            .put("notes", JSONArray().put(JSONObject().put("phthong", "XX").put("octave", 0).put("length", 1)))
        val stored = JSONObject()
            .put("schemaVersion", 1)
            .put(
                "exercises",
                JSONArray()
                    .put(JSONObject().put("name", "  Αρχή  ").put("savedAt", 7L).put("melody", TrainerMelodyCodec.encode(leadingGorgon)))
                    .put(JSONObject().put("name", "Νεότερη").put("savedAt", 8L).put("melody", unknownPhthong)),
            )
            .toString()

        val carried = accepted(export(mapOf("trainer_exercises" to stored))).changes.getValue(TRAINER).getValue("trainer_exercises") as String

        assertEquals("exactly what the Trainer writes back", ExerciseBook.decode(stored).encode(), carried)
        val book = ExerciseBook.decode(carried)
        assertEquals("Αρχή", book.exercises.single().name)
        assertFalse("the γοργόν on the first note is taken off, as the Trainer does", book.exercises.single().melody.notes.first().hasGorgo)
        assertEquals(2, book.storedCount)
        assertTrue("the entry this version cannot read travels unchanged", "\"XX\"" in carried)
    }

    @Test
    fun `nothing to carry, or what the Trainer cannot use, stays on this phone`() {
        listOf(
            // A melody without notes and a list without entries: an import never replaces them with nothing.
            TrainerMelodyCodec.encodeString(TrainerMelody()) to ExerciseBook.EMPTY.encode(),
            """{"schemaVersion":2,"bpm":80,"notes":[]}""" to """{"schemaVersion":2,"exercises":[]}""",
            "{ not json" to "not json either",
        ).forEach { (lastMelody, exercises) ->
            val file = export(mapOf("trainer_last_melody" to lastMelody, "trainer_exercises" to exercises))

            assertFalse("nothing of the Trainer is in the file", TRAINER.fileName in file)
            assertEquals(mapOf(SETTINGS to mapOf("app_font_step" to 80)), accepted(file).changes)
        }
    }

    @Test
    fun `a value not in the Trainer's own form rejects the whole file`() {
        val canonical = JSONObject(TrainerMelodyCodec.encodeString(melody))
        val badValues = listOf(
            "trainer_last_melody" to TrainerMelodyCodec.encodeString(
                melody.copy(notes = listOf(TrainerNote(PhthongName.NI, signs = setOf(TimeSign.GORGON))) + melody.notes)
            ),
            "trainer_last_melody" to JSONObject(canonical.toString()).put("bpm", 999).toString(),
            "trainer_last_melody" to JSONObject(canonical.toString()).put("schemaVersion", 2).toString(),
            "trainer_last_melody" to "{ not json",
            "trainer_last_melody" to TrainerMelodyCodec.encodeString(TrainerMelody()),
            "trainer_exercises" to JSONObject()
                .put("schemaVersion", 1)
                .put("exercises", JSONArray().put(JSONObject().put("name", " Αρχή ").put("savedAt", 7L).put("melody", canonical)))
                .toString(),
            "trainer_exercises" to """{"schemaVersion":2,"exercises":[]}""",
            "trainer_exercises" to "not json",
            "trainer_exercises" to ExerciseBook.EMPTY.encode(),
        )
        badValues.forEach { (key, value) ->
            assertEquals("$key = $value", Rejected(Reason.BAD_VALUE, key), LearningDataFile.decode(fileWith(key, value)))
        }
    }

    @Test
    fun `the dialog says how many exercises the list will hold`() {
        // An exercise a newer app wrote: it travels along, but this version cannot open it.
        val newer = JSONObject().put("name", "δ").put("savedAt", 1L).put("melody", JSONObject().put("schemaVersion", 2))
        val withUnreadable = JSONObject(bookOf("α", "β", "γ").encode()).apply { getJSONArray("exercises").put(newer) }.toString()

        val file = export(mapOf("trainer_exercises" to withUnreadable, "trainer_last_melody" to TrainerMelodyCodec.encodeString(melody)))

        assertEquals(
            listOf(Line(Item.FONT_SIZE, 1), Line(Item.TRAINER_EXERCISES, 3), Line(Item.TRAINER_LAST_MELODY, 1)),
            LearningDataFile.summary(accepted(file))
        )
    }

    @Test
    fun `the largest list the Trainer keeps still fits in a file the import reads`() {
        // The longest note the Trainer writes: the longest φθόγγος name, the low octave, a half length, a
        // γοργόν — and since J2 (ClickUp `869f5x2cv`) the longest syllable, of the one character JSON
        // escapes at both levels of the file, the quotation mark.
        val longestSyllable = Char(34).toString().repeat(TrainerNote.MAX_SYLLABLE_LENGTH)
        val longest = TrainerMelody(
            notes = List(MelodySequence.MAX_NOTES) { index ->
                TrainerNote(
                    PhthongName.VOU,
                    octaveShift = -1,
                    baseDurationBeats = 3.5f,
                    signs = if (index == 0) emptySet() else setOf(TimeSign.GORGON),
                    syllable = longestSyllable,
                )
            },
            bpm = 240,
            scale = TrainerScale(Mode.PLAGAL_SECOND, -12),
        )
        assertTrue("the melody must be one the Trainer keeps as it is", MelodySequence(longest.notes).isValid)
        val full = (0 until ExerciseBook.MAX_EXERCISES).fold(ExerciseBook.EMPTY) { book, index ->
            val name = "Άσκηση $index ".padEnd(ExerciseBook.MAX_NAME_LENGTH, 'ω')
            (book.saveAs(name, longest, nowMillis = exportedAt + index) as ExerciseChange.Done).book
        }

        val file = export(mapOf("trainer_exercises" to full.encode(), "trainer_last_melody" to TrainerMelodyCodec.encodeString(longest)))

        assertTrue("${file.length} characters", file.length <= LearningDataFile.MAX_CHARS)
        val carried = accepted(file).changes.getValue(TRAINER).getValue("trainer_exercises") as String
        assertEquals(ExerciseBook.MAX_EXERCISES, ExerciseBook.decode(carried).exercises.size)
    }
}
