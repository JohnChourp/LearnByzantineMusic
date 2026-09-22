package com.johnchourp.learnbyzantinemusic.music

/**
 * The seven φθόγγοι, in ascending order within an octave (ClickUp `869f4tpxj`).
 *
 * [displayName] is the **only** place a φθόγγος name is spelled. Everything that used to compare or
 * build the Greek text now compares enum constants instead, so a mistyped "Βου" cannot silently fail
 * to match — it stops compiling.
 */
enum class PhthongName(val displayName: String) {
    NI("Νη"),
    PA("Πα"),
    VOU("Βου"),
    GA("Γα"),
    DI("Δι"),
    KE("Κε"),
    ZO("Ζω");

    companion object {
        /** Boundary parser: display text → the φθόγγος it names, or null when it names none. */
        fun fromDisplayName(value: String): PhthongName? = entries.firstOrNull { it.displayName == value }
    }
}

/**
 * A φθόγγος at a specific octave — the thing the app actually plays, highlights and compares.
 *
 * ## Why this is not a String
 *
 * The scale spans three octaves, so "Πα" appears four times and they are **different pitches**. The
 * app used to carry them as decorated labels (`Πα,` `Πα` `Πα΄` `Πα΄΄`) and compare those strings.
 * That works until someone strips a suffix to "normalise" a name, at which point four distinct
 * pitches collapse into one and the bug is silent — the drone sounds an octave off, or an απήχημα
 * highlights the wrong key.
 *
 * With a type, the octave cannot be dropped by accident: it is a field, not a decoration on text.
 *
 * ## Octave numbering
 *
 * [octave] `0` is the middle register the app treats as home — rendered with no suffix at all.
 * Negative goes down (`,`), positive goes up (`΄` repeated). This mirrors exactly what the screens
 * already displayed; [label] is the single renderer, and `PhthongLabelParityTest` pins it against
 * the previous hand-rolled decoration for every base and octave count.
 */
data class Phthong(val name: PhthongName, val octave: Int = 0) : Comparable<Phthong> {

    /**
     * The display form — the **rendering boundary**. UI text and any stored label go through here;
     * nothing else in the app builds a φθόγγος name by concatenation.
     */
    val label: String
        get() = name.displayName + when {
            octave < 0 -> LOW_SUFFIX.repeat(-octave)
            octave == 0 -> ""
            else -> HIGH_SUFFIX.repeat(octave)
        }

    /** Position in the endless ascending run, so φθόγγοι of different octaves order correctly. */
    private val ordinalInRun: Int get() = octave * PhthongName.entries.size + name.ordinal

    override fun compareTo(other: Phthong): Int = ordinalInRun.compareTo(other.ordinalInRun)

    /** The next φθόγγος up, rolling into the octave above after Ζω. */
    fun next(): Phthong {
        val nextOrdinal = name.ordinal + 1
        return if (nextOrdinal < PhthongName.entries.size) {
            Phthong(PhthongName.entries[nextOrdinal], octave)
        } else {
            Phthong(PhthongName.NI, octave + 1)
        }
    }

    override fun toString(): String = label

    companion object {
        private const val LOW_SUFFIX = ","
        private const val HIGH_SUFFIX = "΄"

        /**
         * Boundary parser: a decorated label back into a φθόγγος, or null when the text names none.
         *
         * Returns null rather than guessing, because the callers that parse labels — the απήχημα
         * teaching strings, and any stored value — would otherwise turn a typo into a confident
         * wrong pitch.
         */
        fun parse(label: String): Phthong? {
            val trimmed = label.trim()
            if (trimmed.isEmpty()) return null

            val lowOctaves = trimmed.count { it == LOW_SUFFIX.single() }
            val highOctaves = trimmed.count { it == HIGH_SUFFIX.single() }
            if (lowOctaves > 0 && highOctaves > 0) return null

            val bare = trimmed.trimEnd(LOW_SUFFIX.single(), HIGH_SUFFIX.single())
            val name = PhthongName.fromDisplayName(bare) ?: return null

            // Suffixes are a tail decoration; anything before the bare name means it is not a label.
            if (bare + trimmed.drop(bare.length) != trimmed) return null

            return Phthong(name, highOctaves - lowOctaves)
        }
    }
}
