package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * In «Ψάλλε μαζί» the guide melody fades round by round — full, half, silent — and then stays silent
 * for as long as the loop runs, until «Στάση» (ClickUp `869f5x2cv`, J2). Nothing stops by itself, and
 * the ison and the metronome are not on this curve at all.
 */
class SingAlongGuideGainTest {

    private val melody = MelodySequence(listOf(TrainerNote(PhthongName.NI), TrainerNote(PhthongName.PA)))

    @Test
    fun fullThenHalfThenSilentAndSilentFromThenOn() {
        assertEquals(1f, MelodyPlaybackPlanner.guideGain(0), 0f)
        assertEquals(0.5f, MelodyPlaybackPlanner.guideGain(1), 0f)
        assertEquals(0f, MelodyPlaybackPlanner.guideGain(2), 0f)
        (3..1_000).forEach { round ->
            assertEquals("round $round", 0f, MelodyPlaybackPlanner.guideGain(round), 0f)
        }
    }

    @Test
    fun eachRoundOfThePlanCarriesItsGain() {
        val expected = listOf(1f, 0.5f, 0f, 0f, 0f, 0f)
        expected.forEachIndexed { round, gain ->
            val planned = MelodyPlaybackPlanner.planRound(melody, MelodyTempo(80), round) { 220.0 }
            assertEquals("round $round", gain, planned.guideGain, 0f)
            // A silent round still has its notes, so the line stays lit, and its clicks.
            assertEquals(2, planned.notes.size)
            assertEquals(2, planned.ticks.size)
        }
    }

    @Test
    fun theCardPrintsTheLevelAsAPercentage() {
        assertEquals(listOf(100, 50, 0, 0, 0), (0..4).map(SingAlong::guidePercent))
    }
}
