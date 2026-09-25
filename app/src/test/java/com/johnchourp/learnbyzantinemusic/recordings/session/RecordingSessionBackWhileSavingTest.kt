package com.johnchourp.learnbyzantinemusic.recordings.session

import com.johnchourp.learnbyzantinemusic.recordings.RecordingStateUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * The second loss path of ClickUp `869f5x26x`: Back while «Αποθήκευση…» showed finished the screen,
 * whose `onDestroy` deleted the WAV while FFmpeg and the copy were still running — and the «saved»
 * toast and the «πρόσφατες» registration lived in the screen's own scope, so they died with it.
 *
 * Decided: the user may leave; the save belongs to the session and finishes on its own, registers,
 * and says how it ended. The transcoder here holds the save open until the test has left.
 */
class RecordingSessionBackWhileSavingTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var rig: SessionRig
    private val screens = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Before
    fun setUp() {
        rig = SessionRig(tmp.root, holdTranscode = true)
    }

    @After
    fun tearDown() = rig.close()

    private fun recordAndStop() {
        assertTrue(rig.session.start(RecordingTarget(listOf("Αναστασιματάριο"), null, "Ήχος Α΄")))
        rig.record(SessionTestKit.pcm(10_000))
        assertTrue(rig.session.stop())
        assertTrue("the save reached the transcoder", rig.transcoder.entered.await(5, TimeUnit.SECONDS))
    }

    @Test
    fun leavingWhileSavingLetsTheSaveFinishRegisterAndReport() = runBlocking {
        val screen = screens.launch { rig.session.state.collect { } }
        recordAndStop()
        assertEquals(
            "stop() returned while the conversion is still running: the save is not the screen's",
            RecordingStateUi.SAVING,
            rig.session.state.value.phase,
        )

        // Back: the screen closes while «Αποθήκευση…» is showing.
        screen.cancelAndJoin()
        rig.transcoder.release()

        SessionTestKit.awaitPhase(rig.session, RecordingStateUi.IDLE)
        val name = "${SessionRig.baseName}.flac"
        assertEquals("the file is in the folder", listOf(name), rig.folder.dir.list()!!.toList())
        assertEquals("«πρόσφατες» still learns about it", listOf(name), rig.folder.registered.toList())
        SessionTestKit.waitUntil("the toast event") { rig.events.isNotEmpty() }
        assertEquals("and the toast still says so", listOf(RecordingEvent.Saved(name, null)), rig.events.toList())
        assertEquals("the temporary audio went only after all that", emptyList<File>(), rig.captureFiles())
    }

    @Test
    fun nothingCanDeleteTheAudioWhileItIsBeingSaved() = runBlocking {
        recordAndStop()
        val capture = rig.captureFiles().single()

        assertFalse("«Απόρριψη» only applies to a recording still in progress", rig.session.discard())
        rig.session.recoverOrphans(legacyCacheDir = null).join()
        assertTrue("the sweep skips a recording being saved", capture.exists())
        assertTrue("it is not pending: it is being saved", rig.pending.list().isEmpty())

        rig.transcoder.release()
        SessionTestKit.awaitPhase(rig.session, RecordingStateUi.IDLE)
        assertEquals(listOf("${SessionRig.baseName}.flac"), rig.folder.dir.list()!!.toList())
    }

    @Test
    fun aNewRecordingCannotStartWhileTheLastOneIsSaving() {
        recordAndStop()

        assertFalse(rig.session.start(RecordingTarget()))
        assertEquals("no second capture file", 1, rig.captureFiles().size)

        rig.transcoder.release()
        SessionTestKit.awaitPhase(rig.session, RecordingStateUi.IDLE)
        assertTrue("and once it is saved, recording works again", rig.session.start(RecordingTarget()))
    }
}
