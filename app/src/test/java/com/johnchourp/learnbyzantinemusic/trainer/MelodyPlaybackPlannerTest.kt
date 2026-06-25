package com.johnchourp.learnbyzantinemusic.trainer

import org.junit.Assert.assertEquals
import org.junit.Test

class MelodyPlaybackPlannerTest {

    @Test
    fun `plan lays notes end to end in absolute time`() {
        val sequence = MelodySequence(
            listOf(
                TrainerNote(TrainerPhthong.NI, baseDurationBeats = 1f),
                TrainerNote(TrainerPhthong.PA, baseDurationBeats = 2f)
            )
        )
        val plan = MelodyPlaybackPlanner.plan(sequence, MelodyTempo(60)) // 1000 ms per beat

        assertEquals(2, plan.size)
        assertEquals(0L, plan[0].startMillis)
        assertEquals(1000L, plan[0].durationMillis)
        assertEquals(1000L, plan[1].startMillis)
        assertEquals(2000L, plan[1].durationMillis)
        assertEquals(3000L, plan[1].endMillis)
        assertEquals(3000L, MelodyPlaybackPlanner.totalDurationMillis(plan))
    }

    @Test
    fun `plan carries each note's frequency`() {
        val sequence = MelodySequence(listOf(TrainerNote(TrainerPhthong.NI)))
        val plan = MelodyPlaybackPlanner.plan(sequence, MelodyTempo(120))
        assertEquals(220.0, plan.single().frequencyHz, 1e-6)
        assertEquals(TrainerPhthong.NI, plan.single().phthong)
    }

    @Test
    fun `empty sequence yields an empty plan`() {
        val plan = MelodyPlaybackPlanner.plan(MelodySequence(emptyList()), MelodyTempo(80))
        assertEquals(emptyList<PlannedNoteEvent>(), plan)
        assertEquals(0L, MelodyPlaybackPlanner.totalDurationMillis(plan))
    }
}
