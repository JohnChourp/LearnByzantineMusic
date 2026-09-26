package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import com.johnchourp.learnbyzantinemusic.music.TimeSign
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Everything the Melody Trainer needs to put a melody back on screen: its notes, its tempo and the
 * scale it plays and listens on (ClickUp `869f5x261`, F6). The last melody and every saved exercise
 * are one of these.
 */
data class TrainerMelody(
    val notes: List<TrainerNote> = emptyList(),
    val bpm: Int = MelodyTempo.DEFAULT_BPM,
    val scale: TrainerScale = TrainerScale.DIATONIC,
) {
    /**
     * This melody with its ήχος's «Μεταφορά βάσης» taken from [liveShiftMoria] — the value the
     * shared per-mode key holds now — when there is one.
     *
     * The shift belongs to the **singer's voice**, not to the melody: since F2 it is one value per
     * ήχος, shared by the Trainer and the 8 Ήχοι page. So a melody reopened later follows the shift
     * the singer uses today, and never quietly changes the 8 Ήχοι setting back. The shift stored
     * with the melody is only the fallback, for a ήχος that has no stored shift yet (a new phone,
     * an imported exercise). «Διατονικός» has no shift at all.
     */
    fun withLiveShift(liveShiftMoria: Int?): TrainerMelody {
        val mode = scale.mode ?: return this
        val shift = liveShiftMoria?.let(BaseShift::clamp) ?: return this
        return copy(scale = TrainerScale(mode, shift))
    }
}

/**
 * The stored form of a [TrainerMelody]: JSON, and **frozen** once written (ClickUp `869f5x261`).
 *
 * ```
 * {"schemaVersion": 1, "bpm": 80, "mode": "second", "baseShift": -4,
 *  "notes": [{"phthong": "PA", "octave": 0, "length": 1.5, "signs": ["gorgo"]}]}
 * ```
 *
 * | Field | Written as | On read |
 * |---|---|---|
 * | `schemaVersion` | 1 | anything else: rejected — a newer app wrote it |
 * | `bpm` | χρόνοι per minute | clamped to [MelodyTempo.MIN_BPM] … [MelodyTempo.MAX_BPM]; missing: rejected |
 * | `mode` | `Mode.key`, frozen by H3; absent for «Διατονικός» | a key no mode has: rejected |
 * | `baseShift` | μόρια; absent for «Διατονικός» | clamped to `BaseShift.RANGE`; ignored for «Διατονικός» |
 * | `notes` | at most [MelodySequence.MAX_NOTES] | missing or more: rejected |
 * | `notes[].phthong` | `PhthongName` constant name, frozen by H2 | unknown: rejected |
 * | `notes[].octave` | [TrainerScale.MIN_NOTE_OCTAVE] … [TrainerScale.MAX_NOTE_OCTAVE] | clamped; missing: rejected |
 * | `notes[].length` | the note's own length in χρόνοι | clamped to ½ … 4; missing: rejected |
 * | `notes[].signs` | `TimeSign.id`s — `gorgo` and `fraction` are the Trainer's, frozen by H5 | an id no sign has, or a rest's: rejected |
 * | `notes[].syllable` | optional: the syllable sung on the note (J2); absent when there is none | cleaned by `TrainerNote.cleanSyllable`: trimmed, cut to 12 characters, blank = none |
 *
 * **Rejected** means [decode] returns null: never a crash, and never half a melody — a note with a sign
 * the app does not know would play with the wrong timing, so the whole melody is refused instead.
 * So is a rest (the βαρεία signs of F4, ClickUp `869f5x25n`): the Trainer's notes are sung and its
 * player has no silence, so a rest read onto one would sound. Before F4 their ids were unknown here.
 * Clamping only brings a value back into the range the app itself writes.
 *
 * On the way in the notes also go through `MelodySequence.normalised`, the rule of H5: a γοργόν that
 * can never stand on the first note is taken off, exactly as a deletion in the Trainer would.
 *
 * **`syllable` did not raise the version** (ClickUp `869f5x2cv`). It is optional, and every reader of
 * version 1 takes only the fields it knows, through `opt`: a melody without syllables is written
 * exactly as before, and a version 1 reader that meets one with syllables loads its notes and
 * leaves the syllables out — it never refuses the melody.
 */
object TrainerMelodyCodec {

    const val SCHEMA_VERSION = 1

    private const val SCHEMA_VERSION_FIELD = "schemaVersion"
    private const val BPM = "bpm"
    private const val MODE = "mode"
    private const val BASE_SHIFT = "baseShift"
    private const val NOTES = "notes"
    private const val PHTHONG = "phthong"
    private const val OCTAVE = "octave"
    private const val LENGTH = "length"
    private const val SIGNS = "signs"
    private const val SYLLABLE = "syllable"

    fun encode(melody: TrainerMelody): JSONObject = JSONObject().apply {
        put(SCHEMA_VERSION_FIELD, SCHEMA_VERSION)
        put(BPM, melody.bpm)
        melody.scale.mode?.let { mode ->
            put(MODE, mode.key)
            put(BASE_SHIFT, melody.scale.baseShiftMoria)
        }
        put(NOTES, JSONArray().apply { melody.notes.forEach { put(encodeNote(it)) } })
    }

    fun encodeString(melody: TrainerMelody): String = encode(melody).toString()

    /** The melody [json] holds, or null when it cannot be read whole — see the table above. */
    fun decode(json: JSONObject?): TrainerMelody? {
        if (json == null || json.opt(SCHEMA_VERSION_FIELD) != SCHEMA_VERSION) return null
        val bpm = (json.opt(BPM) as? Number)?.toInt() ?: return null
        val modeValue = json.opt(MODE)
        val mode = if (modeValue == null || modeValue == JSONObject.NULL) {
            null
        } else {
            (modeValue as? String)?.let(Mode::fromKey) ?: return null
        }
        val shift = if (mode == null) {
            BaseShift.DEFAULT_MORIA
        } else {
            BaseShift.clamp((json.opt(BASE_SHIFT) as? Number)?.toInt() ?: BaseShift.DEFAULT_MORIA)
        }
        val notesJson = json.optJSONArray(NOTES) ?: return null
        if (notesJson.length() > MelodySequence.MAX_NOTES) return null
        val notes = ArrayList<TrainerNote>(notesJson.length())
        for (index in 0 until notesJson.length()) {
            notes += decodeNote(notesJson.optJSONObject(index)) ?: return null
        }
        return TrainerMelody(
            notes = MelodySequence(notes).normalised().notes,
            bpm = MelodyTempo.clampBpm(bpm),
            scale = TrainerScale(mode, shift),
        )
    }

    /** [decode] from the stored text; anything that is not a JSON object is null too. */
    fun decodeString(raw: String?): TrainerMelody? {
        if (raw.isNullOrBlank()) return null
        return try {
            decode(JSONObject(raw))
        } catch (_: JSONException) {
            null
        }
    }

    private fun encodeNote(note: TrainerNote): JSONObject = JSONObject().apply {
        put(PHTHONG, note.phthong.name)
        put(OCTAVE, note.octaveShift)
        put(LENGTH, note.baseDurationBeats.toDouble())
        if (note.signs.isNotEmpty()) put(SIGNS, JSONArray(note.signs.map { it.id }.sorted()))
        note.syllable?.let { put(SYLLABLE, it) }
    }

    private fun decodeNote(json: JSONObject?): TrainerNote? {
        if (json == null) return null
        val phthong = PhthongName.entries.firstOrNull { it.name == json.opt(PHTHONG) } ?: return null
        val octave = (json.opt(OCTAVE) as? Number)?.toInt() ?: return null
        val length = (json.opt(LENGTH) as? Number)?.toDouble()?.takeIf { it.isFinite() } ?: return null
        val signs = mutableSetOf<TimeSign>()
        val signsJson = json.optJSONArray(SIGNS)
        if (signsJson != null) {
            for (index in 0 until signsJson.length()) {
                signs += (signsJson.opt(index) as? String)?.let(TimeSign::fromId)?.takeUnless { it.isRest } ?: return null
            }
        }
        return TrainerNote(
            phthong = phthong,
            octaveShift = octave.coerceIn(TrainerScale.MIN_NOTE_OCTAVE, TrainerScale.MAX_NOTE_OCTAVE),
            baseDurationBeats = length.toFloat()
                .coerceIn(MelodySequence.MIN_LENGTH_BEATS, MelodySequence.MAX_LENGTH_BEATS),
            signs = signs,
            syllable = TrainerNote.cleanSyllable(json.opt(SYLLABLE) as? String),
        )
    }
}
