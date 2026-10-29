package com.johnchourp.learnbyzantinemusic.eartrainer

import com.johnchourp.learnbyzantinemusic.music.Mode
import kotlin.random.Random

/**
 * Which ήχος the next question of «Μάντεψε τον ήχο» is (ClickUp `869f4tqb9`).
 *
 * Any of [EarTrainerLevel.modes] at random, except the one just asked: the same ήχος twice in a row
 * teaches nothing that one question did not, and it reads like a bug.
 */
object EarTrainerQuestions {

    /** The ήχος of the question after [previous], on [level], drawn with [random]. */
    fun next(level: EarTrainerLevel, previous: Mode?, random: Random = Random.Default): Mode {
        val candidates = level.modes.filter { it != previous }.ifEmpty { level.modes }
        return candidates[random.nextInt(candidates.size)]
    }
}
