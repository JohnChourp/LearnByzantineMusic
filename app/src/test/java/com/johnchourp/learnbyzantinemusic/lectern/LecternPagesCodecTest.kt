package com.johnchourp.learnbyzantinemusic.lectern

import com.johnchourp.learnbyzantinemusic.lectern.LecternPagesCodec.Decoded
import com.johnchourp.learnbyzantinemusic.music.BaseShift
import com.johnchourp.learnbyzantinemusic.music.Mode
import com.johnchourp.learnbyzantinemusic.music.Phthong
import com.johnchourp.learnbyzantinemusic.music.PhthongName
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The stored form of one PDF's page → ήχος map (ClickUp `869f5x2e7`): it reads back what it wrote, it
 * is one canonical text, a newer version is left alone, and anything damaged is refused whole.
 */
class LecternPagesCodecTest {

    private val map = listOf(
        PageAssignment(0, Mode.FIRST, 0),
        PageAssignment(4, Mode.PLAGAL_FIRST, -3, Phthong(PhthongName.PA)),
        PageAssignment(12, Mode.PLAGAL_SECOND, BaseShift.MAX_MORIA, Phthong(PhthongName.ZO)),
        PageAssignment(LecternPagesCodec.MAX_PAGE_INDEX, Mode.VARYS, BaseShift.MIN_MORIA),
    )

    private fun decoded(stored: String?): List<PageAssignment> {
        val result = LecternPagesCodec.decode(stored)
        return (result as? Decoded.Assignments)?.assignments ?: throw AssertionError("expected assignments, got $result")
    }

    @Test
    fun `what is written reads back exactly, every ήχος included`() {
        assertEquals(map, decoded(LecternPagesCodec.encode(map)))
        Mode.entries.forEach { mode ->
            val one = listOf(PageAssignment(3, mode, 2))
            assertEquals(mode.key, one, decoded(LecternPagesCodec.encode(one)))
        }
    }

    @Test
    fun `the text is canonical - one order, no spaces, pages ascending - whatever order it was given in`() {
        val text = LecternPagesCodec.encode(map.reversed())
        assertEquals(LecternPagesCodec.encode(map), text)
        assertEquals(
            """{"schemaVersion":1,"assignments":[{"pageIndex":0,"mode":"first","shiftMoria":0},""" +
                """{"pageIndex":4,"mode":"plagal_first","shiftMoria":-3,"ison":{"phthong":"PA","octave":0}}]}""",
            LecternPagesCodec.encode(map.take(2)),
        )
        // And it is JSON any parser reads.
        assertEquals(1, JSONObject(text).getInt("schemaVersion"))
    }

    @Test
    fun `nothing stored is an empty map, and the text carries no URI or file name`() {
        assertEquals(emptyList<PageAssignment>(), decoded(null))
        val text = LecternPagesCodec.encode(map)
        assertFalse(text, "://" in text)
        assertFalse(text, ".pdf" in text)
    }

    @Test
    fun `a newer version is left alone, not read as damage`() {
        val newer = LecternPagesCodec.encode(map).replace("\"schemaVersion\":1", "\"schemaVersion\":2")
        assertEquals(Decoded.Newer, LecternPagesCodec.decode(newer))
        assertNull("nothing of a newer map travels", LecternPagesCodec.normalized(newer))
    }

    @Test
    fun `a value without a usable version is not one of ours`() {
        val good = JSONObject(LecternPagesCodec.encode(map))
        listOf<Any?>(0, -1, "1", 1.5, null).forEach { version ->
            val broken = JSONObject(good.toString()).apply { if (version == null) remove("schemaVersion") else put("schemaVersion", version) }
            assertEquals("schemaVersion $version", Decoded.Unreadable, LecternPagesCodec.decode(broken.toString()))
        }
        listOf("", "not json", "[]", "{}", """{"schemaVersion":1}""", """{"schemaVersion":1,"assignments":{}}""").forEach { text ->
            assertEquals(text, Decoded.Unreadable, LecternPagesCodec.decode(text))
        }
    }

    @Test
    fun `one damaged entry refuses the whole map rather than half of it`() {
        val damaged = listOf(
            """{"pageIndex":-1,"mode":"first","shiftMoria":0}""",
            """{"pageIndex":${LecternPagesCodec.MAX_PAGE_INDEX + 1},"mode":"first","shiftMoria":0}""",
            """{"pageIndex":"3","mode":"first","shiftMoria":0}""",
            """{"pageIndex":3,"mode":"ninth","shiftMoria":0}""",
            """{"pageIndex":3,"mode":"first"}""",
            """{"pageIndex":3,"mode":"first","shiftMoria":1.5}""",
            """{"pageIndex":3,"mode":"first","shiftMoria":0,"ison":"PA"}""",
            """{"pageIndex":3,"mode":"first","shiftMoria":0,"ison":{"phthong":"XX","octave":0}}""",
            """{"pageIndex":3,"mode":"first","shiftMoria":0,"ison":{"phthong":"PA"}}""",
            "7",
        )
        damaged.forEach { entry ->
            val text = """{"schemaVersion":1,"assignments":[{"pageIndex":0,"mode":"first","shiftMoria":0},$entry]}"""
            assertEquals(entry, Decoded.Unreadable, LecternPagesCodec.decode(text))
        }
        val twice = """{"schemaVersion":1,"assignments":[{"pageIndex":2,"mode":"first","shiftMoria":0},{"pageIndex":2,"mode":"second","shiftMoria":0}]}"""
        assertEquals("a page set twice", Decoded.Unreadable, LecternPagesCodec.decode(twice))
    }

    @Test
    fun `reading gives the app's own view - the shift in range, the base as no choice, an unknown φθόγγος as the base`() {
        val stored = """{"schemaVersion":1,"assignments":[""" +
            """{"pageIndex":5,"mode":"second","shiftMoria":400,"ison":{"phthong":"DI","octave":0}},""" +
            """{"pageIndex":1,"mode":"plagal_first","shiftMoria":-400,"ison":{"phthong":"PA","octave":7}},""" +
            """{"pageIndex":3,"mode":"third","shiftMoria":0,"ison":null}]}"""
        assertEquals(
            listOf(
                // Πα΄΄΄΄΄΄΄ is on no ladder: the base.
                PageAssignment(1, Mode.PLAGAL_FIRST, BaseShift.MIN_MORIA, null),
                PageAssignment(3, Mode.THIRD, 0, null),
                // Δι is Β΄'s base: «no choice».
                PageAssignment(5, Mode.SECOND, BaseShift.MAX_MORIA, null),
            ),
            decoded(stored),
        )
    }

    @Test
    fun `normalized is the canonical text of what the app reads, and empty maps carry nothing`() {
        val canonical = LecternPagesCodec.encode(map)
        assertEquals(canonical, LecternPagesCodec.normalized(canonical))
        assertEquals(canonical, LecternPagesCodec.normalized(JSONObject(canonical).toString(2)))
        assertNull(LecternPagesCodec.normalized(LecternPagesCodec.encode(emptyList())))
        assertNull(LecternPagesCodec.normalized("not json"))
    }
}
