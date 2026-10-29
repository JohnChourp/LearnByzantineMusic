package com.johnchourp.learnbyzantinemusic.eartrainer

import com.johnchourp.learnbyzantinemusic.music.Mode
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The record of «Μάντεψε τον ήχο» as stored on the device (ClickUp `869f4tqb9`): what is written,
 * and that reading never throws — a damaged preference costs the record, never the screen.
 */
class EarTrainerScoresCodecTest {

    private val sample = EarTrainerScores()
        .record(Mode.THIRD, correct = true)
        .record(Mode.THIRD, correct = false)
        .record(Mode.PLAGAL_FOURTH, correct = true)

    @Test
    fun eachAnswerCountsForTheModeThatSounded() {
        assertEquals(ModeScore(answered = 2, correct = 1), sample.of(Mode.THIRD))
        assertEquals(ModeScore(answered = 1, correct = 1), sample.of(Mode.PLAGAL_FOURTH))
        assertEquals(ModeScore(answered = 0, correct = 0), sample.of(Mode.FIRST))
    }

    @Test
    fun whatIsWrittenReadsBackTheSame() {
        assertEquals(sample, EarTrainerScoresCodec.decode(EarTrainerScoresCodec.encode(sample)))
        assertEquals(EarTrainerScores(), EarTrainerScoresCodec.decode(EarTrainerScoresCodec.encode(EarTrainerScores())))
    }

    @Test
    fun theStoredShapeIsVersionOneInTheOrderOfTheCycle() {
        val json = JSONObject(EarTrainerScoresCodec.encode(sample))
        assertEquals(1, json.getInt("schemaVersion"))
        val modes = json.getJSONArray("modes")
        assertEquals(2, modes.length())
        // Γ΄ (3) before Πλ.Δ΄ (8), whatever order the answers came in.
        assertEquals("third", modes.getJSONObject(0).getString("mode"))
        assertEquals(2, modes.getJSONObject(0).getInt("answered"))
        assertEquals(1, modes.getJSONObject(0).getInt("correct"))
        assertEquals("plagal_fourth", modes.getJSONObject(1).getString("mode"))
        // The same scores always give the same text: the import compares it as text.
        val reversed = EarTrainerScores()
            .record(Mode.PLAGAL_FOURTH, correct = true)
            .record(Mode.THIRD, correct = false)
            .record(Mode.THIRD, correct = true)
        assertEquals(EarTrainerScoresCodec.encode(sample), EarTrainerScoresCodec.encode(reversed))
    }

    @Test
    fun aDamagedValueReadsAsNoScores() {
        listOf(null, "", "   ", "not json", "[]", "{}", """{"modes":7}""", """{"modes":{}}""").forEach { text ->
            assertEquals("«$text»", EarTrainerScores(), EarTrainerScoresCodec.decode(text))
        }
    }

    @Test
    fun anEntryThatCannotBeTrueIsSkippedAndTheRestKept() {
        val text = """
            {"schemaVersion":2,"modes":[
              {"mode":"first","answered":3,"correct":2},
              {"mode":"second","answered":-1,"correct":0},
              {"mode":"third","answered":2,"correct":5},
              {"mode":"fourth","answered":"many"},
              {"mode":"ninth","answered":1,"correct":1},
              7
            ]}
        """.trimIndent()
        assertEquals(
            EarTrainerScores(mapOf(Mode.FIRST to ModeScore(answered = 3, correct = 2))),
            EarTrainerScoresCodec.decode(text),
        )
    }

    @Test
    fun theSameModeTwiceIsAddedUp() {
        val text = """{"modes":[{"mode":"varys","answered":2,"correct":1},{"mode":"varys","answered":3,"correct":3}]}"""
        assertEquals(ModeScore(answered = 5, correct = 4), EarTrainerScoresCodec.decode(text).of(Mode.VARYS))
    }
}
