package com.johnchourp.learnbyzantinemusic.modes

/**
 * Which φθόγγος the ισοκράτημα holds, and at what frequency (ClickUp `869f4tpeu`).
 *
 * Byzantine music is chanted **over** a drone, so practising a scale without one trains the wrong
 * thing: the ear never learns the interval against the base. The 8 Ήχοι page could only sound a
 * pitch while a finger held it down, which is exactly the one situation where you cannot also sing.
 *
 * ## Why this is a lookup and not a second calculation
 *
 * The drone must sound the *same* pitch as the diagram's base key — including after the «Μεταφορά
 * βάσης» slider moves it. The safe way to guarantee that is not to recompute the frequency from the
 * μόρια a second time, but to **index into the frequency list the diagram itself is drawn from**.
 * Then there is no arithmetic that can disagree, however the transposition is applied later.
 *
 * ## Which occurrence of the base
 *
 * The scale spans three octaves, so the mode's base φθόγγος appears four times, labelled by octave:
 * `Πα,` `Πα` `Πα΄` `Πα΄΄`. The undecorated label is the middle one, and that is the ison — low enough
 * to sing against, high enough to hear under a voice.
 */
object IsonDrone {

    /**
     * Index of the mode's base φθόγγος in a **top-to-bottom** label list, or `-1` when absent.
     *
     * Matches the bare label exactly, so the octave decorations (`,` and `΄`) pick out the middle
     * octave on their own — no suffix stripping, which would match all four occurrences.
     */
    fun baseLabelIndex(phthongsTopToBottom: List<String>, basePhthong: String): Int =
        phthongsTopToBottom.indexOf(basePhthong)

    /**
     * Frequency the drone should hold, taken from [frequenciesTopToBottom] — the very list the
     * diagram sounds — or `null` when the base is not present, in which case the caller must not
     * start a drone rather than guess a pitch.
     */
    fun frequencyHz(
        phthongsTopToBottom: List<String>,
        frequenciesTopToBottom: List<Double>,
        basePhthong: String,
    ): Double? {
        val index = baseLabelIndex(phthongsTopToBottom, basePhthong)
        if (index !in frequenciesTopToBottom.indices) return null
        return frequenciesTopToBottom[index]
    }
}
