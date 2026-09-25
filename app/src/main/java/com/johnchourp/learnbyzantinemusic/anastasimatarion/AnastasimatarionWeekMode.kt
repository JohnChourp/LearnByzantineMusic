package com.johnchourp.learnbyzantinemusic.anastasimatarion

import com.johnchourp.learnbyzantinemusic.calendar.LiturgicalToneCycle
import java.time.LocalDate

/**
 * The mode the Anastasimatarion opens on for a day, and badges as the mode of the week.
 *
 * It follows [LiturgicalToneCycle]: the tone of the week in an ordinary week, the day's own tone in
 * Bright Week, and null in the weeks that have no tone at all (Palm Sunday and Holy Week, the week
 * of Pentecost). Null keeps the screen's fallback: the first mode, with no badge.
 *
 * The tone index becomes a mode key through [AnastasimatarionLabels.MODE_ORDER], which is in the
 * order of the liturgical cycle. The 8 Ήχοι screen lists the modes by genus, so the same index there
 * names a different mode.
 *
 * Pure and dated so it can be tested for any day, not only for today (ClickUp `869f5x26r`).
 */
object AnastasimatarionWeekMode {
    private val toneCycle = LiturgicalToneCycle()

    fun weekModeKey(date: LocalDate): String? =
        toneCycle.resolveTone(date).toneIndex?.let { AnastasimatarionLabels.MODE_ORDER[it] }
}
