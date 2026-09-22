package com.johnchourp.learnbyzantinemusic.modes

import android.content.Context
import com.johnchourp.learnbyzantinemusic.prefs.AppPrefs

/**
 * Starred theory pages (ClickUp `869f4tphx`).
 *
 * A learner works the same two or three pages over and over and has to find them again every time.
 * Stars fix that without a new screen: the «8 Ήχοι» pages menu lists them first.
 *
 * ## Keyed by topic id, never by title
 *
 * The stored value is the [TheoryTopicCatalog] key, not the visible title. Titles are string
 * resources — they differ between Ελληνικά and English, and they get reworded. Keying by title would
 * mean a star silently disappearing when someone improved the wording, or when the user switched
 * language. The key is the one thing about a page that is meant to be stable.
 *
 * ## Why unknown ids are dropped on read and not on write
 *
 * [favorites] filters to keys the catalog currently knows, so a renamed or removed topic cannot show
 * up as a phantom menu row. But the write path does not prune the stored set: a topic briefly absent
 * (a work-in-progress page, a build where the catalog changed) would otherwise have its star erased
 * permanently the first time the menu was opened.
 *
 * Local only. No account, no backend — the same as every other preference here.
 */
object TheoryTopicFavorites {

    /** Stored ids, filtered to topics the catalog currently has, in catalog order. */
    fun favorites(context: Context): List<String> {
        val stored = storedIds(context)
        return TheoryTopicCatalog.topics.map { it.key }.filter { it in stored }
    }

    fun isFavorite(context: Context, topicKey: String): Boolean = topicKey in storedIds(context)

    /**
     * Flips the star and returns the new state. Unknown keys are refused, so a stray extra cannot
     * write a row that the menu will then never be able to show.
     */
    fun toggle(context: Context, topicKey: String): Boolean {
        if (TheoryTopicCatalog.topics.none { it.key == topicKey }) return false
        val current = storedIds(context)
        val updated = if (topicKey in current) current - topicKey else current + topicKey
        AppPrefs.open(context, AppPrefs.Store.SETTINGS)
            .edit()
            .putStringSet(AppPrefs.FavoriteTopicIds.name, updated)
            .apply()
        return topicKey in updated
    }

    /** getStringSet may hand back the live instance, so copy before anything mutates it. */
    private fun storedIds(context: Context): Set<String> =
        AppPrefs.open(context, AppPrefs.Store.SETTINGS)
            .getStringSet(AppPrefs.FavoriteTopicIds.name, emptySet())
            ?.toSet()
            ?: emptySet()

    /**
     * Pure ordering rule, extracted so it can be tested without Android: favourites first in catalog
     * order, then everything else in catalog order. Stable, so the menu does not reshuffle.
     */
    fun order(allKeys: List<String>, favoriteKeys: Set<String>): List<String> =
        allKeys.filter { it in favoriteKeys } + allKeys.filterNot { it in favoriteKeys }
}
