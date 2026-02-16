package com.johnchourp.learnbyzantinemusic.calendar

import org.junit.Assume.assumeTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LiturgicalToneCycleKnownFixturesTest {
    private val toneCycle = LiturgicalToneCycle()

    @Test
    fun matchesKnownDateFixturesWhenProvided() {
        // Συμπλήρωσε από τη δική σου πηγή αναφοράς (ημερομηνία -> toneIndex 0..7).
        val knownFixtures: List<KnownToneFixture> = emptyList()

        assumeTrue(
            "Δεν έχουν δοθεί ακόμα known fixtures (ημερομηνία/ήχος).",
            knownFixtures.isNotEmpty()
        )

        knownFixtures.forEach { fixture ->
            val actual = toneCycle.resolveTone(fixture.date)
            assertEquals(fixture.toneIndex, actual.toneIndex)
        }
    }

    private data class KnownToneFixture(
        val date: LocalDate,
        val toneIndex: Int
    )
}
