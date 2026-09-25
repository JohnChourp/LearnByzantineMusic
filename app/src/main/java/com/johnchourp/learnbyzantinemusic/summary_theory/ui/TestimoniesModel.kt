package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.modes.ModeResources
import com.johnchourp.learnbyzantinemusic.music.Mode

/**
 * Pure-Kotlin data model for the «Μαρτυρίες» (Testimonies) page — no Android/Compose dependencies
 * beyond resource ids, so the phthong→martyria→mode mapping is trivially unit-testable.
 *
 * The phthong→mode correspondence is locked to the app's own evidence: every [ModeBadge] is backed
 * by a `mode_theory_sign_*_desc` string AND uses the same testimonial drawable that
 * `ModeTheoryCatalog` assigns that mode. Phthongs without a backing string carry no badge.
 *
 * Since ClickUp `869f5x299` the badges are not written here at all: [badgesFor] derives them from
 * [Mode] and `ModeResources`, the column whose drawable is a mode's martyria sign lists that mode.
 */

/** One mode (ήχος) that uses a given martyria, with a short name and the app's verbatim description. */
internal data class ModeBadge(
    @StringRes val nameRes: Int,
    @StringRes val descRes: Int,
)

/** One column of the testimonies row: a phthong, its martyria drawable, an a11y description, and the modes that use it. */
internal data class MartyriaUiModel(
    @StringRes val phthongRes: Int,
    @DrawableRes val drawable: Int,
    @StringRes val contentDescRes: Int,
    val modes: List<ModeBadge>,
)

/**
 * The eight on-screen columns in display order: Νη Πα Βου Γα Δι Κε Ζω Νη΄ — one full octave (διαπασών),
 * which is why it begins and ends on Νη. The two Νη use DIFFERENT drawables (intermediate vs filamentous):
 * the opening Νη has no `mode_theory_sign_*_desc` backing, so it carries no mode badge, while the octave
 * Νη is the filamentous drawable that backs πλ. Δ΄. Πα is the only phthong the app's strings map to two
 * modes (Α΄ and Β΄), so it carries two badges.
 */
internal val TESTIMONY_ROW: List<MartyriaUiModel> = listOf(
    MartyriaUiModel(
        phthongRes = R.string.phthong_ni,
        drawable = R.drawable.diatonic_intermediates_testimonial_ni,
        contentDescRes = R.string.cd_testimonial_ni,
        modes = badgesFor(R.drawable.diatonic_intermediates_testimonial_ni),
    ),
    MartyriaUiModel(
        phthongRes = R.string.phthong_pa,
        drawable = R.drawable.diatonic_intermediates_testimonial_pa,
        contentDescRes = R.string.cd_testimonial_pa,
        modes = badgesFor(R.drawable.diatonic_intermediates_testimonial_pa),
    ),
    MartyriaUiModel(
        phthongRes = R.string.phthong_bou,
        drawable = R.drawable.diatonic_intermediates_testimonial_bou,
        contentDescRes = R.string.cd_testimonial_bou,
        modes = badgesFor(R.drawable.diatonic_intermediates_testimonial_bou),
    ),
    MartyriaUiModel(
        phthongRes = R.string.phthong_ga,
        drawable = R.drawable.diatonic_intermediates_testimonial_ga,
        contentDescRes = R.string.cd_testimonial_ga,
        modes = badgesFor(R.drawable.diatonic_intermediates_testimonial_ga),
    ),
    MartyriaUiModel(
        phthongRes = R.string.phthong_di,
        drawable = R.drawable.diatonic_intermediates_testimonial_di,
        contentDescRes = R.string.cd_testimonial_di,
        modes = badgesFor(R.drawable.diatonic_intermediates_testimonial_di),
    ),
    MartyriaUiModel(
        phthongRes = R.string.phthong_ke,
        drawable = R.drawable.diatonic_intermediates_testimonial_ke,
        contentDescRes = R.string.cd_testimonial_ke,
        modes = badgesFor(R.drawable.diatonic_intermediates_testimonial_ke),
    ),
    MartyriaUiModel(
        phthongRes = R.string.phthong_zo,
        drawable = R.drawable.diatonic_filamentous_testimonial_zo,
        contentDescRes = R.string.cd_testimonial_zo,
        modes = badgesFor(R.drawable.diatonic_filamentous_testimonial_zo),
    ),
    MartyriaUiModel(
        // Same Νη note as the opening column, one octave higher — shown as «Νη΄» to mark the octave close.
        phthongRes = R.string.phthong_ni_high,
        drawable = R.drawable.diatonic_filamentous_testimonial_ni,
        contentDescRes = R.string.cd_testimonial_ni_high,
        modes = badgesFor(R.drawable.diatonic_filamentous_testimonial_ni),
    ),
)

/**
 * The modes whose martyria sign is [drawable], in the order of the cycle — derived, so a badge can
 * never name a mode whose sign the column does not draw.
 */
private fun badgesFor(@DrawableRes drawable: Int): List<ModeBadge> =
    Mode.entries
        .filter { ModeResources.martyriaSignRes(it) == drawable }
        .map { ModeBadge(nameRes = ModeResources.nameRes(it), descRes = ModeResources.martyriaDescriptionRes(it)) }
