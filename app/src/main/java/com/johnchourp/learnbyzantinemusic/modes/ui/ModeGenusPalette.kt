package com.johnchourp.learnbyzantinemusic.modes.ui

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.modes.ModeScaleGenus
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentBlueContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentBlueContent
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGoldContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentGoldContent
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentOrangeContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentOrangeContent
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentPurpleContainer
import com.johnchourp.learnbyzantinemusic.ui.theme.AccentPurpleContent

/**
 * Maps each Byzantine genus to one of the shared accent families so the genus colour-coding is
 * identical across the mode picker, the genus legend, the scale diagram and the mode-details badge.
 * Mirrors the legacy spinner colours (hard chromatic = blue, soft chromatic = purple,
 * enharmonic = orange) and gives the diatonic genus the brand gold/brown.
 */
data class GenusAccent(val container: Color, val content: Color, @StringRes val nameRes: Int)

object ModeGenusPalette {
    fun accent(genus: ModeScaleGenus): GenusAccent = when (genus) {
        ModeScaleGenus.DIATONIC -> GenusAccent(
            AccentGoldContainer, AccentGoldContent, R.string.eight_modes_genus_diatonic,
        )
        ModeScaleGenus.SOFT_CHROMATIC -> GenusAccent(
            AccentPurpleContainer, AccentPurpleContent, R.string.eight_modes_genus_soft_chromatic,
        )
        ModeScaleGenus.HARD_CHROMATIC -> GenusAccent(
            AccentBlueContainer, AccentBlueContent, R.string.eight_modes_genus_hard_chromatic,
        )
        ModeScaleGenus.ENHARMONIC -> GenusAccent(
            AccentOrangeContainer, AccentOrangeContent, R.string.eight_modes_genus_enharmonic,
        )
    }

    val ordered: List<ModeScaleGenus> = listOf(
        ModeScaleGenus.DIATONIC,
        ModeScaleGenus.SOFT_CHROMATIC,
        ModeScaleGenus.HARD_CHROMATIC,
        ModeScaleGenus.ENHARMONIC,
    )
}
