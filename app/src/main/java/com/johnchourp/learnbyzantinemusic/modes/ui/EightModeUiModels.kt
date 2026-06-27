package com.johnchourp.learnbyzantinemusic.modes.ui

import androidx.annotation.StringRes
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.modes.EightModeScaleDefinitions
import com.johnchourp.learnbyzantinemusic.modes.ModeScaleDefinition
import com.johnchourp.learnbyzantinemusic.modes.ModeScaleGenus
import com.johnchourp.learnbyzantinemusic.modes.ModeTheoryCatalog

/**
 * A single ήχος as the 8 Ήχοι screen presents it: the display/selector names, the απήχημα (with its
 * optional alternative), the theory key that joins it to [ModeTheoryCatalog], and the scale whose
 * genus drives the colour-coding. Lifted out of the old `EightModesActivity` so the Compose screen
 * (and any test) can read the catalogue without the Activity. Genus comes from [scale].genus.
 */
data class EightModeUiModel(
    @StringRes val nameRes: Int,
    @StringRes val selectorNameRes: Int,
    @StringRes val selectorGenusRes: Int,
    @StringRes val apichimaRes: Int,
    @StringRes val apichimaAlternativeRes: Int?,
    @StringRes val apichimaSyllablesRes: Int,
    @StringRes val apichimaAlternativeSyllablesRes: Int?,
    val theoryKey: String,
    val scale: ModeScaleDefinition,
) {
    val genus: ModeScaleGenus get() = scale.genus
}

/** The eight ήχοι in screen order, identical to the legacy `EightModesActivity.modes` list. */
val EIGHT_MODES: List<EightModeUiModel> = listOf(
    EightModeUiModel(
        nameRes = R.string.mode_first,
        selectorNameRes = R.string.mode_first,
        selectorGenusRes = R.string.mode_genus_diatonic,
        apichimaRes = R.string.mode_apichima_first,
        apichimaAlternativeRes = null,
        apichimaSyllablesRes = R.string.mode_apichima_syllables_first,
        apichimaAlternativeSyllablesRes = null,
        theoryKey = ModeTheoryCatalog.keyForPosition(0),
        scale = EightModeScaleDefinitions.DIATONIC,
    ),
    EightModeUiModel(
        nameRes = R.string.mode_fourth,
        selectorNameRes = R.string.mode_fourth,
        selectorGenusRes = R.string.mode_genus_diatonic,
        apichimaRes = R.string.mode_apichima_fourth,
        apichimaAlternativeRes = R.string.mode_apichima_alternative_fourth,
        apichimaSyllablesRes = R.string.mode_apichima_syllables_fourth,
        apichimaAlternativeSyllablesRes = R.string.mode_apichima_alternative_syllables_fourth,
        theoryKey = ModeTheoryCatalog.keyForPosition(1),
        scale = EightModeScaleDefinitions.DIATONIC,
    ),
    EightModeUiModel(
        nameRes = R.string.mode_plagal_first,
        selectorNameRes = R.string.mode_selector_plagal_first,
        selectorGenusRes = R.string.mode_genus_diatonic,
        apichimaRes = R.string.mode_apichima_plagal_first,
        apichimaAlternativeRes = null,
        apichimaSyllablesRes = R.string.mode_apichima_syllables_plagal_first,
        apichimaAlternativeSyllablesRes = null,
        theoryKey = ModeTheoryCatalog.keyForPosition(2),
        scale = EightModeScaleDefinitions.DIATONIC,
    ),
    EightModeUiModel(
        nameRes = R.string.mode_plagal_fourth,
        selectorNameRes = R.string.mode_selector_plagal_fourth,
        selectorGenusRes = R.string.mode_genus_diatonic,
        apichimaRes = R.string.mode_apichima_plagal_fourth,
        apichimaAlternativeRes = null,
        apichimaSyllablesRes = R.string.mode_apichima_syllables_plagal_fourth,
        apichimaAlternativeSyllablesRes = null,
        theoryKey = ModeTheoryCatalog.keyForPosition(3),
        scale = EightModeScaleDefinitions.DIATONIC,
    ),
    EightModeUiModel(
        nameRes = R.string.mode_third,
        selectorNameRes = R.string.mode_third,
        selectorGenusRes = R.string.mode_genus_enharmonic,
        apichimaRes = R.string.mode_apichima_third,
        apichimaAlternativeRes = null,
        apichimaSyllablesRes = R.string.mode_apichima_syllables_third,
        apichimaAlternativeSyllablesRes = null,
        theoryKey = ModeTheoryCatalog.keyForPosition(4),
        scale = EightModeScaleDefinitions.ENHARMONIC,
    ),
    EightModeUiModel(
        nameRes = R.string.mode_varys,
        selectorNameRes = R.string.mode_varys,
        selectorGenusRes = R.string.mode_genus_enharmonic,
        apichimaRes = R.string.mode_apichima_varys,
        apichimaAlternativeRes = null,
        apichimaSyllablesRes = R.string.mode_apichima_syllables_varys,
        apichimaAlternativeSyllablesRes = null,
        theoryKey = ModeTheoryCatalog.keyForPosition(5),
        scale = EightModeScaleDefinitions.ENHARMONIC,
    ),
    EightModeUiModel(
        nameRes = R.string.mode_second,
        selectorNameRes = R.string.mode_second,
        selectorGenusRes = R.string.mode_genus_chromatic_second,
        apichimaRes = R.string.mode_apichima_second,
        apichimaAlternativeRes = null,
        apichimaSyllablesRes = R.string.mode_apichima_syllables_second,
        apichimaAlternativeSyllablesRes = null,
        theoryKey = ModeTheoryCatalog.keyForPosition(6),
        scale = EightModeScaleDefinitions.SOFT_CHROMATIC,
    ),
    EightModeUiModel(
        nameRes = R.string.mode_plagal_second,
        selectorNameRes = R.string.mode_selector_plagal_second,
        selectorGenusRes = R.string.mode_genus_chromatic_plagal_second,
        apichimaRes = R.string.mode_apichima_plagal_second,
        apichimaAlternativeRes = null,
        apichimaSyllablesRes = R.string.mode_apichima_syllables_plagal_second,
        apichimaAlternativeSyllablesRes = null,
        theoryKey = ModeTheoryCatalog.keyForPosition(7),
        scale = EightModeScaleDefinitions.HARD_CHROMATIC,
    ),
)
