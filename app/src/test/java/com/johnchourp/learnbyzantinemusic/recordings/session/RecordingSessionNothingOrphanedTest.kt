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
 * Every recording ends in exactly one of three places — the user's folder, the pending list, or
 * deleted because the user said so — whatever happens next (ClickUp `869f5x26x`).
 *
 * The old screen kept one reference to "the" temp WAV: a failed save left it in the cache, and the
 * next recording overwrote the reference, orphaning the audio where no screen would ever show it.
 */
class RecordingSessionNothingOrphanedTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var rig: SessionRig

    @Before
    fun setUp() {
        rig = SessionRig(tmp.root)
    }

    @After
    fun tearDown() = rig.close()

    private fun recordAndStop(audio: ByteArray, expected: RecordingStateUi) {
        assertTrue(rig.session.start(RecordingTarget()))
        rig.record(audio)
        assertTrue(rig.session.stop())
        SessionTestKit.awaitPhase(rig.session, expected)
    }

    @Test
    fun aRecordingKeptInTheAppIsStillListedAfterTheNextOneIsSaved() {
        val first = SessionTestKit.pcm(8_000, seed = 3)
        rig.sink = { null } // the folder's permission was lost
        recordAndStop(first, RecordingStateUi.ERROR)
        assertEquals(RecordingEvent.KeptInApp(SessionRig.baseName), rig.session.state.value.lastEvent)
        SessionTestKit.waitUntil("the kept recording is listed") { rig.session.pendingState.value.recordings.size == 1 }
        val kept = rig.session.pendingState.value.recordings.single()

        rig.sink = { rig.folder } // access is back
        recordAndStop(SessionTestKit.pcm(6_000, seed = 4), RecordingStateUi.IDLE)

        val stillKept = rig.pending.list().single()
        assertEquals("the first is still there, still listed", kept.id, stillKept.id)
        assertArrayEquals(SessionTestKit.finishedWav(File(tmp.root, "expected.wav"), first), stillKept.wav.readBytes())
        assertEquals(listOf("${SessionRig.baseName}.flac"), rig.folder.dir.list()!!.toList())
        assertEquals("no capture file left anywhere", emptyList<File>(), rig.captureFiles())
    }

    @Test
    fun aStartThatFailsLeavesNoFileBehind() {
        rig.microphoneFails = true

        assertFalse(rig.session.start(RecordingTarget()))

        assertEquals(RecordingStateUi.ERROR, rig.session.state.value.phase)
        assertEquals(RecordingEvent.StartFailed, rig.session.state.value.lastEvent)
        assertEquals(emptyList<String>(), rig.captureDir.list().orEmpty().toList())
    }

    @Test
    fun discardIsTheOneWayARecordingInProgressIsThrownAway() {
        assertTrue(rig.session.start(RecordingTarget()))
        rig.record(SessionTestKit.pcm(4_000))

        assertTrue(rig.session.discard())

        assertEquals(RecordingStateUi.IDLE, rig.session.state.value.phase)
        assertTrue("the microphone is given back", rig.microphone.released)
        SessionTestKit.waitUntil("the discarded audio is deleted") { rig.captureDir.list().orEmpty().isEmpty() }
        assertTrue(rig.pending.list().isEmpty())
        assertTrue("nothing reached the folder", rig.folder.created.isEmpty())
    }

    @Test
    fun aKeptRecordingSavedFromTheListLandsInTheFolderAndLeavesTheList() {
        val audio = SessionTestKit.pcm(7_000)
        rig.sink = { null }
        recordAndStop(audio, RecordingStateUi.ERROR)
        SessionTestKit.waitUntil("listed") { rig.session.pendingState.value.recordings.size == 1 }
        val kept = rig.session.pendingState.value.recordings.single()

        rig.sink = { rig.folder }
        rig.selectedFormat = RecordingFormatOption.WAV // the format selected *now* applies
        assertTrue(rig.session.savePending(kept))

        SessionTestKit.waitUntil("the list empties") { rig.session.pendingState.value.recordings.isEmpty() }
        SessionTestKit.awaitPhase(rig.session, RecordingStateUi.IDLE) // the error it was kept with is resolved
        assertArrayEquals(
            SessionTestKit.finishedWav(File(tmp.root, "expected.wav"), audio),
            File(rig.folder.dir, "${SessionRig.baseName}.wav").readBytes(),
        )
        assertEquals(listOf("${SessionRig.baseName}.wav"), rig.folder.registered.toList())
        SessionTestKit.waitUntil("the toast event") { rig.events.size == 2 }
        assertEquals(RecordingEvent.Saved("${SessionRig.baseName}.wav", null), rig.events.last())
    }

    @Test
    fun aKeptRecordingIsDeletedOnlyWhenTheUserSaysSo() {
        rig.sink = { null }
        recordAndStop(SessionTestKit.pcm(3_000), RecordingStateUi.ERROR)
        SessionTestKit.waitUntil("listed") { rig.session.pendingState.value.recordings.size == 1 }
        val kept = rig.session.pendingState.value.recordings.single()

        assertTrue(rig.session.deletePending(kept))

        SessionTestKit.waitUntil("the list empties") { rig.session.pendingState.value.recordings.isEmpty() }
        assertEquals(emptyList<String>(), rig.pendingDir.list()!!.toList())
    }
}
