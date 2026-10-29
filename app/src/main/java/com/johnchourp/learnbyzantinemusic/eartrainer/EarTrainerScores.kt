package com.johnchourp.learnbyzantinemusic.eartrainer

import com.johnchourp.learnbyzantinemusic.music.Mode
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** How a ήχος has gone in «Μάντεψε τον ήχο»: questions answered, and how many of them right. */
data class ModeScore(val answered: Int, val correct: Int)

/**
 * The record of «Μάντεψε τον ήχο» (ClickUp `869f4tqb9`), per ήχος **asked**: each answer counts for
 * the ήχος that sounded, whatever was chosen. Local only — no account, no server.
 *
 * A round counts nowhere else (owner decision, 2026-10-29): not in the streak of «Πεντάλεπτο της
 * ημέρας», whose days mean a completed session.
 */
data class EarTrainerScores(val byMode: Map<Mode, ModeScore> = emptyMap()) {

    fun of(mode: Mode): ModeScore = byMode[mode] ?: ModeScore(answered = 0, correct = 0)

    /** One more answer to a question in [asked], right or not. */
    fun record(asked: Mode, correct: Boolean): EarTrainerScores {
        val before = of(asked)
        val after = ModeScore(before.answered + 1, before.correct + if (correct) 1 else 0)
        return copy(byMode = byMode + (asked to after))
    }
}

/**
 * [EarTrainerScores] ↔ the JSON stored on the device:
 *
 *     {"schemaVersion":1,"modes":[{"mode":"first","answered":4,"correct":3}, …]}
 *
 * In the order of the cycle, by `Mode.key`, which is frozen. An array and not an object keyed by
 * mode, so the order of the modes never depends on a map: the «Δεδομένα μάθησης» file imports this
 * value only in the exact form [encode] writes. (The keys inside each object follow org.json, which
 * writes them in one order on every Android device and may use another on the JVM of the tests.) **The format is stored on users' devices**: add fields, never
 * rename one, and write a new field only when it has something to say.
 *
 * Reading never throws. An entry it cannot read — an unknown ήχος, a negative count, more right than
 * answered — is skipped; the same ήχος twice is added up; a value that is not JSON at all reads as no
 * scores: a damaged preference costs the record, never the screen. A newer schemaVersion is read as
 * far as its entries go.
 */
object EarTrainerScoresCodec {
    const val SCHEMA_VERSION = 1

    fun encode(scores: EarTrainerScores): String {
        val modes = JSONArray()
        Mode.entries.forEach { mode ->
            val score = scores.byMode[mode] ?: return@forEach
            modes.put(
                JSONObject()
                    .put("mode", mode.key)
                    .put("answered", score.answered)
                    .put("correct", score.correct)
            )
        }
        return JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("modes", modes)
            .toString()
    }

    fun decode(text: String?): EarTrainerScores {
        if (text.isNullOrBlank()) return EarTrainerScores()
        val root = try {
            JSONObject(text)
        } catch (_: JSONException) {
            return EarTrainerScores()
        }
        val entries = root.optJSONArray("modes") ?: return EarTrainerScores()
        val byMode = mutableMapOf<Mode, ModeScore>()
        for (index in 0 until entries.length()) {
            val entry = entries.optJSONObject(index) ?: continue
            val mode = Mode.fromKey(entry.optString("mode")) ?: continue
            val answered = entry.optInt("answered", -1)
            val correct = entry.optInt("correct", -1)
            if (answered < 0 || correct !in 0..answered) continue
            val before = byMode[mode]
            byMode[mode] = if (before == null) {
                ModeScore(answered, correct)
            } else {
                ModeScore(before.answered + answered, before.correct + correct)
            }
        }
        return EarTrainerScores(byMode)
    }
}
