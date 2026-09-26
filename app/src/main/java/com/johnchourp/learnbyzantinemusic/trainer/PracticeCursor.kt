package com.johnchourp.learnbyzantinemusic.trainer

/**
 * Where the learner is in the written line: which note is lit, and which round of the line this is
 * (ClickUp `869f5x2cv`, J2). One cursor for the two ways of practising a line:
 *
 * - **«Ψάλλε μαζί» (J2):** the timeline moves it. Each note the loop plays puts the cursor on it
 *   ([at]), round after round, whether or not the learner sings.
 * - **«Παραλλαγή με αναμονή» (J1, ClickUp `869f5x2cd`):** the voice will move it — one note forward
 *   ([advance]) only once that note is sung, and never by itself.
 *
 * Pure and immutable: every move returns a new cursor, so what is lit and what the next move will do
 * can be tested without a screen, a clock or a microphone.
 */
data class PracticeCursor(
    /** How many notes the line has. */
    val noteCount: Int,
    /** The lit note, 0-based; always 0 for an empty line. */
    val index: Int = 0,
    /** Which pass over the line this is, 0-based. */
    val round: Int = 0,
) {
    init {
        require(noteCount >= 0) { "noteCount $noteCount" }
        require(round >= 0) { "round $round" }
        require(index == 0 || index in 0 until noteCount) { "index $index of $noteCount" }
    }

    /** The lit note, or null when the line has no notes. */
    val current: Int? get() = if (noteCount == 0) null else index

    /** True on the line's last note: the next [advance] starts the next round. */
    val isOnLastNote: Boolean get() = noteCount > 0 && index == noteCount - 1

    /** The next note; after the last one, the first note of the next round. An empty line stays put. */
    fun advance(): PracticeCursor = when {
        noteCount == 0 -> this
        isOnLastNote -> copy(index = 0, round = round + 1)
        else -> copy(index = index + 1)
    }

    /** Note [index] of round [round] — where the timeline says the loop is. */
    fun at(round: Int, index: Int): PracticeCursor = copy(round = round, index = index)

    companion object {
        /** The first note of the first round of a line of [noteCount] notes. */
        fun start(noteCount: Int): PracticeCursor = PracticeCursor(noteCount)
    }
}
