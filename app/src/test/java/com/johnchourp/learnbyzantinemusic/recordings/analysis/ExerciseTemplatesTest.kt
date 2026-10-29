package com.johnchourp.learnbyzantinemusic.recordings.analysis

import com.johnchourp.learnbyzantinemusic.docs.KotlinSource
import com.johnchourp.learnbyzantinemusic.music.PhthongName.DI
import com.johnchourp.learnbyzantinemusic.music.PhthongName.GA
import com.johnchourp.learnbyzantinemusic.music.PhthongName.PA
import com.johnchourp.learnbyzantinemusic.music.PhthongName.VOU
import com.johnchourp.learnbyzantinemusic.music.TimeSign
import com.johnchourp.learnbyzantinemusic.trainer.ExerciseBook
import com.johnchourp.learnbyzantinemusic.trainer.ExerciseChange
import com.johnchourp.learnbyzantinemusic.trainer.TrainerMelody
import com.johnchourp.learnbyzantinemusic.trainer.TrainerNote
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** «Από τις ασκήσεις μου» on the analysis screen (ClickUp `869f4tqef`, E6). */
class ExerciseTemplatesTest {

    private fun book(vararg exercises: Pair<String, TrainerMelody>): ExerciseBook =
        exercises.foldIndexed(ExerciseBook.EMPTY) { i, book, (name, melody) ->
            (book.saveAs(name, melody, nowMillis = i.toLong()) as ExerciseChange.Done).book
        }

    @Test
    fun onlyThePhthongsAreTakenInTheirOrder() {
        val melody = TrainerMelody(
            notes = listOf(
                TrainerNote(PA, octaveShift = 1, baseDurationBeats = 2f, syllable = "Κύ"),
                TrainerNote(VOU, signs = setOf(TimeSign.GORGON)),
                TrainerNote(PA, octaveShift = -1),
                TrainerNote(DI, baseDurationBeats = 0.5f),
            ),
        )
        assertEquals(listOf(PA, VOU, PA, DI), ExerciseTemplates.phthongs(melody))
    }

    @Test
    fun thePickerListsTheExercisesNewestFirstAndSkipsAnEmptyOne() {
        val picker = ExerciseTemplates.picker(
            book(
                "Πρώτη" to TrainerMelody(listOf(TrainerNote(PA), TrainerNote(VOU))),
                "Άδεια" to TrainerMelody(),
                "Τρίτη" to TrainerMelody(listOf(TrainerNote(GA))),
            ),
        )
        assertFalse(picker.newerFormat)
        assertEquals(
            listOf(ExerciseChoice("Τρίτη", listOf(GA)), ExerciseChoice("Πρώτη", listOf(PA, VOU))),
            picker.choices,
        )
    }

    @Test
    fun aListANewerAppWroteOffersNothing() {
        val newer = JSONObject().put("schemaVersion", ExerciseBook.SCHEMA_VERSION + 1).put("exercises", JSONArray()).toString()
        val picker = ExerciseTemplates.picker(ExerciseBook.decode(newer))
        assertTrue(picker.newerFormat)
        assertTrue(picker.choices.isEmpty())
    }

    /**
     * The analysis only reads the Trainer's exercises. This catches the Trainer's own writers and keys
     * named under `recordings/analysis`; a write straight through the preferences file it would not see.
     */
    @Test
    fun theAnalysisNeverWritesTheTrainersStore() {
        val analysis = File(KotlinSource.mainRoot, "com/johnchourp/learnbyzantinemusic/recordings/analysis")
        val writers = analysis.walkTopDown().filter { it.isFile && it.extension == "kt" }.filter { file ->
            val code = KotlinSource.withoutComments(file.readText())
            listOf("saveExercises", "saveLastMelody", "AppPrefs.TrainerExercises", "AppPrefs.TrainerLastMelody").any { it in code }
        }.map { it.name }.toList()
        assertEquals(emptyList<String>(), writers)
    }
}
