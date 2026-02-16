package com.johnchourp.learnbyzantinemusic.analysis

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
}
