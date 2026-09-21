package com.johnchourp.learnbyzantinemusic.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningPathTest {

    @Test
    fun `path has no duplicate steps`() {
        assertEquals(LearningPath.stepIds.size, LearningPath.stepIds.toSet().size)
    }

    @Test
    fun `path has no blank ids`() {
        assertTrue(LearningPath.stepIds.none { it.isBlank() })
    }

    @Test
    fun `practice tools and settings are not lesson steps`() {
        // They are things you use, not steps you finish — counting them would inflate progress.
        listOf("melody_trainer", "calendar", "recordings", "notes", "settings").forEach {
            assertFalse("$it must not be a path step", LearningPath.isStep(it))
        }
    }

    @Test
    fun `next step of a fresh learner is the first step`() {
        assertEquals(LearningPath.stepIds.first(), LearningPath.nextStepId(emptySet()))
    }

    @Test
    fun `next step is null once every step is done`() {
        assertNull(LearningPath.nextStepId(LearningPath.stepIds.toSet()))
    }

    @Test
    fun `next step is the first gap when the learner jumped ahead`() {
        // Opened steps 1 and 3; step 2 is still the next thing to do.
        val completed = setOf(LearningPath.stepIds[0], LearningPath.stepIds[2])
        assertEquals(LearningPath.stepIds[1], LearningPath.nextStepId(completed))
    }

    @Test
    fun `completed count ignores ids that are not in the path`() {
        // A step removed from a later path version can still sit in stored preferences.
        val completed = setOf(LearningPath.stepIds.first(), "a_step_that_no_longer_exists")
        assertEquals(1, LearningPath.completedCount(completed))
    }

    @Test
    fun `completed count can never exceed the path size`() {
        val completed = LearningPath.stepIds.toSet() + setOf("ghost_one", "ghost_two")
        assertEquals(LearningPath.size, LearningPath.completedCount(completed))
        assertTrue(LearningPath.completedCount(completed) <= LearningPath.size)
    }

    @Test
    fun `completed count of a fresh learner is zero`() {
        assertEquals(0, LearningPath.completedCount(emptySet()))
    }

    @Test
    fun `position is one based and zero for a non step`() {
        assertEquals(1, LearningPath.positionOf(LearningPath.stepIds.first()))
        assertEquals(LearningPath.size, LearningPath.positionOf(LearningPath.stepIds.last()))
        assertEquals(0, LearningPath.positionOf("settings"))
    }

    @Test
    fun `every step reports itself as a step`() {
        assertTrue(LearningPath.stepIds.all { LearningPath.isStep(it) })
    }
}
