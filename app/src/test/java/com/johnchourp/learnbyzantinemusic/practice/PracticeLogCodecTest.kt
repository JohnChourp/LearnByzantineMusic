package com.johnchourp.learnbyzantinemusic.practice

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * The practice history as stored on the device (ClickUp `869f5x2dy`): what is written, and that
 * reading never throws — a damaged preference costs the history, never the screen.
 */
class PracticeLogCodecTest {

    private fun day(text: String) = LocalDate.parse(text)

    private val sample = PracticeLog()
        .record(day("2026-09-25"), 5)
        .record(day("2026-09-23"), 6)
        .record(day("2026-09-25"), 4)

    @Test
    fun whatIsWrittenReadsBackTheSame() {
        assertEquals(sample, PracticeLogCodec.decode(PracticeLogCodec.encode(sample)))
        assertEquals(PracticeLog(), PracticeLogCodec.decode(PracticeLogCodec.encode(PracticeLog())))
    }

    @Test
    fun theStoredShapeIsVersionOneOldestDayFirst() {
        val json = JSONObject(PracticeLogCodec.encode(sample))
        assertEquals(1, json.getInt("schemaVersion"))
        val days = json.getJSONArray("days")
        assertEquals(2, days.length())
        assertEquals("2026-09-23", days.getJSONObject(0).getString("date"))
        assertEquals(1, days.getJSONObject(0).getInt("sessions"))
        assertEquals(6, days.getJSONObject(0).getInt("minutes"))
        assertEquals("2026-09-25", days.getJSONObject(1).getString("date"))
        assertEquals(2, days.getJSONObject(1).getInt("sessions"))
        assertEquals(9, days.getJSONObject(1).getInt("minutes"))
    }

    @Test
    fun unreadableTextReadsAsAnEmptyHistory() {
        listOf(null, "", "   ", "not json", "[1,2]", "{}", "{\"days\":\"nope\"}").forEach { text ->
            assertEquals("«$text»", PracticeLog(), PracticeLogCodec.decode(text))
        }
    }

    @Test
    fun aBadDayIsSkippedAndTheRestKept() {
        val text = """
            {"schemaVersion":1,"days":[
              {"date":"2026-09-20","sessions":1,"minutes":5},
              {"date":"20/09/2026","sessions":1,"minutes":5},
              {"date":"2026-09-21","sessions":-1,"minutes":5},
              {"date":"2026-09-22","minutes":5},
              "not a day",
              {"date":"2026-09-24","sessions":1,"minutes":7}
            ]}
        """.trimIndent()
        val log = PracticeLogCodec.decode(text)
        assertEquals(setOf(day("2026-09-20"), day("2026-09-24")), log.days.keys)
    }

    @Test
    fun theSameDayTwiceIsAddedUp() {
        val text = """{"schemaVersion":1,"days":[{"date":"2026-09-20","sessions":1,"minutes":5},{"date":"2026-09-20","sessions":2,"minutes":8}]}"""
        assertEquals(PracticeDay(sessions = 3, minutes = 13), PracticeLogCodec.decode(text).days.getValue(day("2026-09-20")))
    }

    @Test
    fun aNewerVersionIsReadAsFarAsItsDaysGo() {
        val text = """{"schemaVersion":2,"source":"future","days":[{"date":"2026-09-20","sessions":1,"minutes":5,"kind":"cards"}]}"""
        assertEquals(setOf(day("2026-09-20")), PracticeLogCodec.decode(text).practisedDays)
    }
}
