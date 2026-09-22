package com.johnchourp.learnbyzantinemusic.modes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ClickUp `869f4tph0` (A3): *«Αναζήτηση χωρίς τόνους/case (Ελληνικά + Αγγλικά)»*.
 *
 * The ranges half matters as much as the matching half: a highlight that lands one character off on
 * accented Greek is worse than no highlight, because it looks like the page mis-rendered.
 */
class TheorySearchTest {

    // ---- matching ----------------------------------------------------------------------------

    @Test
    fun greekMatchesWithoutTheTonos() {
        // The accent is exactly the key learners skip when typing.
        assertTrue(TheorySearch.matches("Η πεταστή ανεβάζει", "πεταστη"))
        assertTrue(TheorySearch.matches("πεταστη", "πεταστή"))
        assertTrue(TheorySearch.matches("Έλξη", "ελξη"))
        assertTrue(TheorySearch.matches("μαρτυρία", "ΜΑΡΤΥΡΙΑ"))
    }

    @Test
    fun englishMatchesTheSameWay() {
        assertTrue(TheorySearch.matches("The Octave (Diapason)", "octave"))
        assertTrue(TheorySearch.matches("Testimonies", "TESTIMONIES"))
    }

    @Test
    fun finalSigmaAndCaseAreHandled() {
        assertTrue(TheorySearch.matches("ΤΡΟΧΟΣ", "τροχοσ"))
    }

    @Test
    fun aBlankQueryMatchesEverythingSoTheMenuShowsAllRows() {
        assertTrue(TheorySearch.matches("anything", ""))
        assertTrue(TheorySearch.matches("anything", "   "))
    }

    @Test
    fun anAbsentTermDoesNotMatch() {
        assertFalse(TheorySearch.matches("Η πεταστή ανεβάζει", "κλάσμα"))
    }

    // ---- ranges, in the ORIGINAL string's indices -----------------------------------------------

    @Test
    fun rangesPointAtTheAccentedOriginalNotTheNormalisedCopy() {
        val text = "Η πεταστή ανεβάζει"
        val ranges = TheorySearch.matchRanges(text, "πεταστη")
        assertEquals(1, ranges.size)
        val range = ranges.single()
        assertEquals("πεταστή", text.substring(range.first, range.last + 1))
    }

    @Test
    fun aMatchStartingOnAnAccentedCharacterIsNotOffByOne() {
        val text = "Έλξη προς τον φθόγγο"
        val range = TheorySearch.matchRanges(text, "ελξη").single()
        assertEquals("Έλξη", text.substring(range.first, range.last + 1))
        assertEquals(0, range.first)
    }

    @Test
    fun everyOccurrenceIsReturnedAndTheyDoNotOverlap() {
        val text = "ήχος και ήχος και ΗΧΟΣ"
        val ranges = TheorySearch.matchRanges(text, "ηχος")
        assertEquals(3, ranges.size)
        ranges.forEach { assertEquals(4, it.last - it.first + 1) }
        ranges.zipWithNext().forEach { (a, b) ->
            assertTrue("ranges must not overlap: $a then $b", b.first > a.last)
        }
        assertEquals(listOf("ήχος", "ήχος", "ΗΧΟΣ"), ranges.map { text.substring(it.first, it.last + 1) })
    }

    @Test
    fun noMatchAndBlankQueryBothYieldNoRanges() {
        assertTrue(TheorySearch.matchRanges("Η πεταστή", "κλάσμα").isEmpty())
        assertTrue(TheorySearch.matchRanges("Η πεταστή", "").isEmpty())
        assertTrue(TheorySearch.matchRanges("", "πεταστη").isEmpty())
    }

    @Test
    fun rangesAreAlwaysInsideTheText() {
        // An out-of-bounds range would crash setSpan at runtime rather than fail a test.
        val text = "Απήχημα: Άνανες — μαρτυρία"
        listOf("ανανες", "μαρτυρια", "α").forEach { query ->
            TheorySearch.matchRanges(text, query).forEach { range ->
                assertTrue("$query -> $range escapes 0..${text.lastIndex}", range.first >= 0)
                assertTrue("$query -> $range escapes 0..${text.lastIndex}", range.last <= text.lastIndex)
                assertTrue(range.first <= range.last)
            }
        }
    }

    @Test
    fun matchingAndRangesAgreeWithEachOther() {
        // The bug this guards: the menu finds a page, the page highlights nothing, and it reads as
        // a rendering fault rather than a near-miss in the matcher.
        val samples = listOf("Η πεταστή ανεβάζει", "Έλξη", "ΤΡΟΧΟΣ", "The Octave (Diapason)")
        val queries = listOf("πεταστη", "ελξη", "τροχοσ", "octave")
        samples.forEach { text ->
            queries.forEach { query ->
                assertEquals(
                    "matches() and matchRanges() disagree for \"$query\" in \"$text\"",
                    TheorySearch.matches(text, query),
                    TheorySearch.matchRanges(text, query).isNotEmpty(),
                )
            }
        }
    }

    @Test
    fun aCombiningMarkInsideTheMatchDoesNotShiftTheRange() {
        // Text is not always precomposed: content pasted from some sources arrives in NFD form, where
        // «Απήχημα» is 8 code points — Α π η ◌́ χ η μ α — and the mark sits INSIDE the word.
        //
        // That mark normalises away, so the normalised string is shorter than the source. Anything
        // that computes the end as "start + query length" lands one character short and the
        // highlight clips the last letter. Only a per-character index map gets this right, and only
        // a case with a mark in the MIDDLE can tell the two apart — a trailing accent cannot.
        val nfd = "\u0391\u03C0\u03B7\u0301\u03C7\u03B7\u03BC\u03B1"
        assertEquals("the fixture must really be decomposed", 8, nfd.length)

        val range = TheorySearch.matchRanges(nfd, "\u03B1\u03C0\u03B7\u03C7\u03B7\u03BC\u03B1").single()
        assertEquals(0, range.first)
        assertEquals("the match must cover the whole word", nfd.lastIndex, range.last)
        assertEquals(nfd, nfd.substring(range.first, range.last + 1))
    }
}
