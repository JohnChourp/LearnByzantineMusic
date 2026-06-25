package com.johnchourp.learnbyzantinemusic.modes

import com.johnchourp.learnbyzantinemusic.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ModeTheoryCatalogTest {
    @Test
    fun catalogCoversAllEightModesWithThreeStyleRows() {
        assertEquals(8, ModeTheoryCatalog.modes.size)

        ModeTheoryCatalog.modes.forEach { theory ->
            assertNotEquals(0, theory.titleRes)
            assertNotEquals(0, theory.apichimaRes)
            assertNotEquals(0, theory.modulationsRes)
            assertNotEquals(0, theory.attractionsRes)
            assertNotEquals(0, theory.phthoresRes)
            assertEquals(3, theory.styleRows.size)

            theory.styleRows.forEach { row ->
                assertNotEquals(0, row.styleNameRes)
                assertNotEquals(0, row.systemRes)
                assertNotEquals(0, row.scaleRes)
                assertNotEquals(0, row.dominantsCadencesRes)
            }
        }
    }

    @Test
    fun selectedModeTheoryScreenHasRequiredSectionAndTableLabels() {
        val requiredStringResources = listOf(
            R.string.mode_theory_section_apichima,
            R.string.mode_theory_section_syllables_phthongs,
            R.string.mode_theory_style_eirmologic,
            R.string.mode_theory_style_sticheraric,
            R.string.mode_theory_style_papadic,
            R.string.mode_theory_section_modulations,
            R.string.mode_theory_section_attractions,
            R.string.mode_theory_section_phthores,
            R.string.mode_theory_field_system,
            R.string.mode_theory_field_scale,
            R.string.mode_theory_field_dominant_phthongs,
            R.string.mode_theory_field_cadences,
            R.string.mode_theory_field_final_cadences
        )

        requiredStringResources.forEach { resId ->
            assertNotEquals(0, resId)
        }
    }
}
