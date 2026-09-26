package com.johnchourp.learnbyzantinemusic.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The steps of «Βρες τη φωνή σου» ([VoiceRangeTest], ClickUp `869f5x2dd`, J4), walked with no
 * microphone: nothing listens before «Ξεκίνα», each note needs a held pitch, and the result is only a
 * suggestion — the test itself never changes a shift.
 */
class VoiceRangeStepsTest {

    private val enough = VoiceRangeAdvisor.MIN_SAMPLES

    private fun VoiceRangeTest.hearing(hz: Double, times: Int = enough): VoiceRangeTest {
        var test = this
        repeat(times) { test = test.hear(hz) }
        return test
    }

    @Test
    fun nothingListensBeforeStart() {
        val intro = VoiceRangeTest()
        assertEquals(VoiceRangeTest.Step.INTRO, intro.step)
        assertFalse("the microphone is asked for only on «Ξεκίνα»", intro.listening)
        assertSame(intro, intro.hear(220.0))
        assertFalse(intro.canFinishStep)
        assertSame(intro, intro.finishStep())
    }

    @Test
    fun theLowestNoteThenTheHighestGiveTheAdvice() {
        var test = VoiceRangeTest().start()
        assertEquals(VoiceRangeTest.Step.LOWEST, test.step)
        assertTrue(test.listening)
        // Not a held note yet: «Έτοιμο» stays disabled, and tapping it anyway changes nothing.
        test = test.hearing(110.0, enough - 1)
        assertFalse(test.canFinishStep)
        assertSame(test, test.finishStep())

        test = test.hearing(110.0, 1)
        assertTrue(test.canFinishStep)
        test = test.finishStep()
        assertEquals(VoiceRangeTest.Step.HIGHEST, test.step)
        assertEquals(110.0, test.lowestHz!!, 0.0)
        assertTrue("the highest note starts from nothing heard", test.heardHz.isEmpty())
        assertFalse(test.canFinishStep)

        test = test.hearing(330.0).finishStep()
        assertEquals(VoiceRangeTest.Step.RESULT, test.step)
        assertFalse("the result releases the microphone", test.listening)
        assertEquals(VoiceRangeAdvisor.advise(110.0, 330.0), test.advice)
    }

    @Test
    fun aHighNoteNotAboveTheLowOneAsksToSingAgain() {
        val retry = VoiceRangeTest().start().hearing(300.0).finishStep().hearing(250.0).finishStep()
        assertEquals(VoiceRangeTest.Step.RETRY, retry.step)
        assertNull(retry.advice)
        assertFalse(retry.listening)
        // Again from the lowest note, with nothing carried over.
        val again = retry.start()
        assertEquals(VoiceRangeTest.Step.LOWEST, again.step)
        assertNull(again.lowestHz)
        assertTrue(again.heardHz.isEmpty())
    }

    @Test
    fun silenceAndWhatCannotBeHeardAreIgnored() {
        val listening = VoiceRangeTest().start()
        assertSame(listening, listening.hear(null))
        assertSame(listening, listening.hear(VoiceRangeAdvisor.LOWEST_HEARD_HZ - 1))
        assertSame(listening, listening.hear(VoiceRangeAdvisor.HIGHEST_HEARD_HZ + 1))
        assertEquals(listOf(220.0), listening.hear(220.0).heardHz)
    }

    @Test
    fun onlyTheMostRecentDetectionsAreKept() {
        val long = VoiceRangeTest().start().hearing(150.0, VoiceRangeAdvisor.MAX_SAMPLES).hearing(200.0, 5)
        assertEquals(VoiceRangeAdvisor.MAX_SAMPLES, long.heardHz.size)
        assertEquals(200.0, long.heardHz.last(), 0.0)
    }
}
