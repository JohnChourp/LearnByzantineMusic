package com.johnchourp.learnbyzantinemusic.trainer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The scrolling trace of «Παραλλαγή με αναμονή» (ClickUp `869f5x2cd`, J1): the last seconds of the
 * voice, placed on the time axis by capture time, oldest at the left, with silence kept as a gap.
 */
class VoiceTraceTest {

    @Test
    fun theNewestPointIsAtTheRightEdgeAndTheOldestFallOff() {
        val trace = VoiceTrace(windowMillis = 1_000)
        (0..30).forEach { trace.add(it * 100L, it.toDouble()) } // three seconds
        val points = trace.points()
        assertEquals(1f, points.last().x, 0f)
        assertEquals("only the last second is kept", 11, points.size)
        assertEquals(20.0, points.first().offsetMoria!!, 0.0)
        assertEquals(0f, points.first().x, 0f)
        assertTrue("oldest first, left to right", points.zipWithNext().all { (a, b) -> a.x < b.x })
    }

    @Test
    fun silenceIsAGapNotAZero() {
        val trace = VoiceTrace(windowMillis = 1_000)
        trace.add(0, 1.5)
        trace.add(100, null)
        trace.add(200, -2.0)
        assertEquals(listOf(1.5, null, -2.0), trace.points().map { it.offsetMoria })
        assertEquals(listOf(0.8f, 0.9f, 1f), trace.points().map { it.x })
    }

    @Test
    fun aNewNoteStartsAClearTrace() {
        val trace = VoiceTrace()
        trace.add(0, 3.0)
        trace.clear()
        assertTrue(trace.points().isEmpty())
        trace.add(50, null)
        assertNull(trace.points().single().offsetMoria)
    }
}
