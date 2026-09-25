package com.johnchourp.learnbyzantinemusic.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * «The next lesson» can be opened from outside the home screen only if every step of «Από το μηδέν»
 * has a screen here (ClickUp `869f5x2dy`: the daily session opens it through [LessonScreens]).
 */
class LessonScreensTest {

    @Test
    fun everyStepOfThePathHasAScreenAndATitle() {
        assertEquals(LearningPath.stepIds.toSet(), LessonScreens.ids)
        LearningPath.stepIds.forEach { id -> assertNotEquals(id, 0, LessonScreens.titleRes(id)) }
    }

    @Test
    fun anUnknownLessonIsAnError() {
        assertThrows(IllegalArgumentException::class.java) { LessonScreens.titleRes("not_a_lesson") }
    }
}
