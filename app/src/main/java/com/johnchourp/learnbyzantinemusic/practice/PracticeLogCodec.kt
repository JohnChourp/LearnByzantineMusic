package com.johnchourp.learnbyzantinemusic.practice

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * [PracticeLog] ↔ the JSON stored on the device (ClickUp `869f5x2dy`):
 *
 *     {"schemaVersion":1,"days":[{"date":"2026-09-25","sessions":1,"minutes":5}, …]}
 *
 * Oldest day first. **The format is stored on users' devices**: add fields, never rename one.
 *
 * Reading never throws. What it cannot read it skips — a day with a bad date, a negative count — and
 * a value that is not JSON at all reads as an empty log, so a damaged preference costs the history,
 * never the screen. The same day twice (a hand edit, a merge) is added up. A newer schemaVersion is
 * read as far as its days go.
 */
object PracticeLogCodec {
    const val SCHEMA_VERSION = 1

    fun encode(log: PracticeLog): String {
        val days = JSONArray()
        log.days.toSortedMap().forEach { (date, day) ->
            days.put(
                JSONObject()
                    .put("date", date.toString())
                    .put("sessions", day.sessions)
                    .put("minutes", day.minutes)
            )
        }
        return JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("days", days)
            .toString()
    }

    fun decode(text: String?): PracticeLog {
        if (text.isNullOrBlank()) return PracticeLog()
        val root = try {
            JSONObject(text)
        } catch (_: JSONException) {
            return PracticeLog()
        }
        val entries = root.optJSONArray("days") ?: return PracticeLog()
        val days = mutableMapOf<LocalDate, PracticeDay>()
        for (index in 0 until entries.length()) {
            val entry = entries.optJSONObject(index) ?: continue
            val date = parseDate(entry.optString("date")) ?: continue
            val sessions = entry.optInt("sessions", -1)
            val minutes = entry.optInt("minutes", -1)
            if (sessions < 0 || minutes < 0) continue
            val before = days[date]
            days[date] = if (before == null) {
                PracticeDay(sessions, minutes)
            } else {
                PracticeDay(before.sessions + sessions, before.minutes + minutes)
            }
        }
        return PracticeLog(days)
    }

    private fun parseDate(text: String): LocalDate? =
        try {
            LocalDate.parse(text)
        } catch (_: DateTimeParseException) {
            null
        }
}
