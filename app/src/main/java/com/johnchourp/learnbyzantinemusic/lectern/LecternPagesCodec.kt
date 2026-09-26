package com.johnchourp.learnbyzantinemusic.lectern

import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.Phthong
import com.johnchourp.learnbyzantinemusic.recordings.analysis.StoredPhthongs
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * The stored form of one PDF's page → ήχος map (ClickUp `869f5x2e7`): the value of
 * `AppPrefs.lecternPagesKeyName(<sha256>)` — and, through that key, what the «Δεδομένα μάθησης» file
 * carries to another phone.
 *
 * **Format, version 1 — readable forever:**
 * `{"schemaVersion":1,"assignments":[{"pageIndex":0,"mode":"first","shiftMoria":0},`
 * `{"pageIndex":4,"mode":"plagal_first","shiftMoria":-3,"ison":{"phthong":"PA","octave":0}}]}`.
 * Only frozen identifiers are written: `Mode.key` and the φθόγγος constant names of [StoredPhthongs].
 * No URI, no file name — the PDF itself is named by the key, its SHA-256. `ison` is absent for the
 * mode's base. A newer format raises the version and keeps reading this one.
 *
 * **One canonical text.** [encode] writes the keys in a fixed order, without spaces, pages ascending.
 * It builds the text itself rather than calling `JSONObject.toString()`, whose key order differs between
 * Android's org.json and the JVM's; so the same map is always the same string, on the phone and in the
 * tests. That is what lets the import accept exactly what the export wrote ([normalized]).
 *
 * **Reading is all or nothing.** [decode] refuses the whole value when any part is off — a page twice,
 * an unknown ήχος or φθόγγος, a wrong type — rather than guess at half of it. What it returns is the
 * app's own view: the shift inside [BaseShift], the base as «no choice», a φθόγγος the ήχος does not
 * offer read as the base (`PageAssignments.normalizedChoice`). A value from a newer app is [Decoded.Newer]:
 * the lectern then shows nothing for that PDF and does not save over it.
 */
object LecternPagesCodec {

    const val SCHEMA_VERSION = 1

    /** The last page index a map may name: a hundred thousand pages is past any real book. */
    const val MAX_PAGE_INDEX = 99_999

    sealed interface Decoded {
        /** Readable: the assignments, sorted by page, in the app's own terms. Empty when nothing is stored. */
        data class Assignments(val assignments: List<PageAssignment>) : Decoded

        /** Written by a newer app, in a format this one cannot read: leave it alone. */
        data object Newer : Decoded

        /** Not this format, or damaged. */
        data object Unreadable : Decoded
    }

    /** The canonical text of [assignments] (see the class header). */
    fun encode(assignments: List<PageAssignment>): String = buildString {
        append("{\"schemaVersion\":").append(SCHEMA_VERSION).append(",\"assignments\":[")
        assignments.sortedBy { it.pageIndex }.forEachIndexed { index, assignment ->
            if (index > 0) append(',')
            append("{\"pageIndex\":").append(assignment.pageIndex)
            // Mode keys are [a-z_] and φθόγγος names [A-Z]: nothing in them needs escaping.
            append(",\"mode\":\"").append(assignment.mode.key).append('"')
            append(",\"shiftMoria\":").append(assignment.baseShiftMoria)
            assignment.isonChoice?.let { choice ->
                append(",\"ison\":{\"phthong\":\"").append(StoredPhthongs.encode(choice.name))
                append("\",\"octave\":").append(choice.octave).append('}')
            }
            append('}')
        }
        append("]}")
    }

    fun decode(stored: String?): Decoded {
        if (stored == null) return Decoded.Assignments(emptyList())
        val root = try {
            JSONObject(stored)
        } catch (_: JSONException) {
            return Decoded.Unreadable
        }
        val version = root.opt("schemaVersion") as? Int ?: return Decoded.Unreadable
        if (version > SCHEMA_VERSION) return Decoded.Newer
        // One branch per version ever written: a new format adds a branch and keeps this one.
        return when (version) {
            1 -> decodeVersion1(root)
            else -> Decoded.Unreadable
        }
    }

    private fun decodeVersion1(root: JSONObject): Decoded {
        val items = root.opt("assignments") as? JSONArray ?: return Decoded.Unreadable
        val assignments = ArrayList<PageAssignment>(items.length())
        for (index in 0 until items.length()) {
            val item = items.opt(index) as? JSONObject ?: return Decoded.Unreadable
            assignments += assignmentOf(item) ?: return Decoded.Unreadable
        }
        if (assignments.map { it.pageIndex }.distinct().size != assignments.size) return Decoded.Unreadable
        return Decoded.Assignments(assignments.sortedBy { it.pageIndex })
    }

    private fun assignmentOf(item: JSONObject): PageAssignment? {
        val pageIndex = (item.opt("pageIndex") as? Int)?.takeIf { it in 0..MAX_PAGE_INDEX } ?: return null
        val mode = Mode.fromKey(item.opt("mode") as? String) ?: return null
        val shift = item.opt("shiftMoria") as? Int ?: return null
        val ison = item.opt("ison")
        val choice = if (ison == null || ison == JSONObject.NULL) {
            null
        } else {
            val phthong = ison as? JSONObject ?: return null
            val name = StoredPhthongs.decode(phthong.opt("phthong") as? String) ?: return null
            val octave = phthong.opt("octave") as? Int ?: return null
            Phthong(name, octave)
        }
        val clamped = BaseShift.clamp(shift)
        return PageAssignment(pageIndex, mode, clamped, PageAssignments.normalizedChoice(mode, clamped, choice))
    }

    /**
     * The value the app itself would store for [stored], or null when there is nothing to carry — it is
     * unreadable, newer, or empty. `LearningDataFile` exports this and imports a value only when it is
     * already exactly this, so whatever the export wrote, the import takes back.
     */
    fun normalized(stored: String): String? {
        val decoded = decode(stored) as? Decoded.Assignments ?: return null
        return decoded.assignments.takeIf { it.isNotEmpty() }?.let(::encode)
    }
}
