package com.johnchourp.learnbyzantinemusic.recordings.player

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.recordings.session.RecordingSessions
import com.johnchourp.learnbyzantinemusic.recordings.ui.components.RecordingPlayerActions
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** A recording the in-app player can open — and hand to another app, if it cannot play it. */
data class PlayerItem(val uri: Uri, val name: String, val mimeType: String?)

/**
 * The in-app player of the «Ηχογραφήσεις», «Διαχείριση ηχογραφήσεων» and hymn screens (ClickUp
 * `869f5x268`): a [PlayerController] over Android's `MediaPlayer`, kept in a ViewModel so a re-created
 * screen finds the same recording, position, loop, speed and shift.
 *
 * The screen calls [onScreenStopped] from `onStop`: the player is released there, so nothing plays in
 * the background, and it is prepared again on the next play. Nothing plays while a recording is in
 * progress, on any of these screens — it would go straight into the microphone.
 */
class InAppPlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val controller = PlayerController<Uri> { uri -> MediaPlayerAudio(application, uri) }
    val state: StateFlow<PlayerState> = controller.state

    /** What is loaded, for the «Άνοιγμα σε άλλη εφαρμογή» action. */
    var current: PlayerItem? = null
        private set

    private var ticking: Job? = null

    init {
        viewModelScope.launch {
            RecordingSessions.get(application).state.collect { controller.setBlocked(playbackBlockedBy(it)) }
        }
        viewModelScope.launch {
            state.map { it.phase == PlayerPhase.PLAYING }.distinctUntilChanged().collect { playing ->
                ticking?.cancel()
                ticking = if (playing) launch { tickWhilePlaying() } else null
            }
        }
    }

    fun open(item: PlayerItem) {
        current = item
        controller.open(item.uri, item.name)
    }

    fun togglePlay() = controller.toggle()

    fun seekTo(positionMs: Int) = controller.seekTo(positionMs)

    fun setSpeed(speed: Float) = controller.setSpeed(speed)

    fun setShift(moria: Int) = controller.setShift(moria)

    fun resetTuning() = controller.resetTuning()

    fun markLoopStart(): LoopMarkResult = controller.markLoopStart()

    fun markLoopEnd(): LoopMarkResult = controller.markLoopEnd()

    fun clearLoop() = controller.clearLoop()

    fun close() {
        current = null
        controller.close()
    }

    /** Before a recording starts: pause at once, rather than when the session's state arrives. */
    fun holdForRecording() = controller.setBlocked(true)

    /** After a start that did not happen: back to what the session says (it may not emit anything new). */
    fun releaseHold() {
        controller.setBlocked(playbackBlockedBy(RecordingSessions.get(getApplication()).state.value))
    }

    fun onScreenStopped() = controller.suspend()

    override fun onCleared() {
        controller.close()
    }

    private suspend fun tickWhilePlaying() {
        while (currentCoroutineContext().isActive) {
            controller.tick()
            delay(TICK_MS)
        }
    }

    private companion object {
        /** How often the position is read: the most a loop can run past B, and the slider's refresh. */
        const val TICK_MS = 40L
    }
}

/** The player card's callbacks for this player; a loop mark that makes no loop says why, as a toast on [context]. */
fun InAppPlayerViewModel.cardActions(context: Context, openExternally: () -> Unit): RecordingPlayerActions =
    RecordingPlayerActions(
        onTogglePlay = this::togglePlay,
        onSeek = this::seekTo,
        onSpeedChange = this::setSpeed,
        onShiftChange = this::setShift,
        onResetTuning = this::resetTuning,
        onMarkLoopStart = { explainLoopMark(context, markLoopStart()) },
        onMarkLoopEnd = { explainLoopMark(context, markLoopEnd()) },
        onClearLoop = this::clearLoop,
        onOpenExternally = openExternally,
        onClose = this::close,
    )

private fun explainLoopMark(context: Context, result: LoopMarkResult) {
    val message = when (result) {
        LoopMarkResult.NEEDS_START -> R.string.recordings_player_loop_need_a
        LoopMarkResult.BEFORE_START -> R.string.recordings_player_loop_b_before_a
        LoopMarkResult.TOO_SHORT -> R.string.recordings_player_loop_too_short
        LoopMarkResult.SET, LoopMarkResult.UNAVAILABLE -> return
    }
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
