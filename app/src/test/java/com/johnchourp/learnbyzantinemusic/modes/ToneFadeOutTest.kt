package com.johnchourp.learnbyzantinemusic.modes

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A stopping tone fades from the level it is sounding at, never from full (ClickUp `869f5x2cv`).
 *
 * The fade used to step from full level down, whatever the tone's level was. That was harmless while
 * every tone played at full level; «Ψάλλε μαζί» plays its guide at 50 %, and a fade that starts at
 * full jumps up before it falls — a blip at the end of every note of the half round.
 */
class ToneFadeOutTest {

    @Test
    fun aFadeStartsAtTheTonesLevelAndOnlyFalls() {
        listOf(1f, 0.6f, 0.5f, 0.25f, 0.01f).forEach { level ->
            val steps = ToneFadeOut.levels(level)
            assertEquals("$level: the first step is where the tone already is", level, steps.first(), 0f)
            assertTrue("$level: never above it", steps.all { it <= level })
            assertTrue("$level: always falling", steps.toList().zipWithNext().all { (a, b) -> b < a })
            assertEquals(ToneFadeOut.STEPS, steps.size)
        }
    }

    @Test
    fun atFullLevelItIsTheFadeEveryToneAlwaysHad() {
        assertArrayEquals(floatArrayOf(1f, 0.8f, 0.6f, 0.4f, 0.2f), ToneFadeOut.levels(1f), 0f)
    }

    @Test
    fun aSilentToneStaysSilentAndALevelOutOfRangeIsBroughtBack() {
        assertTrue(ToneFadeOut.levels(0f).all { it == 0f })
        assertArrayEquals(ToneFadeOut.levels(1f), ToneFadeOut.levels(3f), 0f)
        assertTrue(ToneFadeOut.levels(-1f).all { it == 0f })
    }

    @Test
    fun thePlayerKeepsTheLevelItWasGivenWithinZeroToOne() {
        val player = PhthongTonePlayer()
        assertEquals("full level until told otherwise", 1f, player.volume, 0f)
        player.setVolume(0.3f)
        assertEquals(0.3f, player.volume, 0f)
        player.setVolume(1.7f)
        assertEquals(1f, player.volume, 0f)
        player.setVolume(-0.5f)
        assertEquals(0f, player.volume, 0f)
    }
}
