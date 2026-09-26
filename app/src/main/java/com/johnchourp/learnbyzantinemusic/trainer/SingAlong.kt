package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.modes.IsonDrone
import com.johnchourp.learnbyzantinemusic.music.Phthong
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import kotlin.math.roundToInt

/**
 * «Ψάλλε μαζί» (ClickUp `869f5x2cv`, J2): the written line plays in a loop with the current φθόγγος
 * lit, and the guide melody fades round by round — 100 %, 50 %, then silence — while the ison and the
 * metronome go on, until «Στάση». Nothing stops by itself. It is the cheapest step from copying a
 * line to chanting it alone.
 *
 * The timing is not here: notes, clicks and the guide's level per round all come from the one
 * planner the Trainer already uses ([MelodyPlaybackPlanner.planRound]), and are played by the one
 * scheduler that plays the melody ([MelodySequencePlayer.playLoop]). This holds the rest — pure, so
 * it is tested without a device:
 *
 * - **The ison** holds the base of the chosen ήχος — the end of its απήχημα ([IsonDrone.base]) — at
 *   the very frequency the 8 Ήχοι page's ison sounds, «Μεταφορά βάσης» included ([IsonDrone.held]).
 *   For «Διατονικός», which is no ήχος, it holds Νη on the Trainer's own ladder: 220 Hz.
 * - **What the line shows**: the φθόγγοι, or the syllables of the text once the learner has typed
 *   them ([lineLabel]) — the step from the παραλλαγή to the μέλος.
 * - **The three volumes** — melody, ison, metronome — are separate, and start at [Volumes.DEFAULT].
 */
object SingAlong {

    /** The φθόγγος the ison holds on [scale]: its ήχος's base, or Νη for «Διατονικός». */
    fun isonPhthong(scale: TrainerScale): Phthong = scale.mode?.let(IsonDrone::base) ?: Phthong(PhthongName.NI)

    /**
     * Where the ison sounds on [scale], or null when the ladder has no such rung — then no ison is
     * started rather than a guessed pitch.
     */
    fun isonFrequencyHz(scale: TrainerScale): Double? {
        val mode = scale.mode ?: return scale.ladder.stepFor(isonPhthong(scale))?.frequencyHz
        return IsonDrone.held(IsonDrone.Request(mode, scale.baseShiftMoria))?.frequencyHz
    }

    /**
     * What the line shows for [note]: its syllable when [showSyllables] and it has one, otherwise the
     * φθόγγος with its octave marks. A note without a syllable keeps its φθόγγος, so a line with only
     * some syllables typed is still whole.
     */
    fun lineLabel(note: TrainerNote, showSyllables: Boolean): String =
        note.syllable?.takeIf { showSyllables } ?: note.pitch.label

    /** The guide's level in [round] as a whole percentage, as the card prints it. */
    fun guidePercent(round: Int): Int = (MelodyPlaybackPlanner.guideGain(round) * 100).roundToInt()

    /** The three separate levels, each 0 … 1 of that sound's own full level. */
    data class Volumes(
        val melody: Float = 1f,
        val ison: Float = 0.6f,
        val metronome: Float = 0.6f,
    ) {
        init {
            require(melody in 0f..1f && ison in 0f..1f && metronome in 0f..1f) { "volumes are 0 … 1: $this" }
        }

        companion object {
            /** Guide at full level; ison and click a little under it, so the melody leads the first round. */
            val DEFAULT = Volumes()
        }
    }
}
