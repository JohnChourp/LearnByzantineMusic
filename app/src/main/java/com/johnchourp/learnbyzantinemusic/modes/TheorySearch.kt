package com.johnchourp.learnbyzantinemusic.modes

import com.johnchourp.learnbyzantinemusic.search.SearchNormalizer

/**
 * The one rule for matching theory text (ClickUp `869f4tph0`).
 *
 * ## Why one place
 *
 * Two things need to agree about what "matching" means: the menu that decides a page is a **result**,
 * and the page that then **highlights** the term inside it. If they normalised differently, the user
 * would tap a row the search found and see nothing lit — which reads as a broken page rather than a
 * near-miss in the matcher. So both go through here.
 *
 * ## What normalising does
 *
 * Lowercase, no diacritics, `ς` = `σ` — [SearchNormalizer] does it, the same function the notes search
 * uses, so a word is findable the same way on a theory page and in a note.
 *
 * ## Mapping back to the original text
 *
 * Highlighting needs ranges in the **original** string, not the normalised one, because the two can
 * differ in length: a decomposed character contributes a base letter plus marks that are then
 * dropped. [matchRanges] therefore normalises character by character and keeps an index back to the
 * source, so a highlight can never land one character off — which, on accented Greek, it otherwise
 * would.
 */
object TheorySearch {

    /** Normalised form of a whole string, for comparison. Trimmed, because queries carry spaces. */
    fun normalize(value: String): String = SearchNormalizer.normalize(value)

    /** True when [haystack] contains [query] under [normalize]. A blank query matches everything. */
    fun matches(haystack: String, query: String): Boolean {
        val needle = normalize(query)
        if (needle.isEmpty()) return true
        return normalize(haystack).contains(needle)
    }

    /**
     * Ranges **in [text]'s own indices** where [query] occurs, ignoring case and diacritics.
     * Empty when the query is blank or absent, so a caller can highlight unconditionally.
     */
    fun matchRanges(text: String, query: String): List<IntRange> {
        val needle = normalize(query)
        if (needle.isEmpty() || text.isEmpty()) return emptyList()

        val normalized = StringBuilder(text.length)
        // sourceIndex[i] is the index in `text` that produced normalized[i].
        val sourceIndex = ArrayList<Int>(text.length)
        text.forEachIndexed { index, char ->
            SearchNormalizer.normalizeUntrimmed(char.toString()).forEach { normalizedChar ->
                normalized.append(normalizedChar)
                sourceIndex.add(index)
            }
        }

        val hay = normalized.toString()
        val ranges = mutableListOf<IntRange>()
        var from = 0
        while (from <= hay.length - needle.length) {
            val at = hay.indexOf(needle, from)
            if (at < 0) break
            val start = sourceIndex[at]
            // The last source character the match covers — not `start + needle.length`, which would
            // be wrong wherever normalisation changed the length.
            val end = sourceIndex[at + needle.length - 1]
            ranges += start..end
            from = at + needle.length
        }
        return ranges
    }
}
