package com.johnchourp.learnbyzantinemusic.practice

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.calendar.WeeklyToneAnnouncement
import com.johnchourp.learnbyzantinemusic.learning.LearningPath
import com.johnchourp.learnbyzantinemusic.learning.LearningProgress
import com.johnchourp.learnbyzantinemusic.learning.LessonScreens
import com.johnchourp.learnbyzantinemusic.lessons.Duotrioquatro
import com.johnchourp.learnbyzantinemusic.modes.EightModesActivity
import com.johnchourp.learnbyzantinemusic.practice.ui.DailyPracticeScreen
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme
import java.time.Instant

/**
 * «Πεντάλεπτο της ημέρας» (ClickUp `869f5x2dy`): a guided chain through screens the app already has,
 * not a new all-in-one page. 1′ the απήχημα of the current tone and a φθόγγος over the ison · 2′ the
 * voice, «Πού είμαι» over the ison · 1′ rhythm on the metronome · 1′ the next lesson of «Από το
 * μηδέν». This is the page the user comes back to between steps: it names the step, opens its screen,
 * and carries «Επόμενο» and «Τέλος». The plan and its rules are [DailySession], pure and tested.
 *
 * **Nothing advances by itself** — no timer — so the session never leaves a step the user is still on.
 * The day counts when the user leaves the LAST step, by «Επόμενο» or «Τέλος»; the session is then
 * written to [PracticeLogStore] once, and the page shows the streak. «Τέλος» earlier stops without
 * counting.
 *
 * **The plan is fixed when the session starts** (the current tone, the next lesson) and kept in the
 * instance state with the step and the start time, so a recreation neither restarts nor re-plans it —
 * opening the lesson marks it done, and re-planning would then skip to the following one.
 *
 * **Inputs:** none. **Opens:** 8 Ήχοι (one-shot: the current mode, ison on, and listening for the voice
 * step), the metronome page, the next lesson (marked opened, as its home tile does), «Ιστορικό
 * εξάσκησης». **Touches:** `practice_log` (written on completion), `learning_completed_step_ids`.
 */
class DailyPracticeActivity : BaseActivity() {

    private lateinit var store: PracticeLogStore

    /** The session's plan, fixed at its start; null tone = a week without one. */
    private var toneIndex: Int? = null
    private var lessonId: String? = null
    private var startedAtMillis: Long = 0L

    private var steps by mutableStateOf<List<PracticeStep>>(emptyList())
    private var stepIndex by mutableIntStateOf(0)

    /** Non-null once the session is completed; the page then shows the result. */
    private var result by mutableStateOf<PracticeSummary?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = PracticeLogStore(this)
        if (savedInstanceState == null) {
            toneIndex = runCatching { WeeklyToneAnnouncement().now().currentToneIndex }.getOrNull()
            lessonId = LearningPath.nextStepId(LearningProgress.completedSteps(this))
            startedAtMillis = store.now().toEpochMilli()
        } else {
            toneIndex = savedInstanceState.getInt(STATE_TONE_INDEX, NO_TONE).takeIf { it != NO_TONE }
            lessonId = savedInstanceState.getString(STATE_LESSON_ID)
            startedAtMillis = savedInstanceState.getLong(STATE_STARTED_AT)
            stepIndex = savedInstanceState.getInt(STATE_STEP_INDEX)
            if (savedInstanceState.getBoolean(STATE_COMPLETED)) result = store.summary()
        }
        steps = DailySession.plan(toneIndex, lessonId)
        stepIndex = stepIndex.coerceIn(steps.indices)

        setContent {
            LbmTheme(palette = currentPalette()) {
                DailyPracticeScreen(
                    steps = steps,
                    stepIndex = stepIndex,
                    result = result,
                    lessonTitleRes = lessonId?.let { LessonScreens.titleRes(it) },
                    onBack = ::finish,
                    onOpenStep = { open(steps[stepIndex]) },
                    onNext = { move(DailySession.onNext(stepIndex, steps.size)) },
                    onEnd = { move(DailySession.onEnd(stepIndex, steps.size)) },
                    onOpenHistory = { startActivity(Intent(this, PracticeHistoryActivity::class.java)) },
                    onClose = ::finish,
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_TONE_INDEX, toneIndex ?: NO_TONE)
        outState.putString(STATE_LESSON_ID, lessonId)
        outState.putLong(STATE_STARTED_AT, startedAtMillis)
        outState.putInt(STATE_STEP_INDEX, stepIndex)
        outState.putBoolean(STATE_COMPLETED, result != null)
    }

    private fun move(move: SessionMove) {
        when (move) {
            is SessionMove.GoTo -> stepIndex = move.index
            SessionMove.Complete -> if (result == null) {
                store.recordCompletedSession(Instant.ofEpochMilli(startedAtMillis))
                result = store.summary()
            }
            SessionMove.Quit -> finish()
        }
    }

    /** Opens the screen of [step]. Each is a page the app already has. */
    private fun open(step: PracticeStep) {
        val intent = when (step) {
            is PracticeStep.Apichima -> EightModesActivity.intent(this, step.mode.key, isonOn = true)
            is PracticeStep.Voice -> voiceExercise(step)
            PracticeStep.Rhythm -> Intent(this, Duotrioquatro::class.java)
            is PracticeStep.NextLesson -> {
                // Opening a lesson counts as having opened it, exactly as its home tile does.
                LearningProgress.markCompleted(this, step.lessonId)
                LessonScreens.intent(this, step.lessonId)
            }
        }
        intent?.let(::startActivity)
    }

    /**
     * **J1 hook** (ClickUp `869f5x2cd`): the voice exercise is «Πού είμαι» over the ison until the
     * wait-mode trainer («Παραλλαγή με αναμονή») exists. Replace only this function then — the plan,
     * the order of the steps and the streak do not change.
     */
    private fun voiceExercise(step: PracticeStep.Voice): Intent =
        EightModesActivity.intent(this, step.mode.key, isonOn = true, listen = true)

    private companion object {
        const val NO_TONE = -1
        const val STATE_TONE_INDEX = "tone_index"
        const val STATE_LESSON_ID = "lesson_id"
        const val STATE_STARTED_AT = "started_at"
        const val STATE_STEP_INDEX = "step_index"
        const val STATE_COMPLETED = "completed"
    }
}
