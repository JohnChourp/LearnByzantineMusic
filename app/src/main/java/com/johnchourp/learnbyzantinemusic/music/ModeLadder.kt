package com.johnchourp.learnbyzantinemusic.music

/**
 * A mode's φθόγγοι together with the pitch each one sounds (ClickUp `869f4tpxj`).
 *
 * ## The duplication this removes
 *
 * Three places on the «8 Ήχοι» screen needed the same thing — the scale diagram, the ison drone and
 * the απήχημα playback — and each rebuilt it with the same four lines: take the ascending φθόγγοι,
 * reverse them, find where Νη sits, ask for the frequencies. Three copies of an ordering convention
 * (**top to bottom**, because the diagram is drawn downwards) and three chances for one of them to
 * drift. The drone and the diagram silently disagreeing about a pitch is exactly the bug the typed
 * model is meant to make impossible.
 *
 * Now there is one ladder. [stepFor] answers "which pitch is this φθόγγος", and the positional list
 * is available for the diagram that genuinely needs indices.
 *
 * ## Ordering
 *
 * [steps] runs **highest first**, matching how the diagram is laid out. That convention is stated
 * once here instead of living in a `.reversed()` at each call site.
 */
class ModeLadder private constructor(
    /** Highest φθόγγος first. */
    val steps: List<Step>,
) {
    /** One rung: a φθόγγος, how far it sits from Νη, and the pitch it sounds. */
    data class Step(
        val phthong: Phthong,
        val moriaFromNi: Moria,
        val frequencyHz: Double,
    )

    val phthongi: List<Phthong> get() = steps.map { it.phthong }

    /** Rendering boundary: the labels the diagram prints, in the same order as [steps]. */
    val labels: List<String> get() = steps.map { it.phthong.label }

    val frequencies: List<Double> get() = steps.map { it.frequencyHz }

    /**
     * The rung for [phthong], matched **by value including octave**, or null when this mode's ladder
     * does not contain it. Null rather than a guess: a caller that cannot find its φθόγγος must stay
     * silent rather than sound an arbitrary pitch.
     */
    fun stepFor(phthong: Phthong): Step? = steps.firstOrNull { it.phthong == phthong }

    /** Convenience for the one φθόγγος every mode is anchored on. */
    fun stepForBase(base: Phthong): Step? = stepFor(base)

    companion object {
        /**
         * Builds the ladder for a scale.
         *
         * @param ascendingPhthongi the φθόγγοι low to high — the scale's own ladder.
         * @param ascendingIntervals μόρια between successive φθόγγοι, one fewer than the φθόγγοι.
         * @param referenceMoria where the reference Νη sits in that ascending run.
         * @param baseShift the per-mode «Μεταφορά βάσης», folded in **before** the frequency is
         *   computed so a transposed ladder is the same arithmetic as an untransposed one.
         */
        fun build(
            ascendingPhthongi: List<Phthong>,
            ascendingIntervals: List<Moria>,
            referenceMoria: Moria,
            baseShift: Moria = Moria.ZERO,
        ): ModeLadder {
            require(ascendingPhthongi.size == ascendingIntervals.size + 1) {
                "a ladder of ${ascendingPhthongi.size} φθόγγοι needs ${ascendingPhthongi.size - 1} " +
                    "intervals, got ${ascendingIntervals.size}"
            }
            var cumulative = Moria.ZERO
            val ascending = ArrayList<Step>(ascendingPhthongi.size)
            ascendingPhthongi.forEachIndexed { index, phthong ->
                if (index > 0) cumulative += ascendingIntervals[index - 1]
                val fromNi = cumulative - referenceMoria + baseShift
                ascending.add(Step(phthong, fromNi, fromNi.toFrequencyHz()))
            }
            return ModeLadder(ascending.reversed())
        }
    }
}
