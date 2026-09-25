package com.johnchourp.learnbyzantinemusic.calendar

import com.johnchourp.learnbyzantinemusic.modes.ModeResources
import com.johnchourp.learnbyzantinemusic.music.Mode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The tone (ήχος) of any date: what the calendar shows and what the Anastasimatarion opens on.
 *
 * The eight-tone cycle restarts every year from that year's Pascha (offsets in days):
 *
 *     −7 … −1    Palm Sunday and Holy Week       no tone — the Triodion, not the Octoechos
 *      0 … +6    Bright Week                     a tone per DAY: Α΄ Β΄ Γ΄ Δ΄ Πλ.Α΄ Πλ.Β΄ Πλ.Δ΄ (Βαρύς skipped)
 *     +7         Thomas Sunday                   Α΄ — the weekly cycle is anchored here
 *     +14 … +42  Myrrhbearers … Holy Fathers     Β΄ Γ΄ Δ΄ Πλ.Α΄ Πλ.Β΄
 *     +49 … +55  Pentecost and its week          no tone — the feast; the week still counts
 *     +56        All Saints                      Πλ.Δ΄
 *     +63        2nd Sunday after Pentecost      Α΄ again, then one tone per Sunday–Saturday week
 *                                                until the Saturday before the next Palm Sunday
 *
 * Until ClickUp `869f5x26r` the cycle was anchored at the 2nd Sunday after Pentecost and every date
 * before it continued the PREVIOUS year's cycle. That lands right only when the weeks between two
 * Paschas are a multiple of 8, so the six Sundays from the Myrrhbearers to All Saints (and their
 * weeks) were wrong in every year from 1901 to 2100. Outside Palm Sunday … Pascha+62 every date keeps
 * the tone it had, because the new anchor is exactly eight weeks before the old one.
 *
 * The table follows the Pentecostarion rubrics and matches the Tone column of published Sunday
 * liturgical charts; the fixtures are in LiturgicalTonePentecostarionTest.
 */
class LiturgicalToneCycle {
    fun resolveTone(date: LocalDate): WeeklyToneResult {
        val weekStart = startOfWeekSunday(date)
        val weekEnd = weekStart.plusDays(6)
        // Every period that breaks the weekly cycle lies between late March and early July, so the
        // Pascha of the date's own year is the only one that can own it.
        val daysFromPascha = ChronoUnit.DAYS.between(OrthodoxPaschaCalculator.computePaschaDate(date.year), date)
        val kind = when (daysFromPascha) {
            in HOLY_WEEK_DAYS -> LiturgicalToneKind.HOLY_WEEK
            in BRIGHT_WEEK_DAYS -> LiturgicalToneKind.BRIGHT_WEEK_DAY
            in PENTECOST_WEEK_DAYS -> LiturgicalToneKind.PENTECOST_WEEK
            else -> LiturgicalToneKind.WEEKLY
        }
        val toneIndex = when (kind) {
            LiturgicalToneKind.WEEKLY -> {
                val weeksBetween = ChronoUnit.WEEKS.between(computeCycleStartForDate(date), weekStart).toInt()
                Math.floorMod(weeksBetween, Mode.entries.size)
            }
            LiturgicalToneKind.BRIGHT_WEEK_DAY -> BRIGHT_WEEK_DAY_TONES[daysFromPascha.toInt()]
            LiturgicalToneKind.HOLY_WEEK, LiturgicalToneKind.PENTECOST_WEEK -> null
        }

        return WeeklyToneResult(
            selectedDate = date,
            weekStart = weekStart,
            weekEnd = weekEnd,
            kind = kind,
            toneIndex = toneIndex,
            toneNameRes = toneIndex?.let { ModeResources.nameRes(Mode.ofToneIndex(it)) }
        )
    }

    /** Thomas Sunday of the cycle [date] belongs to: this year's, or last year's before it. */
    internal fun computeCycleStartForDate(date: LocalDate): LocalDate {
        val weekStart = startOfWeekSunday(date)
        val cycleStartCurrentYear = computeCycleStartForYear(date.year)
        if (!weekStart.isBefore(cycleStartCurrentYear)) {
            return cycleStartCurrentYear
        }
        return computeCycleStartForYear(date.year - 1)
    }

    /** Thomas Sunday (Pascha + 7) of [year]: Α΄, the first week of that year's cycle. */
    internal fun computeCycleStartForYear(year: Int): LocalDate =
        OrthodoxPaschaCalculator.computePaschaDate(year).plusWeeks(1)

    internal fun startOfWeekSunday(date: LocalDate): LocalDate {
        val daysFromSunday = date.dayOfWeek.value % 7
        return date.minusDays(daysFromSunday.toLong())
    }

    private companion object {
        val HOLY_WEEK_DAYS = -7L..-1L
        val BRIGHT_WEEK_DAYS = 0L..6L
        val PENTECOST_WEEK_DAYS = 49L..55L

        /** Pascha Sunday … Bright Saturday. Βαρύς (6) is skipped, so Saturday is Πλ. Δ΄ (7). */
        val BRIGHT_WEEK_DAY_TONES = listOf(0, 1, 2, 3, 4, 5, 7)
    }
}

/** Which rule gives a date its tone. The two no-tone kinds are whole Sunday–Saturday weeks. */
enum class LiturgicalToneKind {
    /** An ordinary week: one tone from Sunday to Saturday. */
    WEEKLY,

    /** Bright Week: each day has its own tone and there is no tone of the week. */
    BRIGHT_WEEK_DAY,

    /** Palm Sunday to Holy Saturday: no tone. */
    HOLY_WEEK,

    /** Pentecost Sunday to the Saturday after it: no tone. */
    PENTECOST_WEEK,
}

data class WeeklyToneResult(
    val selectedDate: LocalDate,
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val kind: LiturgicalToneKind,
    /**
     * 0 = Α΄ … 7 = Πλ. Δ΄, in the order of the liturgical cycle: the tone of the week, or the day's own
     * tone in Bright Week. Null when [kind] has no tone — never index a list of modes without checking.
     * The mode is `Mode.ofToneIndex(toneIndex)` (its `number` is `toneIndex + 1`).
     */
    val toneIndex: Int?,
    /** String resource of [toneIndex]'s name; null with it. */
    val toneNameRes: Int?
)
