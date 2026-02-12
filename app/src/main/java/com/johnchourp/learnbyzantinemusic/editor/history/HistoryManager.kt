package com.johnchourp.learnbyzantinemusic.editor.history

class HistoryManager<T>(private val maxSize: Int = 120) {
    private val past = ArrayDeque<T>()
    private val future = ArrayDeque<T>()

    fun reset() {
        past.clear()
        future.clear()
    }

    fun push(snapshot: T) {
        if (past.size >= maxSize) {
            past.removeFirst()
        }
        past.addLast(snapshot)
        future.clear()
    }

    fun undo(current: T): T? {
        val previous = past.removeLastOrNull() ?: return null
        future.addLast(current)
        return previous
    }

    fun redo(current: T): T? {
        val next = future.removeLastOrNull() ?: return null
        if (past.size >= maxSize) {
            past.removeFirst()
        }
        past.addLast(current)
        return next
    }
}
