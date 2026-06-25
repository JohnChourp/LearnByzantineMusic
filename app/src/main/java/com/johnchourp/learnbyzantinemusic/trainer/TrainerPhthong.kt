package com.johnchourp.learnbyzantinemusic.trainer

/**
 * The seven Byzantine phthongi (notes) in ascending order, each tagged with its
 * position in the natural diatonic scale measured in moria above base Νη.
 *
 * Byzantine theory divides the octave into 72 moria. In the natural diatonic scale
 * the phthongi sit at these cumulative moria above Νη:
 *
 *     Νη 0, Πα 12, Βου 22, Γα 30, Δι 42, Κε 54, Ζω 64   (Νη' = 72 closes the octave)
 *
 * These positions match the diatonic genus used by the 8 Ήχοι screen.
 */
enum class TrainerPhthong(val displayName: String, val diatonicMoriaFromNi: Int) {
    NI("Νη", 0),
    PA("Πα", 12),
    VOU("Βου", 22),
    GA("Γα", 30),
    DI("Δι", 42),
    KE("Κε", 54),
    ZO("Ζω", 64);

    companion object {
        /** The phthongi in ascending pitch order (Νη … Ζω). */
        val ascending: List<TrainerPhthong> = values().toList()

        fun fromDisplayName(displayName: String): TrainerPhthong? =
            values().firstOrNull { it.displayName == displayName }
    }
}
