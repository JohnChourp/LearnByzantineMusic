package com.johnchourp.learnbyzantinemusic.music

import com.johnchourp.learnbyzantinemusic.recordings.analysis.PhthongSegmenter
import com.johnchourp.learnbyzantinemusic.recordings.analysis.PitchTrackAnalyzer
import com.johnchourp.learnbyzantinemusic.recordings.analysis.RecordingDecoder
import com.johnchourp.learnbyzantinemusic.trainer.PitchGreeningEvaluator
import com.johnchourp.learnbyzantinemusic.trainer.TrainerPitchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The numbers in [IntonationProfile]'s KDoc table, and the promise that writing the thresholds in
 * milliseconds changed **no** count the two pipelines use (ClickUp `869f5x28t`, H1).
 *
 * Every count is read at its use site — the pitch engine, the analyzer, the evaluator, the
 * segmenter — not recomputed here, so a consumer that stops reading the profile is caught too.
 */
class IntonationProfileTest {

    @Test
    fun theToleranceIsThreeMoriaWhichIsFiftyCents() {
        assertEquals(3.0, IntonationProfile.IN_TUNE_MORIA, 0.0)
        assertEquals(50.0, ByzantineTuning.moriaToCents(IntonationProfile.IN_TUNE_MORIA), 1e-9)
        // The table's conversion factor: one μόριο is 1200 / 72 cents.
        assertEquals(16.7, ByzantineTuning.moriaToCents(1.0), 0.05)
    }

    @Test
    fun theToleranceIsAWholeNumberBecauseTheSummaryPrintsItWithD() {
        // analysis_sung_summary formats it with %4$d; a fractional tolerance would be printed rounded.
        val tolerance = IntonationProfile.IN_TUNE_MORIA
        assertEquals(tolerance, tolerance.roundToInt().toDouble(), 0.0)
    }

    @Test
    fun theBoundaryIsInclusiveOnBothSides() {
        val tolerance = IntonationProfile.IN_TUNE_MORIA
        assertTrue(IntonationProfile.isInTune(tolerance))
        assertTrue(IntonationProfile.isInTune(-tolerance))
        assertFalse(IntonationProfile.isInTune(tolerance + 1e-6))
        assertFalse(IntonationProfile.isInTune(-tolerance - 1e-6))
    }

    @Test
    fun theDurationsAreTheOnesTheTableStates() {
        assertEquals(0.012, IntonationProfile.SILENCE_RMS, 0.0)
        assertEquals(46.44, IntonationProfile.WINDOW_MS, 0.0)
        assertEquals(IntonationProfile.WINDOW_MS, IntonationProfile.LIVE_HOP_MS, 0.0)
        assertEquals(23.22, IntonationProfile.OFFLINE_HOP_MS, 0.0)
        assertEquals(139.0, IntonationProfile.LIVE_MIN_STABLE_MS, 0.0)
        assertEquals(93.0, IntonationProfile.OFFLINE_MIN_NOTE_MS, 0.0)
        assertEquals(46.0, IntonationProfile.OFFLINE_MAX_GAP_MS, 0.0)
        assertEquals(116.0, IntonationProfile.OFFLINE_STEADY_MS, 0.0)
    }

    @Test
    fun theWindowsInSamplesAreExactlyTheOnesUsedBefore() {
        assertEquals(2048, TrainerPitchEngine.DEFAULT_WINDOW_SIZE)
        assertEquals(1024, PitchTrackAnalyzer.WINDOW)
        assertEquals(512, PitchTrackAnalyzer.HOP)
    }

    @Test
    fun theLiveAndTheRecordingWindowsLastTheSameMilliseconds() {
        val liveMs = TrainerPitchEngine.DEFAULT_WINDOW_SIZE * 1000.0 / TrainerPitchEngine.DEFAULT_SAMPLE_RATE
        val recordingMs = PitchTrackAnalyzer.WINDOW * 1000.0 / RecordingDecoder.SAMPLE_RATE
        assertEquals("one ear for both pipelines", liveMs, recordingMs, 0.0)
        assertEquals(IntonationProfile.WINDOW_MS, liveMs, 0.01)
    }

    @Test
    fun theThresholdsInFramesAreExactlyTheOnesUsedBefore() {
        assertEquals("live: held 139 ms", 3, PitchGreeningEvaluator.DEFAULT_MIN_STABLE_FRAMES)
        assertEquals("recording: a note lasts 93 ms", 4, PhthongSegmenter.MIN_NOTE_FRAMES)
        assertEquals("recording: a 46 ms gap is bridged", 2, PhthongSegmenter.MAX_GAP_FRAMES)
        assertEquals("recording: a steady stretch is 116 ms", 5, PhthongSegmenter.STEADY_FRAMES)
    }

    @Test
    fun theSameDurationIsTwiceAsManyFramesInARecording() {
        // The confusion the ticket named: one frame is 46 ms live and 23 ms offline, so "2 frames"
        // meant two durations. In milliseconds the conversion is explicit and checkable.
        val gapMs = IntonationProfile.OFFLINE_MAX_GAP_MS
        assertEquals(1, IntonationProfile.framesIn(gapMs, IntonationProfile.LIVE_HOP_MS))
        assertEquals(2, IntonationProfile.framesIn(gapMs, IntonationProfile.OFFLINE_HOP_MS))
    }

    @Test
    fun theRecordingAnalyzerGatesSilenceAtTheProfileLevel() {
        val sampleRate = RecordingDecoder.SAMPLE_RATE
        // A sine's RMS is its amplitude / √2; put one tone 10% under the gate and one 10% over it.
        val gateAmplitude = IntonationProfile.SILENCE_RMS * sqrt(2.0)
        fun tone(amplitude: Double) = FloatArray(sampleRate / 2) { (amplitude * sin(2 * PI * 220.0 * it / sampleRate)).toFloat() }

        val under = PitchTrackAnalyzer.analyze(tone(gateAmplitude * 0.9), sampleRate)
        val over = PitchTrackAnalyzer.analyze(tone(gateAmplitude * 1.1), sampleRate)
        assertTrue("the fixture must produce frames", under.frames.isNotEmpty() && over.frames.isNotEmpty())
        assertTrue("under the gate is silence", under.frames.none { it.isVoiced })
        assertTrue("over the gate is a voice", over.frames.all { it.isVoiced })
    }
}
