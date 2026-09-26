package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.trainer.WaitModeScore.Run
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The stars at the end of «Παραλλαγή με αναμονή» (ClickUp `869f5x2cd`, J1): 3, less one for any skip,
 * one more for skipping over a quarter of the line, one for a slow median time to lock; never under
 * 1 once a note was sung, and 0 when every note was skipped.
 */
class WaitModeScoreTest {

    private fun stars(notes: Int, skips: Int, vararg lockMillis: Long) =
        WaitModeScore.stars(Run(noteCount = notes, skips = skips, lockMillis = lockMillis.toList()))

    @Test
    fun aQuickRunWithoutSkipsHasThreeStars() {
        assertEquals(3, stars(4, 0, 600, 900, 1_200, 700))
    }

    @Test
    fun eachKindOfTroubleCostsAStar() {
        assertEquals("one skip", 2, stars(8, 1, 600, 900, 1_200, 700, 800, 650, 720))
        assertEquals("more than a quarter skipped", 1, stars(8, 3, 600, 900, 1_200, 700, 800))
        assertEquals("exactly a quarter is not more than a quarter", 2, stars(8, 2, 600, 900, 1_200, 700, 800, 650))
        assertEquals("slow", 2, stars(4, 0, 3_500, 4_000, 3_200, 5_000))
        assertEquals("slow and a skip", 1, stars(4, 1, 3_500, 4_000, 3_200))
        assertEquals("never under one once something was sung", 1, stars(4, 3, 9_000))
    }

    @Test
    fun theSlowLineIsTheMedianNotTheMean() {
        // One long search among quick ones: the mean is over 3 s, the median is not.
        assertEquals(3, stars(5, 0, 600, 700, 800, 900, 20_000))
        assertEquals(800L, WaitModeScore.medianLockMillis(listOf(600, 700, 800, 900, 20_000)))
        assertEquals("even count: the middle two averaged", 750L, WaitModeScore.medianLockMillis(listOf(900, 600, 700, 800)))
        assertEquals("exactly the limit is not slow", 3, stars(3, 0, WaitModeScore.SLOW_MEDIAN_MS, WaitModeScore.SLOW_MEDIAN_MS, 100))
    }

    @Test
    fun aRunWhereEveryNoteWasSkippedHasNone() {
        assertEquals(0, stars(3, 3))
        assertNull(WaitModeScore.medianLockMillis(emptyList()))
    }
}
