package com.johnchourp.learnbyzantinemusic.recordings.analysis

import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.music.PhthongName.DI
import com.johnchourp.learnbyzantinemusic.music.PhthongName.GA
import com.johnchourp.learnbyzantinemusic.music.PhthongName.KE
import com.johnchourp.learnbyzantinemusic.music.PhthongName.NI
import com.johnchourp.learnbyzantinemusic.music.PhthongName.PA
import com.johnchourp.learnbyzantinemusic.music.PhthongName.VOU
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class PhthongSegmenterTest {
    private val diatonic = ModeScalePositions.forMode(Mode.FIRST)
    private val singerNi = 196.0 // the singer's own Νη; not the trainer's 220 Hz
    private val hopMs = 23.2

    /** A frame-level track: each entry is (phthong or null for silence, octave, frames, moria offset). */
    private fun track(vararg parts: Part): PitchTrack {
        val frames = ArrayList<PitchFrame>()
        for (part in parts) {
            repeat(part.frames) {
                val hz = if (part.phthong == null) {
                    PitchFrame.UNVOICED
                } else {
                    val moria = part.octave * 72.0 + diatonic[part.phthong.ordinal] + part.offsetMoria
                    (singerNi * 2.0.pow(moria / 72.0)).toFloat()
                }
                frames += PitchFrame((frames.size * hopMs).toLong(), hz)
            }
        }
        return PitchTrack(frames, hopMs, (frames.size * hopMs).toLong())
    }

    private data class Part(val phthong: PhthongName?, val frames: Int, val octave: Int = 0, val offsetMoria: Double = 0.0)

    private fun note(phthong: PhthongName, frames: Int = 12, octave: Int = 0, offset: Double = 0.0) = Part(phthong, frames, octave, offset)
    private fun rest(frames: Int) = Part(null, frames)

    @Test
    fun calibratesOnTheStartingPhthong() {
        val niHz = PhthongSegmenter.calibrate(track(note(PA), note(VOU)), diatonic, PA)!!
        assertEquals(singerNi, niHz, 0.5)
    }

    @Test
    fun recognisesAMelodyInOrder() {
        val melody = track(note(PA), note(VOU), note(GA), rest(4), note(VOU), note(PA), note(NI))
        val niHz = PhthongSegmenter.calibrate(melody, diatonic, PA)!!
        val notes = PhthongSegmenter.segment(melody, niHz, diatonic)
        assertEquals(listOf(PA, VOU, GA, VOU, PA, NI), notes.map { it.phthong })
        notes.forEach { assertEquals(0, it.octave) }
    }

    @Test
    fun aRepeatedNoteCountsTwiceOnlyWhenSeparatedBySilence() {
        val separated = track(note(PA), rest(5), note(PA))
        val joined = track(note(PA), rest(1), note(PA))
        val niHz = singerNi
        assertEquals(2, PhthongSegmenter.segment(separated, niHz, diatonic).size)
        assertEquals(1, PhthongSegmenter.segment(joined, niHz, diatonic).size)
    }

    @Test
    fun ignoresShortBlipsInsideANote() {
        val blip = track(note(DI, frames = 10), note(KE, frames = 2), note(DI, frames = 10))
        val notes = PhthongSegmenter.segment(blip, singerNi, diatonic)
        assertEquals(listOf(DI), notes.map { it.phthong })
    }

    @Test
    fun keepsOctavesAndDeviation() {
        val melody = track(note(ZO_LOW.first, octave = ZO_LOW.second), note(NI), note(PA, octave = 1, offset = 3.0))
        val notes = PhthongSegmenter.segment(melody, singerNi, diatonic)
        assertEquals(listOf(-1, 0, 1), notes.map { it.octave })
        assertEquals(3.0, notes.last().deviationMoria, 0.5)
        assertTrue(notes[1].degree > notes[0].degree && notes[2].degree > notes[1].degree)
    }

    @Test
    fun nearestDegreeResolvesTheOctaveSeamUpwards() {
        val (degree, deviation) = PhthongSegmenter.nearestDegree(71.0, diatonic)
        assertEquals(7, degree) // Νη΄
        assertEquals(-1.0, deviation, 1e-9)
    }

    companion object {
        private val ZO_LOW = PhthongName.ZO to -1
    }
}
