package com.johnchourp.learnbyzantinemusic.practice

import com.johnchourp.learnbyzantinemusic.music.Mode

/**
 * One step of «Πεντάλεπτο της ημέρας». Every step opens a screen the app already has; the session
 * adds only the order, and a place to come back to between steps.
 */
sealed interface PracticeStep {
    /** How long the step is meant to take. Guidance on the screen, never a timer. */
    val minutes: Int

    /** 1′: the απήχημα of the current tone, then a φθόγγος held over the ison — 8 Ήχοι, ison on. */
    data class Apichima(val mode: Mode, val noToneThisWeek: Boolean) : PracticeStep {
        override val minutes = 1
    }

    /**
     * 2′: the voice — «Πού είμαι» over the ison, in the 8 Ήχοι, listening.
     *
     * **J1 hook** (ClickUp `869f5x2cd`, not built yet): once the wait-mode trainer («Παραλλαγή με
     * αναμονή») exists, this step should open it instead. Only the screen this step opens changes
     * (`DailyPracticeActivity.open`); the plan, its order and the streak stay as they are.
     */
    data class Voice(val mode: Mode, val noToneThisWeek: Boolean) : PracticeStep {
        override val minutes = 2
    }

    /** 1′: rhythm, on the metronome of the «Δίσημος / Τρίσημος / Τετράσημος» page. */
    data object Rhythm : PracticeStep {
        override val minutes = 1
    }

    /** 1′: the next lesson of «Από το μηδέν». */
    data class NextLesson(val lessonId: String) : PracticeStep {
        override val minutes = 1
    }
}

/** What an «Επόμενο» or a «Τέλος» does from a step. */
sealed interface SessionMove {
    /** Show step [index]. */
    data class GoTo(val index: Int) : SessionMove

    /** The session reached its end: the day counts. */
    data object Complete : SessionMove

    /** Stopped before the last step: nothing is recorded. */
    data object Quit : SessionMove
}

/**
 * «Πεντάλεπτο της ημέρας» (ClickUp `869f5x2dy`): the plan of a session, and how it moves.
 *
 * **The plan.** 1′ the απήχημα of the current tone and a φθόγγος over the ison · 2′ «Πού είμαι» over
 * the ison · 1′ rhythm on the metronome · 1′ the next lesson of «Από το μηδέν». The current tone is the
 * one the home card announces (`WeeklyToneAnnouncement`: from Saturday's vespers, next week's).
 *
 * **A week with no tone** (Palm Sunday and Holy Week, the week of Pentecost): the two ison steps use
 * [NO_TONE_FALLBACK], Α΄, and say why on the screen, rather than skipping. The session keeps its
 * shape — a beginner still needs the practice — and Α΄ is where the cycle starts.
 *
 * **When «Από το μηδέν» is finished** the fourth step is left out: there is no next lesson to open,
 * and a step that opens nothing would only teach the user to press «Επόμενο» without reading.
 *
 * **Moving.** Nothing advances on its own: only «Επόμενο» moves on, never a timer, so the session can
 * never leave a step the user is still on. The session is COMPLETED — the day counts — when the user
 * leaves the LAST step, by «Επόμενο» or by «Τέλος»; «Τέλος» on an earlier step stops without counting.
 */
object DailySession {

    /** The mode of the two ison steps in a week that has no tone of its own. */
    val NO_TONE_FALLBACK = Mode.FIRST

    /** The plan for [currentToneIndex] (null: a week with no tone) and [nextLessonId] (null: path done). */
    fun plan(currentToneIndex: Int?, nextLessonId: String?): List<PracticeStep> =
        steps(
            mode = currentToneIndex?.let { Mode.ofToneIndex(it) } ?: NO_TONE_FALLBACK,
            noToneThisWeek = currentToneIndex == null,
            nextLessonId = nextLessonId,
        )

    /** The same plan from its stored parts, so a recreated session shows the steps it started with. */
    fun steps(mode: Mode, noToneThisWeek: Boolean, nextLessonId: String?): List<PracticeStep> =
        listOfNotNull(
            PracticeStep.Apichima(mode, noToneThisWeek),
            PracticeStep.Voice(mode, noToneThisWeek),
            PracticeStep.Rhythm,
            nextLessonId?.let { PracticeStep.NextLesson(it) },
        )

    fun onNext(index: Int, stepCount: Int): SessionMove =
        if (index >= stepCount - 1) SessionMove.Complete else SessionMove.GoTo(index + 1)

    fun onEnd(index: Int, stepCount: Int): SessionMove =
        if (index >= stepCount - 1) SessionMove.Complete else SessionMove.Quit
}
