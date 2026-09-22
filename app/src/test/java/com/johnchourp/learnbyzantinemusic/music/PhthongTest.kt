package com.johnchourp.learnbyzantinemusic.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ClickUp `869f4tpxj`: the φθόγγος value type and its boundary parser.
 *
 * The point of the type is that **the octave cannot be lost**. As a decorated String it could be:
 * anything that trimmed a suffix to "normalise" a name collapsed four distinct pitches into one.
 */
class PhthongTest {

    @Test
    fun theSameNameInDifferentOctavesIsNotTheSameThing() {
        // The whole reason this is a type.
        assertNotEquals(Phthong(PhthongName.PA, 0), Phthong(PhthongName.PA, 1))
        assertNotEquals(Phthong(PhthongName.PA, -1), Phthong(PhthongName.PA, 0))
        assertEquals(Phthong(PhthongName.PA, 2), Phthong(PhthongName.PA, 2))
    }

    @Test
    fun labelsMatchWhatTheScreensHaveAlwaysShown() {
        assertEquals("Πα", Phthong(PhthongName.PA).label)
        assertEquals("Πα,", Phthong(PhthongName.PA, -1).label)
        assertEquals("Πα΄", Phthong(PhthongName.PA, 1).label)
        assertEquals("Πα΄΄", Phthong(PhthongName.PA, 2).label)
        assertEquals("Νη", Phthong(PhthongName.NI).label)
    }

    @Test
    fun parsingRoundTripsEveryNameAndOctaveTheAppUses() {
        PhthongName.entries.forEach { name ->
            (-1..2).forEach { octave ->
                val phthong = Phthong(name, octave)
                assertEquals("round trip of ${phthong.label}", phthong, Phthong.parse(phthong.label))
            }
        }
    }

    @Test
    fun parsingToleratesSurroundingSpace() {
        assertEquals(Phthong(PhthongName.VOU), Phthong.parse("  Βου  "))
    }

    @Test
    fun textThatNamesNoPhthongIsRefusedRatherThanGuessed() {
        // A guess here becomes a confidently wrong pitch, which is worse than silence.
        listOf("", "   ", "Ωμέγα", "Π", "Πα Βου", "12", "΄", ",").forEach { input ->
            assertNull("\"$input\" must not parse", Phthong.parse(input))
        }
    }

    @Test
    fun aLabelCannotMixOctaveDirections() {
        assertNull(Phthong.parse("Πα,΄"))
        assertNull(Phthong.parse("Πα΄,"))
    }

    @Test
    fun anOctaveMarkBeforeTheNameIsNotALabel() {
        assertNull(Phthong.parse("΄Πα"))
        assertNull(Phthong.parse(",Πα"))
    }

    @Test
    fun nextWalksTheScaleAndRollsTheOctaveAtNi() {
        // Νη is where a Byzantine octave begins, which is why the roll happens there.
        assertEquals(Phthong(PhthongName.VOU), Phthong(PhthongName.PA).next())
        assertEquals(Phthong(PhthongName.NI, 1), Phthong(PhthongName.ZO, 0).next())
        assertEquals(Phthong(PhthongName.NI, 0), Phthong(PhthongName.ZO, -1).next())
    }

    @Test
    fun orderingSpansOctavesRatherThanJustNames() {
        assertTrue(Phthong(PhthongName.PA, 0) < Phthong(PhthongName.PA, 1))
        assertTrue(Phthong(PhthongName.ZO, 0) < Phthong(PhthongName.NI, 1))
        assertTrue(Phthong(PhthongName.NI, 1) > Phthong(PhthongName.ZO, 0))
        assertEquals(
            listOf(
                Phthong(PhthongName.NI, -1),
                Phthong(PhthongName.ZO, -1),
                Phthong(PhthongName.NI, 0),
                Phthong(PhthongName.PA, 0),
            ),
            listOf(
                Phthong(PhthongName.PA, 0),
                Phthong(PhthongName.NI, 0),
                Phthong(PhthongName.ZO, -1),
                Phthong(PhthongName.NI, -1),
            ).sorted(),
        )
    }

    @Test
    fun everyNameIsSpelledInExactlyOnePlace() {
        assertEquals(7, PhthongName.entries.size)
        assertEquals(
            listOf("Νη", "Πα", "Βου", "Γα", "Δι", "Κε", "Ζω"),
            PhthongName.entries.map { it.displayName },
        )
        PhthongName.entries.forEach { name ->
            assertEquals(name, PhthongName.fromDisplayName(name.displayName))
        }
        assertNull(PhthongName.fromDisplayName("Νη΄"))
    }
}
