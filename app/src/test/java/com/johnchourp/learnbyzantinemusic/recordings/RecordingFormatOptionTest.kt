package com.johnchourp.learnbyzantinemusic.recordings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Decisions D1–D2 of ClickUp `869fhmxpt`, after FFmpegKit was replaced by Android's encoders:
 * MP3 is never recorded (a stored MP3 reads as M4A), OPUS only from API 29 — while every old
 * recording, MP3 and OPUS included, is still recognised.
 */
class RecordingFormatOptionTest {

    @Test
    fun mp3IsNeverOfferedForRecording() {
        for (sdk in listOf(24, 28, 29, 36)) {
            assertFalse("API $sdk", RecordingFormatOption.MP3.isRecordableOn(sdk))
            assertFalse("API $sdk", RecordingFormatOption.MP3 in RecordingFormatOption.recordableOn(sdk))
        }
    }

    @Test
    fun opusIsOfferedFromAndroid10Only() {
        assertFalse(RecordingFormatOption.OPUS.isRecordableOn(28))
        assertTrue(RecordingFormatOption.OPUS.isRecordableOn(29))
        assertEquals(
            listOf(RecordingFormatOption.FLAC, RecordingFormatOption.WAV, RecordingFormatOption.AAC, RecordingFormatOption.M4A),
            RecordingFormatOption.recordableOn(24),
        )
        assertEquals(
            listOf(
                RecordingFormatOption.FLAC, RecordingFormatOption.WAV, RecordingFormatOption.AAC,
                RecordingFormatOption.M4A, RecordingFormatOption.OPUS,
            ),
            RecordingFormatOption.recordableOn(29),
        )
    }

    @Test
    fun aStoredMp3ReadsAsM4a() {
        assertEquals(RecordingFormatOption.M4A, RecordingFormatOption.fromStoredValue("MP3"))
        assertEquals(RecordingFormatOption.M4A, RecordingFormatOption.fromStoredValue("mp3"))
        assertEquals(RecordingFormatOption.M4A, RecordingFormatOption.forRecording("MP3", 36))
    }

    @Test
    fun aStoredOpusReadsAsM4aOnlyWhereItCannotBeRecorded() {
        assertEquals(RecordingFormatOption.OPUS, RecordingFormatOption.fromStoredValue("OPUS"))
        assertEquals(RecordingFormatOption.M4A, RecordingFormatOption.forRecording("OPUS", 28))
        assertEquals(RecordingFormatOption.OPUS, RecordingFormatOption.forRecording("OPUS", 29))
    }

    @Test
    fun unknownOrMissingStillFallBackToFlac() {
        assertEquals(RecordingFormatOption.FLAC, RecordingFormatOption.fromStoredValue(null))
        assertEquals(RecordingFormatOption.FLAC, RecordingFormatOption.fromStoredValue(""))
        assertEquals(RecordingFormatOption.FLAC, RecordingFormatOption.fromStoredValue("OGG"))
        assertEquals(RecordingFormatOption.FLAC, RecordingFormatOption.forRecording(null, 24))
    }

    @Test
    fun everyRecordableChoiceSurvivesTheRoundTrip() {
        for (sdk in listOf(24, 29)) {
            for (format in RecordingFormatOption.recordableOn(sdk)) {
                assertEquals("API $sdk", format, RecordingFormatOption.forRecording(format.name, sdk))
            }
        }
    }

    @Test
    fun oldMp3AndOpusRecordingsAreStillRecognised() {
        assertTrue(RecordingFormatOption.supportsFileName("Ήχος α.mp3"))
        assertTrue(RecordingFormatOption.supportsFileName("Ήχος α.OPUS"))
        assertEquals("audio/mpeg", RecordingFormatOption.resolveMimeTypeByFileName("x.mp3"))
        assertEquals("audio/ogg", RecordingFormatOption.resolveMimeTypeByFileName("x.opus"))
    }
}
