package com.johnchourp.learnbyzantinemusic.modes

import com.johnchourp.learnbyzantinemusic.music.Genus
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.ModeLadder
import com.johnchourp.learnbyzantinemusic.music.Moria
import com.johnchourp.learnbyzantinemusic.music.Phthong
import com.johnchourp.learnbyzantinemusic.music.PhthongName

/**
 * The γένος of a scale. Kept as an alias so the name this package has always used still resolves,
 * while there is only ever one enum — see [Genus] (ClickUp `869f4tpxj`).
 */
typealias ModeScaleGenus = Genus

/**
 * The φθόγγος a scale is built from.
 *
 * [phthong] stays a String because it is what call sites compare against labels; [base] is the typed
 * value the arithmetic uses. Both describe the same note and cannot drift: [phthong] is computed
 * from [base], not stored beside it, so there is nothing for a test to pin.
 */
enum class ModeScaleBase(val base: Phthong) {
    PA(Phthong(PhthongName.PA)),
    NI(Phthong(PhthongName.NI));

    /** Display text of the base. The rendering boundary, derived — never a second spelling. */
    val phthong: String get() = base.label
}

data class ModeScaleInterval(
    val from: String,
    val to: String,
    val moria: Int
)

/**
 * One of the four scale shapes: a γένος, a base φθόγγος, and the μόρια between successive φθόγγοι.
 *
 * ## What changed with the typed model
 *
 * The ladder is now built as [Phthong] values and [Moria] distances, and the String labels are
 * produced from them at the edge. Before, the labels *were* the model: the code decorated names with
 * `,` and `΄` and then compared those strings, so the octave lived in text that anything could strip.
 *
 * The String-returning members below are kept deliberately — they are the **rendering boundary**,
 * and several are exactly what the UI and the existing tests consume. Each now delegates to the
 * typed computation rather than re-deriving anything.
 */
data class ModeScaleDefinition(
    val genus: ModeScaleGenus,
    val base: ModeScaleBase,
    val intervals: List<Int>
) {
    /** μόρια between successive φθόγγοι, typed. [intervals] is the same list at the boundary. */
    val stepMoria: List<Moria> get() = intervals.map(::Moria)

    /**
     * The mode's φθόγγοι with the pitch each sounds, spanning [octaves] octaves and transposed by
     * [baseShift] μόρια.
     *
     * This is the single entry point for anything that needs pitches: the diagram, the ison drone
     * and the απήχημα playback all take the same ladder, so they cannot disagree about a φθόγγος.
     * Since ClickUp `869f5x24v` (F2) the Melody Trainer plays and listens on it too.
     *
     * [lowestOctave] is the octave of the base φθόγγος the ladder starts from — see
     * [EightModeScaleDefinitions.ascendingPhthongi]. The Trainer starts an octave lower than the
     * diagram, because its lowest note, Νη one octave down, sits below a Πα-based ladder's first rung.
     */
    fun ladder(
        octaves: Int,
        reference: Phthong = Phthong(PhthongName.NI),
        baseShift: Moria = Moria.ZERO,
        lowestOctave: Int = EightModeScaleDefinitions.LOW_OCTAVE,
    ): ModeLadder = ModeLadder.build(
        ascendingPhthongi = ascendingPhthongi(octaves, lowestOctave),
        ascendingIntervals = repeatedIntervals(octaves).map(::Moria),
        referenceMoria = referenceMoria(reference, octaves, lowestOctave),
        baseShift = baseShift,
    )

    val upperBase: String
        get() = Phthong(base.base.name, base.base.octave + 1).label

    fun repeatedIntervals(octaves: Int): List<Int> {
        require(octaves > 0) { "octaves must be positive" }
        return buildList(intervals.size * octaves) {
            repeat(octaves) {
                addAll(intervals)
            }
        }
    }

    /**
     * The ladder as **typed φθόγγοι**, ascending. This is the primary form; [ascendingPhthongs] is
     * its rendering.
     */
    fun ascendingPhthongi(
        octaves: Int,
        lowestOctave: Int = EightModeScaleDefinitions.LOW_OCTAVE,
    ): List<Phthong> = EightModeScaleDefinitions.ascendingPhthongi(base, octaves, lowestOctave)

    /** Rendering boundary: the same ladder as display labels. */
    fun ascendingPhthongs(octaves: Int): List<String> =
        ascendingPhthongi(octaves).map { it.label }

    fun intervalPairs(): List<ModeScaleInterval> {
        val phthongs = EightModeScaleDefinitions.singleOctavePhthongs(base)
        return intervals.mapIndexed { index, moria ->
            ModeScaleInterval(
                from = phthongs[index],
                to = phthongs[index + 1],
                moria = moria
            )
        }
    }

    fun intervalSummary(): String =
        intervalPairs().joinToString(separator = ", ") { "${it.from}-${it.to} ${it.moria}" }

    /**
     * How far [reference] sits above the bottom of the ladder, in μόρια.
     *
     * Typed throughout: the φθόγγος is matched **by value, octave included**, so it cannot be
     * confused with the same name an octave away — which a label comparison would do the moment
     * anything trimmed the suffix.
     */
    fun referenceMoria(
        reference: Phthong,
        octaves: Int,
        lowestOctave: Int = EightModeScaleDefinitions.LOW_OCTAVE,
    ): Moria {
        val ladder = ascendingPhthongi(octaves, lowestOctave)
        val referenceIndex = ladder.indexOf(reference)
        require(referenceIndex >= 0) {
            "reference phthong ${reference.label} is not present in ${ladder.joinToString { it.label }}"
        }
        return cumulativeMoria(octaves)[referenceIndex]
    }

    /**
     * Boundary overload taking display text. Kept because the UI and the existing tests address the
     * reference by its written name; it parses and delegates rather than doing its own matching.
     */
    fun referenceMoriaFromBottom(referencePhthong: String, octaves: Int): Int {
        val reference = Phthong.parse(referencePhthong)
        require(reference != null) { "reference phthong $referencePhthong is not a φθόγγος" }
        return referenceMoria(reference, octaves).value
    }

    private fun cumulativeMoria(octaves: Int): List<Moria> {
        val cumulative = mutableListOf(Moria.ZERO)
        var current = Moria.ZERO
        for (interval in repeatedIntervals(octaves)) {
            current += Moria(interval)
            cumulative.add(current)
        }
        return cumulative
    }
}

object EightModeScaleDefinitions {
    val DIATONIC = ModeScaleDefinition(
        genus = ModeScaleGenus.DIATONIC,
        base = ModeScaleBase.PA,
        intervals = listOf(10, 8, 12, 12, 10, 8, 12)
    )

    val SOFT_CHROMATIC = ModeScaleDefinition(
        genus = ModeScaleGenus.SOFT_CHROMATIC,
        base = ModeScaleBase.PA,
        intervals = listOf(8, 14, 8, 12, 8, 14, 8)
    )

    val HARD_CHROMATIC = ModeScaleDefinition(
        genus = ModeScaleGenus.HARD_CHROMATIC,
        base = ModeScaleBase.NI,
        intervals = listOf(6, 20, 4, 12, 6, 20, 4)
    )

    val ENHARMONIC = ModeScaleDefinition(
        genus = ModeScaleGenus.ENHARMONIC,
        base = ModeScaleBase.NI,
        intervals = listOf(12, 12, 6, 12, 12, 6, 12)
    )

    /** The scale of every ήχος, typed. [MODE_SCALES] is the same table addressed by stored key. */
    val SCALE_BY_MODE: Map<Mode, ModeScaleDefinition> = linkedMapOf(
        Mode.FIRST to DIATONIC,
        Mode.SECOND to SOFT_CHROMATIC,
        Mode.THIRD to ENHARMONIC,
        Mode.FOURTH to DIATONIC,
        Mode.PLAGAL_FIRST to DIATONIC,
        Mode.PLAGAL_SECOND to HARD_CHROMATIC,
        Mode.VARYS to ENHARMONIC,
        Mode.PLAGAL_FOURTH to DIATONIC
    )

    /** Boundary view of [SCALE_BY_MODE], keyed by the stored mode key. */
    val MODE_SCALES: Map<String, ModeScaleDefinition> =
        SCALE_BY_MODE.entries.associate { (mode, scale) -> mode.key to scale }

    /**
     * The ascending ladder as typed φθόγγοι, spanning [octaves] octaves.
     *
     * By default it starts one octave **below** the middle register ([LOW_OCTAVE]), which is why
     * the first φθόγγος renders with `,`: the diagram is meant to reach comfortably under a singer's
     * base as well as above it. [lowestOctave] moves that start; only the Melody Trainer does, one
     * octave further down, so that a Πα-based ladder still holds its Νη one octave down.
     * The octave advances when the run crosses Νη, because Νη is where a Byzantine octave begins —
     * that is the rule this used to express by incrementing a suffix counter.
     */
    fun ascendingPhthongi(base: ModeScaleBase, octaves: Int, lowestOctave: Int = LOW_OCTAVE): List<Phthong> {
        require(octaves > 0) { "octaves must be positive" }
        var current = Phthong(base.base.name, lowestOctave)
        val ladder = mutableListOf(current)
        repeat(PhthongName.entries.size * octaves) {
            current = current.next()
            ladder.add(current)
        }
        return ladder
    }

    /** Rendering boundary for [ascendingPhthongi]. */
    fun ascendingPhthongs(base: ModeScaleBase, octaves: Int): List<String> =
        ascendingPhthongi(base, octaves).map { it.label }

    /**
     * Column labels for the interval table: one octave from [base] to [base]΄.
     *
     * **This is not [ascendingPhthongi] and must not be implemented from it.** The ladder assigns a
     * real octave to every φθόγγος, so a Πα-based run crossing Νη yields `Νη΄`. This table has always
     * rendered the intermediate φθόγγοι **undecorated** — `Πα Βου Γα Δι Κε Ζω Νη Πα΄` — because it
     * labels the *steps between intervals*, not pitches to sound.
     *
     * The distinction was invisible while both were Strings, and a straight port to the typed ladder
     * silently turned that `Νη` into `Νη΄`. Nothing consumes these today, so nothing broke — but it
     * would have been a display change smuggled in by a refactor, which ClickUp `869f4tpxj`
     * explicitly forbids. `PhthongRenderingParityTest` now pins it.
     *
     * **Decided in ClickUp `869f5cnyx` on 2026-09-22: the table keeps the bare `Νη`.** The rule it
     * codifies is that the octave mark here means **disambiguation, not register** — it is written
     * where a name repeats inside the table (`Πα` appears twice, `Νη` once), which is why the last
     * rung is `Πα΄` while the middle `Νη` stays bare. The ladder is the opposite by design: its
     * φθόγγοι are *sounded*, so it marks register. `PhthongRenderingParityTest` pins both forms and
     * their disagreement, so neither can be "simplified" into the other.
     */
    fun singleOctavePhthongs(base: ModeScaleBase): List<String> {
        val names = PhthongName.entries
        val startIndex = names.indexOf(base.base.name)
        val rotated = names.drop(startIndex) + names.take(startIndex)
        return rotated.map { it.displayName } + Phthong(base.base.name, 1).label
    }

    /** The ladder opens an octave below the middle register — see [ascendingPhthongi]. */
    const val LOW_OCTAVE = -1
}
