package com.johnchourp.learnbyzantinemusic.recordings.session

import com.johnchourp.learnbyzantinemusic.recordings.RecordingFormatOption
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
 * When the user's folder cannot take a recording at all — permission lost, folder deleted — the WAV
 * moves to app-private `recordings_pending/`, never the cache, and stays there, listed on the
 * «Ηχογραφήσεις» page, until the user saves or deletes it (ClickUp `869f5x26x`).
 *
 * What it was for travels with it: a hymn's recording, saved later, still goes to that hymn's
 * folder under the name it was given when it stopped.
 */
class RecordingSaverPendingFallbackTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val audio = SessionTestKit.pcm(12_000)
    private val hymn = RecordingTarget(listOf("Αναστασιματάριο", "Ήχος Γ΄"), "05 ", "Ήχος Γ΄ — Εὐφραινέσθω")
    private val meta = RecordingMeta(hymn, baseName = "recording_20260925_183000")
    private lateinit var wav: File
    private lateinit var finished: ByteArray
    private lateinit var pendingDir: File
    private lateinit var pending: PendingRecordings
    private lateinit var folder: FakeSink
    private lateinit var transcoder: FakeTranscoder
    private lateinit var saver: RecordingSaver

    @Before
    fun setUp() {
        finished = SessionTestKit.finishedWav(File(tmp.root, "expected.wav"), audio)
        wav = SessionTestKit.captureFile(File(tmp.root, "recordings_capture/capture_1.wav"), audio)
        pendingDir = File(tmp.root, "recordings_pending")
        pending = PendingRecordings(pendingDir)
        folder = FakeSink(File(tmp.root, "folder"))
        transcoder = FakeTranscoder()
        saver = RecordingSaver(transcoder, pending, tmp.newFolder("cache"))
    }

    @Test
    fun aFolderThatIsGoneKeepsTheRecordingInsideTheApp() {
        val outcome = saver.save(wav, meta, RecordingFormatOption.FLAC, sink = null)

        assertTrue("expected KeptPending, got $outcome", outcome is SaveOutcome.KeptPending)
        val kept = pending.list().single()
        assertEquals("app-private, not the cache", pendingDir.canonicalFile, kept.wav.parentFile!!.canonicalFile)
        assertArrayEquals(finished, kept.wav.readBytes())
        assertEquals("the hymn and the name travel with it", meta, kept.meta)
        assertFalse("moved, not copied", wav.exists())
    }

    @Test
    fun aFolderThatRefusesTheFileKeepsTheRecordingToo() {
        folder.refuseCreate = true

        val outcome = saver.save(wav, meta, RecordingFormatOption.FLAC, folder)

        assertTrue(outcome is SaveOutcome.KeptPending)
        assertArrayEquals(finished, pending.list().single().wav.readBytes())
        assertEquals(emptyList<String>(), folder.dir.list().orEmpty().toList())
    }

    @Test
    fun aFailedTranscodeWithTheFolderGoneStillKeepsTheWav() {
        transcoder.succeed = false

        val outcome = saver.save(wav, meta, RecordingFormatOption.FLAC, sink = null)

        assertEquals(RecordingFormatOption.FLAC, (outcome as SaveOutcome.KeptPending).fellBackFrom)
        assertArrayEquals(finished, pending.list().single().wav.readBytes())
    }

    @Test
    fun savingFromThePendingListLaterPutsItInTheFolderAndEmptiesTheList() {
        saver.save(wav, meta, RecordingFormatOption.FLAC, sink = null)
        val kept = pending.list().single()

        val outcome = saver.save(kept.wav, kept.meta, RecordingFormatOption.FLAC, folder)

        assertTrue("expected Saved, got $outcome", outcome is SaveOutcome.Saved)
        assertEquals("the name it got when it stopped", listOf("recording_20260925_183000.flac"), folder.created.toList())
        assertArrayEquals(
            FakeTranscoder.encoded(finished, RecordingFormatOption.FLAC),
            File(folder.dir, "recording_20260925_183000.flac").readBytes(),
        )
        assertTrue("saved, so no longer pending", pending.list().isEmpty())
        assertEquals("sidecar gone too", emptyList<String>(), pendingDir.list()!!.toList())
    }

    @Test
    fun aRetryThatFailsAgainLeavesOneEntryNotTwo() {
        saver.save(wav, meta, RecordingFormatOption.FLAC, sink = null)
        val kept = pending.list().single()

        val outcome = saver.save(kept.wav, kept.meta, RecordingFormatOption.FLAC, sink = null)

        assertTrue(outcome is SaveOutcome.KeptPending)
        val again = pending.list().single()
        assertEquals(kept.id, again.id)
        assertArrayEquals(finished, again.wav.readBytes())
    }

    @Test
    fun nothingThereAtAllIsAFailureThatInventsNoFile() {
        wav.delete()

        val outcome = saver.save(wav, meta, RecordingFormatOption.FLAC, folder)

        assertEquals(SaveOutcome.Failed, outcome)
        assertFalse("no empty WAV is conjured up", wav.exists())
        assertTrue(folder.created.isEmpty())
    }
}
