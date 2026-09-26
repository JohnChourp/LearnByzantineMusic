package com.johnchourp.learnbyzantinemusic.trainer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one cursor over the written line (ClickUp `869f5x2cv`, J2): the timeline moves it in «Ψάλλε
 * μαζί», and the voice will move it in «Παραλλαγή με αναμονή» (J1, `869f5x2cd`). Both must mean the
 * same by "the next note" and "the next round".
 */
class PracticeCursorTest {

    @Test
    fun aLineStartsOnItsFirstNoteInTheFirstRound() {
        val cursor = PracticeCursor.start(4)
        assertEquals(0, cursor.current)
        assertEquals(0, cursor.round)
        assertFalse(cursor.isOnLastNote)
    }

    @Test
    fun advancingWalksTheNotesAndWrapsIntoTheNextRound() {
        var cursor = PracticeCursor.start(3)
        val visited = mutableListOf<Pair<Int, Int?>>()
        repeat(7) {
            visited += cursor.round to cursor.current
            cursor = cursor.advance()
        }
        assertEquals(listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0, 1 to 1, 1 to 2, 2 to 0), visited)
    }

    @Test
    fun theTimelineAndTheVoiceAgreeOnWhereTheLineIs() {
        val noteCount = 5
        (0..3).forEach { round ->
            (0 until noteCount).forEach { index ->
                var byVoice = PracticeCursor.start(noteCount)
                repeat(round * noteCount + index) { byVoice = byVoice.advance() }
                val byTimeline = PracticeCursor.start(noteCount).at(round, index)
                assertEquals("round $round, note $index", byTimeline, byVoice)
            }
        }
    }

    @Test
    fun onlyTheLastNoteLeadsIntoTheNextRound() {
        val cursor = PracticeCursor.start(3).at(0, 2)
        assertTrue(cursor.isOnLastNote)
        assertEquals(PracticeCursor(3, index = 0, round = 1), cursor.advance())
        assertFalse(PracticeCursor.start(3).at(4, 1).isOnLastNote)
    }

    @Test
    fun anEmptyLineHasNoNoteAndStaysPut() {
        val empty = PracticeCursor.start(0)
        assertNull(empty.current)
        assertFalse(empty.isOnLastNote)
        assertEquals(empty, empty.advance())
    }

    @Test
    fun aNoteOutsideTheLineIsRefused() {
        val cursor = PracticeCursor.start(3)
        assertThrows(IllegalArgumentException::class.java) { cursor.at(0, 3) }
        assertThrows(IllegalArgumentException::class.java) { cursor.at(0, -1) }
        assertThrows(IllegalArgumentException::class.java) { cursor.at(-1, 0) }
    }
}
