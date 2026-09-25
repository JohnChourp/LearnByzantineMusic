package com.johnchourp.learnbyzantinemusic.recordings.session

import com.johnchourp.learnbyzantinemusic.recordings.RecordingFormatOption
import com.johnchourp.learnbyzantinemusic.recordings.RecordingStateUi
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * However the foreground service sees a recording end, it ends **saved** (ClickUp `869f5x273`):
 * swiping the app away from recents, or «Στάση» in the notification. And the service holds the
 * process in the foreground through the save, leaving only once the session is idle — leaving
 * earlier would let a backgrounded process be killed with the file half-written.
 */
class RecordingServiceEndsBySavingTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var rig: SessionRig
    private var promoted = 0
    private var left = 0
    private val failures = mutableListOf<Throwable>()

    @Before
    fun setUp() {
        rig = SessionRig(tmp.root)
    }

    @After
    fun tearDown() {
        rig.close()
        assertEquals("nothing should have failed", emptyList<Throwable>(), failures)
    }

    private fun service() = RecordingServiceCore(
        session = rig.session,
        promote = { promoted++ },
        refresh = {},
        leave = { left++ },
        log = { _, failure -> failures += failure },
    )

    private fun assertSaved(audio: ByteArray) {
        SessionTestKit.awaitPhase(rig.session, RecordingStateUi.IDLE)
        assertArrayEquals(
            FakeTranscoder.encoded(SessionTestKit.finishedWav(File(tmp.root, "expected.wav"), audio), RecordingFormatOption.FLAC),
            File(rig.folder.dir, "${SessionRig.baseName}.flac").readBytes(),
        )
        assertTrue("nothing was left behind or kept aside", rig.captureFiles().isEmpty() && rig.pending.list().isEmpty())
    }

    @Test
    fun swipingTheAppAwayStopsAndSaves() {
        val audio = SessionTestKit.pcm(8_000)
        rig.session.start(RecordingTarget())
        rig.record(audio)
        val service = service()
        assertTrue(service.onStartCommand(null))

        service.onTaskRemoved()

        assertSaved(audio)
    }

    @Test
    fun stopInTheNotificationSaves() {
        val audio = SessionTestKit.pcm(6_000)
        rig.session.start(RecordingTarget())
        rig.record(audio)
        val service = service()
        assertTrue(service.onStartCommand(null))

        assertTrue("still needed while it saves", service.onStartCommand(RecordingNotificationAction.STOP.intentAction))

        assertSaved(audio)
    }

    @Test
    fun theServiceStaysThroughTheSaveAndLeavesOnceIdle() {
        rig.session.start(RecordingTarget())
        val service = service()
        assertTrue(service.onStartCommand(null))

        service.onState(RecordingSessionState(phase = RecordingStateUi.PAUSED))
        service.onState(RecordingSessionState(phase = RecordingStateUi.SAVING))
        assertEquals("not while the file is being written", 0, left)
        assertTrue(service.inForeground)

        service.onState(RecordingSessionState(phase = RecordingStateUi.IDLE))
        assertEquals(1, left)
        assertFalse(service.inForeground)
    }

    @Test
    fun aStartWithNothingToKeepAliveLeavesAtOnce() {
        val service = service()

        assertFalse(service.onStartCommand(null))

        assertEquals("never promoted", 0, promoted)
        assertEquals(1, left)
    }

    @Test
    fun pauseAndResumeInTheNotificationReachTheRecording() {
        rig.session.start(RecordingTarget())
        val service = service()
        assertTrue(service.onStartCommand(null))

        service.onStartCommand(RecordingNotificationAction.PAUSE.intentAction)
        assertEquals(RecordingStateUi.PAUSED, rig.session.state.value.phase)
        service.onStartCommand(RecordingNotificationAction.RESUME.intentAction)
        assertEquals(RecordingStateUi.RECORDING, rig.session.state.value.phase)
        assertEquals("promoted once, not per button", 1, promoted)
    }
}
