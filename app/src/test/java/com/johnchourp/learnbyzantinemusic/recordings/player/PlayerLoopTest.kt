package com.johnchourp.learnbyzantinemusic.recordings.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The A-B loop as the player uses it (ClickUp `869f5x268`): A then B, validated as they are marked,
 * and playback that goes round the passage — at B, and at the end of the file when B is the end.
 * The loop lives for the screen's session only; nothing here stores it.
 */
class PlayerLoopTest {

    private val rig = PlayerRig(durationMs = 60_000)

    private fun at(positionMs: Int) {
        rig.player.position = positionMs
    }

    @Test
    fun marksNeedARecording() {
        assertEquals(LoopMarkResult.UNAVAILABLE, rig.controller.markLoopStart())
        assertEquals(LoopMarkResult.UNAVAILABLE, rig.controller.markLoopEnd())
    }

    @Test
    fun bNeedsAFirst() {
        rig.playingAt(10_000)
        assertEquals(LoopMarkResult.NEEDS_START, rig.controller.markLoopEnd())
    }

    @Test
    fun bAtOrBeforeAIsRefused() {
        rig.playingAt(10_000)
        rig.controller.markLoopStart()

        at(8_000)
        assertEquals(LoopMarkResult.BEFORE_START, rig.controller.markLoopEnd())
        at(10_000)
        assertEquals(LoopMarkResult.BEFORE_START, rig.controller.markLoopEnd())
        assertNull(rig.state.loop)
    }

    @Test
    fun aLoopShorterThanHalfASecondIsRefused() {
        rig.playingAt(10_000)
        rig.controller.markLoopStart()

        at(10_300)
        assertEquals(LoopMarkResult.TOO_SHORT, rig.controller.markLoopEnd())
        assertNull(rig.state.loop)
    }

    @Test
    fun playbackGoesBackToAAtB() {
        rig.playingAt(10_000)
        rig.controller.markLoopStart()
        at(15_000)
        assertEquals(LoopMarkResult.SET, rig.controller.markLoopEnd())
        assertEquals(LoopRegion(10_000, 15_000), rig.state.loop)

        at(14_000)
        rig.controller.tick()
        assertEquals("inside the passage, nothing jumps", 14_000, rig.state.positionMs)

        at(15_030)
        rig.controller.tick()
        assertEquals(10_000, rig.player.seeks.last())
        assertEquals(10_000, rig.state.positionMs)
        assertTrue("and it keeps playing", rig.player.isPlaying)
    }

    @Test
    fun aNewAPastBDropsB() {
        rig.playingAt(10_000)
        rig.controller.markLoopStart()
        at(15_000)
        rig.controller.markLoopEnd()

        at(16_000)
        assertEquals(LoopMarkResult.SET, rig.controller.markLoopStart())

        assertEquals(16_000, rig.state.loopStartMs)
        assertNull("a B before the new A no longer fits", rig.state.loopEndMs)
    }

    @Test
    fun theEndOfTheFileGoesRoundTheLoopAgain() {
        rig.playingAt(50_000)
        rig.controller.markLoopStart()
        at(60_000)
        rig.controller.markLoopEnd()

        rig.player.complete()

        assertEquals(50_000, rig.player.seeks.last())
        assertTrue(rig.player.isPlaying)
        assertEquals(PlayerPhase.PLAYING, rig.state.phase)
    }

    @Test
    fun withoutALoopTheEndRewindsAndWaits() {
        rig.playingAt(59_000)

        rig.player.complete()

        assertEquals(0, rig.player.seeks.last())
        assertEquals(PlayerPhase.PAUSED, rig.state.phase)
        assertEquals(0, rig.state.positionMs)
        assertFalse(rig.player.isPlaying)
    }

    @Test
    fun clearingTheLoopLetsPlaybackRunOn() {
        rig.playingAt(10_000)
        rig.controller.markLoopStart()
        at(15_000)
        rig.controller.markLoopEnd()

        rig.controller.clearLoop()
        at(15_500)
        rig.controller.tick()

        assertNull(rig.state.loop)
        assertEquals(15_500, rig.state.positionMs)
    }
}
