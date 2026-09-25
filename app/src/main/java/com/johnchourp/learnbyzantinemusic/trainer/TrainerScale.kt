package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.modes.EightModeScaleDefinitions
import com.johnchourp.learnbyzantinemusic.modes.LadderPitchMirror
import com.johnchourp.learnbyzantinemusic.modes.ModeScaleDefinition
import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.ModeLadder
import com.johnchourp.learnbyzantinemusic.music.Moria

/**
 * The scale the Melody Trainer plays **and** listens on (ClickUp `869f5x24v`, F2).
 *
 * ## Why
 *
 * Until F2 the Trainer judged every voice against one fixed table — the natural diatonic scale with
 * Νη = 220 Hz (`TrainerPitchTable`) — so a singer at home a little lower than that was «έξω» on
 * every note, and a correct Δι of the soft chromatic was 4 μόρια off. The 8 Ήχοι page had already
 * solved both, with a per-mode ladder and its «Μεταφορά βάσης». This puts the Trainer on the same
 * [ModeLadder]: the one the 8 Ήχοι diagram draws, the ison sounds and «Πού είμαι» reads. There is no
 * third table of pitches.
 *
 * - **Playback:** [frequencyHz] looks each note up on the ladder.
 * - **Listening:** [match] folds the sung frequency into the ladder's range (the octave a singer
 *   chooses is ignored, as before), reads it with [LadderPitchMirror.read] and hands the evaluators
 *   the same [PitchMatch] they have always taken — so they, and their tests, are unchanged.
 *
 * ## The default is today's Trainer
 *
 * [mode] null is «Διατονικός»: [EightModeScaleDefinitions.DIATONIC] at shift 0, which is exactly
 * the old table — every one of the Trainer's 21 notes sounds at the same frequency, bit for bit, and
 * every sung pitch is judged the same (`TrainerScaleDefaultIsTodaysTableTest`). Its shift is always
 * 0: the default stays what it always was, and needs no new stored preference. To transpose, the
 * singer picks a ήχος; its shift is **that mode's own key**, `AppPrefs.baseShiftKeyName`, shared
 * with the 8 Ήχοι page, so both screens sing the same ήχος at the same height.
 *
 * ## The ladder's range
 *
 * The Trainer writes notes from Νη one octave down to Ζω one octave up ([MIN_NOTE_OCTAVE] ..
 * [MAX_NOTE_OCTAVE]). A Πα-based ladder built the 8 Ήχοι way starts at Πα one octave down and would
 * miss that lowest Νη, so the Trainer's ladder starts one octave lower still ([LOWEST_OCTAVE]) and
 * spans [OCTAVES] octaves. `TrainerScaleCoversEveryNoteTest` resolves every note in every mode
 * across the whole shift range.
 *
 * Pure Kotlin — [mode] and [baseShiftMoria] are the whole state, which is what ClickUp `869f5x261`
 * (F6) will save with a melody.
 */
data class TrainerScale(
    /** The ήχος, or null for «Διατονικός», the default. */
    val mode: Mode? = null,
    /** The «Μεταφορά βάσης», in μόρια; always 0 for «Διατονικός». */
    val baseShiftMoria: Int = BaseShift.DEFAULT_MORIA,
) {
    init {
        require(baseShiftMoria in BaseShift.RANGE) {
            "base shift $baseShiftMoria is outside ${BaseShift.RANGE}"
        }
        require(mode != null || baseShiftMoria == BaseShift.DEFAULT_MORIA) {
            "«Διατονικός» is the Trainer's fixed default at shift 0; pick a mode to transpose"
        }
    }

    /** The interval table: the mode's own ([Mode.scale]), or the natural diatonic one for the default. */
    val definition: ModeScaleDefinition
        get() = mode?.scale ?: EightModeScaleDefinitions.DIATONIC

    /** The ladder every pitch of this scale comes from, built once. */
    val ladder: ModeLadder by lazy {
        definition.ladder(octaves = OCTAVES, baseShift = Moria(baseShiftMoria), lowestOctave = LOWEST_OCTAVE)
    }

    /** Where [note] sounds on this scale. */
    fun frequencyHz(note: TrainerNote): Double =
        requireNotNull(ladder.stepFor(note.pitch)) {
            "${note.pitch.label} is outside the Trainer's ladder for $this"
        }.frequencyHz

    /**
     * The φθόγγος a sung [frequencyHz] is closest to on this scale, with its deviation in μόρια —
     * the evaluators' input — or null when the input is not a usable pitch.
     *
     * The octave the singer is in does not count, exactly as before F2: the pitch is folded by whole
     * octaves into the ladder's span first. Multiplying by 2 is exact in floating point, and the
     * ladder repeats every octave, so the nearest rung is the same as in the singer's own octave.
     * [PitchMatch.frequencyHz] keeps the frequency as sung.
     */
    fun match(frequencyHz: Double): PitchMatch? {
        if (frequencyHz <= 0.0 || !frequencyHz.isFinite()) return null
        val reading = LadderPitchMirror.read(ladder, foldIntoLadder(frequencyHz)) ?: return null
        return PitchMatch(reading.step.phthong.name, reading.deviationMoria, frequencyHz)
    }

    private fun foldIntoLadder(frequencyHz: Double): Double {
        // steps run highest first; the ladder spans several octaves, so both loops end inside it.
        val lowest = ladder.steps.last().frequencyHz
        val highest = ladder.steps.first().frequencyHz
        var folded = frequencyHz
        while (folded < lowest) folded *= 2.0
        while (folded > highest) folded /= 2.0
        return folded
    }

    companion object {
        /** «Διατονικός»: the Trainer as it always was. */
        val DIATONIC = TrainerScale()

        /** The lowest octave a Trainer note can be written in: Νη one octave down. */
        const val MIN_NOTE_OCTAVE = -1

        /** The highest octave a Trainer note can be written in: up to Ζω one octave up. */
        const val MAX_NOTE_OCTAVE = 1

        /** The octave of the base φθόγγος the Trainer's ladder starts from — one below the diagram's. */
        const val LOWEST_OCTAVE = -2

        /** Octaves the Trainer's ladder spans: from [LOWEST_OCTAVE] up past the highest note. */
        const val OCTAVES = 4
    }
}
