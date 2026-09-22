package com.johnchourp.learnbyzantinemusic.modes

import com.johnchourp.learnbyzantinemusic.music.ModeLadder
import com.johnchourp.learnbyzantinemusic.music.Phthong

/**
 * Turns the απήχημα teaching string into a playable sequence (ClickUp `869f4tpkv`).
 *
 * The page already shows the απήχημα as syllables paired with the φθόγγος each one is sung on:
 *
 *     Α(Πα) - να(Βου) - νές(Πα)
 *
 * That string is **the** source of truth for the text, so the sequence is parsed straight out of it
 * rather than duplicated into a second table. A second table would drift the moment someone corrects
 * a φθόγγος in the visible text and not in the hidden copy — and the drift would be inaudible until
 * a learner trusted the wrong one.
 *
 * The φθόγγος labels carry their octave decoration (`Νη΄`, `Πα,`) exactly as the scale diagram's
 * labels do, so resolving a step to a pitch is a lookup in the diagram's own label list — the same
 * discipline as [IsonDrone].
 */
object ApichimaSequence {

    /**
     * One syllable of the απήχημα and the φθόγγος it is sung on.
     *
     * [phthongLabel] is the text exactly as the teaching string wrote it; [phthong] is that text
     * parsed at the boundary, or null when it names no φθόγγος. Both are kept because the label is
     * what the page displays and the type is what the pitch lookup uses — deriving one from the
     * other at each call site is how they would drift.
     */
    data class Step(val syllable: String, val phthongLabel: String) {
        val phthong: Phthong? get() = Phthong.parse(phthongLabel)
    }

    private val STEP = Regex("""^\s*(.+?)\s*\(\s*([^)]+?)\s*\)\s*$""")

    /**
     * Parses a teaching string into steps. Segments that do not carry a φθόγγος in brackets are
     * skipped rather than guessed at, so a malformed entry loses that syllable instead of sounding
     * an arbitrary pitch.
     */
    fun parse(teachingText: String): List<Step> =
        teachingText
            .split("-")
            .mapNotNull { segment ->
                STEP.find(segment)?.let { match ->
                    val syllable = match.groupValues[1].trim()
                    val phthong = match.groupValues[2].trim()
                    if (syllable.isEmpty() || phthong.isEmpty()) null else Step(syllable, phthong)
                }
            }

    /**
     * Frequencies for [steps], looked up in [frequenciesTopToBottom] through
     * [phthongsTopToBottom] — the very lists the diagram is drawn from.
     *
     * Returns null when any step's φθόγγος is not on this mode's ladder. All-or-nothing on purpose:
     * a sequence with a silent hole in it teaches the απήχημα wrong, so the button should stay
     * disabled instead.
     */
    /**
     * Typed primary: the pitch for each step, taken from [ladder]. Null when any step's φθόγγος is
     * absent from this mode's ladder — all-or-nothing, because a phrase with a silent hole teaches
     * the απήχημα wrong.
     */
    fun frequencies(steps: List<Step>, ladder: ModeLadder): List<Double>? {
        if (steps.isEmpty()) return null
        return steps.map { step ->
            val phthong = step.phthong ?: return null
            ladder.stepFor(phthong)?.frequencyHz ?: return null
        }
    }

    /** Boundary overload for callers holding parallel label/frequency lists. */
    fun frequencies(
        steps: List<Step>,
        phthongsTopToBottom: List<String>,
        frequenciesTopToBottom: List<Double>,
    ): List<Double>? {
        if (steps.isEmpty()) return null
        val resolved = steps.map { step ->
            val index = phthongsTopToBottom.indexOf(step.phthongLabel)
            if (index !in frequenciesTopToBottom.indices) return null
            frequenciesTopToBottom[index]
        }
        return resolved
    }

    /** Playback speeds the page offers. Milliseconds a single syllable is held. */
    enum class Speed(val millisPerStep: Long) {
        /** «σύντομο» — the way an απήχημα is normally intoned before a hymn. */
        SHORT(520),

        /** «αργό» — slow enough to sing along with while learning. */
        SLOW(1_100),
    }
}
