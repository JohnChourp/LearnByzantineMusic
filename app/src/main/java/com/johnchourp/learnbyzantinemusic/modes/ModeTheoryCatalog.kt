package com.johnchourp.learnbyzantinemusic.modes

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.music.Mode

/**
 * The theory page of one [mode]. Its key, title and martyria sign are derived from [Mode] and
 * [ModeResources] (ClickUp `869f5x299`); what is written here is only the theory itself.
 */
data class ModeTheory(
    val mode: Mode,
    @StringRes val subtitleRes: Int,
    @StringRes val apichimaRes: Int,
    @StringRes val heroSummaryRes: Int,
    val styleRows: List<ModeTheoryStyleRow>,
    @StringRes val modulationsRes: Int,
    @StringRes val attractionsRes: Int,
    @StringRes val phthoresRes: Int,
    @StringRes val ethosRes: Int
) {
    val key: String get() = mode.key

    @get:StringRes
    val titleRes: Int get() = ModeResources.nameRes(mode)

    @get:DrawableRes
    val signRes: Int get() = ModeResources.martyriaSignRes(mode)

    @get:StringRes
    val signDescriptionRes: Int get() = ModeResources.martyriaDescriptionRes(mode)
}

data class ModeTheoryStyleRow(
    @StringRes val styleNameRes: Int,
    @StringRes val systemRes: Int,
    @StringRes val scaleRes: Int,
    @StringRes val baseRes: Int,
    @StringRes val dominantsCadencesRes: Int
)

object ModeTheoryCatalog {
    const val EXTRA_MODE_KEY = "mode_key"

    val modes: List<ModeTheory> = listOf(
        ModeTheory(
            mode = Mode.FIRST,
            subtitleRes = R.string.mode_theory_subtitle_first,
            apichimaRes = R.string.mode_theory_apichima_first_source,
            heroSummaryRes = R.string.mode_theory_summary_first,
            styleRows = rows(
                eirmologic(
                    system = R.string.mode_theory_system_octave_pentachord,
                    scale = R.string.mode_theory_not_specified,
                    base = R.string.mode_theory_base_pa,
                    dominants = R.string.mode_theory_dominants_first_eirmologic
                ),
                sticheraric(
                    system = R.string.mode_theory_system_octave_pentachord,
                    scale = R.string.mode_theory_not_specified,
                    base = R.string.mode_theory_base_pa,
                    dominants = R.string.mode_theory_dominants_first_sticheraric
                ),
                papadic()
            ),
            modulationsRes = R.string.mode_theory_modulations_first,
            attractionsRes = R.string.mode_theory_attractions_first,
            phthoresRes = R.string.mode_theory_not_specified,
            ethosRes = R.string.mode_theory_ethos_diatonic
        ),
        ModeTheory(
            mode = Mode.SECOND,
            subtitleRes = R.string.mode_theory_subtitle_second,
            apichimaRes = R.string.mode_theory_apichima_second_source,
            heroSummaryRes = R.string.mode_theory_summary_second,
            styleRows = rows(
                eirmologic(
                    system = R.string.mode_theory_system_pentachord_trochos,
                    scale = R.string.mode_theory_scale_hard_first,
                    base = R.string.mode_theory_base_pa,
                    dominants = R.string.mode_theory_dominants_second_eirmologic
                ),
                sticheraric(
                    system = R.string.mode_theory_system_pentachord_trochos,
                    scale = R.string.mode_theory_scale_soft_second,
                    base = R.string.mode_theory_base_di,
                    dominants = R.string.mode_theory_dominants_second_sticheraric
                ),
                papadic()
            ),
            modulationsRes = R.string.mode_theory_modulations_second,
            attractionsRes = R.string.mode_theory_attractions_second,
            phthoresRes = R.string.mode_theory_not_specified,
            ethosRes = R.string.mode_theory_ethos_chromatic
        ),
        ModeTheory(
            mode = Mode.THIRD,
            subtitleRes = R.string.mode_theory_subtitle_third,
            apichimaRes = R.string.mode_theory_apichima_third_source,
            heroSummaryRes = R.string.mode_theory_summary_third,
            styleRows = rows(
                eirmologic(
                    system = R.string.mode_theory_system_tetrachord_triphonia,
                    scale = R.string.mode_theory_not_specified,
                    base = R.string.mode_theory_base_ga,
                    dominants = R.string.mode_theory_dominants_third_eirmologic
                ),
                sticheraric(
                    system = R.string.mode_theory_system_tetrachord_triphonia,
                    scale = R.string.mode_theory_not_specified,
                    base = R.string.mode_theory_base_ga,
                    dominants = R.string.mode_theory_dominants_third_sticheraric
                ),
                papadic()
            ),
            modulationsRes = R.string.mode_theory_modulations_third,
            attractionsRes = R.string.mode_theory_attractions_third,
            phthoresRes = R.string.mode_theory_phthores_third,
            ethosRes = R.string.mode_theory_ethos_enharmonic
        ),
        ModeTheory(
            mode = Mode.FOURTH,
            subtitleRes = R.string.mode_theory_subtitle_fourth,
            apichimaRes = R.string.mode_theory_apichima_fourth_source,
            heroSummaryRes = R.string.mode_theory_summary_fourth,
            styleRows = rows(
                eirmologic(
                    system = R.string.mode_theory_system_octave_diapason,
                    scale = R.string.mode_theory_not_specified,
                    base = R.string.mode_theory_base_bou,
                    dominants = R.string.mode_theory_dominants_fourth_eirmologic
                ),
                sticheraric(
                    system = R.string.mode_theory_system_octave_diapason,
                    scale = R.string.mode_theory_not_specified,
                    base = R.string.mode_theory_base_pa,
                    dominants = R.string.mode_theory_dominants_fourth_sticheraric
                ),
                papadic(base = R.string.mode_theory_base_di)
            ),
            modulationsRes = R.string.mode_theory_modulations_fourth,
            attractionsRes = R.string.mode_theory_attractions_fourth,
            phthoresRes = R.string.mode_theory_not_specified,
            ethosRes = R.string.mode_theory_ethos_diatonic
        ),
        ModeTheory(
            mode = Mode.PLAGAL_FIRST,
            subtitleRes = R.string.mode_theory_subtitle_plagal_first,
            apichimaRes = R.string.mode_theory_apichima_plagal_first_source,
            heroSummaryRes = R.string.mode_theory_summary_plagal_first,
            styleRows = rows(
                eirmologic(
                    system = R.string.mode_theory_system_pentachord,
                    scale = R.string.mode_theory_not_specified,
                    base = R.string.mode_theory_base_ke,
                    dominants = R.string.mode_theory_dominants_plagal_first_eirmologic
                ),
                sticheraric(
                    system = R.string.mode_theory_system_octave_pentachord,
                    scale = R.string.mode_theory_scale_diatonic,
                    base = R.string.mode_theory_base_pa,
                    dominants = R.string.mode_theory_dominants_plagal_first_sticheraric
                ),
                papadic()
            ),
            modulationsRes = R.string.mode_theory_modulations_plagal_first,
            attractionsRes = R.string.mode_theory_attractions_plagal_first,
            phthoresRes = R.string.mode_theory_not_specified,
            ethosRes = R.string.mode_theory_ethos_diatonic
        ),
        ModeTheory(
            mode = Mode.PLAGAL_SECOND,
            subtitleRes = R.string.mode_theory_subtitle_plagal_second,
            apichimaRes = R.string.mode_theory_apichima_plagal_second_source,
            heroSummaryRes = R.string.mode_theory_summary_plagal_second,
            styleRows = rows(
                eirmologic(
                    system = R.string.mode_theory_system_pentachord_trochos,
                    scale = R.string.mode_theory_scale_soft_second,
                    base = R.string.mode_theory_base_di,
                    dominants = R.string.mode_theory_dominants_plagal_second_eirmologic
                ),
                sticheraric(
                    system = R.string.mode_theory_system_pentachord_trochos,
                    scale = R.string.mode_theory_scale_hard_first,
                    base = R.string.mode_theory_base_pa,
                    dominants = R.string.mode_theory_dominants_plagal_second_sticheraric
                ),
                papadic()
            ),
            modulationsRes = R.string.mode_theory_modulations_plagal_second,
            attractionsRes = R.string.mode_theory_attractions_plagal_second,
            phthoresRes = R.string.mode_theory_not_specified,
            ethosRes = R.string.mode_theory_ethos_chromatic
        ),
        ModeTheory(
            mode = Mode.VARYS,
            subtitleRes = R.string.mode_theory_subtitle_varys,
            apichimaRes = R.string.mode_theory_apichima_varys_source,
            heroSummaryRes = R.string.mode_theory_summary_varys,
            styleRows = rows(
                eirmologic(
                    system = R.string.mode_theory_system_tetrachord_triphonia,
                    scale = R.string.mode_theory_not_specified,
                    base = R.string.mode_theory_base_ga,
                    dominants = R.string.mode_theory_not_specified
                ),
                sticheraric(
                    system = R.string.mode_theory_system_tetrachord_triphonia,
                    scale = R.string.mode_theory_scale_enharmonic,
                    base = R.string.mode_theory_base_ga,
                    dominants = R.string.mode_theory_dominants_varys_sticheraric
                ),
                papadic(base = R.string.mode_theory_base_zo)
            ),
            modulationsRes = R.string.mode_theory_modulations_varys,
            attractionsRes = R.string.mode_theory_attractions_varys,
            phthoresRes = R.string.mode_theory_not_specified,
            ethosRes = R.string.mode_theory_ethos_enharmonic
        ),
        ModeTheory(
            mode = Mode.PLAGAL_FOURTH,
            subtitleRes = R.string.mode_theory_subtitle_plagal_fourth,
            apichimaRes = R.string.mode_theory_apichima_plagal_fourth_source,
            heroSummaryRes = R.string.mode_theory_summary_plagal_fourth,
            styleRows = rows(
                eirmologic(
                    system = R.string.mode_theory_system_octave,
                    scale = R.string.mode_theory_not_specified,
                    base = R.string.mode_theory_base_ni,
                    dominants = R.string.mode_theory_dominants_plagal_fourth_eirmologic
                ),
                sticheraric(
                    system = R.string.mode_theory_system_tetrachord,
                    scale = R.string.mode_theory_not_specified,
                    base = R.string.mode_theory_base_ga_as_ni,
                    dominants = R.string.mode_theory_dominants_plagal_fourth_sticheraric
                ),
                papadic()
            ),
            modulationsRes = R.string.mode_theory_modulations_plagal_fourth,
            attractionsRes = R.string.mode_theory_attractions_plagal_fourth,
            phthoresRes = R.string.mode_theory_not_specified,
            ethosRes = R.string.mode_theory_ethos_diatonic
        )
    )

    fun byKey(key: String?): ModeTheory =
        modes.firstOrNull { it.key == key } ?: modes.first()

    private fun rows(
        eirmologic: ModeTheoryStyleRow,
        sticheraric: ModeTheoryStyleRow,
        papadic: ModeTheoryStyleRow
    ): List<ModeTheoryStyleRow> = listOf(eirmologic, sticheraric, papadic)

    private fun eirmologic(
        @StringRes system: Int,
        @StringRes scale: Int,
        @StringRes base: Int,
        @StringRes dominants: Int
    ): ModeTheoryStyleRow = ModeTheoryStyleRow(
        styleNameRes = R.string.mode_theory_style_eirmologic,
        systemRes = system,
        scaleRes = scale,
        baseRes = base,
        dominantsCadencesRes = dominants
    )

    private fun sticheraric(
        @StringRes system: Int,
        @StringRes scale: Int,
        @StringRes base: Int,
        @StringRes dominants: Int
    ): ModeTheoryStyleRow = ModeTheoryStyleRow(
        styleNameRes = R.string.mode_theory_style_sticheraric,
        systemRes = system,
        scaleRes = scale,
        baseRes = base,
        dominantsCadencesRes = dominants
    )

    private fun papadic(
        @StringRes system: Int = R.string.mode_theory_not_specified,
        @StringRes scale: Int = R.string.mode_theory_not_specified,
        @StringRes base: Int = R.string.mode_theory_not_specified,
        @StringRes dominants: Int = R.string.mode_theory_not_specified
    ): ModeTheoryStyleRow = ModeTheoryStyleRow(
        styleNameRes = R.string.mode_theory_style_papadic,
        systemRes = system,
        scaleRes = scale,
        baseRes = base,
        dominantsCadencesRes = dominants
    )
}
