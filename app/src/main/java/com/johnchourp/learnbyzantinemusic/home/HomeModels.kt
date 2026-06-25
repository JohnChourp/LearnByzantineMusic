package com.johnchourp.learnbyzantinemusic.home

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

/** Color family used to tint a [HomeTile]'s icon badge so sections read as visually distinct groups. */
enum class TileAccent { Gold, Blue, Purple, Orange, Green, Brown }

/** Optional informational block rendered inside a section (e.g. the quantity-character explainer). */
enum class HomeInfo { QuantityVoices }

/** A single navigation entry on the home screen. */
data class HomeTile(
    val id: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val subtitleRes: Int?,
    val icon: ImageVector,
    val accent: TileAccent,
    val onClick: () -> Unit,
)

/** A titled group of navigation tiles, optionally preceded by an [HomeInfo] block. */
data class HomeSection(
    @param:StringRes val titleRes: Int,
    @param:StringRes val subtitleRes: Int?,
    val tiles: List<HomeTile>,
    val info: HomeInfo? = null,
)
