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
import java.util.Collections

/**
 * The service must never be able to crash or stop a recording (ClickUp `869f5x273`).
 *
 * Putting a microphone service in the foreground can be refused for reasons no test controls: the app
 * reached the background in between (`ForegroundServiceStartNotAllowedException`), a permission
 * (`SecurityException`), an OEM's own exception. Each is simulated here by a `startForeground` that
 * throws, against a real recording: the service logs, steps aside, and the recording goes on — every
 * byte captured after the refusal ends up in the saved file.
 */
class RecordingServiceFallbackTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var rig: SessionRig
    private val logged: MutableList<Pair<String, Throwable>> = Collections.synchronizedList(mutableListOf())
    private var left = 0

    @Before
    fun setUp() {
        rig = SessionRig(tmp.root)
    }

    @After
    fun tearDown() = rig.close()

    private fun service(
        promote: (RecordingSessionState) -> Unit = {},
        refresh: (RecordingSessionState) -> Unit = {},
    ) = RecordingServiceCore(
        session = rig.session,
        promote = promote,
        refresh = refresh,
        leave = { left++ },
        log = { message, failure -> logged += message to failure },
    )

    @Test
    fun aRefusedForegroundLeavesTheRecordingRunningAndWhole() {
        val refusals = listOf(
            IllegalStateException("startForeground not allowed: app is in the background (fake)"),
            SecurityException("FOREGROUND_SERVICE_MICROPHONE missing (fake)"),
            RuntimeException("an OEM's own refusal (fake)"),
        )
        assertTrue(rig.session.start(RecordingTarget()))
        val captured = mutableListOf<ByteArray>()

        refusals.forEachIndexed { index, refusal ->
            val service = service(promote = { throw refusal })

            assertFalse("the service steps aside", service.onStartCommand(null))
            assertFalse(service.inForeground)
            assertEquals("the recording does not", RecordingStateUi.RECORDING, rig.session.state.value.phase)
            assertFalse("the microphone keeps listening", rig.microphone.stopped)

            val part = SessionTestKit.pcm(3_000, seed = index)
            rig.record(part) // capture goes on after each refusal
            captured += part
        }

        assertEquals("each refusal ends in the service leaving", refusals.size, left)
        assertEquals("and each is logged", refusals, logged.map { it.second })

        rig.session.stop()
        SessionTestKit.awaitPhase(rig.session, RecordingStateUi.IDLE)
        val audio = captured.reduce { all, part -> all + part }
        assertArrayEquals(
            FakeTranscoder.encoded(SessionTestKit.finishedWav(File(tmp.root, "expected.wav"), audio), RecordingFormatOption.FLAC),
            File(rig.folder.dir, "${SessionRig.baseName}.flac").readBytes(),
        )
    }

    @Test
    fun theScreenFailingToStartTheServiceIsHarmlessToo() {
        assertTrue(rig.session.start(RecordingTarget()))

        val started = ForegroundGuard.attempt("start the recording service", { m, t -> logged += m to t }) {
            throw IllegalStateException("Not allowed to start service Intent: app is in background (fake)")
        }

        assertFalse(started)
        assertEquals(1, logged.size)
        assertEquals(RecordingStateUi.RECORDING, rig.session.state.value.phase)
        rig.record(SessionTestKit.pcm(2_000))
    }

    @Test
    fun aNotificationThatCannotBeUpdatedChangesNothingElse() {
        assertTrue(rig.session.start(RecordingTarget()))
        val service = service(refresh = { throw SecurityException("POST_NOTIFICATIONS revoked (fake)") })
        assertTrue(service.onStartCommand(null))

        rig.session.pause()
        service.onState(rig.session.state.value)

        assertTrue("still in the foreground", service.inForeground)
        assertEquals(0, left)
        assertEquals(1, logged.size)
        assertEquals(RecordingStateUi.PAUSED, rig.session.state.value.phase)
    }

    @Test
    fun evenALogThatThrowsCannotReachTheRecording() {
        val result = ForegroundGuard.attempt("anything", { _, _ -> error("logging broke (fake)") }) {
            throw IllegalStateException("refused (fake)")
        }
        assertFalse(result)
    }
}
