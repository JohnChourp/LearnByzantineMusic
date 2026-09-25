package com.johnchourp.learnbyzantinemusic.anastasimatarion

import androidx.annotation.StringRes
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.calendar.LiturgicalToneCycle
import com.johnchourp.learnbyzantinemusic.calendar.LiturgicalToneKind
import com.johnchourp.learnbyzantinemusic.calendar.WeeklyToneAnnouncement
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The mode the Anastasimatarion opens on, and the badge that names it.
 *
 * It follows [LiturgicalToneCycle]: the tone of the week in an ordinary week, the day's own tone in
 * Bright Week, and null in the weeks that have no tone at all (Palm Sunday and Holy Week, the week
 * of Pentecost). Null keeps the screen's fallback: the first mode, with no badge.
 *
 * [currentMode] adds the time of day, through [WeeklyToneAnnouncement]: from its VESPERS hour on,
 * the page opens on the tone that tonight's vespers begins when that is a different one — the same
 * tone the home card announces (ClickUp `869f5x24r`). The badge says which of the three it is, so it
 * is true in every kind: «Ήχος της εβδομάδας», «Ήχος της ημέρας» (Bright Week), «Από τον εσπερινό
 * απόψε». [weekModeKey] stays for a whole date, with no time of day.
 *
 * The tone index becomes a mode key through [AnastasimatarionLabels.MODE_ORDER], which is in the
 * order of the liturgical cycle. The 8 Ήχοι screen lists the modes by genus, so the same index there
 * names a different mode.
 */
object AnastasimatarionWeekMode {
    private val toneCycle = LiturgicalToneCycle()
    private val announcement = WeeklyToneAnnouncement(toneCycle = toneCycle)

    fun weekModeKey(date: LocalDate): String? =
        toneCycle.resolveTone(date).toneIndex?.let { AnastasimatarionLabels.MODE_ORDER[it] }

    fun currentMode(moment: LocalDateTime): CurrentMode? {
        val now = announcement.at(moment)
        val fromVespers = now.fromVespers?.toneIndex
        if (fromVespers != null) {
            return CurrentMode(
                AnastasimatarionLabels.MODE_ORDER[fromVespers],
                R.string.anastasimatarion_vespers_mode_template,
            )
        }
        val today = now.today.toneIndex ?: return null
        val badge = if (now.today.kind == LiturgicalToneKind.BRIGHT_WEEK_DAY) {
            R.string.anastasimatarion_day_mode_template
        } else {
            R.string.anastasimatarion_week_mode_template
        }
        return CurrentMode(AnastasimatarionLabels.MODE_ORDER[today], badge)
    }

    /** A mode key, and the badge template that names it (formatted with the mode's name). */
    data class CurrentMode(val modeKey: String, @StringRes val badgeRes: Int)
}
