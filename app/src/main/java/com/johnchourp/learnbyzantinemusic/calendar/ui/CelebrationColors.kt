package com.johnchourp.learnbyzantinemusic.calendar.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.johnchourp.learnbyzantinemusic.calendar.CalendarCelebrationType
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPalette

/**
 * Everything a celebration card in the Ημερολόγιο is painted with, for one type in one palette
 * (ClickUp `869f5x286`).
 *
 * **Why one place.** The card's background used to be a light-only colour resource while its text
 * followed the theme, so in dark mode the two came from different themes and met at 1.08:1. Taking
 * the background, the border, the badge and both text colours from the same [LbmPalette] is what
 * makes that mismatch impossible rather than merely fixed.
 *
 * **Why pure.** No Compose runtime and no resources: `CalendarCelebrationContrastTest` calls
 * [celebrationColors] directly, so the ratios it measures are the ones the screen draws.
 */
internal data class CelebrationColors(
    val background: Color,
    val border: Color,
    /** The type badge's text. */
    val accent: Color,
    val title: Color,
    val description: Color,
) {
    /**
     * The badge's fill: the accent at [BADGE_TINT_ALPHA] over the card, flattened to the colour
     * that actually reaches the screen. The badge sits directly on the card, so drawing this
     * opaque colour looks exactly like drawing the translucent tint — and unlike the tint, it is
     * something a contrast ratio can be computed against.
     */
    val badgeFill: Color get() = accent.copy(alpha = BADGE_TINT_ALPHA).compositeOver(background)

    companion object {
        const val BADGE_TINT_ALPHA = 0.16f
    }
}

internal fun celebrationColors(type: CalendarCelebrationType, palette: LbmPalette): CelebrationColors =
    when (type) {
        CalendarCelebrationType.PUBLIC_HOLIDAY -> CelebrationColors(
            background = palette.celebrationPublicBg,
            border = palette.celebrationPublicBorder,
            accent = palette.celebrationPublicAccent,
            title = palette.textPrimary,
            description = palette.textSecondary,
        )
        CalendarCelebrationType.HALF_HOLIDAY -> CelebrationColors(
            background = palette.celebrationHalfBg,
            border = palette.celebrationHalfBorder,
            accent = palette.celebrationHalfAccent,
            title = palette.textPrimary,
            description = palette.textSecondary,
        )
        CalendarCelebrationType.RELIGIOUS_OBSERVANCE -> CelebrationColors(
            background = palette.celebrationReligiousBg,
            border = palette.celebrationReligiousBorder,
            accent = palette.celebrationReligiousAccent,
            title = palette.textPrimary,
            description = palette.textSecondary,
        )
        // A normal day has no colour of its own: its badge reads in the secondary text colour.
        CalendarCelebrationType.NORMAL_DAY -> CelebrationColors(
            background = palette.celebrationNormalBg,
            border = palette.celebrationNormalBorder,
            accent = palette.textSecondary,
            title = palette.textPrimary,
            description = palette.textSecondary,
        )
    }
