package com.johnchourp.learnbyzantinemusic.recordings.session

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
 * The sweep of ClickUp `869f5x26x`: nothing that holds audio accumulates unseen on the device.
 *
 * A capture file outlives its process only when that process died mid-recording or mid-save; at the
 * next start it is recovered — header rewritten from its length, hymn and name kept — into the
 * pending list, which shows it until the user acts. The recording in progress is never touched. The
 * old screen's cache temp files (`recording_<digits>.wav`) are recovered the same way: that is where
 * a failed conversion's audio used to be stranded.
 */
class PendingRecordingsSweepTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var captureDir: File
    private lateinit var cacheDir: File
    private lateinit var pending: PendingRecordings

    @Before
    fun setUp() {
        captureDir = tmp.newFolder("recordings_capture")
        cacheDir = tmp.newFolder("cache")
        pending = PendingRecordings(File(tmp.root, "recordings_pending"))
    }

    @Test
    fun aCaptureLeftByADeadProcessJoinsThePendingListWholeAndPlayable() {
        val audio = SessionTestKit.pcm(30_000)
        val orphan = SessionTestKit.captureFile(File(captureDir, "capture_1.wav"), audio)
        val hymn = RecordingTarget(listOf("Αναστασιματάριο", "Ήχος Δ΄"), "02 ", "Ήχος Δ΄")
        RecordingFiles.writeMeta(orphan, RecordingMeta(hymn))
        orphan.setLastModified(1_758_000_000_000L)

        val recovered = pending.recoverOrphans(captureDir, legacyCacheDir = null) { false }

        assertEquals(1, recovered)
        val kept = pending.list().single()
        assertArrayEquals(
            "a finished header over the untouched audio",
            SessionTestKit.finishedWav(File(tmp.root, "expected.wav"), audio),
            kept.wav.readBytes(),
        )
        assertEquals("still that hymn's recording", hymn, kept.meta.target)
        assertEquals("named by when it was last written", RecordingFiles.baseNameFor(1_758_000_000_000L), kept.baseName)
        assertEquals("nothing left behind", emptyList<String>(), captureDir.list()!!.toList())
    }

    @Test
    fun theRecordingInProgressIsNeverTouched() {
        val live = SessionTestKit.captureFile(File(captureDir, "capture_2.wav"), SessionTestKit.pcm(5_000))
        RecordingFiles.writeMeta(live, RecordingMeta())
        val before = live.readBytes()

        val recovered = pending.recoverOrphans(captureDir, legacyCacheDir = null) { it == live }

        assertEquals(0, recovered)
        assertArrayEquals("not moved, not re-headed", before, live.readBytes())
        assertTrue("its metadata stays too", RecordingFiles.sidecarOf(live).exists())
        assertTrue(pending.list().isEmpty())
    }

    @Test
    fun aCaptureThatNeverReceivedASampleIsDropped() {
        SessionTestKit.captureFile(File(captureDir, "capture_3.wav"), ByteArray(0))

        val recovered = pending.recoverOrphans(captureDir, legacyCacheDir = null) { false }

        assertEquals(0, recovered)
        assertTrue(pending.list().isEmpty())
        assertEquals(emptyList<String>(), captureDir.list()!!.toList())
    }

    @Test
    fun theOldScreensCacheTempFilesAreRecoveredAndNothingElseIsTouched() {
        val audio = SessionTestKit.pcm(9_000)
        SessionTestKit.captureFile(File(cacheDir, "recording_8812334455667788.wav"), audio)
        val userNamed = File(cacheDir, "recording_20260101_120000.wav").apply { writeBytes(ByteArray(500)) }
        val encodedLeftover = File(cacheDir, "recording_encoded_123.flac").apply { writeBytes(ByteArray(10)) }
        val openCopy = File(cacheDir, "byz_recordings_open/recording_5.wav").apply {
            parentFile!!.mkdirs()
            writeBytes(ByteArray(500))
        }

        val recovered = pending.recoverOrphans(captureDir, legacyCacheDir = cacheDir) { false }

        assertEquals(1, recovered)
        assertArrayEquals(
            SessionTestKit.finishedWav(File(tmp.root, "expected.wav"), audio),
            pending.list().single().wav.readBytes(),
        )
        assertTrue("a user-style name is not a temp file", userNamed.exists())
        assertTrue("converted leftovers are not audio we own the only copy of", encodedLeftover.exists())
        assertTrue("sub-folders of the cache are other features'", openCopy.exists())
    }

    @Test
    fun metadataWhoseAudioIsGoneIsClearedUnlessItsRecordingIsLive() {
        val stray = File(captureDir, "capture_4.json").apply { writeText("{}") }
        val liveWav = File(captureDir, "capture_5.wav")
        val liveMeta = File(captureDir, "capture_5.json").apply { writeText("{}") }

        pending.recoverOrphans(captureDir, legacyCacheDir = null) { it == liveWav }

        assertFalse(stray.exists())
        assertTrue("a starting recording's metadata may exist a moment before its audio", liveMeta.exists())
    }

    @Test
    fun keptRecordingsStayUntilTheUserDeletesThem() {
        SessionTestKit.captureFile(File(captureDir, "capture_6.wav"), SessionTestKit.pcm(2_000))
        pending.recoverOrphans(captureDir, legacyCacheDir = null) { false }

        // A later process: a new instance over the same folder, and another sweep.
        val reopened = PendingRecordings(File(tmp.root, "recordings_pending"))
        assertEquals(0, reopened.recoverOrphans(captureDir, legacyCacheDir = null) { false })
        val kept = reopened.list().single()

        assertTrue(reopened.delete(kept))
        assertTrue(reopened.list().isEmpty())
        assertEquals("audio and metadata both gone", emptyList<String>(), File(tmp.root, "recordings_pending").list()!!.toList())
    }
}
