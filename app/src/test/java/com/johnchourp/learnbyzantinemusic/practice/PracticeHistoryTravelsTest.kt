package com.johnchourp.learnbyzantinemusic.practice

import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile.Item
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile.Line
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile.Reason
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile.Result.Accepted
import com.johnchourp.learnbyzantinemusic.settings.LearningDataFile.Result.Rejected
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The practice history in the «Δεδομένα μάθησης» file (ClickUp `869f5x2dy` on top of `869f5x25w`):
 * the streak goes with the learner to a new phone, in the one form the app writes; an empty history
 * is never carried, so an import cannot wipe one; and the reminder, which Android has to allow on
 * each phone, never travels.
 */
class PracticeHistoryTravelsTest {

    private val practiceFile = AppPrefs.Store.PRACTICE.fileName

    private fun day(dayOfMonth: Int) = LocalDate.of(2026, 9, dayOfMonth)

    private fun exported(storedLog: String): Accepted {
        val file = LearningDataFile.encode(mapOf(AppPrefs.Store.PRACTICE to mapOf("practice_log" to storedLog)), 1L, "1.18.0")
        val result = LearningDataFile.decode(file)
        assertTrue("expected the export to import back, got $result", result is Accepted)
        return result as Accepted
    }

    /** A version 1 file carrying one entry of the practice preferences file. */
    private fun fileWith(name: String, type: String, value: Any): String =
        JSONObject()
            .put("schemaVersion", 1)
            .put("exportedAt", 1L)
            .put("appVersion", "1.18.0")
            .put("stores", JSONObject().put(practiceFile, JSONObject().put(name, JSONObject().put("type", type).put("value", value))))
            .toString()

    @Test
    fun `a history travels in the form the app writes it, and imports back exactly`() {
        // Out of order, and the same day twice: what a damaged or hand-edited preference could hold.
        val stored = """
            {"schemaVersion":1,"days":[
              {"date":"2026-09-25","sessions":1,"minutes":6},
              {"date":"2026-09-24","sessions":1,"minutes":5},
              {"date":"2026-09-25","sessions":1,"minutes":5}
            ]}
        """.trimIndent()
        val asTheAppWritesIt = PracticeLogCodec.encode(
            PracticeLog(mapOf(day(24) to PracticeDay(1, 5), day(25) to PracticeDay(2, 11)))
        )

        val changes = exported(stored).changes

        assertEquals(mapOf(AppPrefs.Store.PRACTICE to mapOf("practice_log" to asTheAppWritesIt)), changes)
        // And the new phone reads the same streak from it.
        val imported = PracticeLogCodec.decode(changes.getValue(AppPrefs.Store.PRACTICE).getValue("practice_log") as String)
        assertEquals(2, PracticeStreak.current(imported.practisedDays, day(25)))
    }

    @Test
    fun `an empty or unreadable history is not carried, so an import never replaces a history with nothing`() {
        listOf("", "not json", """{"schemaVersion":1,"days":[]}""", """{"schemaVersion":1}""").forEach { stored ->
            assertEquals("stored: $stored", emptyMap<AppPrefs.Store, Map<String, Any>>(), exported(stored).changes)
        }
    }

    @Test
    fun `a history that is not in that form rejects the whole file`() {
        listOf(
            """{"schemaVersion":1,"days":[]}""",
            """{"schemaVersion":1,"days":[{"date":"yesterday","sessions":1,"minutes":5}]}""",
            """{"days":[{"date":"2026-09-25","sessions":1,"minutes":5}],"schemaVersion":1} """,
        ).forEach { value ->
            assertEquals(
                "value: $value",
                Rejected(Reason.BAD_VALUE, "practice_log"),
                LearningDataFile.decode(fileWith("practice_log", "STRING", value)),
            )
        }
    }

    @Test
    fun `the reminder never travels, and a file that carries it is refused`() {
        val stored = mapOf(
            "practice_reminder_enabled" to true,
            "practice_reminder_minute_of_day" to 1140,
        )
        val file = LearningDataFile.encode(mapOf(AppPrefs.Store.PRACTICE to stored), 1L, "1.18.0")
        assertEquals(emptyMap<AppPrefs.Store, Map<String, Any>>(), (LearningDataFile.decode(file) as Accepted).changes)

        assertEquals(
            Rejected(Reason.NOT_IMPORTABLE, "practice_reminder_enabled"),
            LearningDataFile.decode(fileWith("practice_reminder_enabled", "BOOLEAN", true)),
        )
        assertEquals(
            Rejected(Reason.NOT_IMPORTABLE, "practice_reminder_minute_of_day"),
            LearningDataFile.decode(fileWith("practice_reminder_minute_of_day", "INT", 1140)),
        )
    }

    @Test
    fun `the import dialog says how many days the history holds`() {
        val stored = PracticeLogCodec.encode(
            PracticeLog(mapOf(day(1) to PracticeDay(1, 5), day(2) to PracticeDay(3, 16), day(20) to PracticeDay(1, 30)))
        )

        assertEquals(listOf(Line(Item.PRACTICE, 3)), LearningDataFile.summary(exported(stored)))
    }
}
