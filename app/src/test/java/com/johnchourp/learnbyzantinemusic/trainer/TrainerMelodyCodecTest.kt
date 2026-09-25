package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.music.TimeSign
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The stored form of a Trainer melody (ClickUp `869f5x261`, F6): what is written is read back whole,
 * a value the app could not have written is rejected rather than half-read, and a value out of range
 * is brought back into it.
 */
class TrainerMelodyCodecTest {

    private val melody = TrainerMelody(
        notes = listOf(
            TrainerNote(PhthongName.NI, octaveShift = -1, baseDurationBeats = 0.5f),
            TrainerNote(PhthongName.PA, octaveShift = 0, baseDurationBeats = 1.5f, signs = setOf(TimeSign.GORGON)),
            TrainerNote(PhthongName.ZO, octaveShift = 1, baseDurationBeats = 4f),
        ),
        bpm = 100,
        scale = TrainerScale(Mode.SECOND, -4),
    )

    private fun stored(melody: TrainerMelody): JSONObject = JSONObject(TrainerMelodyCodec.encodeString(melody))

    /** [melody]'s stored form with [edit] applied, read back. */
    private fun readAfter(edit: JSONObject.() -> Unit): TrainerMelody? = TrainerMelodyCodec.decode(stored(melody).apply(edit))

    private fun JSONObject.firstNote(): JSONObject = getJSONArray("notes").getJSONObject(0)

    @Test
    fun whatIsWrittenIsReadBackWhole() {
        assertEquals(melody, TrainerMelodyCodec.decodeString(TrainerMelodyCodec.encodeString(melody)))
    }

    @Test
    fun theStoredIdentifiersAreTheFrozenOnes() {
        val json = stored(melody)
        assertEquals(1, json.get("schemaVersion"))
        assertEquals(100, json.get("bpm"))
        assertEquals("second", json.get("mode"))
        assertEquals(-4, json.get("baseShift"))
        val second = json.getJSONArray("notes").getJSONObject(1)
        assertEquals("PA", second.get("phthong"))
        assertEquals(0, second.get("octave"))
        assertEquals(1.5, second.getDouble("length"), 0.0)
        assertEquals("gorgo", second.getJSONArray("signs").getString(0))
    }

    @Test
    fun theDefaultScaleStoresNoShift() {
        val json = stored(melody.copy(scale = TrainerScale.DIATONIC))
        assertFalse(json.has("mode"))
        assertFalse(json.has("baseShift"))
        assertEquals(TrainerScale.DIATONIC, TrainerMelodyCodec.decode(json)!!.scale)
    }

    @Test
    fun anotherSchemaVersionIsRejected() {
        assertNull("newer", readAfter { put("schemaVersion", 2) })
        assertNull("missing", readAfter { remove("schemaVersion") })
    }

    @Test
    fun anUnknownPhthongRejectsTheMelody() {
        assertNull(readAfter { firstNote().put("phthong", "XX") })
        assertNull("names are exact", readAfter { firstNote().put("phthong", "ni") })
    }

    @Test
    fun anUnknownSignOrModeRejectsTheMelody() {
        // A sign the app does not know would play with the wrong timing: no half-read melody.
        assertNull(readAfter { getJSONArray("notes").getJSONObject(1).put("signs", JSONArray(listOf("gorgo", "tsakisma"))) })
        assertNull(readAfter { put("mode", "ninth") })
    }

    @Test
    fun missingRequiredFieldsAndTooManyNotesAreRejected() {
        assertNull(readAfter { remove("bpm") })
        assertNull(readAfter { remove("notes") })
        assertNull(readAfter { firstNote().remove("octave") })
        assertNull(readAfter { firstNote().remove("length") })
        val tooLong = TrainerMelody(notes = List(MelodySequence.MAX_NOTES + 1) { TrainerNote(PhthongName.DI) })
        assertNull(TrainerMelodyCodec.decode(stored(tooLong)))
        val atTheCap = TrainerMelody(notes = List(MelodySequence.MAX_NOTES) { TrainerNote(PhthongName.DI) })
        assertEquals(atTheCap, TrainerMelodyCodec.decode(stored(atTheCap)))
    }

    @Test
    fun valuesOutsideTheirRangeAreClamped() {
        assertEquals(MelodyTempo.MIN_BPM, readAfter { put("bpm", 5) }!!.bpm)
        assertEquals(MelodyTempo.MAX_BPM, readAfter { put("bpm", 999) }!!.bpm)
        assertEquals(TrainerScale.MIN_NOTE_OCTAVE, readAfter { firstNote().put("octave", -9) }!!.notes[0].octaveShift)
        assertEquals(TrainerScale.MAX_NOTE_OCTAVE, readAfter { firstNote().put("octave", 5) }!!.notes[0].octaveShift)
        assertEquals(MelodySequence.MIN_LENGTH_BEATS, readAfter { firstNote().put("length", 0.1) }!!.notes[0].baseDurationBeats)
        assertEquals(MelodySequence.MAX_LENGTH_BEATS, readAfter { firstNote().put("length", 9.0) }!!.notes[0].baseDurationBeats)
        assertEquals(BaseShift.MAX_MORIA, readAfter { put("baseShift", 40) }!!.scale.baseShiftMoria)
        // «Διατονικός» has no shift: a stray one is ignored, not applied.
        val diatonic = stored(melody.copy(scale = TrainerScale.DIATONIC)).apply { put("baseShift", 7) }
        assertEquals(TrainerScale.DIATONIC, TrainerMelodyCodec.decode(diatonic)!!.scale)
    }

    @Test
    fun aGorgonOnTheFirstNoteIsTakenOffOnTheWayIn() {
        // The rule of H5: a γοργόν shares a χρόνο with the note before it, so none can stand first.
        val restored = readAfter { firstNote().put("signs", JSONArray(listOf("gorgo"))) }!!
        assertFalse(restored.notes[0].hasGorgo)
        assertTrue("the others keep theirs", restored.notes[1].hasGorgo)
        assertEquals(melody.notes.map { it.phthong }, restored.notes.map { it.phthong })
    }

    @Test
    fun textThatIsNotAMelodyIsNothing() {
        listOf(null, "", "   ", "not json", "[]", "{}", "{\"schemaVersion\": 1}").forEach { raw ->
            assertNull("«$raw»", TrainerMelodyCodec.decodeString(raw))
        }
    }

    @Test
    fun anEmptyMelodyIsAMelody() {
        // The user cleared the melody: that is what comes back, not an error.
        val empty = TrainerMelody()
        assertEquals(empty, TrainerMelodyCodec.decodeString(TrainerMelodyCodec.encodeString(empty)))
    }
}
