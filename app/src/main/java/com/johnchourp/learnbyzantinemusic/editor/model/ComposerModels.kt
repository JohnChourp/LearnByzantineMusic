package com.johnchourp.learnbyzantinemusic.editor.model

data class SymbolPlacement(
    val id: String,
    val symbolKey: String,
    val symbolText: String,
    val symbolFontId: String = "mk_byzantine",
    val charIndex: Int,
    val dxDp: Float,
    val dyDp: Float,
    val scale: Float
)

data class CompositionLine(
    val id: String,
    val lyrics: String,
    val symbols: List<SymbolPlacement>
)

data class CompositionProject(
    val version: Int,
    val title: String,
    val updatedAt: Long,
    val lines: List<CompositionLine>
)

data class SymbolDefinition(
    val key: String,
    val label: String,
    val text: String,
    val fontId: String = "mk_byzantine",
    val category: String,
    val defaultDyDp: Float
)

fun CompositionProject.deepCopy(): CompositionProject {
    return copy(
        lines = lines.map { line ->
            line.copy(symbols = line.symbols.map { symbol -> symbol.copy() })
        }
    )
}
