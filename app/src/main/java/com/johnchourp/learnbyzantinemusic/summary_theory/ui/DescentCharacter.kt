package com.johnchourp.learnbyzantinemusic.summary_theory.ui

/**
 * The simple descending "quantity characters" (χαρακτήρες ποσότητος κατιόντες) taught on the
 * «Κατιόντες» page, each lowering the voice by a fixed number of φωνές (phthongs):
 *
 *  - Απόστροφος −1  (a plain step down)
 *  - Ελαφρόν    −2  (a step of two phthongs down)
 *  - Υπορροή    −2  (written as one neume but worth two apostrophes — a "run" down two phthongs)
 *  - Χαμηλή     −4  (the deepest single descending character)
 *
 * [voices] is the *magnitude* of the descent (always positive); the page renders it as "−N".
 *
 * Pure Kotlin with no Android dependencies so the interval logic stays unit-testable. Localized
 * names, neume drawables and definitions live in the UI layer (see the `nameRes`, `diagramRes`,
 * `definitionRes` and `cdRes` extensions in `DescentsScreen`).
 */
enum class DescentCharacter(val voices: Int, val hasDefinition: Boolean = false) {
    APOSTROPHOS(1),
    ELAFRON(2),
    YPORROI(2, hasDefinition = true),
    CHAMILI(4);

    companion object {
        /** Ordered Απόστροφος → Ελαφρόν → Υπορροή → Χαμηλή. */
        val all: List<DescentCharacter> = entries.toList()

        /** The deepest descent among the simple characters (Χαμηλή = −4). */
        val maxVoices: Int = entries.maxOf { it.voices }
    }
}
