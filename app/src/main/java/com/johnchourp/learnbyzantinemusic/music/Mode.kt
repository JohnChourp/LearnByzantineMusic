package com.johnchourp.learnbyzantinemusic.music

import com.johnchourp.learnbyzantinemusic.modes.EightModeScaleDefinitions
import com.johnchourp.learnbyzantinemusic.modes.ModeScaleDefinition

/**
 * The eight ήχοι, as a closed set (ClickUp `869f4tpxj`) — and the one place their facts are written
 * (ClickUp `869f5x299`): every other list of the modes in the app is derived from this enum.
 *
 * ## Why an enum and not a map key
 *
 * The modes were addressed by string keys (`"first"`, `"plagal_second"`), and every lookup returned
 * a nullable that each call site handled differently — some `?: error(...)`, some falling back to
 * the first mode. A typo produced a runtime surprise rather than a compile error, and "is this
 * string a mode key, a topic key or a theory key?" was answerable only by reading.
 *
 * There are exactly eight and there will not be a ninth, so the set belongs in the type system.
 *
 * ## What a mode carries
 *
 * - [key] — the stored/lookup spelling. **Frozen**: it is inside preference names
 *   (`mode_base_shift_moria_<key>`) and the Anastasimatarion catalog; renaming one would silently
 *   reset a user's «Μεταφορά βάσης» for that mode.
 * - [number] — 1 … 8 in the order of the liturgical cycle, which is also the order of the entries.
 *   The calendar's tone index is `number − 1`: [ofToneIndex].
 * - [martyria] — the φθόγγος of the mode's martyria, as the «Μαρτυρίες» page draws it. The phthong
 *   analysis starts there unless the user picks another.
 * - [anastasimatarionFolder] — the folder of the mode's recordings under «Αναστασιματάριο».
 *   **Frozen, byte for byte**: those folders exist on users' devices, and «Α΄» ends in U+0384 GREEK
 *   TONOS, not an apostrophe. Never normalise it.
 * - [scale] — from `EightModeScaleDefinitions.SCALE_BY_MODE`, the one mode → scale table.
 *
 * Android resources (the name, the martyria's sign and description) are mapped in `modes/ModeResources`,
 * so this enum stays free of `R`. Derived from here: the calendar's tone names, `AnastasimatarionLabels`,
 * `HymnFolders`, `ModeScalePositions`, `ModeTheoryCatalog`, the «Μαρτυρίες» badges and the rows of
 * `EIGHT_MODES`, whose genus DISPLAY order stays deliberately its own. The Anastasimatarion generator
 * script is checked against this list by a test.
 *
 * ## An unknown key
 *
 * [fromKey] is the one parse boundary: a key that names no mode is null there, and each caller says
 * what that means. A value read back from storage falls back explicitly (a corrupted preference must
 * never crash the screen); a key the app produced itself goes through [of], which throws.
 */
enum class Mode(
    val key: String,
    val number: Int,
    val martyria: PhthongName,
    val anastasimatarionFolder: String,
) {
    FIRST("first", 1, PhthongName.PA, "Ήχος Α΄"),
    SECOND("second", 2, PhthongName.PA, "Ήχος Β΄"),
    THIRD("third", 3, PhthongName.GA, "Ήχος Γ΄"),
    FOURTH("fourth", 4, PhthongName.VOU, "Ήχος Δ΄"),
    PLAGAL_FIRST("plagal_first", 5, PhthongName.KE, "Ήχος πλ. Α΄"),
    PLAGAL_SECOND("plagal_second", 6, PhthongName.DI, "Ήχος πλ. Β΄"),
    VARYS("varys", 7, PhthongName.ZO, "Ήχος Βαρύς"),
    PLAGAL_FOURTH("plagal_fourth", 8, PhthongName.NI, "Ήχος πλ. Δ΄");

    /** The mode's scale: `EightModeScaleDefinitions.SCALE_BY_MODE` is the only mode → scale table. */
    val scale: ModeScaleDefinition get() = EightModeScaleDefinitions.SCALE_BY_MODE.getValue(this)

    companion object {
        /** Boundary parser: a stored or catalog key → the mode, or null when it names none. */
        fun fromKey(key: String?): Mode? = entries.firstOrNull { it.key == key }

        /** A key the app produced itself: one that names no mode is a bug, so it throws. */
        fun of(key: String): Mode =
            fromKey(key) ?: throw IllegalArgumentException("\"$key\" is not a mode key")

        /** The mode of the calendar's tone index: 0 = Α΄ … 7 = Πλ. Δ΄, i.e. [number] − 1. */
        fun ofToneIndex(toneIndex: Int): Mode =
            entries.firstOrNull { it.number == toneIndex + 1 }
                ?: throw IllegalArgumentException("$toneIndex is not a tone index (0 … 7)")
    }
}
