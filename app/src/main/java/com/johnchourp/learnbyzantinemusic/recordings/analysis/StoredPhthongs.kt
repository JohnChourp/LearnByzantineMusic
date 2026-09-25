package com.johnchourp.learnbyzantinemusic.recordings.analysis

import com.johnchourp.learnbyzantinemusic.music.PhthongName

/**
 * How the recording analysis writes φθόγγοι into its preferences and reads them back — pulled out of
 * [AnalysisSettingsStore] so the format can be tested without Android (ClickUp `869f5x291`, H2).
 *
 * The stored form is the [PhthongName] **constant name**: `"KE"` for a starting φθόγγος, and the
 * names joined with commas for an expected melody, `"NI,PA,VOU"`. These strings are already on
 * users' devices — written by the Melody Trainer's former `TrainerPhthong` enum, whose constants had
 * exactly these names in exactly this order — so the format is frozen:
 *
 * - a constant of [PhthongName] may never be renamed, or the notes saved under the old name vanish;
 * - an unknown token is **dropped** on read, as it always was, so one bad entry costs one note rather
 *   than the whole melody;
 * - tokens are matched exactly: no trimming, no case folding. That is what every earlier version did,
 *   and `StoredPhthongsTest` pins it against a verbatim copy of that decoder.
 *
 * Only names are stored, never a display label, so an octave mark can never reach storage.
 */
object StoredPhthongs {

    /** A melody as stored: the constant names, comma-separated. An empty melody is `""`. */
    fun encodeList(phthongs: List<PhthongName>): String = phthongs.joinToString(",") { encode(it) }

    /** Reads [encodeList]'s form; `null` (nothing stored) is an empty melody, unknown tokens are dropped. */
    fun decodeList(stored: String?): List<PhthongName> =
        stored?.split(',')?.mapNotNull { decode(it) }.orEmpty()

    /** One φθόγγος as stored: its constant name. */
    fun encode(phthong: PhthongName): String = phthong.name

    /** Reads [encode]'s form, or null when [stored] is missing or names no φθόγγος. */
    fun decode(stored: String?): PhthongName? = PhthongName.entries.firstOrNull { it.name == stored }
}
