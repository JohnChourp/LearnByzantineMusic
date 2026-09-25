package com.johnchourp.learnbyzantinemusic.recordings.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The player's life around the screen's (ClickUp `869f5x268`): released in `onStop` — no sound in the
 * background — with the recording, position, loop, speed and shift kept for the next play; a file the
 * device cannot decode fails quietly, leaving the way to another app; and callbacks from a player that
 * was already replaced change nothing.
 */
class PlayerLifecycleTest {

    private val rig = PlayerRig()

    @Test
    fun leavingTheScreenReleasesThePlayerAndKeepsTheRest() {
        rig.playingAt(20_000)
        rig.controller.setSpeed(0.7f)
        rig.controller.markLoopStart()
        rig.player.position = 25_000
        rig.controller.markLoopEnd()
        rig.player.position = 23_000

        rig.controller.suspend()

        assertTrue("released: nothing can play in the background", rig.players.single().released)
        assertEquals(PlayerPhase.PAUSED, rig.state.phase)
        assertEquals(23_000, rig.state.positionMs)
        assertEquals(0.7f, rig.state.tuning.speed)
        assertEquals(LoopRegion(20_000, 25_000), rig.state.loop)
    }

    @Test
    fun playingAgainPreparesAFreshPlayerWhereItWas() {
        rig.playingAt(20_000)
        rig.controller.setSpeed(0.7f)
        rig.controller.suspend()

        rig.controller.play()

        assertEquals("a new player — the old one is gone", 2, rig.players.size)
        assertEquals(20_000, rig.player.seeks.first())
        assertEquals(0.7f to 1f, rig.player.tunings.last())
        assertTrue(rig.player.isPlaying)
    }

    @Test
    fun aFileTheDeviceCannotDecodeFailsQuietly() {
        rig.failPrepare = true

        rig.controller.open("chant.opus", "chant.opus")

        assertEquals(PlayerPhase.FAILED, rig.state.phase)
        assertEquals("the card still names it, for «Άνοιγμα σε άλλη εφαρμογή»", "chant.opus", rig.state.title)
        assertTrue(rig.player.released)
        rig.controller.play()
        assertEquals(PlayerPhase.FAILED, rig.state.phase)
    }

    @Test
    fun aPlayerThatCannotEvenBeCreatedFailsQuietlyToo() {
        rig.factoryThrows = true

        rig.controller.open("chant.opus", "chant.opus")

        assertEquals(PlayerPhase.FAILED, rig.state.phase)
    }

    @Test
    fun aDeviceThatRefusesSpeedStillPlaysAndSaysSo() {
        rig.refuseTuning = true
        rig.playingAt(0)

        rig.controller.setSpeed(0.7f)

        assertFalse(rig.state.tuningAvailable)
        assertTrue("it plays, unchanged", rig.player.isPlaying)
        assertEquals(PlayerPhase.PLAYING, rig.state.phase)
    }

    @Test
    fun closingForgetsTheRecordingButKeepsSpeedAndShift() {
        rig.playingAt(0)
        rig.controller.setShift(3)

        rig.controller.close()

        assertEquals(PlayerPhase.EMPTY, rig.state.phase)
        assertNull(rig.state.title)
        assertEquals(3, rig.state.tuning.shiftMoria)
        assertTrue(rig.player.released)
    }

    @Test
    fun aNewRecordingStartsAtItsBeginningWithoutTheOldLoop() {
        rig.playingAt(20_000)
        rig.controller.markLoopStart()

        rig.controller.open("next.flac", "next.flac")

        assertEquals(0, rig.state.positionMs)
        assertNull(rig.state.loopStartMs)
        assertEquals(2, rig.state.openCount)
        assertTrue(rig.players.first().released)
    }

    @Test
    fun lateCallbacksOfAReplacedPlayerChangeNothing() {
        rig.prepareAtOnce = false
        rig.controller.open("first.flac", "first.flac")
        rig.controller.open("second.flac", "second.flac")
        rig.players[1].finishPreparing()

        rig.players[0].finishPreparing()

        assertEquals("second.flac", rig.state.title)
        assertTrue(rig.players[1].isPlaying)
        assertFalse("the replaced player never starts", rig.players[0].isPlaying)
    }
}
