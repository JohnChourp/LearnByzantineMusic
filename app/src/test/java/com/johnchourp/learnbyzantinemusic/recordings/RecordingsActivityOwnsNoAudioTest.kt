package com.johnchourp.learnbyzantinemusic.recordings

import com.johnchourp.learnbyzantinemusic.docs.KotlinSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * ClickUp `869f5x26x`, the half no session test can see: the root cause lived in the screen.
 * `RecordingsActivity` owned the capture, its `onDestroy` stopped it and deleted the WAV, and its
 * `onCreate` forced the state to IDLE — so any re-creation lost the recording and hid that it had.
 *
 * An Activity cannot run in a JVM test, so this checks how the screen is *built*, like
 * `ShareUsesCacheCopyOnlyTest`: comments are stripped first with [KotlinSource] (a comment explaining
 * the old bug must not trip it), each check reads only the function it is about, and
 * [theChecksCatchTheOldScreen] proves every check fails on the code this replaced.
 */
class RecordingsActivityOwnsNoAudioTest {

    private val code: String by lazy {
        val file = File(KotlinSource.mainRoot, "com/johnchourp/learnbyzantinemusic/recordings/RecordingsActivity.kt")
        KotlinSource.withoutComments(file.readText())
    }

    /** From the declaration to the next class-level member; null when the function does not exist. */
    private fun bodyOf(text: String, declaration: String): String? {
        val start = text.indexOf(declaration)
        if (start < 0) return null
        val rest = text.substring(start)
        val next = Regex("""\n {4}(private |override |protected |internal )*(fun|val|var|companion|class|enum|object) """)
            .find(rest, startIndex = 1)
        return if (next != null) rest.substring(0, next.range.first) else rest
    }

    private val machinery = listOf("AudioRecord", "FileOutputStream", "RandomAccessFile", "createTempFile", "cacheDir", ".delete(")

    private fun captureMachineryIn(text: String) = machinery.filter { it in text }

    /**
     * What a lifecycle callback must never do to a recording: reach the session, or clean up or delete
     * anything. Releasing the in-app player in `onStop` is allowed — that is the player's rule
     * (ClickUp `869f5x268`) — so the check names the recording's machinery, not the word "stop".
     */
    private fun destructionIn(text: String): List<String> =
        listOf("override fun onDestroy(", "override fun onStop(", "override fun onPause(").flatMap { callback ->
            val body = bodyOf(text, callback) ?: return@flatMap emptyList()
            listOf("session.", "RecordingSessions", "discard", "delete", "cleanup", "stopCapture")
                .filter { body.contains(it) }
                .map { "$callback … $it" }
        }

    private fun forcesIdleOnCreate(text: String): Boolean =
        bodyOf(text, "override fun onCreate(")?.contains("setRecordingState(RecordingStateUi.IDLE)") == true

    @Test
    fun theSourceIsReallyRead() {
        // A slice that missed would make every negative check below pass by reading nothing.
        assertTrue(code.contains("class RecordingsActivity"))
        assertTrue("onCreate slice too short", (bodyOf(code, "override fun onCreate(")?.length ?: 0) > 1_000)
        assertTrue(bodyOf(code, "private fun handleBackRequested(")!!.contains("AlertDialog.Builder"))
    }

    @Test
    fun theScreenHoldsNoCaptureMachinery() {
        assertEquals("the capture belongs to the session", emptyList<String>(), captureMachineryIn(code))
    }

    @Test
    fun goingAwayStopsNothingAndDeletesNothing() {
        assertEquals(emptyList<String>(), destructionIn(code))
    }

    @Test
    fun aNewScreenTakesTheRecordingStateFromTheSession() {
        assertFalse("a forced IDLE hid a running recording", forcesIdleOnCreate(code))
        assertTrue(bodyOf(code, "override fun onCreate(")!!.contains("session.state.collect"))
    }

    @Test
    fun onlyTheBackDialogDiscardsARecording() {
        assertEquals(1, Regex("""session\.discard\(\)""").findAll(code).count())
        assertTrue(bodyOf(code, "private fun handleBackRequested(")!!.contains("session.discard()"))
    }

    @Test
    fun theChecksCatchTheOldScreen() {
        // Negative control: the shape this replaced (abridged from ad68f8d) fails every check above.
        val old = """
            class RecordingsActivity : BaseActivity() {
                private var tempOutputStream: FileOutputStream? = null

                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    restoreSavedFolder()
                    viewModel.setRecordingState(RecordingStateUi.IDLE)
                }

                override fun onDestroy() {
                    super.onDestroy()
                    stopCaptureInfrastructure()
                    cleanupTempFiles()
                }

                private fun cleanupTempFiles() {
                    tempWavFile?.delete()
                }
            }
        """.trimIndent()
        assertEquals(listOf("FileOutputStream", ".delete("), captureMachineryIn(old))
        assertEquals(
            listOf("override fun onDestroy( … cleanup", "override fun onDestroy( … stopCapture"),
            destructionIn(old),
        )
        // The same shape, reaching the session instead: the discard G2 removed from onDestroy.
        assertEquals(
            listOf("override fun onStop( … session.", "override fun onStop( … discard"),
            destructionIn("    override fun onStop() {\n        super.onStop()\n        session.discard()\n    }\n"),
        )
        // And what the in-app player legitimately does there is not mistaken for it.
        assertEquals(
            emptyList<String>(),
            destructionIn("    override fun onStop() {\n        super.onStop()\n        player.onScreenStopped()\n    }\n"),
        )
        assertTrue(forcesIdleOnCreate(old))
    }
}
