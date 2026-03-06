package com.johnchourp.learnbyzantinemusic.notes

import org.junit.Assert.assertEquals
import org.junit.Test

class NotesPendingSyncTrackerTest {
    @Test
    fun `enqueue adds unique pending file name`() {
        val initial = listOf("a.json", "b.json")
        val updated = NotesPendingSyncTracker.enqueue(initial, "b.json")
        val withNew = NotesPendingSyncTracker.enqueue(updated, "c.json")

        assertEquals(listOf("a.json", "b.json", "c.json"), withNew)
    }

    @Test
    fun `dequeue removes synced names only`() {
        val initial = listOf("a.json", "b.json", "c.json")
        val remaining = NotesPendingSyncTracker.dequeue(initial, setOf("a.json", "c.json"))

        assertEquals(listOf("b.json"), remaining)
    }
}
