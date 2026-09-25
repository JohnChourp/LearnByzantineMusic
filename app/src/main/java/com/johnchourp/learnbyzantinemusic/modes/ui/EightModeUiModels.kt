package com.johnchourp.learnbyzantinemusic.modes.ui

import androidx.annotation.StringRes
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.modes.ModeResources
import com.johnchourp.learnbyzantinemusic.modes.ModeScaleDefinition
import com.johnchourp.learnbyzantinemusic.modes.ModeScaleGenus
import com.johnchourp.learnbyzantinemusic.modes.ModeTheoryCatalog
import com.johnchourp.learnbyzantinemusic.music.Mode

/**
 * A single ήχος as the 8 Ήχοι screen presents it: the display/selector names, the απήχημα (with its
 * optional alternative), the theory key that joins it to [ModeTheoryCatalog], and the scale whose
 * genus drives the colour-coding. Lifted out of the old `EightModesActivity` so the Compose screen
 * (and any test) can read the catalogue without the Activity. Genus comes from [scale].genus.
 *
 * Each row names its [mode], and the facts that belong to the ήχος itself — its key, name and scale
 * — are derived from it (ClickUp `869f5x299`) rather than written beside it. What stays here is
 * what only this screen needs: the selector's short name and genus label, and the απήχημα.
 */
data class EightModeUiModel(
    val mode: Mode,
    @StringRes val selectorNameRes: Int,
    @StringRes val selectorGenusRes: Int,
    @StringRes val apichimaRes: Int,
    @StringRes val apichimaAlternativeRes: Int?,
    @StringRes val apichimaSyllablesRes: Int,
    @StringRes val apichimaAlternativeSyllablesRes: Int?,
) {
    @get:StringRes
    val nameRes: Int get() = ModeResources.nameRes(mode)

    /**
     * The **stored** spelling of [mode] — it appears in the preference `mode_base_shift_moria_<key>`,
     * and renaming one would silently reset a user's «Μεταφορά βάσης». Derived, so it cannot drift.
     */
    val theoryKey: String get() = mode.key

    val scale: ModeScaleDefinition get() = mode.scale

    val genus: ModeScaleGenus get() = scale.genus
}

/**
 * The eight ήχοι in screen order, identical to the legacy `EightModesActivity.modes` list. The order
 * groups the modes by genus and is deliberately NOT the order of the cycle; `EightModeUiModelsTest`
 * pins it.
 */
val EIGHT_MODES: List<EightModeUiModel> = listOf(
    EightModeUiModel(
        mode = Mode.FIRST,
        selectorNameRes = R.string.mode_first,
        selectorGenusRes = R.string.mode_genus_diatonic,
        apichimaRes = R.string.mode_apichima_first,
        apichimaAlternativeRes = null,
        apichimaSyllablesRes = R.string.mode_apichima_syllables_first,
        apichimaAlternativeSyllablesRes = null,
    ),
    EightModeUiModel(
        mode = Mode.FOURTH,
        selectorNameRes = R.string.mode_fourth,
        selectorGenusRes = R.string.mode_genus_diatonic,
        apichimaRes = R.string.mode_apichima_fourth,
        apichimaAlternativeRes = R.string.mode_apichima_alternative_fourth,
        apichimaSyllablesRes = R.string.mode_apichima_syllables_fourth,
        apichimaAlternativeSyllablesRes = R.string.mode_apichima_alternative_syllables_fourth,
    ),
    EightModeUiModel(
        mode = Mode.PLAGAL_FIRST,
        selectorNameRes = R.string.mode_selector_plagal_first,
        selectorGenusRes = R.string.mode_genus_diatonic,
        apichimaRes = R.string.mode_apichima_plagal_first,
        apichimaAlternativeRes = null,
        apichimaSyllablesRes = R.string.mode_apichima_syllables_plagal_first,
        apichimaAlternativeSyllablesRes = null,
    ),
    EightModeUiModel(
        mode = Mode.PLAGAL_FOURTH,
        selectorNameRes = R.string.mode_selector_plagal_fourth,
        selectorGenusRes = R.string.mode_genus_diatonic,
        apichimaRes = R.string.mode_apichima_plagal_fourth,
        apichimaAlternativeRes = null,
        apichimaSyllablesRes = R.string.mode_apichima_syllables_plagal_fourth,
        apichimaAlternativeSyllablesRes = null,
    ),
    EightModeUiModel(
        mode = Mode.THIRD,
        selectorNameRes = R.string.mode_third,
        selectorGenusRes = R.string.mode_genus_enharmonic,
        apichimaRes = R.string.mode_apichima_third,
        apichimaAlternativeRes = null,
        apichimaSyllablesRes = R.string.mode_apichima_syllables_third,
        apichimaAlternativeSyllablesRes = null,
    ),
    EightModeUiModel(
        mode = Mode.VARYS,
        selectorNameRes = R.string.mode_varys,
        selectorGenusRes = R.string.mode_genus_enharmonic,
        apichimaRes = R.string.mode_apichima_varys,
        apichimaAlternativeRes = null,
        apichimaSyllablesRes = R.string.mode_apichima_syllables_varys,
        apichimaAlternativeSyllablesRes = null,
    ),
    EightModeUiModel(
        mode = Mode.SECOND,
        selectorNameRes = R.string.mode_second,
        selectorGenusRes = R.string.mode_genus_chromatic_second,
        apichimaRes = R.string.mode_apichima_second,
        apichimaAlternativeRes = null,
        apichimaSyllablesRes = R.string.mode_apichima_syllables_second,
        apichimaAlternativeSyllablesRes = null,
    ),
    EightModeUiModel(
        mode = Mode.PLAGAL_SECOND,
        selectorNameRes = R.string.mode_selector_plagal_second,
        selectorGenusRes = R.string.mode_genus_chromatic_plagal_second,
        apichimaRes = R.string.mode_apichima_plagal_second,
        apichimaAlternativeRes = null,
        apichimaSyllablesRes = R.string.mode_apichima_syllables_plagal_second,
        apichimaAlternativeSyllablesRes = null,
    ),
)

/**
 * Row of [EIGHT_MODES] for a stored or passed mode key; null when the key names none.
 *
 * [EIGHT_MODES] is in GENUS order, not in the order of the liturgical cycle, so a tone index is never
 * a row index here — tone Β΄ is row 6. Always go through the key (ClickUp `869f5x24r`).
 */
fun eightModesIndexOf(theoryKey: String?): Int? =
    EIGHT_MODES.indexOfFirst { it.theoryKey == theoryKey }.takeIf { it >= 0 }
