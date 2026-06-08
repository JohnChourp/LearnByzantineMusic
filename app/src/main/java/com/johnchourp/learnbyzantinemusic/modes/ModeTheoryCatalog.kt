package com.johnchourp.learnbyzantinemusic.modes

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.johnchourp.learnbyzantinemusic.R

data class ModeTheory(
    val key: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @StringRes val apichimaRes: Int,
    @StringRes val heroSummaryRes: Int,
    @DrawableRes val signRes: Int,
    @StringRes val signDescriptionRes: Int,
    val styleRows: List<ModeTheoryStyleRow>,
    @StringRes val modulationsRes: Int,
    @StringRes val attractionsRes: Int,
    @StringRes val phthoresRes: Int,
    @StringRes val ethosRes: Int
)

data class ModeTheoryStyleRow(
    @StringRes val styleNameRes: Int,
    @StringRes val systemRes: Int,
    @StringRes val scaleRes: Int,
    @StringRes val baseRes: Int,
    @StringRes val dominantsCadencesRes: Int
)

object ModeTheoryCatalog {
    const val EXTRA_MODE_KEY = "mode_key"

    private const val FIRST = "first"
    private const val SECOND = "second"
    private const val THIRD = "third"
    private const val FOURTH = "fourth"
    private const val PLAGAL_FIRST = "plagal_first"
    private const val PLAGAL_SECOND = "plagal_second"
    private const val VARYS = "varys"
    private const val PLAGAL_FOURTH = "plagal_fourth"

    private val orderedKeys = listOf(
        FIRST,
        FOURTH,
        PLAGAL_FIRST,
        PLAGAL_FOURTH,
        THIRD,
        VARYS,
        SECOND,
        PLAGAL_SECOND
    )

    val modes: List<ModeTheory> = listOf(
        ModeTheory(
            key = FIRST,
            titleRes = R.string.mode_first,
            subtitleRes = R.string.mode_theory_subtitle_first,
            apichimaRes = R.string.mode_theory_apichima_first_source,
            heroSummaryRes = R.string.mode_theory_summary_first,
            signRes = R.drawable.diatonic_intermediates_testimonial_pa,
            signDescriptionRes = R.string.mode_theory_sign_first_desc,
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
            key = SECOND,
            titleRes = R.string.mode_second,
            subtitleRes = R.string.mode_theory_subtitle_second,
            apichimaRes = R.string.mode_theory_apichima_second_source,
            heroSummaryRes = R.string.mode_theory_summary_second,
            signRes = R.drawable.diatonic_intermediates_testimonial_pa,
            signDescriptionRes = R.string.mode_theory_sign_second_desc,
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
            key = THIRD,
            titleRes = R.string.mode_third,
            subtitleRes = R.string.mode_theory_subtitle_third,
            apichimaRes = R.string.mode_theory_apichima_third_source,
            heroSummaryRes = R.string.mode_theory_summary_third,
            signRes = R.drawable.diatonic_intermediates_testimonial_ga,
            signDescriptionRes = R.string.mode_theory_sign_third_desc,
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
            key = FOURTH,
            titleRes = R.string.mode_fourth,
            subtitleRes = R.string.mode_theory_subtitle_fourth,
            apichimaRes = R.string.mode_theory_apichima_fourth_source,
            heroSummaryRes = R.string.mode_theory_summary_fourth,
            signRes = R.drawable.diatonic_intermediates_testimonial_bou,
            signDescriptionRes = R.string.mode_theory_sign_fourth_desc,
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
            key = PLAGAL_FIRST,
            titleRes = R.string.mode_plagal_first,
            subtitleRes = R.string.mode_theory_subtitle_plagal_first,
            apichimaRes = R.string.mode_theory_apichima_plagal_first_source,
            heroSummaryRes = R.string.mode_theory_summary_plagal_first,
            signRes = R.drawable.diatonic_intermediates_testimonial_ke,
            signDescriptionRes = R.string.mode_theory_sign_plagal_first_desc,
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
            key = PLAGAL_SECOND,
            titleRes = R.string.mode_plagal_second,
            subtitleRes = R.string.mode_theory_subtitle_plagal_second,
            apichimaRes = R.string.mode_theory_apichima_plagal_second_source,
            heroSummaryRes = R.string.mode_theory_summary_plagal_second,
            signRes = R.drawable.diatonic_intermediates_testimonial_di,
            signDescriptionRes = R.string.mode_theory_sign_plagal_second_desc,
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
            key = VARYS,
            titleRes = R.string.mode_varys,
            subtitleRes = R.string.mode_theory_subtitle_varys,
            apichimaRes = R.string.mode_theory_apichima_varys_source,
            heroSummaryRes = R.string.mode_theory_summary_varys,
            signRes = R.drawable.diatonic_filamentous_testimonial_zo,
            signDescriptionRes = R.string.mode_theory_sign_varys_desc,
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
            key = PLAGAL_FOURTH,
            titleRes = R.string.mode_plagal_fourth,
            subtitleRes = R.string.mode_theory_subtitle_plagal_fourth,
            apichimaRes = R.string.mode_theory_apichima_plagal_fourth_source,
            heroSummaryRes = R.string.mode_theory_summary_plagal_fourth,
            signRes = R.drawable.diatonic_filamentous_testimonial_ni,
            signDescriptionRes = R.string.mode_theory_sign_plagal_fourth_desc,
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

    fun keyForPosition(position: Int): String =
        orderedKeys.getOrElse(position) { FIRST }

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
