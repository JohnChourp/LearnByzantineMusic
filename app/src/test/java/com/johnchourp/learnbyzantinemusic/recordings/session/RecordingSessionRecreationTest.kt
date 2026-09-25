package com.johnchourp.learnbyzantinemusic.recordings.session

import com.johnchourp.learnbyzantinemusic.recordings.RecordingFormatOption
import com.johnchourp.learnbyzantinemusic.recordings.RecordingStateUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
 * The first loss path of ClickUp `869f5x26x`: re-creating the «Ηχογραφήσεις» screen — the device's
 * dark theme switching on a schedule, split-screen, a fold, a language or font-size change — threw
 * the recording away, because the screen owned the capture and its `onDestroy` deleted the WAV.
 *
 * Here a "screen" is exactly what `RecordingsActivity` now is to the session: something that watches
 * its state. Destroying one and creating another must neither stop the capture nor delete anything,
 * and the new one must see the recording as it really is, not IDLE.
 */
class RecordingSessionRecreationTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var rig: SessionRig
    private val screens = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Before
    fun setUp() {
        rig = SessionRig(tmp.root)
    }

    @After
    fun tearDown() = rig.close()

    private fun attachScreen(seen: MutableList<RecordingSessionState>): Job =
        screens.launch { rig.session.state.collect { seen += it } }

    @Test
    fun aScreenGoingAwayNeitherStopsNorDeletesTheRecording() = runBlocking {
        val before = SessionTestKit.pcm(12_000, seed = 1)
        val after = SessionTestKit.pcm(9_000, seed = 2)
        val firstScreen = attachScreen(Collections.synchronizedList(mutableListOf()))
        assertTrue(rig.session.start(RecordingTarget()))
        rig.record(before)
        val capture = rig.liveCapture()

        // The theme switches: the screen is destroyed…
        firstScreen.cancelAndJoin()

        assertTrue("the capture file survives the screen", capture.exists())
        assertFalse("the microphone is not stopped", rig.microphone.stopped)
        rig.record(after) // …and capture goes on with no screen at all.

        // …and a new screen is created.
        val seenByNew = Collections.synchronizedList(mutableListOf<RecordingSessionState>())
        val newScreen = attachScreen(seenByNew)
        SessionTestKit.waitUntil("the new screen's first look") { seenByNew.isNotEmpty() }
        assertEquals("it sees the recording as it is — not IDLE", RecordingStateUi.RECORDING, seenByNew.first().phase)

        rig.session.stop()
        SessionTestKit.awaitPhase(rig.session, RecordingStateUi.IDLE)
        newScreen.cancelAndJoin()

        val finished = SessionTestKit.finishedWav(File(tmp.root, "expected.wav"), before + after)
        assertArrayEquals(
            "everything recorded before and after the re-creation, in one file",
            FakeTranscoder.encoded(finished, RecordingFormatOption.FLAC),
            File(rig.folder.dir, "${SessionRig.baseName}.flac").readBytes(),
        )
    }

    @Test
    fun aPausedRecordingIsStillPausedWithItsTimeOnTheNextScreen() {
        rig.clock.set(1_000_000L)
        rig.session.start(RecordingTarget(label = "Ήχος Α΄"))
        rig.clock.addAndGet(42_000L)
        rig.session.pause()

        // A new screen reads the session: paused, 42 s recorded, still for the same hymn.
        val seen = rig.session.state.value
        assertEquals(RecordingStateUi.PAUSED, seen.phase)
        assertEquals(42_000L, seen.elapsedBeforeMs)
        assertEquals("Ήχος Α΄", seen.target?.label)

        rig.clock.set(2_000_000L)
        rig.session.resume()
        assertEquals("the timer continues from 42 s, not from zero", 52_000L, rig.session.state.value.elapsedAt(2_010_000L))
    }
}
