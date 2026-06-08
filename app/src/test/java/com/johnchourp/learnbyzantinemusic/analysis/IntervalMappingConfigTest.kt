package com.johnchourp.learnbyzantinemusic.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.charset.StandardCharsets

class IntervalMappingConfigTest {
    @Test
    fun `contains core mapped tokens and v2 rules`() {
        val candidates = listOf(
            Paths.get("app/src/main/assets/byzantine_interval_mapping_v1.json"),
            Paths.get("src/main/assets/byzantine_interval_mapping_v1.json")
        )
        val mappingPath = candidates.firstOrNull { Files.exists(it) }
            ?: error("Missing byzantine_interval_mapping_v1.json in known locations")
        val content = String(Files.readAllBytes(mappingPath), StandardCharsets.UTF_8)

        assertTrue(content.contains("\"a1\""))
        assertTrue(content.contains("\"a2\""))
        assertTrue(content.contains("\"a8\""))
        assertTrue(content.contains("\"b1\""))
        assertTrue(content.contains("\"tokenDeltas\""))

        val rulesPath = listOf(
            Paths.get("app/src/main/assets/byzantine_core_symbol_rules_v2.json"),
            Paths.get("src/main/assets/byzantine_core_symbol_rules_v2.json")
        ).firstOrNull { Files.exists(it) }
        val modeRulesPath = listOf(
            Paths.get("app/src/main/assets/byzantine_mode_rules_v1.json"),
            Paths.get("src/main/assets/byzantine_mode_rules_v1.json")
        ).firstOrNull { Files.exists(it) }
        assertTrue(rulesPath != null)
        assertTrue(modeRulesPath != null)

        val rulesContent = String(Files.readAllBytes(rulesPath!!), StandardCharsets.UTF_8)
        val modeRulesContent = String(Files.readAllBytes(modeRulesPath!!), StandardCharsets.UTF_8)
        assertTrue(rulesContent.contains("\"petasti\""))
        assertTrue(rulesContent.contains("\"apostrophos\""))
        assertTrue(rulesContent.contains("\"fraction\""))
        assertTrue(modeRulesContent.contains("\"first\""))
        assertTrue(modeRulesContent.contains("\"plagal_second\""))
    }

    @Test
    fun `mode rule profiles match corrected scale families`() {
        val modeRulesContent = readModeRulesContent()
        val diatonicProfile = mapOf(
            "Νη" to 0.0f,
            "Πα" to 0.1875f,
            "Βου" to 0.3438f,
            "Γα" to 0.4688f,
            "Δι" to 0.6563f,
            "Κε" to 0.8438f,
            "Ζω" to 1.0f
        )
        val softChromaticProfile = mapOf(
            "Νη" to 0.0f,
            "Πα" to 0.0769f,
            "Βου" to 0.1923f,
            "Γα" to 0.5769f,
            "Δι" to 0.6538f,
            "Κε" to 0.8846f,
            "Ζω" to 1.0f
        )
        val hardChromaticProfile = mapOf(
            "Νη" to 0.0f,
            "Πα" to 0.1250f,
            "Βου" to 0.3438f,
            "Γα" to 0.4688f,
            "Δι" to 0.6563f,
            "Κε" to 0.7813f,
            "Ζω" to 1.0f
        )
        val enharmonicProfile = mapOf(
            "Νη" to 0.0f,
            "Πα" to 0.2000f,
            "Βου" to 0.4000f,
            "Γα" to 0.5000f,
            "Δι" to 0.7000f,
            "Κε" to 0.9000f,
            "Ζω" to 1.0f
        )

        listOf("first", "fourth", "plagal_first", "plagal_fourth").forEach { modeId ->
            assertProfile(modeRulesContent, modeId, diatonicProfile)
        }
        assertProfile(modeRulesContent, "second", softChromaticProfile)
        assertProfile(modeRulesContent, "plagal_second", hardChromaticProfile)
        listOf("third", "varys").forEach { modeId ->
            assertProfile(modeRulesContent, modeId, enharmonicProfile)
        }
    }

    private fun readModeRulesContent(): String {
        val modeRulesPath = listOf(
            Paths.get("app/src/main/assets/byzantine_mode_rules_v1.json"),
            Paths.get("src/main/assets/byzantine_mode_rules_v1.json")
        ).firstOrNull { Files.exists(it) }
            ?: error("Missing byzantine_mode_rules_v1.json in known locations")
        return String(Files.readAllBytes(modeRulesPath), StandardCharsets.UTF_8)
    }

    private fun assertProfile(content: String, modeId: String, expected: Map<String, Float>) {
        val actual = extractNoteHeights(content, modeId)
        expected.forEach { (phthong, expectedHeight) ->
            val actualHeight = actual[phthong] ?: error("Missing $phthong for $modeId")
            assertEquals("$modeId/$phthong", expectedHeight, actualHeight, 0.0002f)
        }
    }

    private fun extractNoteHeights(content: String, modeId: String): Map<String, Float> {
        val modePattern = Regex(
            "\\{\\s*\"id\"\\s*:\\s*\"$modeId\"\\s*,\\s*\"noteHeights\"\\s*:\\s*\\{([^}]*)\\}\\s*\\}"
        )
        val body = modePattern.find(content)?.groupValues?.get(1)
            ?: error("Missing noteHeights for $modeId")
        val entryPattern = Regex("\"([^\"]+)\"\\s*:\\s*([0-9.]+)")
        return entryPattern.findAll(body).associate { match ->
            match.groupValues[1] to match.groupValues[2].toFloat()
        }
    }
}
