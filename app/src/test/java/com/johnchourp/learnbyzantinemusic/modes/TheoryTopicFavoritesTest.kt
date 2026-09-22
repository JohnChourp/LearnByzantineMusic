package com.johnchourp.learnbyzantinemusic.modes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ClickUp `869f4tphx` (A4), favourites half.
 *
 * The acceptance criterion worth testing is *«Αγαπημένα ανθεκτικά σε μετονομασία τίτλου (κλειδί =
 * topic id, όχι τίτλος)»* — a star must survive a reworded title and a language switch. That is a
 * property of what gets stored, so it is asserted against the real catalog rather than mocked.
 */
class TheoryTopicFavoritesTest {

    private val allKeys = TheoryTopicCatalog.topics.map { it.key }

    @Test
    fun favouritesComeFirstAndNothingIsHidden() {
        val favorites = setOf(TheoryTopicCatalog.TROCHOS, TheoryTopicCatalog.SCALE)
        val ordered = TheoryTopicFavorites.order(allKeys, favorites)

        assertEquals("no page may be dropped from the menu", allKeys.size, ordered.size)
        assertEquals("no page may be duplicated", ordered.size, ordered.distinct().size)
        assertEquals(
            "every favourite sits before every non-favourite",
            favorites.size,
            ordered.take(favorites.size).count { it in favorites },
        )
        assertTrue(allKeys.all { it in ordered })
    }

    @Test
    fun orderingIsStableWithinEachGroup() {
        val favorites = setOf(TheoryTopicCatalog.TROCHOS, TheoryTopicCatalog.SCALE)
        val ordered = TheoryTopicFavorites.order(allKeys, favorites)

        // Catalog order is preserved inside both groups, so the menu never reshuffles under the user.
        assertEquals(allKeys.filter { it in favorites }, ordered.take(favorites.size))
        assertEquals(allKeys.filterNot { it in favorites }, ordered.drop(favorites.size))
    }

    @Test
    fun noFavouritesLeavesTheCatalogOrderUntouched() {
        assertEquals(allKeys, TheoryTopicFavorites.order(allKeys, emptySet()))
    }

    @Test
    fun allFavouritesAlsoLeavesTheOrderUntouched() {
        assertEquals(allKeys, TheoryTopicFavorites.order(allKeys, allKeys.toSet()))
    }

    @Test
    fun aStoredIdThatIsNoLongerACatalogKeyCannotBecomeAMenuRow() {
        val ordered = TheoryTopicFavorites.order(allKeys, setOf("a_topic_that_was_removed"))
        assertEquals(allKeys, ordered)
    }

    @Test
    fun whatIsStoredIsTheKeyNotTheTitle() {
        // The whole point of the criterion: keys are stable identifiers, titles are localisable
        // string resources that get reworded. If a key ever looked like a title, the store would be
        // storing the wrong thing.
        TheoryTopicCatalog.topics.forEach { topic ->
            assertTrue(
                "topic key '${topic.key}' must be a stable identifier, not display text",
                Regex("^[a-z0-9_]+$").matches(topic.key),
            )
            assertTrue("topic ${topic.key} must have a title resource", topic.titleRes != 0)
        }
    }
}
