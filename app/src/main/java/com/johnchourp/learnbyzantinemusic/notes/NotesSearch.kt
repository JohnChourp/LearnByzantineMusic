package com.johnchourp.learnbyzantinemusic.notes

import com.johnchourp.learnbyzantinemusic.search.SearchNormalizer

/**
 * What the notes search box finds: every note whose title or body contains the query, ignoring case
 * and accents, with `ς` = `σ` — through [SearchNormalizer], the same function the theory search uses.
 *
 * **Why in memory.** The search used to be an SQLite `LIKE`, which ignores case for ASCII letters only
 * and knows nothing about accents. Only the query was lowercased, so «Κύριε» was not found even when
 * typed exactly. `%` and `_` were escaped with a backslash but the query had no `ESCAPE` clause, so
 * «50%» was never found either. Filtering here fixes all of that without touching `notes.db`, the only
 * copy of the user's notes: no new column, no migration. `%`, `_` and `\` are plain characters.
 *
 * **Cost.** [index] normalises each note once per list the database sends — after a save, not on every
 * keystroke — and [filter] only compares strings. The ViewModel runs both off the main thread.
 */
object NotesSearch {

    /** A note with its title and body already in searchable form. */
    data class Indexed(val note: NoteEntity, val title: String, val body: String)

    fun index(notes: List<NoteEntity>): List<Indexed> =
        notes.map { Indexed(it, SearchNormalizer.normalize(it.title), SearchNormalizer.normalize(it.body)) }

    /**
     * The notes whose title or body contains [query], in the order given (newest first, from the
     * database). A blank query keeps every note. Title and body are searched separately, so a match
     * never runs from the end of one into the start of the other.
     */
    fun filter(notes: List<Indexed>, query: String): List<NoteEntity> {
        val needle = SearchNormalizer.normalize(query)
        if (needle.isEmpty()) {
            return notes.map { it.note }
        }
        return notes.filter { it.title.contains(needle) || it.body.contains(needle) }.map { it.note }
    }
}
