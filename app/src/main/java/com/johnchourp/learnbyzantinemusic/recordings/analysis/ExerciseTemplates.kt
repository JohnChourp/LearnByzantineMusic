package com.johnchourp.learnbyzantinemusic.recordings.analysis

import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.trainer.ExerciseBook
import com.johnchourp.learnbyzantinemusic.trainer.TrainerMelody

/** One saved exercise as the analysis offers it: its name and the φθόγγοι it would be compared with. */
data class ExerciseChoice(val name: String, val phthongs: List<PhthongName>)

/**
 * «Από τις ασκήσεις μου»: the exercises the analysis can take an expected melody from.
 * [newerFormat] means a newer app wrote the list, and none of it can be read here.
 */
data class ExercisePicker(val choices: List<ExerciseChoice>, val newerFormat: Boolean)

/**
 * How an exercise of the Melody Trainer becomes an expected melody (ClickUp `869f4tqef`, E6).
 *
 * Only the φθόγγοι are taken, in order. The comparison ignores octaves and timing, so octave, length,
 * time signs and syllables have nowhere to go. The analysis only reads the Trainer's exercises; it
 * never writes them.
 */
object ExerciseTemplates {

    fun phthongs(melody: TrainerMelody): List<PhthongName> = melody.notes.map { it.phthong }

    /**
     * The exercises of [book] in its own order, newest first. One without φθόγγοι is left out: as a
     * template it would only clear the melody.
     */
    fun picker(book: ExerciseBook): ExercisePicker = ExercisePicker(
        choices = book.exercises.mapNotNull { exercise ->
            phthongs(exercise.melody).takeIf { it.isNotEmpty() }?.let { ExerciseChoice(exercise.name, it) }
        },
        newerFormat = book.isNewerFormat,
    )
}
