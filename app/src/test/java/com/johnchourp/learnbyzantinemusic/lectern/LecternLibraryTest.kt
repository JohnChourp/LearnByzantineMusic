package com.johnchourp.learnbyzantinemusic.lectern

import com.johnchourp.learnbyzantinemusic.lectern.LecternLibrary.Added
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The lectern's library and its grants (ClickUp `869f5x2e7`): a grant is taken only for a PDF that is
 * kept, released when it is removed, and the library stops far below the platform's grant limit.
 */
class LecternLibraryTest {

    /** The platform's grants as a ledger: what is held, and every call made. */
    private class Ledger(private val refuse: Set<String> = emptySet()) : LecternLibrary.Grants {
        val held = mutableSetOf<String>()
        val taken = mutableListOf<String>()
        val released = mutableListOf<String>()

        override fun take(uri: String): Boolean {
            taken += uri
            if (uri in refuse) return false
            held += uri
            return true
        }

        override fun release(uri: String) {
            released += uri
            held -= uri
        }
    }

    private fun uri(n: Int) = "content://com.android.externalstorage.documents/document/primary%3AMusic%2F$n.pdf"

    @Test
    fun `adding takes one grant and puts the PDF first`() {
        val grants = Ledger()
        val one = LecternLibrary.EMPTY.add(uri(1), "Α", 100, grants)
        val two = one.library.add(uri(2), "Β", 200, grants)

        assertEquals(Added.ADDED, one.outcome)
        assertEquals(Added.ADDED, two.outcome)
        assertEquals(listOf(uri(2), uri(1)), two.library.entries.map { it.uri })
        assertEquals(listOf(uri(1), uri(2)), grants.taken)
        assertEquals(setOf(uri(1), uri(2)), grants.held)
        assertEquals(LibraryEntry(uri(1), "Α", 0, 100), two.library.entry(uri(1)))
    }

    @Test
    fun `adding a PDF that is already there takes no second grant and brings it to the top`() {
        val grants = Ledger()
        val library = LecternLibrary.EMPTY.add(uri(1), "Α", 100, grants).library.add(uri(2), "Β", 200, grants).library

        val again = library.add(uri(1), "Α", 300, grants)

        assertEquals(Added.ALREADY_THERE, again.outcome)
        assertEquals(listOf(uri(1), uri(2)), again.library.entries.map { it.uri })
        assertEquals(300L, again.library.entry(uri(1))?.openedAtEpochMs)
        assertEquals("one grant per PDF", listOf(uri(1), uri(2)), grants.taken)
    }

    @Test
    fun `a provider that gives no lasting grant leaves the library as it was`() {
        val grants = Ledger(refuse = setOf(uri(9)))
        val result = LecternLibrary.EMPTY.add(uri(9), "Χ", 100, grants)

        assertEquals(Added.NOT_KEPT, result.outcome)
        assertEquals(LecternLibrary.EMPTY, result.library)
        assertEquals(emptySet<String>(), grants.held)
    }

    @Test
    fun `a full library takes no grant and adds nothing`() {
        val grants = Ledger()
        var library = LecternLibrary.EMPTY
        (1..LecternLibrary.MAX_ENTRIES).forEach { library = library.add(uri(it), "$it", it.toLong(), grants).library }
        assertEquals(LecternLibrary.MAX_ENTRIES, library.entries.size)

        val beyond = library.add(uri(1000), "1000", 5000, grants)

        assertEquals(Added.FULL, beyond.outcome)
        assertEquals(library, beyond.library)
        assertEquals(LecternLibrary.MAX_ENTRIES, grants.held.size)
        assertTrue("uri(1000) must not be granted", uri(1000) !in grants.taken)
    }

    @Test
    fun `the cap leaves room under the platform's 128 grants for the recordings and notes folders`() {
        assertTrue(LecternLibrary.MAX_ENTRIES + 2 < 128)
    }

    @Test
    fun `removing releases exactly that PDF's grant, and an unknown PDF releases nothing`() {
        val grants = Ledger()
        val library = listOf(1, 2, 3).fold(LecternLibrary.EMPTY) { lib, n -> lib.add(uri(n), "$n", n.toLong(), grants).library }

        val removed = library.remove(uri(2), grants)

        assertEquals(listOf(uri(3), uri(1)), removed.entries.map { it.uri })
        assertEquals(listOf(uri(2)), grants.released)
        assertEquals(setOf(uri(1), uri(3)), grants.held)

        val unchanged = removed.remove(uri(2), grants)
        assertEquals(removed, unchanged)
        assertEquals("nothing released twice", listOf(uri(2)), grants.released)
    }

    @Test
    fun `every grant the library took is released once everything is removed`() {
        val grants = Ledger()
        var library = (1..7).fold(LecternLibrary.EMPTY) { lib, n -> lib.add(uri(n), "$n", n.toLong(), grants).library }
        library = library.add(uri(3), "3", 50, grants).library
        library.entries.map { it.uri }.forEach { library = library.remove(it, grants) }

        assertEquals(emptyList<LibraryEntry>(), library.entries)
        assertEquals("no grant left behind", emptySet<String>(), grants.held)
        assertEquals(grants.taken.sorted(), grants.released.sorted())
    }

    @Test
    fun `the page the reader left is kept per PDF`() {
        val grants = Ledger()
        val library = LecternLibrary.EMPTY.add(uri(1), "Α", 100, grants).library.add(uri(2), "Β", 200, grants).library
        val moved = library.withLastPage(uri(1), 41).withLastPage(uri(2), -3)
        assertEquals(41, moved.entry(uri(1))?.lastPageIndex)
        assertEquals(0, moved.entry(uri(2))?.lastPageIndex)
        assertEquals(moved, moved.withLastPage(uri(99), 5))
    }

    @Test
    fun `the stored library reads back exactly`() {
        val grants = Ledger()
        val library = LecternLibrary.EMPTY
            .add(uri(1), "Ανθολογία «Ψάλατε»", 1_758_800_000_000L, grants).library
            .add(uri(2), "Doxastarion / Δοξαστάριο", 1_758_900_000_000L, grants).library
            .withLastPage(uri(1), 87)
        assertEquals(library, LecternLibrary.decode(library.encode()))
    }

    @Test
    fun `a damaged entry costs that entry, and a damaged library an empty one`() {
        val good = JSONObject().put("uri", uri(1)).put("title", "Α").put("lastPageIndex", 3).put("openedAt", 7L)
        val entries = JSONArray()
            .put(good)
            .put(JSONObject().put("title", "no uri"))
            .put(JSONObject().put("uri", uri(2)))
            .put("not an entry")
            .put(JSONObject(good.toString()))
        val stored = JSONObject().put("schemaVersion", 1).put("entries", entries).toString()

        assertEquals(listOf(LibraryEntry(uri(1), "Α", 3, 7L)), LecternLibrary.decode(stored).entries)
        listOf(null, "", "not json", "{}", """{"schemaVersion":2,"entries":[]}""").forEach {
            assertEquals("$it", LecternLibrary.EMPTY, LecternLibrary.decode(it))
        }
        assertNull(LecternLibrary.EMPTY.entry(uri(1)))
    }
}
