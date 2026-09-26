package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.music.TimeSign
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * A stored Trainer melody never brings back a rest (ClickUp `869f5x25n`, F4).
 *
 * F4 put the rests — βαρεία with απλή, διπλή, τριπλή — into [TimeSign], so their ids became ids a sign
 * has. The Trainer's notes are sung and its player has no silence: a rest read onto one would sound for
 * the rest's length. Before F4 the codec refused those ids as unknown, and it still refuses them — the
 * whole melody, exactly as it refuses a sign the app does not know.
 */
class TrainerMelodyRefusesRestsTest {

    private val melody = TrainerMelody(
        notes = listOf(
            TrainerNote(PhthongName.PA),
            TrainerNote(PhthongName.VOU, signs = setOf(TimeSign.GORGON)),
        ),
    )

    /** [melody] stored, its second note's signs replaced by [ids], and read back. */
    private fun readWithSecondNoteSigns(vararg ids: String): TrainerMelody? {
        val json = JSONObject(TrainerMelodyCodec.encodeString(melody))
        json.getJSONArray("notes").getJSONObject(1).put("signs", JSONArray(ids.toList()))
        return TrainerMelodyCodec.decode(json)
    }

    @Test
    fun aRestOnANoteRefusesTheMelody() {
        val rests = TimeSign.entries.filter { it.isRest }
        // The slice is F4's three rests, and each is an id the rules know: the refusal is the codec's own.
        assertEquals(listOf("vareia_apli", "vareia_dipli", "vareia_tripli"), rests.map { it.id })
        rests.forEach { rest ->
            assertEquals(rest, TimeSign.fromId(rest.id))
            assertNull(rest.id, readWithSecondNoteSigns(rest.id))
            assertNull("${rest.id} next to the γοργόν", readWithSecondNoteSigns("gorgo", rest.id))
        }
    }

    @Test
    fun theSameEditWithTheTrainersOwnSignIsRead() {
        // The positive control: rewriting a note's signs is not what refuses a melody.
        assertEquals(melody, readWithSecondNoteSigns("gorgo"))
    }
}
