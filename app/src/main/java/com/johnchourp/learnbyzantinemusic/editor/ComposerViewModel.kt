package com.johnchourp.learnbyzantinemusic.editor

import com.johnchourp.learnbyzantinemusic.editor.history.HistoryManager
import com.johnchourp.learnbyzantinemusic.editor.model.CompositionLine
import com.johnchourp.learnbyzantinemusic.editor.model.CompositionProject
import com.johnchourp.learnbyzantinemusic.editor.model.SymbolDefinition
import com.johnchourp.learnbyzantinemusic.editor.model.SymbolPlacement
import com.johnchourp.learnbyzantinemusic.editor.model.deepCopy
import java.util.UUID

class ComposerViewModel {
    private val history = HistoryManager<CompositionProject>()

    var project: CompositionProject = createNewProject()
        private set

    var selectedLineId: String? = project.lines.firstOrNull()?.id
        private set

    var selectedSymbolId: String? = null
        private set

    var selectedCursorIndex: Int = 0
        private set

    fun resetProject() {
        history.push(project.deepCopy())
        project = createNewProject()
        selectedLineId = project.lines.firstOrNull()?.id
        selectedSymbolId = null
        selectedCursorIndex = 0
    }

    fun setTitle(title: String) {
        project = project.copy(title = title, updatedAt = System.currentTimeMillis())
    }

    fun setSelectedLine(lineId: String, cursorIndex: Int) {
        selectedLineId = lineId
        selectedCursorIndex = cursorIndex
        val hasSelectedSymbol = project.lines.any { line ->
            line.id == lineId && line.symbols.any { it.id == selectedSymbolId }
        }
        if (!hasSelectedSymbol) {
            selectedSymbolId = null
        }
    }

    fun selectSymbol(lineId: String, symbolId: String?) {
        selectedLineId = lineId
        selectedSymbolId = symbolId
    }

    fun addEmptyLine() {
        history.push(project.deepCopy())
        val line = CompositionLine(id = newId(), lyrics = "", symbols = emptyList())
        project = project.copy(
            updatedAt = System.currentTimeMillis(),
            lines = project.lines + line
        )
        selectedLineId = line.id
        selectedSymbolId = null
        selectedCursorIndex = 0
    }

    fun updateLyrics(lineId: String, lyrics: String, addHistory: Boolean) {
        val line = project.lines.firstOrNull { it.id == lineId } ?: return
        if (line.lyrics == lyrics) {
            return
        }
        if (addHistory) {
            history.push(project.deepCopy())
        }
        project = project.copy(
            updatedAt = System.currentTimeMillis(),
            lines = project.lines.map { currentLine ->
                if (currentLine.id == lineId) {
                    currentLine.copy(lyrics = lyrics)
                } else {
                    currentLine
                }
            }
        )
    }

    fun insertSymbol(symbol: SymbolDefinition) {
        val lineId = selectedLineId ?: project.lines.firstOrNull()?.id ?: return
        history.push(project.deepCopy())

        project = project.copy(
            updatedAt = System.currentTimeMillis(),
            lines = project.lines.map { currentLine ->
                if (currentLine.id != lineId) {
                    return@map currentLine
                }

                val anchor = selectedCursorIndex.coerceIn(0, currentLine.lyrics.length)
                val placement = SymbolPlacement(
                    id = newId(),
                    symbolKey = symbol.key,
                    symbolText = symbol.text,
                    symbolFontId = symbol.fontId,
                    charIndex = anchor,
                    dxDp = 0f,
                    dyDp = symbol.defaultDyDp,
                    scale = 1f
                )
                currentLine.copy(symbols = currentLine.symbols + placement)
            }
        )

        val created = project.lines.firstOrNull { it.id == lineId }?.symbols?.lastOrNull()?.id
        selectedSymbolId = created
    }

    fun moveSymbol(lineId: String, symbolId: String, dxDp: Float, dyDp: Float) {
        history.push(project.deepCopy())
        project = project.copy(
            updatedAt = System.currentTimeMillis(),
            lines = project.lines.map { line ->
                if (line.id != lineId) {
                    return@map line
                }
                line.copy(
                    symbols = line.symbols.map { symbol ->
                        if (symbol.id == symbolId) {
                            symbol.copy(dxDp = dxDp, dyDp = dyDp)
                        } else {
                            symbol
                        }
                    }
                )
            }
        )
        selectedLineId = lineId
        selectedSymbolId = symbolId
    }

    fun nudgeSelectedSymbol(dxDp: Float, dyDp: Float) {
        val lineId = selectedLineId ?: return
        val symbolId = selectedSymbolId ?: return
        val current = project.lines.firstOrNull { it.id == lineId }
            ?.symbols
            ?.firstOrNull { it.id == symbolId }
            ?: return
        moveSymbol(lineId, symbolId, current.dxDp + dxDp, current.dyDp + dyDp)
    }

    fun removeSelectedSymbol() {
        val lineId = selectedLineId ?: return
        val symbolId = selectedSymbolId ?: return
        history.push(project.deepCopy())
        project = project.copy(
            updatedAt = System.currentTimeMillis(),
            lines = project.lines.map { line ->
                if (line.id == lineId) {
                    line.copy(symbols = line.symbols.filterNot { it.id == symbolId })
                } else {
                    line
                }
            }
        )
        selectedSymbolId = null
    }

    fun loadProject(project: CompositionProject) {
        history.reset()
        this.project = project.deepCopy()
        selectedLineId = this.project.lines.firstOrNull()?.id
        selectedSymbolId = null
        selectedCursorIndex = 0
    }

    fun undo(): Boolean {
        val previous = history.undo(project.deepCopy()) ?: return false
        project = previous.deepCopy().copy(updatedAt = System.currentTimeMillis())
        selectedSymbolId = null
        return true
    }

    fun redo(): Boolean {
        val next = history.redo(project.deepCopy()) ?: return false
        project = next.deepCopy().copy(updatedAt = System.currentTimeMillis())
        selectedSymbolId = null
        return true
    }

    private fun createNewProject(): CompositionProject {
        val lines = List(1) {
            CompositionLine(id = newId(), lyrics = "", symbols = emptyList())
        }
        return CompositionProject(
            version = 1,
            title = "Σύνθεση",
            updatedAt = System.currentTimeMillis(),
            lines = lines
        )
    }

    private fun newId(): String = UUID.randomUUID().toString()
}
