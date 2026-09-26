package com.johnchourp.learnbyzantinemusic.music

import com.johnchourp.learnbyzantinemusic.music.RhythmProblem.Reason
import com.johnchourp.learnbyzantinemusic.music.TimeSign.APLI
import com.johnchourp.learnbyzantinemusic.music.TimeSign.ARGON
import com.johnchourp.learnbyzantinemusic.music.TimeSign.DIARGON
import com.johnchourp.learnbyzantinemusic.music.TimeSign.DIGORGON
import com.johnchourp.learnbyzantinemusic.music.TimeSign.DIGORGON_DOT_BOTTOM
import com.johnchourp.learnbyzantinemusic.music.TimeSign.DIGORGON_DOT_MIDDLE
import com.johnchourp.learnbyzantinemusic.music.TimeSign.DIGORGON_DOT_TOP
import com.johnchourp.learnbyzantinemusic.music.TimeSign.DIPLI
import com.johnchourp.learnbyzantinemusic.music.TimeSign.GORGON
import com.johnchourp.learnbyzantinemusic.music.TimeSign.GORGON_DOT_LEFT
import com.johnchourp.learnbyzantinemusic.music.TimeSign.GORGON_DOT_RIGHT
import com.johnchourp.learnbyzantinemusic.music.TimeSign.KLASMA
import com.johnchourp.learnbyzantinemusic.music.TimeSign.TRIARGON
import com.johnchourp.learnbyzantinemusic.music.TimeSign.TRIGORGON
import com.johnchourp.learnbyzantinemusic.music.TimeSign.TRIPLI
import com.johnchourp.learnbyzantinemusic.music.TimeSign.VAREIA_APLI
import com.johnchourp.learnbyzantinemusic.music.TimeSign.VAREIA_DIPLI
import com.johnchourp.learnbyzantinemusic.music.TimeSign.VAREIA_TRIPLI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * One test per row, and per edge case, of the time-rules tables in [ByzantineRhythmMapper] (ClickUp
 * `869f5x29r`, H5), named after it. The expected lengths are written out as fractions of a χρόνος,
 * never computed from the sign data, so a wrong share in [TimeSign] fails here.
 */
class TimeRulesTest {

    private val half = Beats.of(1, 2)
    private val third = Beats.of(1, 3)
    private val quarter = Beats.of(1, 4)
    private val threeQuarters = Beats.of(3, 4)
    private val one = Beats.ONE
    private fun whole(count: Int) = Beats.whole(count)

    private fun note(vararg signs: TimeSign, base: Beats = Beats.ONE) = RhythmNote(signs.toSet(), base)

    private fun durations(vararg notes: RhythmNote): List<Beats> = ByzantineRhythmMapper.durations(notes.toList())

    private fun problems(vararg notes: RhythmNote): List<RhythmProblem> = ByzantineRhythmMapper.problems(notes.toList())

    private fun total(vararg notes: RhythmNote): Beats = ByzantineRhythmMapper.total(notes.toList())

    private fun assertValid(vararg notes: RhythmNote) =
        assertEquals("a melody the rules accept", emptyList<RhythmProblem>(), problems(*notes))

    // ---- the rules ----------------------------------------------------------------------------

    @Test
    fun aPlainNoteLastsOneBeat() {
        assertEquals(listOf(one), durations(note()))
        assertEquals(listOf(one, one, one), durations(note(), note(), note()))
        assertEquals(whole(3), total(note(), note(), note()))
    }

    @Test
    fun klasmaAndApliAddOneBeat() {
        assertEquals(listOf(whole(2)), durations(note(KLASMA)))
        assertEquals(listOf(whole(2)), durations(note(APLI)))
        assertEquals(listOf(one, whole(2), one), durations(note(), note(KLASMA), note()))
    }

    @Test
    fun dipliAddsTwoBeats() {
        assertEquals(listOf(whole(3)), durations(note(DIPLI)))
    }

    @Test
    fun tripliAddsThreeBeats() {
        assertEquals(listOf(whole(4)), durations(note(TRIPLI)))
    }

    @Test
    fun gorgonSharesOneBeatWithThePreviousNote() {
        assertValid(note(), note(GORGON))
        assertEquals(listOf(half, half), durations(note(), note(GORGON)))
        assertEquals(one, total(note(), note(GORGON)))
        // In the middle of a melody it touches only its own note and the one before.
        assertEquals(listOf(one, half, half, one), durations(note(), note(), note(GORGON), note()))
    }

    @Test
    fun dottedGorgonGivesThreeQuartersToTheDotsSide() {
        assertValid(note(), note(GORGON_DOT_LEFT))
        assertEquals(listOf(threeQuarters, quarter), durations(note(), note(GORGON_DOT_LEFT)))
        assertEquals(listOf(quarter, threeQuarters), durations(note(), note(GORGON_DOT_RIGHT)))
    }

    @Test
    fun digorgonSharesOneBeatInThirds() {
        assertValid(note(), note(DIGORGON), note())
        assertEquals(listOf(third, third, third), durations(note(), note(DIGORGON), note()))
        assertEquals("three thirds are exactly one χρόνος", one, total(note(), note(DIGORGON), note()))
    }

    @Test
    fun dottedDigorgonGivesHalfToTheDottedNote() {
        assertEquals(listOf(half, quarter, quarter), durations(note(), note(DIGORGON_DOT_BOTTOM), note()))
        assertEquals(listOf(quarter, half, quarter), durations(note(), note(DIGORGON_DOT_MIDDLE), note()))
        assertEquals(listOf(quarter, quarter, half), durations(note(), note(DIGORGON_DOT_TOP), note()))
    }

    @Test
    fun trigorgonSharesOneBeatInQuarters() {
        assertValid(note(), note(TRIGORGON), note(), note())
        assertEquals(listOf(quarter, quarter, quarter, quarter), durations(note(), note(TRIGORGON), note(), note()))
        assertEquals(one, total(note(), note(TRIGORGON), note(), note()))
    }

    @Test
    fun argonGivesTheOligonTwoBeats() {
        assertValid(note(), note(), note(ARGON))
        assertEquals(listOf(half, half, whole(2)), durations(note(), note(), note(ARGON)))
        // The page reads it as a γοργόν on the κεντήματα and a κλάσμα on the ολίγον: the same lengths.
        assertEquals(durations(note(), note(GORGON), note(KLASMA)), durations(note(), note(), note(ARGON)))
    }

    @Test
    fun diargonGivesTheOligonThreeBeats() {
        assertEquals(listOf(half, half, whole(3)), durations(note(), note(), note(DIARGON)))
        assertEquals(durations(note(), note(GORGON), note(DIPLI)), durations(note(), note(), note(DIARGON)))
    }

    @Test
    fun triargonGivesTheOligonFourBeats() {
        assertEquals(listOf(half, half, whole(4)), durations(note(), note(), note(TRIARGON)))
        assertEquals(durations(note(), note(GORGON), note(TRIPLI)), durations(note(), note(), note(TRIARGON)))
    }

    @Test
    fun aRestIsSilentForTheBeatsItsDotsCount() {
        // On a rest the dots count the χρόνοι — 1, 2, 3 — where on a note they add them: 2, 3, 4.
        assertEquals(listOf(one), durations(note(VAREIA_APLI)))
        assertEquals(listOf(whole(2)), durations(note(VAREIA_DIPLI)))
        assertEquals(listOf(whole(3)), durations(note(VAREIA_TRIPLI)))
        assertEquals(listOf(one, whole(2), one), durations(note(), note(VAREIA_DIPLI), note()))
        assertTrue(note(VAREIA_APLI).isRest)
        assertFalse(note(APLI).isRest)
        assertValid(note(), note(VAREIA_TRIPLI), note())
    }

    @Test
    fun sharingKeepsWhatANoteLastsBeyondTheSharedBeat() {
        // A κλάσμα note gives its last χρόνος to the γοργόν that follows it, and keeps the first.
        assertEquals(listOf(one + half, half), durations(note(KLASMA), note(GORGON)))
        // Before the carrier the last χρόνος is shared, from the carrier on the first.
        assertEquals(
            listOf(whole(2) + third, third, one + third),
            durations(note(DIPLI), note(DIGORGON), note(KLASMA)),
        )
    }

    // ---- the edge cases -----------------------------------------------------------------------

    @Test
    fun gorgonOnTheFirstNoteIsInvalid() {
        assertEquals(listOf(RhythmProblem(0, GORGON, Reason.NO_NOTE_BEFORE)), problems(note(GORGON), note()))
        assertEquals(listOf(RhythmProblem(0, GORGON, Reason.NO_NOTE_BEFORE)), problems(note(GORGON)))
        // It shares nothing: no note of the melody is shortened by it.
        assertEquals(listOf(one, one), durations(note(GORGON), note()))
    }

    @Test
    fun gorgonAndKlasmaOnOneNoteAdd() {
        assertValid(note(), note(GORGON, KLASMA))
        assertEquals(listOf(half, one + half), durations(note(), note(GORGON, KLASMA)))
        assertEquals(whole(2), total(note(), note(GORGON, KLASMA)))
    }

    @Test
    fun consecutiveGorgaRunInHalves() {
        assertValid(note(), note(GORGON), note(GORGON), note(GORGON))
        assertEquals(listOf(half, half, half, half), durations(note(), note(GORGON), note(GORGON), note(GORGON)))
        // The run ends on the first note without a γοργόν, which keeps its whole χρόνο.
        assertEquals(listOf(half, half, half, one), durations(note(), note(GORGON), note(GORGON), note()))
    }

    @Test
    fun aGorgonAfterANoteShorterThanABeatTakesNothingFromIt() {
        // The Trainer lets a note last ½ with no sign; a γοργόν after it continues the halves.
        assertValid(note(base = half), note(GORGON))
        assertEquals(listOf(half, half), durations(note(base = half), note(GORGON)))
        // A note of 1½ does have a whole χρόνο to give.
        assertEquals(listOf(one, half), durations(note(base = one + half), note(GORGON)))
    }

    @Test
    fun aDividerMissingANeighbourIsInvalid() {
        assertEquals(listOf(RhythmProblem(1, DIGORGON, Reason.NO_NOTE_AFTER)), problems(note(), note(DIGORGON)))
        assertEquals(
            listOf(RhythmProblem(1, TRIGORGON, Reason.NO_NOTE_AFTER)),
            problems(note(), note(TRIGORGON), note()),
        )
        assertEquals(listOf(RhythmProblem(1, ARGON, Reason.NO_NOTE_BEFORE)), problems(note(), note(ARGON)))
        // An invalid divider shares nothing; what a sign adds still counts.
        assertEquals(listOf(one, whole(2)), durations(note(), note(ARGON)))
    }

    @Test
    fun twoDividersOnOneNoteAreInvalid() {
        assertEquals(
            listOf(RhythmProblem(1, GORGON, Reason.TWO_DIVIDERS), RhythmProblem(1, DIGORGON, Reason.TWO_DIVIDERS)),
            problems(note(), note(DIGORGON, GORGON), note()),
        )
        assertEquals(listOf(one, one, one), durations(note(), note(DIGORGON, GORGON), note()))
    }

    @Test
    fun aDividerCannotTakeABeatThatIsAlreadyShared() {
        // A δίγοργον right after a γοργόν note: that note already shares its only χρόνος.
        assertEquals(
            listOf(RhythmProblem(2, DIGORGON, Reason.NO_BEAT_TO_SHARE)),
            problems(note(), note(GORGON), note(DIGORGON), note()),
        )
        // A γοργόν on the third note of a δίγοργον: its own χρόνος is already the δίγοργον's.
        assertEquals(
            listOf(RhythmProblem(2, GORGON, Reason.NO_BEAT_TO_SHARE)),
            problems(note(), note(DIGORGON), note(GORGON)),
        )
        // Only the plain γοργόν continues a run: a dotted one cannot take ¾ from a half.
        assertEquals(
            listOf(RhythmProblem(2, GORGON_DOT_LEFT, Reason.NO_BEAT_TO_SHARE)),
            problems(note(), note(GORGON), note(GORGON_DOT_LEFT)),
        )
    }

    @Test
    fun aSignOnARestIsInvalid() {
        assertEquals(listOf(RhythmProblem(0, KLASMA, Reason.SIGN_ON_A_REST)), problems(note(VAREIA_APLI, KLASMA)))
        // The rest keeps its own length, whatever else is written on it.
        assertEquals(listOf(one), durations(note(VAREIA_APLI, KLASMA)))
        assertEquals(listOf(one), durations(note(VAREIA_APLI, base = whole(3))))
    }

    @Test
    fun aDividerCannotShareABeatWithARest() {
        // A γοργόν right after a rest: a silence has no χρόνος to share.
        assertEquals(listOf(RhythmProblem(1, GORGON, Reason.REST_IN_GROUP)), problems(note(VAREIA_APLI), note(GORGON)))
        assertEquals(
            listOf(RhythmProblem(1, DIGORGON, Reason.REST_IN_GROUP)),
            problems(note(), note(DIGORGON), note(VAREIA_APLI)),
        )
        assertEquals(listOf(one, one), durations(note(VAREIA_APLI), note(GORGON)))
    }

    @Test
    fun lengthsShorterThanHalfABeatAreAllowed() {
        // The ½ floor is the Trainer's input rule, not a rhythm rule: ⅓ and ¼ come out as they are.
        assertTrue(durations(note(), note(DIGORGON), note()).all { it < half })
        assertTrue(durations(note(), note(TRIGORGON), note(), note()).all { it < half })
        assertEquals(listOf(quarter), durations(note(base = quarter)))
    }

    // ---- the data -----------------------------------------------------------------------------

    @Test
    fun everySharedBeatIsExactlyOneBeat() {
        val dividers = TimeSign.entries.filter { it.isDivider }
        assertEquals("the γοργόν, δίγοργον, τρίγοργον and αργόν families", 11, dividers.size)
        dividers.forEach { sign ->
            assertEquals("$sign shares one χρόνο", one, sign.shares.fold(Beats.ZERO) { sum, share -> sum + share })
        }
    }

    @Test
    fun thirdsAreExact() {
        assertEquals(one, third * 3)
        listOf(2, 3, 4).forEach { assertEquals("1/$it of a χρόνος is whole ticks", 0, Beats.TICKS_PER_BEAT % it) }
    }
}
