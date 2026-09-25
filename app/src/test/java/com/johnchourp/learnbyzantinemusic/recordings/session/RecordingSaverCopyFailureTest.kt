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
 * A copy into the user's folder that fails — cut short, or not the size it should be — deletes the
 * partial file and keeps the audio (ClickUp `869f5x26x`).
 *
 * Before, the copy's `finally` deleted the WAV whatever happened, leaving a partial file posing as
 * the recording and nothing to recover it from. The size rule is deliberate and pinned both ways:
 * a folder that reports a different size (0 included) holds a partial file; a folder that cannot
 * report a size at all is trusted on the byte count its stream accepted.
 */
class RecordingSaverCopyFailureTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val audio = SessionTestKit.pcm(20_000)
    private val meta = RecordingMeta(RecordingTarget(label = "Ήχος Β΄"), baseName = "recording_20260925_183000")
    private lateinit var wav: File
    private lateinit var finished: ByteArray
    private lateinit var pending: PendingRecordings
    private lateinit var folder: FakeSink
    private lateinit var saver: RecordingSaver

    @Before
    fun setUp() {
        finished = SessionTestKit.finishedWav(File(tmp.root, "expected.wav"), audio)
        wav = SessionTestKit.captureFile(File(tmp.root, "recordings_capture/capture_1.wav"), audio)
        pending = PendingRecordings(File(tmp.root, "recordings_pending"))
        folder = FakeSink(File(tmp.root, "folder"))
        saver = RecordingSaver(FakeTranscoder(), pending, tmp.newFolder("cache"))
    }

    private fun assertKeptIntact(outcome: SaveOutcome) {
        assertTrue("expected KeptPending, got $outcome", outcome is SaveOutcome.KeptPending)
        val kept = pending.list().single()
        assertArrayEquals("every byte of the recording is kept", finished, kept.wav.readBytes())
        assertEquals("and what it was for", meta, kept.meta)
    }

    @Test
    fun aCopyCutShortDeletesThePartialFileAndKeepsTheAudio() {
        folder.failAfterBytes = 1_000

        val outcome = saver.save(wav, meta, RecordingFormatOption.FLAC, folder)

        assertEquals("the partial file is deleted", emptyList<String>(), folder.dir.list()!!.toList())
        assertKeptIntact(outcome)
    }

    @Test
    fun aSizeTheFolderReportsDifferentlyIsAFailedCopy() {
        folder.reportSize = { actual -> actual - 1 }

        val outcome = saver.save(wav, meta, RecordingFormatOption.WAV, folder)

        assertEquals(emptyList<String>(), folder.dir.list()!!.toList())
        assertKeptIntact(outcome)
    }

    @Test
    fun aFolderReportingZeroBytesHoldsAPartialFile() {
        folder.reportSize = { 0L }

        val outcome = saver.save(wav, meta, RecordingFormatOption.FLAC, folder)

        assertEquals(emptyList<String>(), folder.dir.list()!!.toList())
        assertKeptIntact(outcome)
    }

    @Test
    fun aSizeQueryThatFailsIsAFailedCopyNotASuccess() {
        folder.sizeQueryThrows = true

        val outcome = saver.save(wav, meta, RecordingFormatOption.FLAC, folder)

        assertEquals(emptyList<String>(), folder.dir.list()!!.toList())
        assertKeptIntact(outcome)
    }

    @Test
    fun aFolderThatCannotReportASizeIsTrustedOnTheByteCount() {
        folder.reportSize = { null }

        val outcome = saver.save(wav, meta, RecordingFormatOption.WAV, folder)

        assertTrue("expected Saved, got $outcome", outcome is SaveOutcome.Saved)
        assertArrayEquals(finished, File(folder.dir, "recording_20260925_183000.wav").readBytes())
        assertFalse(wav.exists())
        assertTrue(pending.list().isEmpty())
    }
}
