package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.music.TimeSign
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * The syllables are saved with the melody (ClickUp `869f5x2cv`, J2) — as an **optional** field of the
 * record F6 already stores, not a new format. A melody without syllables is written exactly as
 * before, a record written before J2 loads unchanged, and the version stays 1: every version 1
 * reader takes only the fields it knows, so an older reader loads such a melody without its
 * syllables instead of refusing it.
 */
class TrainerSyllablesStorageTest {

    private val sung = TrainerMelody(
        notes = listOf(
            TrainerNote(PhthongName.PA, syllable = "Κύ"),
            TrainerNote(PhthongName.VOU, baseDurationBeats = 0.5f, syllable = "ρι"),
            TrainerNote(PhthongName.GA, signs = setOf(TimeSign.GORGON)),
            TrainerNote(PhthongName.DI, octaveShift = 1, baseDurationBeats = 2f, syllable = "ε"),
        ),
        bpm = 72,
        scale = TrainerScale(Mode.FIRST, -3),
    )

    private fun notesOf(json: String) = JSONObject(json).getJSONArray("notes").let { array ->
        (0 until array.length()).map { array.getJSONObject(it) }
    }

    @Test
    fun aMelodyWithSyllablesComesBackWhole() {
        val stored = TrainerMelodyCodec.encodeString(sung)
        assertEquals(sung, TrainerMelodyCodec.decodeString(stored))
        assertEquals(listOf("Κύ", "ρι", null, "ε"), notesOf(stored).map { it.opt("syllable") as? String })
    }

    @Test
    fun aMelodyWithoutSyllablesIsWrittenExactlyAsBefore() {
        val plain = sung.copy(notes = sung.notes.map { it.withSyllable(null) })
        val stored = TrainerMelodyCodec.encodeString(plain)
        notesOf(stored).forEach { assertFalse("no syllable field at all", it.has("syllable")) }
        assertEquals(plain, TrainerMelodyCodec.decodeString(stored))
    }

    @Test
    fun aRecordWrittenBeforeSyllablesLoadsUnchanged() {
        // Exactly what F6 writes: no syllable anywhere.
        val beforeJ2 = """{"schemaVersion":1,"bpm":80,"mode":"second","baseShift":-4,""" +
            """"notes":[{"phthong":"PA","octave":0,"length":1},{"phthong":"DI","octave":0,"length":1.5,"signs":["gorgo"]}]}"""
        val melody = requireNotNull(TrainerMelodyCodec.decodeString(beforeJ2)) { "a record written before J2 must load" }
        assertEquals(listOf(null, null), melody.notes.map { it.syllable })
        assertEquals(listOf(PhthongName.PA, PhthongName.DI), melody.notes.map { it.phthong })
        assertEquals(TrainerScale(Mode.SECOND, -4), melody.scale)
    }

    @Test
    fun theVersionStaysOne() {
        assertEquals(1, TrainerMelodyCodec.SCHEMA_VERSION)
        assertEquals(1, ExerciseBook.SCHEMA_VERSION)
        assertEquals(1, JSONObject(TrainerMelodyCodec.encodeString(sung)).get("schemaVersion"))
    }

    @Test
    fun aStoredSyllableIsCleanedOnTheWayInAndAStrangeOneIsLeftOut() {
        val stored = JSONObject(TrainerMelodyCodec.encodeString(sung))
        val notes = stored.getJSONArray("notes")
        notes.getJSONObject(0).put("syllable", "  Κύ  ")
        notes.getJSONObject(1).put("syllable", "ρ".repeat(30))
        notes.getJSONObject(2).put("syllable", "   ")
        notes.getJSONObject(3).put("syllable", 7)
        val read = TrainerMelodyCodec.decode(stored)!!
        assertEquals(listOf("Κύ", "ρ".repeat(TrainerNote.MAX_SYLLABLE_LENGTH), null, null), read.notes.map { it.syllable })
        // The rest of the melody is untouched: a syllable never rejects one.
        assertEquals(sung.notes.map { it.phthong }, read.notes.map { it.phthong })
    }

    @Test
    fun anExerciseKeepsItsSyllables() {
        val book = (ExerciseBook.EMPTY.saveAs("Κύριε", sung, nowMillis = 1L) as ExerciseChange.Done).book
        assertEquals(sung, ExerciseBook.decode(book.encode()).find("κύριε")!!.melody)
    }

    @Test
    fun theLearningDataFileCarriesTheSyllablesAsTheyAre() {
        // F5's rule: the export writes what the app reads back, and the import accepts only that form.
        val lastMelody = TrainerMelodyCodec.encodeString(sung)
        assertEquals(lastMelody, LearningDataFile.normalized(AppPrefs.TrainerLastMelody, lastMelody))
        val exercises = (ExerciseBook.EMPTY.saveAs("Κύριε", sung, nowMillis = 1L) as ExerciseChange.Done).book.encode()
        assertEquals(exercises, LearningDataFile.normalized(AppPrefs.TrainerExercises, exercises))
    }
}
