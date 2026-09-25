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
import java.util.Collections

/**
 * The save order of ClickUp `869f5x26x`: transcode → create the file in the user's folder → copy →
 * verify the size → only then delete the temporary audio.
 *
 * The fake folder logs each step together with whether the capture WAV still existed at that
 * moment, so "nothing is deleted before the copy is verified" is observed, not inferred.
 */
class RecordingSaverOrderTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val log: MutableList<String> = Collections.synchronizedList(mutableListOf())
    private val audio = SessionTestKit.pcm(4_410)
    private val meta = RecordingMeta(baseName = "recording_20260925_183000")
    private lateinit var wav: File
    private lateinit var workDir: File
    private lateinit var folder: FakeSink
    private lateinit var transcoder: FakeTranscoder
    private lateinit var saver: RecordingSaver

    @Before
    fun setUp() {
        workDir = tmp.newFolder("cache")
        wav = SessionTestKit.captureFile(File(tmp.root, "recordings_capture/capture_1.wav"), audio)
        folder = FakeSink(File(tmp.root, "folder"), log, watch = wav)
        transcoder = FakeTranscoder(log)
        saver = RecordingSaver(transcoder, PendingRecordings(File(tmp.root, "recordings_pending")), workDir)
    }

    @Test
    fun theFolderSeesNothingUntilTheTranscodeHasSucceeded() {
        saver.save(wav, meta, RecordingFormatOption.FLAC, folder)

        assertEquals(
            listOf(
                "transcode:FLAC",
                "create:recording_20260925_183000.flac wav=present",
                "write:recording_20260925_183000.flac wav=present",
                "verify:recording_20260925_183000.flac wav=present",
            ),
            log.toList(),
        )
    }

    @Test
    fun theTemporaryAudioOutlivesEveryStepAndGoesOnlyAfterVerification() {
        val outcome = saver.save(wav, meta, RecordingFormatOption.FLAC, folder)

        assertTrue(outcome is SaveOutcome.Saved)
        assertFalse("no step may run after the WAV is gone", log.any { it.endsWith("wav=gone") })
        assertFalse("and once verified, it is gone", wav.exists())
    }

    @Test
    fun theConvertedCopyIsWhatReachesTheFolderAndLeavesNoTraceBehind() {
        val finished = SessionTestKit.finishedWav(File(tmp.root, "expected.wav"), audio)

        val outcome = saver.save(wav, meta, RecordingFormatOption.FLAC, folder) as SaveOutcome.Saved

        assertEquals(RecordingFormatOption.FLAC, outcome.format)
        assertEquals(null, outcome.fellBackFrom)
        assertArrayEquals(
            FakeTranscoder.encoded(finished, RecordingFormatOption.FLAC),
            File(folder.dir, "recording_20260925_183000.flac").readBytes(),
        )
        assertEquals("the converted temp file is removed", emptyList<String>(), workDir.list()!!.toList())
    }

    @Test
    fun aWavRecordingIsCopiedAsItIsWithoutTheTranscoder() {
        val outcome = saver.save(wav, meta, RecordingFormatOption.WAV, folder) as SaveOutcome.Saved

        assertTrue("WAV needs no conversion", transcoder.calls.isEmpty())
        assertEquals(null, outcome.fellBackFrom)
        assertArrayEquals(
            SessionTestKit.finishedWav(File(tmp.root, "expected.wav"), audio),
            File(folder.dir, "recording_20260925_183000.wav").readBytes(),
        )
    }
}
