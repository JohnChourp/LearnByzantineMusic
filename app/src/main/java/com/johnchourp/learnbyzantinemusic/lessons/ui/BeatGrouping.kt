package com.johnchourp.learnbyzantinemusic.lessons.ui

/**
 * The three rhythmic groupings taught on the «Δίσημος, Τρίσημος, Τετράσημος» page.
 *
 * A grouping spans [beats] χρόνοι (beats) between two measure bars, and exactly that many
 * phthongs are sung inside it: δίσημος = 2, τρίσημος = 3, τετράσημος = 4.
 *
 * Pure Kotlin with no Android dependencies so the beat/cycle logic stays unit-testable.
 * Localized names, definitions and diagrams live in the UI layer (see the `nameRes`,
 * `definitionRes` and `diagramRes` extensions in `DuotrioquatroScreen`).
 */
enum class BeatGrouping(val beats: Int) {
    DISIMOS(2),
    TRISIMOS(3),
    TETRASIMOS(4);

    /** Beat numbers 1..[beats], used to lay out the metronome row. */
    val beatNumbers: List<Int> get() = (1..beats).toList()

    /**
     * The next beat (1-based) in an endlessly repeating cycle: after the last beat the
     * group starts again at 1, because the grouping repeats every [beats] χρόνους.
     */
    fun nextBeat(current: Int): Int = if (current >= beats) 1 else current + 1

    companion object {
        /** Ordered δίσημος → τρίσημος → τετράσημος. */
        val all: List<BeatGrouping> = listOf(DISIMOS, TRISIMOS, TETRASIMOS)

        /** The grouping with the given beat count (2, 3 or 4). */
        fun ofBeats(beats: Int): BeatGrouping = all.first { it.beats == beats }
    }
}
