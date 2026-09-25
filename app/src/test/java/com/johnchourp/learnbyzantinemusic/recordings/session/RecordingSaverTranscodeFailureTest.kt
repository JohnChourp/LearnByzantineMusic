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
 * The promise in `RecordingsActivity`'s KDoc — «a failed transcode … produces no file» — made true
 * (ClickUp `869f5x26x`).
 *
 * It was false: the target was created in the user's folder *before* FFmpeg ran, so a failed
 * conversion left an empty 0-byte file there, and the good WAV orphaned in the cache. Now a failed
 * conversion produces nothing in the chosen format, and the WAV itself is saved instead — same
 * folder, same base name — with an outcome that says so, for the message the user sees.
 */
class RecordingSaverTranscodeFailureTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val audio = SessionTestKit.pcm(8_820)
    private val meta = RecordingMeta(
        target = RecordingTarget(listOf("Αναστασιματάριο", "Ήχος Α΄"), "03 ", "Ήχος Α΄ — Κύριε ἐκέκραξα"),
        baseName = "recording_20260925_183000",
    )
    private lateinit var wav: File
    private lateinit var workDir: File
    private lateinit var pending: PendingRecordings
    private lateinit var folder: FakeSink
    private lateinit var transcoder: FakeTranscoder
    private lateinit var saver: RecordingSaver

    @Before
    fun setUp() {
        workDir = tmp.newFolder("cache")
        pending = PendingRecordings(File(tmp.root, "recordings_pending"))
        folder = FakeSink(File(tmp.root, "folder"))
        transcoder = FakeTranscoder(succeed = false)
        saver = RecordingSaver(transcoder, pending, workDir)
        wav = SessionTestKit.captureFile(File(tmp.root, "recordings_capture/capture_1.wav"), audio)
        RecordingFiles.writeMeta(wav, meta)
    }

    @Test
    fun aFailedTranscodeLeavesNoFileInTheChosenFormat() {
        saver.save(wav, meta, RecordingFormatOption.FLAC, folder)

        assertEquals(
            "no .flac may be created — not even an empty one",
            emptyList<String>(),
            folder.created.filter { it.endsWith(".flac") },
        )
        assertEquals(listOf("recording_20260925_183000.wav"), folder.dir.list()!!.sorted())
    }

    @Test
    fun theWavIsSavedInsteadUnderTheSameNameAndTheOutcomeSaysSo() {
        val outcome = saver.save(wav, meta, RecordingFormatOption.FLAC, folder)

        assertTrue("expected Saved, got $outcome", outcome is SaveOutcome.Saved)
        outcome as SaveOutcome.Saved
        assertEquals(RecordingFormatOption.WAV, outcome.format)
        assertEquals("the message names the conversion that failed", RecordingFormatOption.FLAC, outcome.fellBackFrom)
        assertArrayEquals(
            "every byte of the recording, with a finished header",
            SessionTestKit.finishedWav(File(tmp.root, "expected.wav"), audio),
            File(folder.dir, "recording_20260925_183000.wav").readBytes(),
        )
    }

    @Test
    fun theTemporaryAudioGoesOnlyOnceTheWavIsSavedAndVerified() {
        saver.save(wav, meta, RecordingFormatOption.FLAC, folder)

        assertFalse("the capture WAV is gone after a verified save", wav.exists())
        assertFalse("and so is its metadata", RecordingFiles.sidecarOf(wav).exists())
        assertEquals("no converted leftovers", emptyList<String>(), workDir.list()!!.toList())
        assertTrue("nothing needed keeping", pending.list().isEmpty())
    }

    @Test
    fun anEmptyOutputIsAFailedTranscodeWhateverTheTranscoderSays() {
        transcoder.succeed = true
        transcoder.writeOutput = false

        val outcome = saver.save(wav, meta, RecordingFormatOption.MP3, folder)

        assertEquals(RecordingFormatOption.MP3, (outcome as SaveOutcome.Saved).fellBackFrom)
        assertEquals(listOf("recording_20260925_183000.wav"), folder.created.toList())
    }

    @Test
    fun aTranscoderThatThrowsIsAFailedTranscodeToo() {
        val throwing = RecordingSaver({ _, _, _ -> error("ffmpeg crashed (fake)") }, pending, workDir)

        val outcome = throwing.save(wav, meta, RecordingFormatOption.OPUS, folder)

        assertEquals(RecordingFormatOption.OPUS, (outcome as SaveOutcome.Saved).fellBackFrom)
        assertEquals(listOf("recording_20260925_183000.wav"), folder.created.toList())
    }
}
