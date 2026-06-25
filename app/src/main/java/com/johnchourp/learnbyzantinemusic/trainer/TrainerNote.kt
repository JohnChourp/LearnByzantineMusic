package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.analysis.ByzantineRhythmMapper

/**
 * One entry in a trainer melody: a phthong at an octave shift, a base duration in
 * χρόνοι (beats), and optional Byzantine rhythm modifiers (γοργόν, κλάσμα, …). The
 * modifier strings reuse the constants from [ByzantineRhythmMapper] so the trainer and
 * the notation analyzer interpret rhythm identically.
 */
data class TrainerNote(
    val phthong: TrainerPhthong,
    val octaveShift: Int = 0,
    val baseDurationBeats: Float = 1f,
    val modifiers: List<String> = emptyList()
) {
    val frequencyHz: Double get() = TrainerPitchTable.frequencyHz(phthong, octaveShift)

    val hasGorgo: Boolean get() = modifiers.contains(ByzantineRhythmMapper.MODIFIER_GORGO)
    val hasFraction: Boolean get() = modifiers.contains(ByzantineRhythmMapper.MODIFIER_FRACTION)

    fun withGorgo(enabled: Boolean): TrainerNote = withModifier(ByzantineRhythmMapper.MODIFIER_GORGO, enabled)
    fun withFraction(enabled: Boolean): TrainerNote = withModifier(ByzantineRhythmMapper.MODIFIER_FRACTION, enabled)

    private fun withModifier(modifier: String, enabled: Boolean): TrainerNote {
        val present = modifiers.contains(modifier)
        if (present == enabled) return this
        val updated = if (enabled) modifiers + modifier else modifiers - modifier
        return copy(modifiers = updated)
    }
}
