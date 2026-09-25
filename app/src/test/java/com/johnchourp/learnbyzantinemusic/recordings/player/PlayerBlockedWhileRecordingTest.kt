package com.johnchourp.learnbyzantinemusic.recordings.player

import com.johnchourp.learnbyzantinemusic.recordings.RecordingStateUi
import com.johnchourp.learnbyzantinemusic.recordings.session.RecordingSessionState
import com.johnchourp.learnbyzantinemusic.recordings.session.RecordingTarget
import com.johnchourp.learnbyzantinemusic.recordings.session.SessionRig
import com.johnchourp.learnbyzantinemusic.recordings.session.SessionTestKit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Nothing plays while a recording is in progress — recording, paused or saving (ClickUp
 * `869f5x268`): the loudspeaker would go straight into the microphone. What was playing pauses, and
 * the player refuses to start until the recording is over.
 */
class PlayerBlockedWhileRecordingTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val rig = PlayerRig()

    @Test
    fun theRecordingPhasesBlockPlaybackAndOnlyThey() {
        assertEquals(
            mapOf(
                RecordingStateUi.IDLE to false,
                RecordingStateUi.RECORDING to true,
                RecordingStateUi.PAUSED to true,
                RecordingStateUi.SAVING to true,
                RecordingStateUi.ERROR to false,
            ),
            RecordingStateUi.entries.associateWith { playbackBlockedBy(RecordingSessionState(phase = it)) },
        )
    }

    @Test
    fun blockingPausesWhatWasPlayingAndRefusesToPlay() {
        rig.playingAt(5_000)

        rig.controller.setBlocked(true)

        assertFalse(rig.player.isPlaying)
        assertEquals(PlayerPhase.PAUSED, rig.state.phase)
        assertFalse("the card's controls are off", rig.state.controlsEnabled)
        rig.controller.play()
        rig.controller.toggle()
        assertFalse("play is refused while blocked", rig.player.isPlaying)

        rig.controller.setBlocked(false)
        rig.controller.play()
        assertTrue(rig.player.isPlaying)
    }

    @Test
    fun aRecordingOpenedWhileBlockedWaitsInsteadOfPlaying() {
        rig.controller.setBlocked(true)

        rig.controller.open("hymn.flac", "hymn.flac")

        assertEquals(PlayerPhase.PAUSED, rig.state.phase)
        assertFalse(rig.player.isPlaying)
    }

    @Test
    fun aRealRecordingBlocksItUntilItIsSaved() {
        val session = SessionRig(tmp.root)
        try {
            assertFalse(playbackBlockedBy(session.session.state.value))
            session.session.start(RecordingTarget())
            assertTrue("recording", playbackBlockedBy(session.session.state.value))
            session.session.pause()
            assertTrue("paused", playbackBlockedBy(session.session.state.value))
            session.session.stop()
            SessionTestKit.awaitPhase(session.session, RecordingStateUi.IDLE)
            assertFalse("saved: listening is free again", playbackBlockedBy(session.session.state.value))
        } finally {
            session.close()
        }
    }
}
