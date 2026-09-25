package com.johnchourp.learnbyzantinemusic.anastasimatarion

import com.johnchourp.learnbyzantinemusic.calendar.LiturgicalToneCycle
import com.johnchourp.learnbyzantinemusic.modes.ModeResources
import com.johnchourp.learnbyzantinemusic.music.Mode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * «The position of each mode in `MODE_ORDER` is the calendar's tone index» was written in a comment
 * and checked by nobody (ClickUp `869f5x299`). Both are derived from [Mode] now; this walks the
 * calendar through one full round of the cycle to prove the Anastasimatarion follows it.
 */
class AnastasimatarionFollowsModeTest {

    private val cycle = LiturgicalToneCycle()

    @Test
    fun eightConsecutiveWeeksOpenTheModeOfTheirTone() {
        // From the 2nd Sunday after Pentecost 2026 (Α΄ on the published charts), eight Sundays in a row.
        val sundays = (0L..7L).map { LocalDate.of(2026, 6, 14).plusWeeks(it) }
        val modes = sundays.map { sunday ->
            val tone = cycle.resolveTone(sunday)
            val toneIndex = checkNotNull(tone.toneIndex) { "$sunday is an ordinary week" }
            val mode = Mode.ofToneIndex(toneIndex)
            assertEquals("$sunday opens the calendar's mode", mode.key, AnastasimatarionWeekMode.weekModeKey(sunday))
            assertEquals("$sunday: MODE_ORDER at the tone index", mode.key, AnastasimatarionLabels.MODE_ORDER[toneIndex])
            assertEquals("$sunday: one name for the tone", tone.toneNameRes, AnastasimatarionLabels.modeName(mode.key))
            mode
        }
        assertEquals("one round, in the order of the cycle", Mode.entries.toList(), modes)
    }

    @Test
    fun theLabelsAreTheModesInTheOrderOfTheCycle() {
        assertEquals(Mode.entries.map { it.key }, AnastasimatarionLabels.MODE_ORDER)
        assertEquals(
            Mode.entries.map { ModeResources.nameRes(it) },
            AnastasimatarionLabels.MODE_ORDER.map { AnastasimatarionLabels.modeName(it) },
        )
    }
}
