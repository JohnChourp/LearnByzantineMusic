package com.johnchourp.learnbyzantinemusic.summary_theory.ui

import com.johnchourp.learnbyzantinemusic.lessons.ui.MetronomeSchedule
import com.johnchourp.learnbyzantinemusic.modes.EightModeScaleDefinitions
import com.johnchourp.learnbyzantinemusic.modes.ModeLadders
import com.johnchourp.learnbyzantinemusic.music.ByzantineRhythmMapper
import com.johnchourp.learnbyzantinemusic.music.ModeLadder
import com.johnchourp.learnbyzantinemusic.music.Phthong
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.music.RhythmNote
import com.johnchourp.learnbyzantinemusic.music.RhythmTimeline

/**
 * What «Άκου» plays for an example of the «Χαρακτήρες Χρόνου» page (ClickUp `869f5x25n`, F4).
 *
 * Nothing here is typed in twice. The lengths are [ByzantineRhythmMapper]'s — the time rules the
 * Melody Trainer plays by — applied to the example's own [TimeEquation.rhythm], and the pitches come
 * from the app's one diatonic ladder, the one the 8 Ήχοι page draws. The page used to explain time
 * with text and pictures only; now every example sounds, with a metronome click under it.
 *
 * | What | Where it comes from |
 * |---|---|
 * | how long each note lasts, rests included | [ByzantineRhythmMapper] on the example's rhythm |
 * | which φθόγγος it is sung on | [START], then each note moves by the φωνές of the sign it is written with ([Neume.voices]); ίσον, and a note with no quantity sign, repeat the previous one |
 * | its pitch | [EightModeScaleDefinitions.DIATONIC]'s ladder, as on the 8 Ήχοι page |
 * | the tempo | the metronome's saved tempo; «Αργά» half of it ([tempo]) |
 * | when anything happens | one [RhythmTimeline]: notes, rests and clicks on one clock |
 */
object TimeExamplePlayback {

    /** Where every example starts: Πα of the middle octave. */
    val START: Phthong = Phthong(PhthongName.PA)

    /** The 8 Ήχοι page's diatonic ladder, unshifted — never a second pitch table. */
    private val ladder: ModeLadder = EightModeScaleDefinitions.DIATONIC.ladder(octaves = ModeLadders.OCTAVES)

    private val ascending: List<Phthong> = ladder.phthongi.reversed()

    /** «Άκου» plays at the metronome's saved tempo; «Αργά» at half of it, never slower than the metronome goes. */
    fun tempo(savedBpm: Int, slow: Boolean): Int =
        if (slow) (savedBpm / 2).coerceAtLeast(MetronomeSchedule.MIN_BPM) else savedBpm

    /**
     * The φθόγγος each note of [rhythm] is sung on — null for the rests. The first note moves from
     * [START], every other from the note before it, by the φωνές of its [quantitySigns] entry.
     */
    fun melody(rhythm: List<RhythmNote>, quantitySigns: List<Neume?>): List<Phthong?> {
        require(quantitySigns.size == rhythm.size) { "${rhythm.size} notes but ${quantitySigns.size} signs" }
        var rung = ascending.indexOf(START)
        return rhythm.mapIndexed { index, note ->
            if (note.isRest) {
                null
            } else {
                rung += quantitySigns[index]?.voices ?: 0
                ascending.getOrNull(rung) ?: error("note $index leaves the ladder")
            }
        }
    }

    /** The frequency of [phthong] on the ladder. */
    fun frequencyOf(phthong: Phthong): Double =
        checkNotNull(ladder.stepFor(phthong)) { "$phthong is not on the diatonic ladder" }.frequencyHz

    /** Everything «Άκου» does for [equation] at [bpm], on one clock. */
    fun cues(equation: TimeEquation, bpm: Int): List<RhythmTimeline.Cue> =
        cues(equation.rhythm, equation.quantitySigns, bpm)

    /** Everything «Άκου» does for a row of the κλάσμα/κουκίδες or the rests table, on one clock. */
    fun cues(row: TimeSymbolRow, bpm: Int): List<RhythmTimeline.Cue> =
        cues(row.rhythm, row.rhythm.map { null }, bpm)

    private fun cues(rhythm: List<RhythmNote>, quantitySigns: List<Neume?>, bpm: Int): List<RhythmTimeline.Cue> =
        RhythmTimeline.of(rhythm, melody(rhythm, quantitySigns).map { it?.let(::frequencyOf) }, bpm)
}
