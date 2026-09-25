package com.johnchourp.learnbyzantinemusic.trainer

import android.content.SharedPreferences
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs

/**
 * Where the Melody Trainer keeps what the user made (ClickUp `869f5x261`, F6): the last melody, so
 * it comes back after the screen is closed or the process is killed, and «Οι ασκήσεις μου».
 *
 * Two registered keys, `AppPrefs.TrainerLastMelody` and `AppPrefs.TrainerExercises`, in their own
 * file, `AppPrefs.Store.TRAINER`. The formats and what happens to a value that cannot be read are
 * [TrainerMelodyCodec]'s and [ExerciseBook]'s; this class only moves the text.
 *
 * It works through [Storage] — a string per key — so that the whole round trip, including a new
 * store reading what an earlier one wrote, the way a new process does after a process death, is
 * tested on the JVM. The app passes [forPrefs].
 */
class TrainerExerciseStore(private val storage: Storage) {

    /** A string per registered key. */
    interface Storage {
        fun read(key: AppPrefs.Key): String?
        fun write(key: AppPrefs.Key, value: String)
    }

    /** The last melody, or null when there is none or it cannot be read. */
    fun loadLastMelody(): TrainerMelody? = TrainerMelodyCodec.decodeString(storage.read(AppPrefs.TrainerLastMelody))

    fun saveLastMelody(melody: TrainerMelody) {
        storage.write(AppPrefs.TrainerLastMelody, TrainerMelodyCodec.encodeString(melody))
    }

    fun loadExercises(): ExerciseBook = ExerciseBook.decode(storage.read(AppPrefs.TrainerExercises))

    /** Writes [book]; a list a newer app wrote is left exactly as it is. */
    fun saveExercises(book: ExerciseBook) {
        if (book.isNewerFormat) return
        storage.write(AppPrefs.TrainerExercises, book.encode())
    }

    companion object {
        /**
         * The app's storage: the Trainer's preferences file. `apply()` is enough for a process
         * death — Android finishes pending writes before an activity's onStop returns.
         */
        fun forPrefs(prefs: SharedPreferences): Storage = object : Storage {
            override fun read(key: AppPrefs.Key): String? = prefs.getString(key.name, null)

            override fun write(key: AppPrefs.Key, value: String) {
                prefs.edit().putString(key.name, value).apply()
            }
        }
    }
}
