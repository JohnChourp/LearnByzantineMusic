package com.johnchourp.learnbyzantinemusic.modes

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.music.Mode

/**
 * The Android resources of each [Mode], in one place (ClickUp `869f5x299`): its name, and its
 * martyria's sign and description.
 *
 * They are kept out of the enum so the domain type does not depend on `R`, and in ONE file so no
 * screen keeps its own copy: the calendar's tone names, the Anastasimatarion's labels, the theory
 * page, the «Μαρτυρίες» badges and the 8 Ήχοι rows all read them from here. Every `when` is
 * exhaustive, so a mode cannot exist without its resources.
 */
object ModeResources {

    @StringRes
    fun nameRes(mode: Mode): Int = when (mode) {
        Mode.FIRST -> R.string.mode_first
        Mode.SECOND -> R.string.mode_second
        Mode.THIRD -> R.string.mode_third
        Mode.FOURTH -> R.string.mode_fourth
        Mode.PLAGAL_FIRST -> R.string.mode_plagal_first
        Mode.PLAGAL_SECOND -> R.string.mode_plagal_second
        Mode.VARYS -> R.string.mode_varys
        Mode.PLAGAL_FOURTH -> R.string.mode_plagal_fourth
    }

    /**
     * The martyria sign drawn for the mode, the same image on its theory page and in «Μαρτυρίες».
     * Α΄ and Β΄ share Πα's; πλ. Δ΄ has the filamentous Νη of the octave, not the opening Νη.
     */
    @DrawableRes
    fun martyriaSignRes(mode: Mode): Int = when (mode) {
        Mode.FIRST -> R.drawable.diatonic_intermediates_testimonial_pa
        Mode.SECOND -> R.drawable.diatonic_intermediates_testimonial_pa
        Mode.THIRD -> R.drawable.diatonic_intermediates_testimonial_ga
        Mode.FOURTH -> R.drawable.diatonic_intermediates_testimonial_bou
        Mode.PLAGAL_FIRST -> R.drawable.diatonic_intermediates_testimonial_ke
        Mode.PLAGAL_SECOND -> R.drawable.diatonic_intermediates_testimonial_di
        Mode.VARYS -> R.drawable.diatonic_filamentous_testimonial_zo
        Mode.PLAGAL_FOURTH -> R.drawable.diatonic_filamentous_testimonial_ni
    }

    /** What the mode's martyria sign says, for its theory page and its «Μαρτυρίες» badge. */
    @StringRes
    fun martyriaDescriptionRes(mode: Mode): Int = when (mode) {
        Mode.FIRST -> R.string.mode_theory_sign_first_desc
        Mode.SECOND -> R.string.mode_theory_sign_second_desc
        Mode.THIRD -> R.string.mode_theory_sign_third_desc
        Mode.FOURTH -> R.string.mode_theory_sign_fourth_desc
        Mode.PLAGAL_FIRST -> R.string.mode_theory_sign_plagal_first_desc
        Mode.PLAGAL_SECOND -> R.string.mode_theory_sign_plagal_second_desc
        Mode.VARYS -> R.string.mode_theory_sign_varys_desc
        Mode.PLAGAL_FOURTH -> R.string.mode_theory_sign_plagal_fourth_desc
    }
}
