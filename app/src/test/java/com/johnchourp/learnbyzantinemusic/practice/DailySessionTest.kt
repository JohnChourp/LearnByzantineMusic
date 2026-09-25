package com.johnchourp.learnbyzantinemusic.practice

import com.johnchourp.learnbyzantinemusic.calendar.WeeklyToneAnnouncement
import com.johnchourp.learnbyzantinemusic.music.Mode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

/**
 * «Πεντάλεπτο της ημέρας» (ClickUp `869f5x2dy`): the order of the steps, what a week with no tone
 * does, and how the session moves — only «Επόμενο» moves on, and only the last step completes it.
 */
class DailySessionTest {

    @Test
    fun theSessionIsFourStepsOfFiveMinutesInThisOrder() {
        val steps = DailySession.plan(currentToneIndex = 6, nextLessonId = "time")
        assertEquals(
            listOf(
                PracticeStep.Apichima(Mode.VARYS, noToneThisWeek = false),
                PracticeStep.Voice(Mode.VARYS, noToneThisWeek = false),
                PracticeStep.Rhythm,
                PracticeStep.NextLesson("time"),
            ),
            steps,
        )
        assertEquals(listOf(1, 2, 1, 1), steps.map { it.minutes })
        assertEquals(5, steps.sumOf { it.minutes })
    }

    @Test
    fun aWeekWithNoToneUsesTheFirstModeAndSaysSo() {
        val steps = DailySession.plan(currentToneIndex = null, nextLessonId = "time")
        assertEquals(PracticeStep.Apichima(Mode.FIRST, noToneThisWeek = true), steps[0])
        assertEquals(PracticeStep.Voice(Mode.FIRST, noToneThisWeek = true), steps[1])
        assertEquals("the session keeps its shape", 4, steps.size)
    }

    @Test
    fun aFinishedPathLeavesOutTheLessonStep() {
        val steps = DailySession.plan(currentToneIndex = 0, nextLessonId = null)
        assertEquals(listOf(PracticeStep.Apichima(Mode.FIRST, false), PracticeStep.Voice(Mode.FIRST, false), PracticeStep.Rhythm), steps)
    }

    @Test
    fun onlyNextMovesOnAndOnlyTheLastStepCompletes() {
        assertEquals(SessionMove.GoTo(1), DailySession.onNext(0, 4))
        assertEquals(SessionMove.GoTo(3), DailySession.onNext(2, 4))
        assertEquals(SessionMove.Complete, DailySession.onNext(3, 4))
        (0..2).forEach { index -> assertEquals("«Τέλος» on step ${index + 1}", SessionMove.Quit, DailySession.onEnd(index, 4)) }
        assertEquals(SessionMove.Complete, DailySession.onEnd(3, 4))
        // Without the lesson step the third step is the last one.
        assertEquals(SessionMove.Complete, DailySession.onNext(2, 3))
        assertEquals(SessionMove.Complete, DailySession.onEnd(2, 3))
    }

    @Test
    fun theSessionUsesTheToneTheHomeCardAnnounces() {
        val announcement = WeeklyToneAnnouncement()
        fun modeAt(moment: String): PracticeStep.Apichima =
            DailySession.plan(announcement.at(LocalDateTime.parse(moment)).currentToneIndex, "time")[0] as PracticeStep.Apichima
        // Holy Wednesday 2026: no tone — Α΄, and the screen says why.
        assertEquals(PracticeStep.Apichima(Mode.FIRST, noToneThisWeek = true), modeAt("2026-04-08T10:00"))
        // Saturday 26 September 2026 after noon: the vespers tone, Πλ. Δ΄, not the week's Βαρύς.
        assertEquals(PracticeStep.Apichima(Mode.PLAGAL_FOURTH, noToneThisWeek = false), modeAt("2026-09-26T15:00"))
        // Bright Tuesday morning: the tone of the day, Γ΄.
        assertEquals(PracticeStep.Apichima(Mode.THIRD, noToneThisWeek = false), modeAt("2026-04-14T10:00"))
    }

    @Test
    fun aRestoredSessionHasTheSamePlan() {
        assertEquals(
            DailySession.plan(currentToneIndex = 3, nextLessonId = "quality"),
            DailySession.steps(Mode.FOURTH, noToneThisWeek = false, nextLessonId = "quality"),
        )
    }
}
