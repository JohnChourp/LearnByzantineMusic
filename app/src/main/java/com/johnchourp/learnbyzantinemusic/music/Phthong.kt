package com.johnchourp.learnbyzantinemusic.music

/**
 * The seven φθόγγοι, in ascending order within an octave (ClickUp `869f4tpxj`) — the **one** model of
 * a φθόγγος in the app (ClickUp `869f5x291`, H2).
 *
 * [displayName] is the **only** place a φθόγγος name is spelled. Everything that used to compare or
 * build the Greek text now compares enum constants instead, so a mistyped "Βου" cannot silently fail
 * to match — it stops compiling.
 *
 * There used to be three of these enums: this one, the Melody Trainer's `TrainerPhthong` (with its own
 * copy of the Greek names) and the lessons' own `Phthong` (with [sourceLetter]). Both were folded in
 * here; `OnePhthongModelTest` fails if a second enum of the seven φθόγγοι appears.
 *
 * ## The constant names and their order are stored — never rename or reorder them
 *
 * The recording analysis writes these **constant names** into its preferences — `"NI,PA,VOU"` for an
 * expected melody, `"KE"` for a starting φθόγγος (`StoredPhthongs`) — and places each φθόγγος on a
 * mode's scale **by ordinal** (`ModeScalePositions`, `PhthongSegmenter`). Renaming a constant makes
 * a user's saved melody drop that note on read; reordering moves every φθόγγος to its neighbour's
 * pitch. `PhthongNamesAreFrozenTest` pins both.
 *
 * ## When a name is translated
 *
 * On every screen that **plays or scores** a note — the 8 Ήχοι diagram, the ison, the απήχημα,
 * «Πού είμαι», the Melody Trainer and the recording analysis — the φθόγγοι are written in Greek
 * script, in every UI language: [displayName] and [Phthong.label], never a string resource. Those
 * labels are also what the app parses back (`ApichimaSequence` reads its pitches from the Greek
 * teaching string only), so a translated label would name no φθόγγος at all.
 *
 * Theory **prose** may transliterate: the English lessons say Ni, Pa, Vou… (the `phthong_*` string
 * resources) because they are text about the names, not a place where a note sounds or is judged.
 *
 * ## Where a φθόγγος sits: one table per screen
 *
 * The names are one model; their **positions** still come from three tables, by screen:
 *
 * | Screen | Table | Why |
 * |---|---|---|
 * | Melody Trainer: playback, voice check, φθόγγος + time | `TrainerPitchTable`: the natural diatonic scale, Νη = 220 Hz | the Trainer has no mode or base shift yet; ClickUp `869f5x24v` (F2) moves it onto [ModeLadder] |
 * | Recording analysis | `ModeScalePositions`: the chosen mode's scale | a recording has no absolute pitch, so the scale is anchored to the singer's first steady note |
 * | 8 Ήχοι diagram, ison, απήχημα, «Πού είμαι» | [ModeLadder]: the mode's scale with its «Μεταφορά βάσης» | what is drawn, sounded and read back must be one object |
 *
 * The last two are built from the same interval tables (`EightModeScaleDefinitions`), and the
 * Trainer's diatonic positions equal the diatonic table (`ModeScalePositionsTest`).
 */
enum class PhthongName(
    val displayName: String,
    /** The letter of the Greek alphabet the name comes from: Πα from Α, Βου from Β … Νη from Η. */
    val sourceLetter: Char,
) {
    NI("Νη", 'Η'),
    PA("Πα", 'Α'),
    VOU("Βου", 'Β'),
    GA("Γα", 'Γ'),
    DI("Δι", 'Δ'),
    KE("Κε", 'Ε'),
    ZO("Ζω", 'Ζ');

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
 * already displayed; [label] is the single renderer, and `PhthongRenderingParityTest` pins it against
 * the previous hand-rolled decoration for every base and octave count.
 *
 * **One mark per direction, one renderer** (ClickUp `869f5x291`). The Melody Trainer had a second
 * renderer of its own and the recording analysis marked the low octave with `͵` (U+0375) instead of
 * `,`; both now render through [label], and the English theory text's `Ni′` (U+2032) became `Ni΄`
 * (U+0384). `OneOctaveMarkPerDirectionTest` scans the code and the strings for any other mark. No
 * label is ever stored — the analysis stores [PhthongName] constant names — so the change is display
 * only. The `΄` inside mode names such as «Ήχος Α΄» is a numeral sign, not an octave mark: it is part
 * of folder names stored on users' devices (`HymnFolders`) and must never be touched.
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
