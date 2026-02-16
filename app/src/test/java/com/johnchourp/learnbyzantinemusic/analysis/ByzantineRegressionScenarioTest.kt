package com.johnchourp.learnbyzantinemusic.analysis

import org.junit.Assert.assertEquals
import org.junit.Test

class ByzantineRegressionScenarioTest {
    @Test
    fun `maps example sequence for first mode base ni`() {
        val mapper = MelodyMapper(
            phthongsOrder = listOf("Νη", "Πα", "Βου", "Γα", "Δι", "Κε", "Ζω"),
            modeProfiles = emptyMap()
        )

        val deltas = listOf(+2, -1, 0, +1, +1, -1, -1, -1)
        val traversal = mapper.map(
            basePhthong = "Νη",
            deltas = deltas,
            modeId = "first"
        )

        assertEquals(
            listOf("Βου", "Πα", "Πα", "Βου", "Γα", "Βου", "Πα", "Νη"),
            traversal.notes
        )
    }
}
