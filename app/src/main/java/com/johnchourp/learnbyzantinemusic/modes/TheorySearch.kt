package com.johnchourp.learnbyzantinemusic.modes

import java.text.Normalizer
import java.util.Locale

/**
 * The one rule for matching theory text (ClickUp `869f4tph0`).
 *
 * ## Why one place
 *
 * Two things need to agree about what "matching" means: the menu that decides a page is a **result**,
 * and the page that then **highlights** the term inside it. If they normalised differently, the user
 * would tap a row the search found and see nothing lit — which reads as a broken page rather than a
 * near-miss in the matcher. So the normalisation lives here and both go through it.
 *
 * ## What normalising does
 *
 * Lowercases, then strips diacritics by decomposing (NFD) and dropping the combining marks. That is
 * what makes «πεταστή» findable by typing `πεταστη`, and `Έλξη` by `ελξη` — Greek learners very often
 * type without accents, and the tonos is exactly the key people skip.
 *
 * Latin gets the same treatment, so the English side of the bilingual content behaves identically.
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

    private val COMBINING_MARKS = Regex("\\p{Mn}+")

    /** Normalised form of a whole string, for comparison. Trimmed, because queries carry spaces. */
    fun normalize(value: String): String = normalizeUntrimmed(value).trim()

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
            normalizeUntrimmed(char.toString()).forEach { normalizedChar ->
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

    private fun normalizeUntrimmed(value: String): String =
        COMBINING_MARKS.replace(
            Normalizer.normalize(value.lowercase(Locale.getDefault()), Normalizer.Form.NFD),
            "",
        ).replace(FINAL_SIGMA, SIGMA)

    /**
     * Greek lowercasing turns a word-final `Σ` into `ς`, but a user typing the word gets `σ` — the
     * keyboard has one sigma key. Without folding the two, «ΤΡΟΧΟΣ» is not findable by typing
     * `τροχοσ`, which is what everyone types. Measured, not assumed: this is what made
     * `finalSigmaAndCaseAreHandled` fail before the fold existed.
     *
     * Folding is one character to one character, so it cannot disturb the index mapping that
     * [matchRanges] relies on.
     */
    private const val FINAL_SIGMA = 'ς'
    private const val SIGMA = 'σ'
}
