package com.johnchourp.learnbyzantinemusic

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the pure font-size step math the redesigned Settings slider relies on: snapping to the
 * five allowed steps, the step→fontScale mapping, and the step↔seek-bar-index round trip.
 */
class AppFontScaleTest {

    @Test
    fun normalizeStep_snaps_to_nearest_allowed_step() {
        assertEquals(20, AppFontScale.normalizeStep(22))
        assertEquals(40, AppFontScale.normalizeStep(38))
        assertEquals(60, AppFontScale.normalizeStep(59))
        assertEquals(80, AppFontScale.normalizeStep(81))
        assertEquals(100, AppFontScale.normalizeStep(95))
    }

    @Test
    fun normalizeStep_clamps_out_of_range_values() {
        assertEquals(20, AppFontScale.normalizeStep(-50))
        assertEquals(20, AppFontScale.normalizeStep(0))
        assertEquals(100, AppFontScale.normalizeStep(1000))
    }

    @Test
    fun defaultStep_is_sixty() {
        assertEquals(60, AppFontScale.defaultStep)
    }

    @Test
    fun stepToFontScale_maps_each_step() {
        assertEquals(0.80f, AppFontScale.stepToFontScale(20), 0.0001f)
        assertEquals(0.90f, AppFontScale.stepToFontScale(40), 0.0001f)
        assertEquals(1.00f, AppFontScale.stepToFontScale(60), 0.0001f)
        assertEquals(1.10f, AppFontScale.stepToFontScale(80), 0.0001f)
        assertEquals(1.20f, AppFontScale.stepToFontScale(100), 0.0001f)
    }

    @Test
    fun stepToFontScale_normalizes_unaligned_input() {
        assertEquals(1.00f, AppFontScale.stepToFontScale(57), 0.0001f)
    }

    @Test
    fun stepToSeekBarIndex_maps_each_step() {
        assertEquals(0, AppFontScale.stepToSeekBarIndex(20))
        assertEquals(1, AppFontScale.stepToSeekBarIndex(40))
        assertEquals(2, AppFontScale.stepToSeekBarIndex(60))
        assertEquals(3, AppFontScale.stepToSeekBarIndex(80))
        assertEquals(4, AppFontScale.stepToSeekBarIndex(100))
    }

    @Test
    fun seekBarIndexToStep_maps_and_coerces() {
        assertEquals(20, AppFontScale.seekBarIndexToStep(0))
        assertEquals(100, AppFontScale.seekBarIndexToStep(4))
        // Out-of-range indices are coerced into the valid range.
        assertEquals(20, AppFontScale.seekBarIndexToStep(-3))
        assertEquals(100, AppFontScale.seekBarIndexToStep(9))
    }

    @Test
    fun index_step_round_trip_is_stable() {
        intArrayOf(20, 40, 60, 80, 100).forEach { step ->
            val index = AppFontScale.stepToSeekBarIndex(step)
            assertEquals(step, AppFontScale.seekBarIndexToStep(index))
        }
    }
}
