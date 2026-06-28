package com.johnchourp.learnbyzantinemusic.trainer.ui

import androidx.compose.runtime.Immutable

/**
 * Immutable snapshot of everything the redesigned Melody Trainer screen renders. The host
 * [com.johnchourp.learnbyzantinemusic.trainer.MelodyTrainerActivity] owns the real state
 * (melody, audio, mic sessions, timing) and rebuilds this object after every change; the
 * Compose [MelodyTrainerScreen] is a pure function of it. Because it is a data class the
 * screen only recomposes when something actually changed (structural equality), so the
 * ~20 fps rebuilds from the mic loop are cheap when nothing visible moved.
 */
@Immutable
data class MelodyTrainerUiState(
    val notes: List<TrainerNoteUi> = emptyList(),
    /** Locale-formatted total length of the melody in χρόνοι (e.g. "4" or "4,5"). */
    val totalBeatsLabel: String = "0",
    /** Signed octave shift label for new notes (e.g. "+1", "0", "-1"). */
    val octaveLabel: String = "0",
    val octaveDownEnabled: Boolean = false,
    val octaveUpEnabled: Boolean = false,
    val bpm: Int = 0,
    val tempoEnabled: Boolean = true,
    val playEnabled: Boolean = false,
    val stopEnabled: Boolean = false,
    val clearEnabled: Boolean = false,
    val addEnabled: Boolean = true,
    val isPlaybackActive: Boolean = false,
    /** Phthong currently sounding during Mode 1 playback, or null when not playing. */
    val nowPlayingLabel: String? = null,
    val voice: PracticeModeUi = PracticeModeUi(),
    val rhythm: PracticeModeUi = PracticeModeUi(),
    val combo: PracticeModeUi = PracticeModeUi(),
)

/** One note row as the screen needs to draw it. */
@Immutable
data class TrainerNoteUi(
    val index: Int,
    /** Phthong name with octave marks, e.g. "Πα΄" or "Δι,". */
    val phthongLabel: String,
    /** Locale-formatted effective duration in χρόνοι (e.g. "1" or "1,5"). */
    val beatsLabel: String,
    val hasGorgo: Boolean,
    /** False while a mode is running — the whole row is read-only then. */
    val editable: Boolean,
    /** Sung / timed correctly — the row greens. */
    val matched: Boolean,
    /** Currently playing or expected — the row glows amber. */
    val active: Boolean,
) {
    /** Duration ±  only when editable and not turned into a γοργόν (which fixes the length). */
    val durationEditable: Boolean get() = editable && !hasGorgo

    /** γοργόν shortens the *previous* note, so it is invalid on the first row. */
    val gorgoEnabled: Boolean get() = editable && index > 0
}

/** State of one of the three mutually-exclusive practice modes. */
@Immutable
data class PracticeModeUi(
    val checked: Boolean = false,
    val enabled: Boolean = true,
    /** Already-resolved status sentence (hint, listening/countdown, or result). */
    val status: String = "",
)
