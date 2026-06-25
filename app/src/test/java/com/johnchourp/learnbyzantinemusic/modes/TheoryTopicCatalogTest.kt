package com.johnchourp.learnbyzantinemusic.modes

import com.johnchourp.learnbyzantinemusic.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TheoryTopicCatalogTest {
    @Test
    fun catalogContainsRequestedTheoryTopics() {
        assertEquals(7, TheoryTopicCatalog.topics.size)

        val topicKeys = TheoryTopicCatalog.topics.map { it.key }
        assertTrue(TheoryTopicCatalog.OCTAVE_DIAPASON in topicKeys)
        assertTrue(TheoryTopicCatalog.MIDDLE_ACUTE_GRAVE_DIAPASON in topicKeys)
        assertTrue(TheoryTopicCatalog.SCALE in topicKeys)
        assertTrue(TheoryTopicCatalog.TESTIMONIES in topicKeys)
        assertTrue(TheoryTopicCatalog.PENTACHORD_TROCHOS in topicKeys)
        assertTrue(TheoryTopicCatalog.TROCHOS in topicKeys)
        assertTrue(TheoryTopicCatalog.FIGURE_XVI in topicKeys)

        TheoryTopicCatalog.topics.forEach { topic ->
            assertNotEquals(0, topic.titleRes)
        }
    }

    @Test
    fun writtenTopicsHaveBodyAndFutureTopicsStayBlank() {
        val octachordTopic = TheoryTopicCatalog.byKey(TheoryTopicCatalog.OCTAVE_DIAPASON)
        assertEquals(R.string.theory_topic_octave_diapason_body, octachordTopic.bodyRes)
        val pentachordTopic = TheoryTopicCatalog.byKey(TheoryTopicCatalog.PENTACHORD_TROCHOS)
        assertEquals(R.string.theory_topic_pentachord_trochos_body, pentachordTopic.bodyRes)
        val trochosTopic = TheoryTopicCatalog.byKey(TheoryTopicCatalog.TROCHOS)
        assertEquals(R.string.theory_topic_trochos_body, trochosTopic.bodyRes)

        val futureTopicKeys = listOf(
            TheoryTopicCatalog.MIDDLE_ACUTE_GRAVE_DIAPASON,
            TheoryTopicCatalog.SCALE,
            TheoryTopicCatalog.TESTIMONIES,
            TheoryTopicCatalog.FIGURE_XVI
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
    fun navigationMenuIncludesHomeAndEveryTheoryTopic() {
        assertEquals(
            listOf(EightModesNavigation.HOME_ENTRY_KEY) + TheoryTopicCatalog.topics.map { it.key },
            EightModesNavigation.navigationEntryKeys()
        )
    }

    @Test
    fun navigationPathsTrackInlineDepthButMenuJumpsDirectly() {
        val octachordPath = EightModesNavigation.pathForLinkedTopic(
            currentPathTopicKeys = emptyList(),
            targetTopicKey = TheoryTopicCatalog.OCTAVE_DIAPASON
        )
        assertEquals(listOf(TheoryTopicCatalog.OCTAVE_DIAPASON), octachordPath)

        val scalePath = EightModesNavigation.pathForLinkedTopic(
            currentPathTopicKeys = octachordPath,
            targetTopicKey = TheoryTopicCatalog.SCALE
        )
        assertEquals(
            listOf(TheoryTopicCatalog.OCTAVE_DIAPASON, TheoryTopicCatalog.SCALE),
            scalePath
        )

        assertEquals(
            listOf(TheoryTopicCatalog.TROCHOS),
            EightModesNavigation.pathForMenuTopic(TheoryTopicCatalog.TROCHOS)
        )
    }

    @Test
    fun topicPathResolutionAppendsCurrentTopicWhenMissing() {
        assertEquals(
            listOf(TheoryTopicCatalog.OCTAVE_DIAPASON, TheoryTopicCatalog.TESTIMONIES),
            EightModesNavigation.resolveTopicPath(
                pathTopicKeys = listOf(TheoryTopicCatalog.OCTAVE_DIAPASON),
                currentTopicKey = TheoryTopicCatalog.TESTIMONIES
            )
        )
    }

    @Test
    fun linkPatternsMatchRequestedReferencesAndSystemWords() {
        assertTrue(hasTopicLink("(2)", TheoryTopicCatalog.TROCHOS))
        assertTrue(hasTopicLink("(6)", TheoryTopicCatalog.MIDDLE_ACUTE_GRAVE_DIAPASON))
        assertTrue(hasTopicLink("(4)", TheoryTopicCatalog.SCALE))
        assertTrue(hasTopicLink("(9)", TheoryTopicCatalog.TESTIMONIES))
        assertTrue(hasTopicLink("(44)", TheoryTopicCatalog.PENTACHORD_TROCHOS))
        assertTrue(hasTopicLink("Σχήμα XVI", TheoryTopicCatalog.FIGURE_XVI))
        assertTrue(hasTopicLink("Οκτάχορδον ή διαπασών", TheoryTopicCatalog.OCTAVE_DIAPASON))
        assertTrue(hasTopicLink("πεντάχορδον ή τον τροχόν", TheoryTopicCatalog.PENTACHORD_TROCHOS))
    }

    @Test
    fun currentTopicLinksDoNotPointBackToTheSamePage() {
        val topicKeys = TheoryTopicLinks.findTopicLinks(
            textValue = "οκτάχορδον σύστημα ή διαπασών (6), κλίμακος (4), εννέα (9)",
            excludedTopicKey = TheoryTopicCatalog.OCTAVE_DIAPASON
        ).map { it.topicKey }

        assertTrue(TheoryTopicCatalog.OCTAVE_DIAPASON !in topicKeys)
        assertTrue(TheoryTopicCatalog.MIDDLE_ACUTE_GRAVE_DIAPASON in topicKeys)
        assertTrue(TheoryTopicCatalog.SCALE in topicKeys)
        assertTrue(TheoryTopicCatalog.TESTIMONIES in topicKeys)
    }

    @Test
    fun pentachordTopicKeepsTrochosFootnoteAndFigureLinkButAvoidsSelfLinks() {
        val topicKeys = TheoryTopicLinks.findTopicLinks(
            textValue = "πεντάχορδον σύστημα ή τροχός (2). Σχήμα XVI.",
            excludedTopicKey = TheoryTopicCatalog.PENTACHORD_TROCHOS
        ).map { it.topicKey }

        assertTrue(TheoryTopicCatalog.PENTACHORD_TROCHOS !in topicKeys)
        assertTrue(TheoryTopicCatalog.TROCHOS in topicKeys)
        assertTrue(TheoryTopicCatalog.FIGURE_XVI in topicKeys)
    }

    private fun hasTopicLink(textValue: String, topicKey: String): Boolean =
        TheoryTopicLinks.linkPatterns.any { pattern ->
            pattern.topicKey == topicKey && pattern.regex.containsMatchIn(textValue)
        }
}
