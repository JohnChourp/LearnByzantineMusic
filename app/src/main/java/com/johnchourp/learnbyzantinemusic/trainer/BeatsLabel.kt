package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.Beats
import java.util.Locale

/**
 * How the Melody Trainer prints a length in χρόνοι — «1», «0,5», «1,5» — with the locale's decimal
 * separator: the note rows, the «Σύνολο» and the numbers of the «Κανόνες χρόνου» card alike.
 *
 * The Trainer only lets you write whole and half χρόνοι, so one decimal is all it normally shows. A
 * finer length the rules can produce (⅓, ¼) is printed to two decimals rather than rounded to a half
 * that is not true.
 */
object BeatsLabel {

    fun of(beats: Beats, decimalSeparator: Char): String {
        val whole = beats.ticks / Beats.TICKS_PER_BEAT
        val rest = beats.ticks % Beats.TICKS_PER_BEAT
        return when {
            rest == 0 -> "$whole"
            rest * 2 == Beats.TICKS_PER_BEAT -> "$whole${decimalSeparator}5"
            else -> String.format(Locale.ROOT, "%.2f", beats.ticks.toDouble() / Beats.TICKS_PER_BEAT)
                .replace('.', decimalSeparator)
        }
    }
}
