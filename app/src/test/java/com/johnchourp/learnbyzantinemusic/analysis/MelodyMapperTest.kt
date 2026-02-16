package com.johnchourp.learnbyzantinemusic.analysis

import org.junit.Assert.assertEquals
import org.junit.Test

class MelodyMapperTest {
    @Test
    fun `maps deltas from selected base phthong`() {
        val mapper = MelodyMapper(
            phthongsOrder = listOf("Νη", "Πα", "Βου", "Γα", "Δι", "Κε", "Ζω"),
            modeProfiles = mapOf(
                "first" to ModeProfile(
                    id = "first",
                    noteHeights = mapOf(
                        "Νη" to 0f,
                        "Πα" to 0.2f,
                        "Βου" to 0.4f,
                        "Γα" to 0.6f,
                        "Δι" to 0.8f,
                        "Κε" to 0.9f,
                        "Ζω" to 1f
                    )
                )
            )
        )

        val traversal = mapper.map(
            basePhthong = "Πα",
            deltas = listOf(1, 1, -1, 0, -2),
            modeId = "first"
        )

        assertEquals(listOf("Βου", "Γα", "Βου", "Βου", "Νη"), traversal.notes)
        assertEquals(1f, traversal.modeHeights["Ζω"])
    }

    @Test
    fun `wraps around when movement exceeds boundaries`() {
        val mapper = MelodyMapper(
            phthongsOrder = listOf("Νη", "Πα", "Βου", "Γα", "Δι", "Κε", "Ζω"),
            modeProfiles = emptyMap()
        )

        val traversal = mapper.map(
            basePhthong = "Ζω",
            deltas = listOf(1, 1, -8),
            modeId = "unknown_mode"
        )

        assertEquals(listOf("Νη", "Πα", "Νη"), traversal.notes)
        assertEquals(0f, traversal.modeHeights["Νη"])
    }
}
