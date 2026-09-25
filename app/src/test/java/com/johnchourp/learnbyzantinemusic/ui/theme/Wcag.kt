package com.johnchourp.learnbyzantinemusic.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/**
 * WCAG 2.1 contrast, in one place for every test that measures a colour pair.
 *
 * Extracted from `GenusContrastTest` (ClickUp `869f5x286`) so the calendar-card and pages-menu
 * checks use the very formula that test pins in `theContrastFormulaIsItselfCorrect`, rather than a
 * second copy that could drift from it.
 */
internal object Wcag {

    /** WCAG AA for normal-size text. */
    const val TEXT_AA = 4.5

    /** WCAG 2.1 relative luminance. */
    fun luminance(color: Color): Double {
        fun channel(v: Float): Double {
            val c = v.toDouble()
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    /** WCAG 2.1 contrast ratio, always ≥ 1.0 and ≤ 21.0. */
    fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }
}
