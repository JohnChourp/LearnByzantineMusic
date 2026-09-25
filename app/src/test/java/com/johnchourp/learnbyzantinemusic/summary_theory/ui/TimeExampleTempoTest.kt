package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import com.johnchourp.learnbyzantinemusic.lessons.ui.MetronomeSchedule
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * «Άκου» plays at the metronome's saved tempo and «Αργά» at half of it, never slower than the metronome
 * itself goes (ClickUp `869f5x25n`, F4 — decision: «Αργά» = half the saved tempo).
 */
class TimeExampleTempoTest {

    @Test
    fun listenPlaysAtTheSavedMetronomeTempo() {
        listOf(40, 80, 110, 160).forEach { saved -> assertEquals(saved, TimeExamplePlayback.tempo(saved, slow = false)) }
    }

    @Test
    fun slowPlaysAtHalfOfIt() {
        assertEquals(40, TimeExamplePlayback.tempo(80, slow = true))
        assertEquals(55, TimeExamplePlayback.tempo(110, slow = true))
        assertEquals(80, TimeExamplePlayback.tempo(160, slow = true))
    }

    @Test
    fun slowNeverGoesUnderTheMetronomesSlowest() {
        assertEquals(MetronomeSchedule.MIN_BPM, TimeExamplePlayback.tempo(MetronomeSchedule.MIN_BPM, slow = true))
        assertEquals(MetronomeSchedule.MIN_BPM, TimeExamplePlayback.tempo(79, slow = true))
    }

    @Test
    fun slowTakesTwiceAsLong() {
        val argo = TimePageEquations.all.getValue("argo")
        val normal = TimeExamplePlayback.cues(argo, TimeExamplePlayback.tempo(80, slow = false)).last().atMillis
        val slow = TimeExamplePlayback.cues(argo, TimeExamplePlayback.tempo(80, slow = true)).last().atMillis
        assertEquals(2250L, normal)
        assertEquals(4500L, slow)
    }
}
