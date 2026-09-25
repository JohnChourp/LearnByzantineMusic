package com.johnchourp.learnbyzantinemusic.recordings.session

import com.johnchourp.learnbyzantinemusic.recordings.RecordingFormatOption
import com.johnchourp.learnbyzantinemusic.recordings.RecordingStateUi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The buttons of the «Ηχογράφηση…» notification (ClickUp `869f5x273`): each does exactly one thing,
 * and «Στάση» stops **and saves** — a tap in the notification shade, often made without looking,
 * must never cost a recording. There is no discard to map to: [RecordingControls] has none.
 */
class RecordingNotificationActionTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** Records what each button asked for. */
    private class RecordedControls : RecordingControls {
        val calls = mutableListOf<String>()
        override val state = MutableStateFlow(RecordingSessionState())
        override fun pause() {
            calls += "pause"
        }

        override fun resume() {
            calls += "resume"
        }

        override fun stop(): Boolean {
            calls += "stop"
            return true
        }
    }

    @Test
    fun eachButtonDoesExactlyOneThing() {
        val expected = mapOf(
            RecordingNotificationAction.PAUSE to listOf("pause"),
            RecordingNotificationAction.RESUME to listOf("resume"),
            RecordingNotificationAction.STOP to listOf("stop"),
        )
        RecordingNotificationAction.entries.forEach { action ->
            val controls = RecordedControls()
            action.applyTo(controls)
            assertEquals("$action", expected.getValue(action), controls.calls)
        }
    }

    @Test
    fun stopFromTheNotificationSavesTheRecordingWithEveryByte() {
        val rig = SessionRig(tmp.root)
        try {
            val audio = SessionTestKit.pcm(9_000)
            rig.session.start(RecordingTarget())
            rig.record(audio)

            RecordingNotificationAction.STOP.applyTo(rig.session)

            SessionTestKit.awaitPhase(rig.session, RecordingStateUi.IDLE)
            val name = "${SessionRig.baseName}.flac"
            assertEquals(RecordingEvent.Saved(name, null), rig.session.state.value.lastEvent)
            assertArrayEquals(
                FakeTranscoder.encoded(SessionTestKit.finishedWav(File(tmp.root, "expected.wav"), audio), RecordingFormatOption.FLAC),
                File(rig.folder.dir, name).readBytes(),
            )
        } finally {
            rig.close()
        }
    }

    @Test
    fun theNotificationHasNoButtonThatThrowsARecordingAway() {
        // A new entry here is a new thing a blind tap can do: it has to be added to this list on purpose.
        assertEquals(listOf("PAUSE", "RESUME", "STOP"), RecordingNotificationAction.entries.map { it.name })
    }

    @Test
    fun theIntentActionsRoundTripAndNothingElseMatches() {
        RecordingNotificationAction.entries.forEach {
            assertEquals(it, RecordingNotificationAction.fromIntentAction(it.intentAction))
        }
        assertNull("the start from the screen carries no action", RecordingNotificationAction.fromIntentAction(null))
        assertNull(RecordingNotificationAction.fromIntentAction(""))
        assertNull(RecordingNotificationAction.fromIntentAction("com.johnchourp.learnbyzantinemusic.recordings.action.DISCARD"))
    }

    @Test
    fun theButtonsFollowThePhase() {
        assertEquals(
            mapOf(
                RecordingStateUi.IDLE to emptyList(),
                RecordingStateUi.RECORDING to listOf(RecordingNotificationAction.PAUSE, RecordingNotificationAction.STOP),
                RecordingStateUi.PAUSED to listOf(RecordingNotificationAction.RESUME, RecordingNotificationAction.STOP),
                RecordingStateUi.SAVING to emptyList(),
                RecordingStateUi.ERROR to emptyList(),
            ),
            RecordingStateUi.entries.associateWith { RecordingNotificationAction.buttonsFor(it) },
        )
    }
}
