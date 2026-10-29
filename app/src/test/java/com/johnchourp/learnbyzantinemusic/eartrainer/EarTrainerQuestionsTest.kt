package com.johnchourp.learnbyzantinemusic.eartrainer

import com.johnchourp.learnbyzantinemusic.music.Mode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** The levels of «Μάντεψε τον ήχο» and how the next question is drawn (ClickUp `869f4tqb9`). */
class EarTrainerQuestionsTest {

    @Test
    fun theStoredLevelKeysAreFrozen() {
        assertEquals(listOf("kyrioi", "all", "all_without_ison"), EarTrainerLevel.entries.map { it.key })
        EarTrainerLevel.entries.forEach { assertEquals(it, EarTrainerLevel.fromKey(it.key)) }
        assertNull(EarTrainerLevel.fromKey("plagal"))
        assertNull(EarTrainerLevel.fromKey(null))
    }

    @Test
    fun theLevelsGoFromTheFourKyrioiOverTheIsonToAllEightWithoutIt() {
        assertEquals(listOf(Mode.FIRST, Mode.SECOND, Mode.THIRD, Mode.FOURTH), EarTrainerLevel.KYRIOI.modes)
        assertEquals(Mode.entries.toList(), EarTrainerLevel.ALL.modes)
        assertEquals(Mode.entries.toList(), EarTrainerLevel.ALL_WITHOUT_ISON.modes)
        assertEquals(listOf(true, true, false), EarTrainerLevel.entries.map { it.withIson })
        assertEquals(EarTrainerLevel.KYRIOI, EarTrainerLevel.DEFAULT)
    }

    @Test
    fun theSameModeNeverComesTwiceInARow() {
        val random = Random(869)
        EarTrainerLevel.entries.forEach { level ->
            var previous: Mode? = null
            repeat(500) {
                val next = EarTrainerQuestions.next(level, previous, random)
                assertNotEquals(previous, next)
                previous = next
            }
        }
    }

    @Test
    fun everyModeOfTheLevelComesUpAndNoOther() {
        val random = Random(4)
        EarTrainerLevel.entries.forEach { level ->
            val drawn = mutableSetOf<Mode>()
            var previous: Mode? = null
            repeat(500) {
                previous = EarTrainerQuestions.next(level, previous, random).also(drawn::add)
            }
            assertEquals(level.modes.toSet(), drawn)
        }
    }

    @Test
    fun theFirstQuestionMayBeAnyMode() {
        val first = EarTrainerQuestions.next(EarTrainerLevel.KYRIOI, previous = null, random = Random(1))
        assertTrue(first in EarTrainerLevel.KYRIOI.modes)
    }
}
