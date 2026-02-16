package com.johnchourp.learnbyzantinemusic.analysis

data class KeyboardTokenEntry(
    val order: Int,
    val token: String,
    val keycode: Int,
    val layer: Int
)

class KeyboardIniParser {
    fun parse(content: String): List<KeyboardTokenEntry> {
        val rows = content
            .lineSequence()
            .map { it.replace("\r", "") }
            .toList()

        var currentKeycode: Int? = null
        var sequence = 0
        val seen = mutableSetOf<String>()
        val result = mutableListOf<KeyboardTokenEntry>()

        for (row in rows) {
            val sectionMatch = SECTION_REGEX.matchEntire(row.trim())
            if (sectionMatch != null) {
                currentKeycode = sectionMatch.groupValues[1].toIntOrNull()
                continue
            }
            if (row.startsWith("[")) {
                currentKeycode = null
                continue
            }
            if (currentKeycode == null) {
                continue
            }

            val layerMatch = LAYER_REGEX.matchEntire(row.trim()) ?: continue
            val layer = layerMatch.groupValues[1].toIntOrNull() ?: continue
            val token = layerMatch.groupValues[2]
            if (!seen.add(token)) {
                continue
            }
            sequence += 1
            result.add(
                KeyboardTokenEntry(
                    order = sequence,
                    token = token,
                    keycode = currentKeycode,
                    layer = layer
                )
            )
        }
        return result
    }

    private companion object {
        val SECTION_REGEX = Regex("^\\[(\\d+)]$")
        val LAYER_REGEX = Regex("^([1-5])=([A-Za-z0-9_]+)$")
    }
}
