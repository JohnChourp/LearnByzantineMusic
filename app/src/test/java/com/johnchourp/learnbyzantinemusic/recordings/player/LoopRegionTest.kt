package com.johnchourp.learnbyzantinemusic.recordings.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The A-B loop's own rules (ClickUp `869f5x268`): A before B, at least half a second long, both marks
 * clamped to the recording, and back to A once playback reaches B. The controller's use of them is
 * pinned in `PlayerLoopTest`.
 */
class LoopRegionTest {

    @Test
    fun aMustComeBeforeB() {
        assertNull(LoopRegion.of(startMs = 20_000, endMs = 10_000, durationMs = 60_000))
        assertNull("A and B in the same place are no loop", LoopRegion.of(20_000, 20_000, 60_000))
        assertEquals(LoopRegion(10_000, 20_000), LoopRegion.of(10_000, 20_000, 60_000))
    }

    @Test
    fun aLoopLastsAtLeastHalfASecond() {
        assertEquals(500, LoopRegion.MIN_LENGTH_MS)
        assertNull(LoopRegion.of(10_000, 10_499, 60_000))
        assertEquals(LoopRegion(10_000, 10_500), LoopRegion.of(10_000, 10_500, 60_000))
    }

    @Test
    fun marksOutsideTheRecordingAreClampedToIt() {
        assertEquals(LoopRegion(0, 5_000), LoopRegion.of(-3_000, 5_000, 60_000))
        assertEquals(LoopRegion(55_000, 60_000), LoopRegion.of(55_000, 90_000, 60_000))
        assertNull("clamping cannot make a loop out of nothing", LoopRegion.of(59_800, 90_000, 60_000))
        assertNull("a recording shorter than a loop has none", LoopRegion.of(0, 400, 400))
    }

    @Test
    fun playbackGoesBackToAOnceItReachesB() {
        val loop = LoopRegion(10_000, 20_000)
        assertNull("inside the loop, nothing changes", loop.wrap(15_000))
        assertNull("before A too — the loop takes over at B", loop.wrap(2_000))
        assertEquals(10_000, loop.wrap(20_000))
        assertEquals("however far past B a tick lands", 10_000, loop.wrap(20_040))
    }
}
