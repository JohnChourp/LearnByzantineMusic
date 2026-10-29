package com.johnchourp.learnbyzantinemusic.eartrainer

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.eartrainer.ui.EarTrainerScreen
import com.johnchourp.learnbyzantinemusic.modes.IsonPlaybackService
import com.johnchourp.learnbyzantinemusic.modes.PhthongTonePlayer
import com.johnchourp.learnbyzantinemusic.modes.ToneTimbre
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * «Μάντεψε τον ήχο» (ClickUp `869f4tqb9`): the app plays a ήχος — its scale around the base,
 * [EarTrainerPhrase] — and the learner names it. Every answer says why: the γένος, the base, where
 * each φθόγγος sits from it and the δεσπόζοντες; a wrong one can play the chosen ήχος next to the
 * right one. The rules are pure and tested: [EarTrainerPhrase], [EarTrainerQuestions],
 * [EarTrainerScores].
 *
 * **Sound.** Two [PhthongTonePlayer]s, as on the 8 Ήχοι page, because a player drives one AudioTrack:
 * the ison would stop at every note. The phrase is one stream that moves from φθόγγος to φθόγγος
 * ([PhthongTonePlayer.retune]), so no note waits for the previous one to fade. A background ison
 * ([IsonPlaybackService]) is stopped before the first note, or two isons would sound at once.
 * Nothing plays until the learner presses «Άκου», and everything stops in onStop.
 *
 * **State.** The level and the scores live in [EarTrainerStore]; the question, the answer and
 * whether it was heard live in the instance state, so a recreation shows the same question instead
 * of drawing a new one.
 *
 * **Inputs:** none. **Touches:** `ear_trainer_scores`, `ear_trainer_level`.
 */
class EarTrainerActivity : BaseActivity() {

    private val tonePlayer: PhthongTonePlayer by lazy { PhthongTonePlayer() }
    private val dronePlayer: PhthongTonePlayer by lazy { PhthongTonePlayer() }

    private lateinit var store: EarTrainerStore

    private var level by mutableStateOf(EarTrainerLevel.DEFAULT)
    private var asked by mutableStateOf(Mode.FIRST)
    private var chosen by mutableStateOf<Mode?>(null)
    private var heard by mutableStateOf(false)
    private var scores by mutableStateOf(EarTrainerScores())

    /** The ήχος whose phrase is sounding, or null. */
    private var sounding by mutableStateOf<Mode?>(null)
    private var playback: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = EarTrainerStore(this)
        level = store.level()
        scores = store.scores()
        val restored = savedInstanceState?.getString(STATE_ASKED)?.let(Mode::fromKey)?.takeIf { it in level.modes }
        if (restored != null) {
            asked = restored
            chosen = savedInstanceState.getString(STATE_CHOSEN)?.let(Mode::fromKey)
            heard = savedInstanceState.getBoolean(STATE_HEARD)
        } else {
            asked = EarTrainerQuestions.next(level, previous = null)
        }

        setContent {
            LbmTheme(palette = currentPalette()) {
                EarTrainerScreen(
                    level = level,
                    asked = asked,
                    chosen = chosen,
                    heard = heard,
                    sounding = sounding,
                    scores = scores,
                    onBack = ::finish,
                    onLevel = ::changeLevel,
                    onPlay = ::play,
                    onStop = ::stopPlayback,
                    onAnswer = ::answer,
                    onNext = ::nextQuestion,
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_ASKED, asked.key)
        outState.putString(STATE_CHOSEN, chosen?.key)
        outState.putBoolean(STATE_HEARD, heard)
    }

    override fun onStop() {
        stopPlayback()
        super.onStop()
    }

    override fun onDestroy() {
        stopPlayback()
        tonePlayer.release()
        dronePlayer.release()
        super.onDestroy()
    }

    /**
     * Plays [mode]'s phrase — the question, or after an answer either of the two ήχοι compared —
     * over its ison when the level has one, which starts a moment early so the ear has the base.
     */
    private fun play(mode: Mode) {
        stopPlayback()
        if (IsonPlaybackService.playing.value != null) IsonPlaybackService.stop(this)
        if (mode == asked) heard = true
        sounding = mode
        val withIson = level.withIson
        playback = lifecycleScope.launch {
            if (withIson) {
                dronePlayer.start(EarTrainerPhrase.ison(mode).frequencyHz, TIMBRE)
                delay(ISON_LEAD_MILLIS)
            }
            val rungs = EarTrainerPhrase.rungs(mode)
            rungs.forEachIndexed { index, rung ->
                if (index == 0 || !tonePlayer.retune(rung.frequencyHz)) tonePlayer.start(rung.frequencyHz, TIMBRE)
                delay(if (index == rungs.lastIndex) EarTrainerPhrase.LAST_NOTE_MILLIS else EarTrainerPhrase.NOTE_MILLIS)
            }
            silence()
        }
    }

    /**
     * Stops whatever sounds, at once. Cancelling the job is not enough on its own: it stops at its
     * next delay, and a phrase started in between must not be cut by the old one.
     */
    private fun stopPlayback() {
        playback?.cancel()
        playback = null
        silence()
    }

    private fun silence() {
        tonePlayer.stop()
        dronePlayer.stop()
        sounding = null
    }

    /** The learner's answer to the current question: recorded once, for the ήχος that sounded. */
    private fun answer(mode: Mode) {
        if (chosen != null || !heard) return
        chosen = mode
        scores = store.record(asked, correct = mode == asked)
    }

    private fun nextQuestion() {
        asked = EarTrainerQuestions.next(level, previous = asked)
        chosen = null
        play(asked)
    }

    /** Another level: saved, and a new question from its ήχοι, not yet played. */
    private fun changeLevel(newLevel: EarTrainerLevel) {
        if (newLevel == level) return
        stopPlayback()
        level = newLevel
        store.saveLevel(newLevel)
        asked = EarTrainerQuestions.next(newLevel, previous = asked)
        chosen = null
        heard = false
    }

    private companion object {
        val TIMBRE = ToneTimbre.SOFT

        /** How long the ison sounds alone before the first φθόγγος. */
        const val ISON_LEAD_MILLIS = 700L

        const val STATE_ASKED = "asked"
        const val STATE_CHOSEN = "chosen"
        const val STATE_HEARD = "heard"
    }
}
