package com.johnchourp.learnbyzantinemusic.trainer.ui

import androidx.compose.runtime.Immutable
import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.IntonationProfile
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.trainer.VoiceTrace

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
    /** The melody holds `MelodySequence.MAX_NOTES` notes: adding stops, and the card says why. */
    val noteLimitReached: Boolean = false,
    /** Phthong currently sounding during Mode 1 playback, or null when not playing. */
    val nowPlayingLabel: String? = null,
    val scale: TrainerScaleUi = TrainerScaleUi(),
    val exercises: TrainerExercisesUi = TrainerExercisesUi(),
    val singAlong: SingAlongUi = SingAlongUi(),
    val waitMode: WaitModeUi = WaitModeUi(),
    /** The syllable being typed for one note, or null (ClickUp `869f5x2cv`). */
    val syllableDialog: SyllableDialogUi? = null,
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
    /** What the line shows for this note: the φθόγγος, or its syllable when «Συλλαβές» is chosen. */
    val lineLabel: String = phthongLabel,
    /** The syllable typed for this note, or null. */
    val syllable: String? = null,
    /** For a note with a syllable, the label the line is not showing — so both stay visible. */
    val otherLabel: String? = null,
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
 * «Ψάλλε μαζί» as the screen draws it (ClickUp `869f5x2cv`). The loop, its timing and its sound are
 * the Activity's; this is only what to show and what the controls hold.
 */
@Immutable
data class SingAlongUi(
    val running: Boolean = false,
    /** There is a line, and no other mode is running. */
    val startEnabled: Boolean = false,
    /** The round being played, from 1, while running; null otherwise. */
    val round: Int? = null,
    /** The guide's level in that round, as a percentage; 0 is silence. */
    val guidePercent: Int? = null,
    /** The lit note as the line shows it, while running — so the card can be read without the line. */
    val nowLabel: String? = null,
    /** The line shows syllables instead of φθόγγοι. */
    val showSyllables: Boolean = false,
    /** At least one note has a syllable; with none, the card says how to add them. */
    val hasSyllables: Boolean = false,
    val melodyPercent: Int = 100,
    val isonPercent: Int = 100,
    val metronomePercent: Int = 100,
    /** The φθόγγος the ison holds, already labelled — the ήχος's base, or Νη for «Διατονικός». */
    val isonLabel: String = "",
)

/**
 * «Παραλλαγή με αναμονή» as the screen draws it (ClickUp `869f5x2cd`). The waiting, the verdicts and
 * the stars are `WaitModeEvaluator`'s and `WaitModeScore`'s; this is only what to show, already worded.
 */
@Immutable
data class WaitModeUi(
    val running: Boolean = false,
    /** There is a line, and no other mode is running. */
    val startEnabled: Boolean = false,
    /** The φθόγγος the line waits on, as the line shows it; null when not running. */
    val targetLabel: String? = null,
    /** Where the voice is, in words: how many μόρια higher or lower, «Κράτα…», or silence. */
    val guidance: String? = null,
    /** The voice is on the φθόγγος now, within the tolerance. */
    val onTarget: Boolean = false,
    /** How much of the hold is done, 0 … 1. */
    val holdProgress: Float = 0f,
    /** The last seconds of the voice, as offsets from the target in μόρια, oldest first. */
    val trace: List<VoiceTrace.Point> = emptyList(),
    /** The rungs around the target, as offsets from it in μόρια; 0 is the target itself. */
    val rungOffsets: List<Double> = emptyList(),
    /** The ± μόρια of this run, from the narrowing levels of `IntonationProfile`. */
    val toleranceMoria: Double = IntonationProfile.IN_TUNE_MORIA,
    /** «Ανοχή ±3 μόρια», already printed. */
    val toleranceLabel: String = "",
    /** The ison sounds during the exercise; off unless the learner turns it on. */
    val isonOn: Boolean = false,
    /** The last run's stars and what they came from, or null before the first run ends. */
    val result: WaitResultUi? = null,
    /** Why it could not start or stopped, when it did; null otherwise. */
    val status: String? = null,
)

/** The end of a run: its stars, and two lines that explain them and the next tolerance. */
@Immutable
data class WaitResultUi(
    val stars: Int,
    val summary: String,
    val nextLevel: String,
)

/** The three sounds of «Ψάλλε μαζί» that have a volume of their own. */
enum class SingAlongSound { MELODY, ISON, METRONOME }

/** The syllable dialog of one note: which note, how it is named, and what is typed so far. */
@Immutable
data class SyllableDialogUi(
    val index: Int,
    val phthongLabel: String,
    val syllable: String,
)

/**
 * The scale the Trainer plays and listens on (ClickUp `869f5x24v`): the ήχος — null for
 * «Διατονικός», the default — and its «Μεταφορά βάσης». Both are read-only while a mode is running.
 */
@Immutable
data class TrainerScaleUi(
    val mode: Mode? = null,
    val baseShiftMoria: Int = BaseShift.DEFAULT_MORIA,
    val enabled: Boolean = true,
    /** The voice's global shift from «Βρες τη φωνή σου», on top of every scale (ClickUp `869f5x2dd`). */
    val globalShiftMoria: Int = BaseShift.DEFAULT_MORIA,
) {
    /** «Διατονικός» is fixed at Νη = 220 Hz; only a chosen ήχος can be transposed. */
    val baseShiftEditable: Boolean get() = enabled && mode != null
}

/**
 * «Οι ασκήσεις μου» as the screen draws it (ClickUp `869f5x261`). The rules and the stored form are
 * `ExerciseBook`'s; this is only what to show.
 */
@Immutable
data class TrainerExercisesUi(
    val items: List<ExerciseItemUi> = emptyList(),
    /** «Αποθήκευση ως…» works: there is a melody, no mode is running, and this version may write the list. */
    val saveEnabled: Boolean = false,
    /** No mode is running: open, rename and delete work. */
    val enabled: Boolean = true,
    /** A newer version of the app wrote the list: it is left untouched, and the card says so. */
    val newerFormat: Boolean = false,
    /** The one dialog that is open, or null. */
    val dialog: ExerciseDialogUi? = null,
)

/** One saved exercise in the list. [mode] null is «Διατονικός». */
@Immutable
data class ExerciseItemUi(
    val name: String,
    val noteCount: Int,
    val mode: Mode?,
    val bpm: Int,
)

/** The dialogs of «Οι ασκήσεις μου»; at most one is open. */
sealed interface ExerciseDialogUi {
    data class SaveAs(val error: ExerciseNameError? = null) : ExerciseDialogUi
    data class Rename(val name: String, val error: ExerciseNameError? = null) : ExerciseDialogUi
    data class ConfirmOpen(val name: String) : ExerciseDialogUi
    data class ConfirmDelete(val name: String) : ExerciseDialogUi
}

/** Why a name, or a save, was refused; the screen puts it into words. */
enum class ExerciseNameError { BLANK, TOO_LONG, TAKEN, FULL, NEWER_FORMAT }

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
