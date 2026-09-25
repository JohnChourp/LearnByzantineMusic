package com.johnchourp.learnbyzantinemusic.lessons.ui

import com.johnchourp.learnbyzantinemusic.music.PhthongName

/**
 * The seven phthongs (note names) of the Byzantine diatonic scale in the two orders the «Ονόματα
 * Φθόγγων» lesson teaches, over the app's one φθόγγος model, [PhthongName] — which also carries the
 * Greek-alphabet letter each name is derived from ([PhthongName.sourceLetter]). This lesson used to
 * keep an enum of its own; it was folded into [PhthongName] (ClickUp `869f5x291`).
 *
 * Pure Kotlin with no Android dependencies, so the octave-wrap logic stays unit-testable.
 * Localized display names live in the UI layer (see `PhthongName.nameRes` in
 * `PhthongsNamesScreen`): this page is theory prose, and plays no note, so English may transliterate.
 */
object PhthongScale {
    /**
     * Greek-alphabet order (Α, Β, Γ, Δ, Ε, Ζ, Η) — how the names are *derived* from the
     * first seven letters of the Greek alphabet.
     */
    val byAlphabet: List<PhthongName> = listOf(
        PhthongName.PA, PhthongName.VOU, PhthongName.GA, PhthongName.DI,
        PhthongName.KE, PhthongName.ZO, PhthongName.NI,
    )

    /**
     * Ascending scale order within one octave, starting from Νη at the bottom of the
     * staircase: Νη, Πα, Βου, Γα, Δι, Κε, Ζω (then Νη again one octave higher).
     */
    val octave: List<PhthongName> = PhthongName.entries

    /** Number of distinct phthongs in one octave (7). */
    val size: Int get() = octave.size

    /**
     * The phthong at scale position [index], wrapping across octaves in both directions:
     * the phthongs never stop to the right or to the left. So position 7 is Νη again and
     * position -1 is Ζω.
     */
    fun phthongAt(index: Int): PhthongName {
        val n = octave.size
        return octave[((index % n) + n) % n]
    }
}
