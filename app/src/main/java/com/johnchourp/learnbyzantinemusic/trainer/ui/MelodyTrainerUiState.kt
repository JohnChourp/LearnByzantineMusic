package com.johnchourp.learnbyzantinemusic.trainer.ui

import androidx.compose.runtime.Immutable
import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.Mode

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
    /** Phthong currently sounding during Mode 1 playback, or null when not playing. */
    val nowPlayingLabel: String? = null,
    val scale: TrainerScaleUi = TrainerScaleUi(),
    val voice: PracticeModeUi = PracticeModeUi(),
    val rhythm: PracticeModeUi = PracticeModeUi(),
    val combo: PracticeModeUi = PracticeModeUi(),
    /** The numbers of the «Κανόνες χρόνου» card, already printed. */
    val ruleNumbers: TimingRuleNumbersUi = TimingRuleNumbersUi(),
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
    /** `MelodySequence.canChangeLength` for this row: the rule lives there, not here. */
    val lengthChangeable: Boolean,
    /** `MelodySequence.canToggleGorgon` for this row: never the first note, and that rule lives there too. */
    val gorgoToggleable: Boolean,
) {
    /** Duration ± only while no mode is running and the melody's rules allow it. */
    val durationEditable: Boolean get() = editable && lengthChangeable

    /** The γοργόν chip only while no mode is running and the melody's rules allow it. */
    val gorgoEnabled: Boolean get() = editable && gorgoToggleable
}

/**
 * The scale the Trainer plays and listens on (ClickUp `869f5x24v`): the ήχος — null for
 * «Διατονικός», the default — and its «Μεταφορά βάσης». Both are read-only while a mode is running.
 */
@Immutable
data class TrainerScaleUi(
    val mode: Mode? = null,
    val baseShiftMoria: Int = BaseShift.DEFAULT_MORIA,
    val enabled: Boolean = true,
) {
    /** «Διατονικός» is fixed at Νη = 220 Hz; only a chosen ήχος can be transposed. */
    val baseShiftEditable: Boolean get() = enabled && mode != null
}

/**
 * The numbers the «Κανόνες χρόνου» card prints, already formatted like the note rows. They come from
 * `TimingRulesHelp` — the time rules — and never from the card's text.
 */
@Immutable
data class TimingRuleNumbersUi(
    val defaultLength: String = "",
    val gorgonNote: String = "",
    val gorgonTakes: String = "",
    val klasmaAdds: String = "",
)

/** State of one of the three mutually-exclusive practice modes. */
@Immutable
data class PracticeModeUi(
    val checked: Boolean = false,
    val enabled: Boolean = true,
    /** Already-resolved status sentence (hint, listening/countdown, or result). */
    val status: String = "",
    /** Live "correct so far" label while this mode is running, else null. */
    val progress: String? = null,
)
