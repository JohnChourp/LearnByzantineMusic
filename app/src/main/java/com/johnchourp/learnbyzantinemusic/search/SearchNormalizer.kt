package com.johnchourp.learnbyzantinemusic.search

import java.text.Normalizer
import java.util.Locale

/**
 * The one rule for turning text into its searchable form, shared by every search that matches what a
 * person typed against text: the theory pages ([com.johnchourp.learnbyzantinemusic.modes.TheorySearch])
 * and the notes ([com.johnchourp.learnbyzantinemusic.notes.NotesSearch]). One function, so a word that
 * is findable in one is findable the same way in the other; `OneSearchNormalizerTest` fails on a second.
 *
 * ## What normalising does
 *
 * Lowercases, then strips diacritics by decomposing (NFD) and dropping the combining marks. That is
 * what makes «πεταστή» findable by typing `πεταστη`, and `Έλξη` by `ελξη` — Greek learners very often
 * type without accents, and the tonos is exactly the key people skip.
 *
 * Latin gets the same treatment, so the English side of the bilingual content behaves identically.
 * Nothing else is special: `%`, `_` and `\` stay ordinary characters.
 */
object SearchNormalizer {

    private val COMBINING_MARKS = Regex("\\p{Mn}+")

    /** Normalised form of a whole string, for comparison. Trimmed, because queries carry spaces. */
    fun normalize(value: String): String = normalizeUntrimmed(value).trim()

    /**
     * [normalize] without the trim. It turns each character into zero or more and never reorders, so
     * TheorySearch.matchRanges can run it character by character and keep an index back to the source.
     */
    fun normalizeUntrimmed(value: String): String =
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
     * TheorySearch.matchRanges relies on.
     */
    private const val FINAL_SIGMA = 'ς'
    private const val SIGMA = 'σ'
}
