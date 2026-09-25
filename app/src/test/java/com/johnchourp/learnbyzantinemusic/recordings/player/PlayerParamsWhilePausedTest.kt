package com.johnchourp.learnbyzantinemusic.recordings.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `MediaPlayer.setPlaybackParams` with a non-zero speed **starts** a paused player (ClickUp
 * `869f5x268`). Moving the speed or shift slider while paused must not suddenly play the recording;
 * the new values wait, and are applied as playback starts. The fake player here has exactly that
 * quirk: any tuning it is given starts it.
 */
class PlayerParamsWhilePausedTest {

    private val rig = PlayerRig()

    private fun pausedAt(positionMs: Int): PlayerRig = rig.playingAt(positionMs).also { it.controller.pause() }

    @Test
    fun speedAndShiftChangedWhilePausedDoNotStartPlayback() {
        pausedAt(12_000)
        val tuningsBefore = rig.player.tunings.size

        rig.controller.setSpeed(0.75f)
        rig.controller.setShift(5)

        assertFalse("a paused recording must stay paused", rig.player.isPlaying)
        assertEquals(PlayerPhase.PAUSED, rig.state.phase)
        assertEquals("nothing reached the player", tuningsBefore, rig.player.tunings.size)
        assertEquals("but the screen shows the new values", PlaybackTuning(speed = 0.75f, shiftMoria = 5), rig.state.tuning)
    }

    @Test
    fun theWaitingTuningIsAppliedAsPlaybackStarts() {
        pausedAt(12_000)
        rig.controller.setSpeed(0.6f)
        rig.controller.setShift(-3)

        rig.controller.play()

        assertEquals(0.6f to PlaybackTuning(shiftMoria = -3).pitchRatio, rig.player.tunings.last())
        assertTrue(rig.player.isPlaying)
        assertEquals(PlayerPhase.PLAYING, rig.state.phase)
    }

    @Test
    fun whilePlayingAChangeAppliesAtOnceAndPlaybackGoesOn() {
        rig.playingAt(3_000)

        rig.controller.setSpeed(0.8f)

        assertEquals(0.8f to 1f, rig.player.tunings.last())
        assertTrue(rig.player.isPlaying)
        assertEquals(PlayerPhase.PLAYING, rig.state.phase)
    }

    @Test
    fun aRecordingAtNormalSpeedIsNeverRetunedForNothing() {
        rig.playingAt(0)

        assertTrue("a fresh player already plays 1×, unshifted", rig.player.tunings.isEmpty())
        assertTrue(rig.player.isPlaying)
    }

    @Test
    fun backToNormalWhilePausedIsAppliedOnPlayTooNotSkipped() {
        rig.playingAt(0)
        rig.controller.setSpeed(0.5f)
        rig.controller.pause()

        rig.controller.resetTuning()
        assertFalse(rig.player.isPlaying)
        rig.controller.play()

        assertEquals("the player remembers 0.5× until told otherwise", 1f to 1f, rig.player.tunings.last())
    }
}
