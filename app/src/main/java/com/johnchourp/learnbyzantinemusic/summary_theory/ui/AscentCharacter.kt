package com.johnchourp.learnbyzantinemusic.summary_theory.ui

/**
 * The simple ascending "quantity characters" (χαρακτήρες ποσότητος ανιόντες) taught on the
 * «Ανιόντες» page, each raising the voice by a fixed number of φωνές (phthongs):
 *
 *  - Ίσον      0  (stays on the same phthong)
 *  - Ολίγον   +1  (a plain step up)
 *  - Πεταστή  +1  (an emphatic step up)
 *  - Κεντήματα +1 (a gentle, connecting step up)
 *  - Κέντημα  +2
 *  - Υψηλή    +4
 *
 * Pure Kotlin with no Android dependencies so the interval logic stays unit-testable.
 * Localized names, neume drawables and definitions live in the UI layer (see the `nameRes`,
 * `diagramRes`, `definitionRes` and `cdRes` extensions in `AscentsScreen`).
 */
enum class AscentCharacter(val voices: Int, val hasDefinition: Boolean = false) {
    ISON(0),
    OLIGON(1),
    PETASTI(1, hasDefinition = true),
    KENTIMATA(1, hasDefinition = true),
    KENTIMA(2),
    YPSILI(4);

    companion object {
        /** Ordered Ίσον → Ολίγον → Πεταστή → Κεντήματα → Κέντημα → Υψηλή. */
        val all: List<AscentCharacter> = entries.toList()

        /** The largest voice rise among the simple characters (Υψηλή = +4). */
        val maxVoices: Int = entries.maxOf { it.voices }
    }
}
