package com.johnchourp.learnbyzantinemusic.modes

import com.johnchourp.learnbyzantinemusic.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TheoryTopicCatalogTest {
    @Test
    fun catalogContainsRequestedTheoryTopics() {
        assertEquals(5, TheoryTopicCatalog.topics.size)

        val topicKeys = TheoryTopicCatalog.topics.map { it.key }
        assertTrue(TheoryTopicCatalog.OCTAVE_DIAPASON in topicKeys)
        assertTrue(TheoryTopicCatalog.MIDDLE_ACUTE_GRAVE_DIAPASON in topicKeys)
        assertTrue(TheoryTopicCatalog.SCALE in topicKeys)
        assertTrue(TheoryTopicCatalog.TESTIMONIES in topicKeys)
        assertTrue(TheoryTopicCatalog.PENTACHORD_TROCHOS in topicKeys)

        TheoryTopicCatalog.topics.forEach { topic ->
            assertNotEquals(0, topic.titleRes)
        }
    }

    @Test
    fun octachordTopicHasBodyAndFutureTopicsStayBlank() {
        val octachordTopic = TheoryTopicCatalog.byKey(TheoryTopicCatalog.OCTAVE_DIAPASON)
        assertEquals(R.string.theory_topic_octave_diapason_body, octachordTopic.bodyRes)

        val futureTopicKeys = listOf(
            TheoryTopicCatalog.MIDDLE_ACUTE_GRAVE_DIAPASON,
            TheoryTopicCatalog.SCALE,
            TheoryTopicCatalog.TESTIMONIES,
            TheoryTopicCatalog.PENTACHORD_TROCHOS
        )
        futureTopicKeys.forEach { key ->
            assertEquals(0, TheoryTopicCatalog.byKey(key).bodyRes)
        }
    }

    @Test
    fun linkPatternsPointOnlyToKnownTopics() {
        val topicKeys = TheoryTopicCatalog.topics.map { it.key }.toSet()
        TheoryTopicLinks.linkPatterns.forEach { pattern ->
            assertTrue(pattern.topicKey in topicKeys)
        }
    }

    @Test
    fun linkPatternsMatchRequestedReferencesAndSystemWords() {
        assertTrue(hasTopicLink("(6)", TheoryTopicCatalog.MIDDLE_ACUTE_GRAVE_DIAPASON))
        assertTrue(hasTopicLink("(4)", TheoryTopicCatalog.SCALE))
        assertTrue(hasTopicLink("(9)", TheoryTopicCatalog.TESTIMONIES))
        assertTrue(hasTopicLink("(44)", TheoryTopicCatalog.PENTACHORD_TROCHOS))
        assertTrue(hasTopicLink("Οκτάχορδον ή διαπασών", TheoryTopicCatalog.OCTAVE_DIAPASON))
        assertTrue(hasTopicLink("πεντάχορδον ή τον τροχόν", TheoryTopicCatalog.PENTACHORD_TROCHOS))
    }

    private fun hasTopicLink(textValue: String, topicKey: String): Boolean =
        TheoryTopicLinks.linkPatterns.any { pattern ->
            pattern.topicKey == topicKey && pattern.regex.containsMatchIn(textValue)
        }
}
