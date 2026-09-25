package com.johnchourp.learnbyzantinemusic.recordings.analysis

import com.johnchourp.learnbyzantinemusic.music.PhthongName.DI
import com.johnchourp.learnbyzantinemusic.music.PhthongName.GA
import com.johnchourp.learnbyzantinemusic.music.PhthongName.KE
import com.johnchourp.learnbyzantinemusic.music.PhthongName.PA
import com.johnchourp.learnbyzantinemusic.music.PhthongName.VOU
import org.junit.Assert.assertEquals
import org.junit.Test

class SequenceAlignerTest {

    @Test
    fun identicalSequencesAreAllCorrect() {
        val result = SequenceAligner.align(listOf(PA, VOU, GA), listOf(PA, VOU, GA))
        assertEquals(3, result.matches)
        assertEquals(1.0, result.accuracy, 1e-9)
    }

    @Test
    fun aWrongNoteIsASubstitution() {
        val result = SequenceAligner.align(listOf(PA, VOU, GA), listOf(PA, DI, GA))
        assertEquals(
            listOf(AlignmentStep.Match(0, 0), AlignmentStep.Substitution(1, 1), AlignmentStep.Match(2, 2)),
            result.steps,
        )
        assertEquals(2.0 / 3, result.accuracy, 1e-9)
    }

    @Test
    fun aSkippedNoteIsMissingAndAnAddedNoteIsExtra() {
        val skipped = SequenceAligner.align(listOf(PA, VOU, GA, DI), listOf(PA, GA, DI))
        assertEquals(AlignmentStep.Missing(1), skipped.steps[1])
        assertEquals(3, skipped.matches)

        val added = SequenceAligner.align(listOf(PA, GA), listOf(PA, KE, GA))
        assertEquals(AlignmentStep.Extra(1), added.steps[1])
        assertEquals(1, added.extras)
        assertEquals(1.0, added.accuracy, 1e-9)
    }

    @Test
    fun emptyInputs() {
        assertEquals(0.0, SequenceAligner.align(emptyList(), listOf(PA)).accuracy, 1e-9)
        assertEquals(listOf(AlignmentStep.Missing(0)), SequenceAligner.align(listOf(PA), emptyList()).steps)
    }
}
