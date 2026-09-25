package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.music.TimeSign
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * «Ο Γυμναστής ξανανοίγει με την τελευταία μελωδία μετά από κλείσιμο και μετά από process death»
 * (ClickUp `869f5x261`, F6), at the store level.
 *
 * A process death loses every object; only what was written survives. So each test writes through
 * one [TrainerExerciseStore] and reads through a **new** one over the same stored strings — exactly
 * what the next process does. What the device adds on top (onStop, recreation) is listed in the PR.
 */
class TrainerMelodyRestoreTest {

    /** The stored strings, by registered key: what survives a process death. */
    private class Disk : TrainerExerciseStore.Storage {
        val values = mutableMapOf<String, String>()
        override fun read(key: AppPrefs.Key): String? = values[key.name]
        override fun write(key: AppPrefs.Key, value: String) {
            values[key.name] = value
        }
    }

    private val melody = TrainerMelody(
        notes = listOf(
            TrainerNote(PhthongName.KE, octaveShift = 1, baseDurationBeats = 2f),
            TrainerNote(PhthongName.DI, signs = setOf(TimeSign.GORGON)),
        ),
        bpm = 66,
        scale = TrainerScale(Mode.PLAGAL_FIRST, -6),
    )

    @Test
    fun theLastMelodyComesBackInTheNextProcess() {
        val disk = Disk()
        TrainerExerciseStore(disk).saveLastMelody(melody)
        assertEquals(melody, TrainerExerciseStore(disk).loadLastMelody())
    }

    @Test
    fun theExercisesComeBackInTheNextProcess() {
        val disk = Disk()
        val before = TrainerExerciseStore(disk)
        val book = (before.loadExercises().saveAs("Πλ. Α΄, αρχή", melody, 42L) as ExerciseChange.Done).book
        before.saveExercises(book)
        val after = TrainerExerciseStore(disk).loadExercises()
        assertEquals(book.exercises, after.exercises)
        assertEquals(melody, after.find("πλ. α΄, αρχή")!!.melody)
    }

    @Test
    fun nothingStoredMeansAnEmptyTrainer() {
        val store = TrainerExerciseStore(Disk())
        assertNull(store.loadLastMelody())
        assertTrue(store.loadExercises().exercises.isEmpty())
    }

    @Test
    fun aLastMelodyThatCannotBeReadIsNotRestored() {
        val disk = Disk()
        disk.values[AppPrefs.TrainerLastMelody.name] =
            JSONObject(TrainerMelodyCodec.encodeString(melody)).put("schemaVersion", 2).toString()
        assertNull(TrainerExerciseStore(disk).loadLastMelody())
    }

    @Test
    fun aListANewerAppWroteIsNeverOverwritten() {
        val disk = Disk()
        val newer = JSONObject().put("schemaVersion", 2).put("exercises", "whatever it is now").toString()
        disk.values[AppPrefs.TrainerExercises.name] = newer
        val store = TrainerExerciseStore(disk)
        store.saveExercises(store.loadExercises())
        assertEquals(newer, disk.values[AppPrefs.TrainerExercises.name])
    }

    @Test
    fun bothValuesLiveUnderTheirRegisteredKeysInTheTrainersOwnFile() {
        val disk = Disk()
        val store = TrainerExerciseStore(disk)
        store.saveLastMelody(melody)
        store.saveExercises((store.loadExercises().saveAs("Α", melody, 1L) as ExerciseChange.Done).book)
        assertEquals(setOf("trainer_last_melody", "trainer_exercises"), disk.values.keys)
        assertEquals(AppPrefs.Store.TRAINER, AppPrefs.TrainerLastMelody.store)
        assertEquals(AppPrefs.Store.TRAINER, AppPrefs.TrainerExercises.store)
    }

    /**
     * The «Μεταφορά βάσης» belongs to the singer's voice, one value per ήχος shared with 8 Ήχοι (F2):
     * a restored melody takes today's value, and its own is only the fallback.
     */
    @Test
    fun aRestoredMelodyFollowsTheShiftItsModeHasNow() {
        assertEquals(-8, melody.withLiveShift(-8).scale.baseShiftMoria)
        assertEquals("no stored shift for that ήχος yet", -6, melody.withLiveShift(null).scale.baseShiftMoria)
        assertEquals(BaseShift.MAX_MORIA, melody.withLiveShift(40).scale.baseShiftMoria)
        assertEquals(Mode.PLAGAL_FIRST, melody.withLiveShift(-8).scale.mode)
        val diatonic = melody.copy(scale = TrainerScale.DIATONIC)
        assertSame("«Διατονικός» has no shift to follow", diatonic, diatonic.withLiveShift(5))
    }
}
