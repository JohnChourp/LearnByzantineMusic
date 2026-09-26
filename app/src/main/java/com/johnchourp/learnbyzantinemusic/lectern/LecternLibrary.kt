package com.johnchourp.learnbyzantinemusic.lectern

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** One PDF in the lectern's library: this phone's grant to read it, and where the reader left it. */
data class LibraryEntry(
    val uri: String,
    val title: String,
    val lastPageIndex: Int,
    val openedAtEpochMs: Long,
)

/**
 * The lectern's library (ClickUp `869f5x2e7`): the user's **own** PDFs — the app ships none — newest
 * opened first, each one a persisted READ grant on this phone.
 *
 * **Grants are bookkeeping, done here.** [add] takes the grant first and keeps the entry only if the
 * provider gave it; [remove] drops the entry and releases its grant, so a removed PDF leaves nothing
 * behind. Android keeps at most 128 persisted grants per app (512 from Android 11) and past that it
 * silently drops the **oldest** — which could be the recordings or the notes folder. So the library
 * stops at [MAX_ENTRIES], well below, and says so instead of adding a 101st.
 *
 * **Stays on this phone.** The URIs mean nothing elsewhere, so the list is neither in the «Δεδομένα
 * μάθησης» file nor in the Android backup (`AppPrefs.Store.LECTERN_LIBRARY`). The page → ήχος maps are
 * separate and keyed by the file ([LecternFileKey]): removing a PDF from here keeps its map, and adding
 * it again — or on another phone — finds it.
 *
 * **The file itself is never touched**, only the grant to read it.
 *
 * Stored as `{"schemaVersion":1,"entries":[{"uri":…,"title":…,"lastPageIndex":…,"openedAt":…}]}`.
 * Reading is lenient, unlike the page maps: this list is this phone's own convenience, so one damaged
 * entry costs that entry, not the library.
 */
data class LecternLibrary(val entries: List<LibraryEntry>) {

    /** The platform's persisted grants, as far as the library needs them. */
    interface Grants {
        /** Takes a persisted READ grant on [uri]; false when the provider does not allow one. */
        fun take(uri: String): Boolean

        /** Releases it. Harmless when there is none. */
        fun release(uri: String)
    }

    enum class Added {
        /** New in the library, with its grant. */
        ADDED,

        /** Already there: moved to the top, no second grant. */
        ALREADY_THERE,

        /** The library is full: no grant taken, nothing added. */
        FULL,

        /** The provider gives no lasting grant: the PDF opens this time only, and is not kept. */
        NOT_KEPT,
    }

    data class AddResult(val library: LecternLibrary, val outcome: Added)

    fun entry(uri: String): LibraryEntry? = entries.firstOrNull { it.uri == uri }

    fun add(uri: String, title: String, nowEpochMs: Long, grants: Grants): AddResult {
        if (entry(uri) != null) return AddResult(opened(uri, nowEpochMs), Added.ALREADY_THERE)
        if (entries.size >= MAX_ENTRIES) return AddResult(this, Added.FULL)
        if (!grants.take(uri)) return AddResult(this, Added.NOT_KEPT)
        return AddResult(LecternLibrary(listOf(LibraryEntry(uri, title, 0, nowEpochMs)) + entries), Added.ADDED)
    }

    /** Without [uri], whose grant is released; unchanged — and nothing released — when it is not here. */
    fun remove(uri: String, grants: Grants): LecternLibrary {
        if (entry(uri) == null) return this
        grants.release(uri)
        return LecternLibrary(entries.filterNot { it.uri == uri })
    }

    /** [uri] opened now: first in the list. */
    fun opened(uri: String, nowEpochMs: Long): LecternLibrary {
        val found = entry(uri) ?: return this
        return LecternLibrary(listOf(found.copy(openedAtEpochMs = nowEpochMs)) + entries.filterNot { it.uri == uri })
    }

    /** The page the reader reopens [uri] on. */
    fun withLastPage(uri: String, pageIndex: Int): LecternLibrary =
        LecternLibrary(entries.map { if (it.uri == uri) it.copy(lastPageIndex = pageIndex.coerceAtLeast(0)) else it })

    fun encode(): String {
        val items = JSONArray()
        entries.forEach { entry ->
            items.put(
                JSONObject()
                    .put("uri", entry.uri)
                    .put("title", entry.title)
                    .put("lastPageIndex", entry.lastPageIndex)
                    .put("openedAt", entry.openedAtEpochMs)
            )
        }
        return JSONObject().put("schemaVersion", SCHEMA_VERSION).put("entries", items).toString()
    }

    companion object {
        const val SCHEMA_VERSION = 1

        /** Far below the platform's 128 persisted grants, next to the recordings' and the notes' folders. */
        const val MAX_ENTRIES = 100

        val EMPTY = LecternLibrary(emptyList())

        /** The stored library; what cannot be read is left out, never guessed. */
        fun decode(stored: String?): LecternLibrary {
            if (stored == null) return EMPTY
            val root = try {
                JSONObject(stored)
            } catch (_: JSONException) {
                return EMPTY
            }
            if (root.opt("schemaVersion") != SCHEMA_VERSION) return EMPTY
            val items = root.opt("entries") as? JSONArray ?: return EMPTY
            val entries = (0 until items.length())
                .mapNotNull { index -> (items.opt(index) as? JSONObject)?.let(::entryOf) }
                .distinctBy { it.uri }
                .take(MAX_ENTRIES)
            return LecternLibrary(entries)
        }

        private fun entryOf(item: JSONObject): LibraryEntry? {
            val uri = (item.opt("uri") as? String)?.takeIf { it.isNotBlank() } ?: return null
            val title = item.opt("title") as? String ?: return null
            val lastPage = (item.opt("lastPageIndex") as? Int)?.takeIf { it >= 0 } ?: 0
            val openedAt = (item.opt("openedAt") as? Number)?.toLong() ?: 0L
            return LibraryEntry(uri, title, lastPage, openedAt)
        }
    }
}
