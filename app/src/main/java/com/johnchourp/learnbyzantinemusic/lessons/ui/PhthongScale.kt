package com.johnchourp.learnbyzantinemusic.lessons.ui

/**
 * The seven phthongs (note names) of the Byzantine diatonic scale together with the
 * Greek-alphabet letter each name is derived from.
 *
 * Pure Kotlin with no Android dependencies, so the octave-wrap logic stays unit-testable.
 * Localized display names live in the UI layer (see `Phthong.nameRes` in
 * `PhthongsNamesScreen`); this enum only models the music theory.
 */
enum class Phthong(val sourceLetter: Char) {
    NI('Η'),
    PA('Α'),
    VOU('Β'),
    GA('Γ'),
    DI('Δ'),
    KE('Ε'),
    ZO('Ζ'),
}

object PhthongScale {
    /**
     * Greek-alphabet order (Α, Β, Γ, Δ, Ε, Ζ, Η) — how the names are *derived* from the
     * first seven letters of the Greek alphabet.
     */
    val byAlphabet: List<Phthong> = listOf(
        Phthong.PA, Phthong.VOU, Phthong.GA, Phthong.DI, Phthong.KE, Phthong.ZO, Phthong.NI,
    )

    /**
     * Ascending scale order within one octave, starting from Νη at the bottom of the
     * staircase: Νη, Πα, Βου, Γα, Δι, Κε, Ζω (then Νη again one octave higher).
     */
    val octave: List<Phthong> = listOf(
        Phthong.NI, Phthong.PA, Phthong.VOU, Phthong.GA, Phthong.DI, Phthong.KE, Phthong.ZO,
    )

    /** Number of distinct phthongs in one octave (7). */
    val size: Int get() = octave.size

    /**
     * The phthong at scale position [index], wrapping across octaves in both directions:
     * the phthongs never stop to the right or to the left. So position 7 is Νη again and
     * position -1 is Ζω.
     */
    fun phthongAt(index: Int): Phthong {
        val n = octave.size
        return octave[((index % n) + n) % n]
    }
}
